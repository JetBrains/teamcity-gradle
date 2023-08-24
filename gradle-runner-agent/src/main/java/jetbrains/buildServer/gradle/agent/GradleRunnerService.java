/*
 * Copyright 2000-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.agent.IncrementalBuild;
import jetbrains.buildServer.agent.ToolCannotBeFoundException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeOptions;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.impl.Lazy;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerService extends BuildServiceAdapter
{
  private final String exePath;
  private final String wrapperName;
  private final Lazy<List<ProcessListener>> listeners;

  public GradleRunnerService(final String exePath, final String wrapperName) {
    this.exePath = exePath;
    this.wrapperName = wrapperName;
    listeners = new Lazy<List<ProcessListener>>() {
      @Override
      protected List<ProcessListener> createValue() {
        return Collections.singletonList(new GradleLoggingListener(getLogger()));
      }
    };
  }

  @Override
  @NotNull public ProgramCommandLine makeProgramCommandLine() throws RunBuildException {
    boolean useWrapper = ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(),
                                                                    GradleRunnerConstants.GRADLE_WRAPPER_FLAG);

    List<String> params = new ArrayList<>();
    Map<String,String> env = new HashMap<>(getEnvironmentVariables());
    File gradleExe;
    String exePath = "gradle";

    if (!useWrapper) {
      if (getRunnerContext().isVirtualContext()) {
        getLogger().message("Step is running in a virtual context, skip detecting GRADLE_HOME");
      } else {
        File gradleHome = getGradleHome();
        gradleExe = new File(gradleHome, this.exePath);
        exePath = gradleExe.getAbsolutePath();

        if(!gradleHome.exists())
          throw new RunBuildException("Gradle home path (" + gradleHome + ") is invalid.");
        if(!gradleExe.exists())
          throw new RunBuildException("Gradle home path ("+gradleHome+") does not contain a Gradle installation.  Cannot find "+
                                      this.exePath +".");
        env.put("GRADLE_HOME", gradleHome.getAbsolutePath());
      }
    } else {
      String relativeGradleWPath = ConfigurationParamsUtil.getGradleWPath(getRunnerParameters());

      gradleExe = new File(getWorkingDirectory(), relativeGradleWPath + File.separator + wrapperName);
      exePath = gradleExe.getAbsolutePath();
      if (!gradleExe.exists())
        throw new RunBuildException("Gradle wrapper script " + wrapperName + " can not be found at " +
                                    gradleExe.getAbsolutePath() + "\n" +
                                    "Please, provide path to wrapper script in build configuration settings.");
    }

    if (SystemInfo.isUnix) {
      params.add(exePath);
      exePath = "bash";
    }

    params.addAll(getParams());

    env.put("GRADLE_EXIT_CONSOLE", "true");

    if (!getRunnerContext().isVirtualContext()) {
      env.put(JavaRunnerConstants.JAVA_HOME, getJavaHome());
    }
    env.put(GradleRunnerConstants.ENV_GRADLE_OPTS, appendTmpDir(buildGradleOpts(), getBuildTempDirectory()));
    env.put(GradleRunnerConstants.ENV_INCREMENTAL_PARAM, getIncrementalMode());
    env.put(GradleRunnerConstants.ENV_SUPPORT_TEST_RETRY, getBuild().getBuildTypeOptionValue(BuildTypeOptions.BT_SUPPORT_TEST_RETRY).toString());
    env.put(GradleRunnerConstants.TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH, getBuildParameters().getSystemProperties().getOrDefault("teamcity.build.parallelTests.excludesFile", ""));

    return new SimpleProgramCommandLine(env, getWorkingDirectory().getPath(), exePath, params);
  }

  private String appendTmpDir(@NotNull final String s, @NotNull final File tempDir) {
    try {
      return s + " \"-Djava.io.tmpdir=" + tempDir.getCanonicalPath() +"\"";
    } catch (IOException e) {
      Loggers.AGENT.warnAndDebugDetails("Failed patch temp dir for Gradle runtime environment: " + e.toString(), e);
      return s;
    }
  }

  @NotNull
  private String buildGradleOpts() {
    final String runnerGradleOpts = getRunnerParameters().get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    final String runnerJavaArguments = getJavaArgs();

    if (StringUtil.isNotEmpty(runnerJavaArguments)) {
      return runnerJavaArguments;
    } else if (StringUtil.isNotEmpty(runnerGradleOpts)) {
      return runnerGradleOpts;
    } else {
      return getEnvironmentVariables().getOrDefault(GradleRunnerConstants.ENV_GRADLE_OPTS, StringUtil.EMPTY);
    }
  }

  @NotNull
  private String getIncrementalMode() {
    boolean incrementalOptionEnabled = Boolean.parseBoolean(getRunnerParameters().get(GradleRunnerConstants.IS_INCREMENTAL));
    if (!incrementalOptionEnabled) return Boolean.FALSE.toString();

    boolean internalFullBuildOverride = !IncrementalBuild.isEnabled();
    if (internalFullBuildOverride) return GradleRunnerConstants.ENV_INCREMENTAL_VALUE_SKIP;

    return GradleRunnerConstants.ENV_INCREMENTAL_VALUE_PROCEED;
  }

  @NotNull
  private String getJavaHome() throws RunBuildException {
    String javaHome = JavaRunnerUtil.findJavaHome(getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME),
                                                  getBuildParameters().getAllParameters(),
                                                  AgentRuntimeProperties.getCheckoutDir(getRunnerParameters()));
    if (javaHome == null) throw new RunBuildException("Unable to find Java home");
    return FileUtil.getCanonicalFile(new File(javaHome)).getPath();
  }

  @NotNull
  private File getGradleHome() throws RunBuildException {
    try {
      final String gradlePath = getRunnerContext().getToolPath(GradleToolProvider.GRADLE_TOOL);
      return new File(gradlePath);
    } catch (ToolCannotBeFoundException e) {
      RunBuildException ex = new RunBuildException(e.getMessage(), e, ErrorData.BUILD_RUNNER_ERROR_TYPE);
      ex.setLogStacktrace(false);
      throw ex;
    }
  }

  @NotNull
  private String getJavaArgs() {
    return ConfigurationParamsUtil.getJavaArgs(getRunnerParameters());
  }

  @NotNull
  private List<String> getParams() {
    final List<String> params = new ArrayList<>();

    params.addAll(getInitScriptParams());
    params.addAll(ConfigurationParamsUtil.getGradleParams(getRunnerParameters()));

    if (!hasDaemonParam())
      params.add("-Dorg.gradle.daemon=false");

    if (ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(), GradleRunnerConstants.DEBUG))
      params.add("-d");

    if (ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(), GradleRunnerConstants.STACKTRACE))
      params.add("-s");

    String path = getRunnerParameters().get(GradleRunnerConstants.PATH_TO_BUILD_FILE);
    if (StringUtil.isNotEmpty(path)) {
      params.add("-b");
      params.add(path);
    }

    String gradleTasks = ConfigurationParamsUtil.getGradleTasks(getRunnerParameters());
    if (gradleTasks.length() > 0) {
      final List<String> tasks = StringUtil.splitHonorQuotes(gradleTasks, ' ');
      for (String task: tasks) {
        params.add(task.trim());
      }
    }

    return params;
  }

  private boolean hasDaemonParam() {
    for (String param: ConfigurationParamsUtil.getGradleParams(getRunnerParameters())) {
      if (param.startsWith("-Dorg.gradle.daemon=")) {
        return true;
      }
      if ("--daemon".equals(param) || "--no-daemon".equals(param)) {
        return true;
      }
    }
    return false;
  }

  private List<String> getInitScriptParams() {
    final String scriptPath = ConfigurationParamsUtil.getGradleInitScript(getRunnerParameters());
    File initScript;

    if (!scriptPath.isEmpty()) {
      initScript = new File(scriptPath);
    } else {
      File pluginsDirectory = getBuild().getAgentConfiguration().getAgentPluginsDirectory();
      File runnerPluginDir = new File(pluginsDirectory, GradleRunnerConstants.RUNNER_TYPE);
      initScript = new File(runnerPluginDir, GradleRunnerConstants.INIT_SCRIPT_SUFFIX);
    }

    return Arrays.asList("--init-script", initScript.getAbsolutePath());
  }

  @NotNull
  @Override
  public List<ProcessListener> getListeners() {
    return listeners.getValue();
  }

}

