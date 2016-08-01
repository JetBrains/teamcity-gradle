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
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerService extends BuildServiceAdapter
{
  private final String exePath;
  private final String wrapperName;
  private List<ProcessListener> myListenerList;

  public GradleRunnerService(final String exePath, final String wrapperName) {
    this.exePath = exePath;
    this.wrapperName = wrapperName;
  }

  @Override
  @NotNull public ProgramCommandLine makeProgramCommandLine() throws RunBuildException
  {
    boolean useWrapper = ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(),
                                                                    GradleRunnerConstants.GRADLE_WRAPPER_FLAG);


    List<String> params = new LinkedList<String>();
    Map<String,String> env = new HashMap<String,String>(getEnvironmentVariables());
    File gradleExe;
    String exePath;

    if (!useWrapper) {
      File gradleHome = getGradleHome();
      gradleExe = new File(gradleHome, this.exePath);
      exePath = gradleExe.getAbsolutePath();

      if(!gradleHome.exists())
        throw new RunBuildException("Gradle home path ("+gradleHome+") is invalid.");
      if(!gradleExe.exists())
        throw new RunBuildException("Gradle home path ("+gradleHome+") does not contain a Gradle installation.  Cannot find "+
                                    this.exePath +".");
      env.put("GRADLE_HOME", gradleHome.getAbsolutePath());
    } else {

      String relativeGradleWPath = ConfigurationParamsUtil.getGradleWPath(getRunnerParameters());

      gradleExe = new File(getWorkingDirectory(), relativeGradleWPath + File.separator + wrapperName);
      exePath = gradleExe.getAbsolutePath();
      if (!gradleExe.exists())
        throw new RunBuildException("Gradle wrapper script " + wrapperName + " can not be found in " +
                                    gradleExe.getAbsolutePath() + "\n" +
                                    "Please, provide path to wrapper script in build configuration settings.");
    }

    if (SystemInfo.isUnix) {
      exePath = "bash";
      params.add(gradleExe.getAbsolutePath());
    }

    params.addAll(getParams());

    env.put("GRADLE_EXIT_CONSOLE", "true");
    env.put(JavaRunnerConstants.JAVA_HOME, getJavaHome());
    env.put(GradleRunnerConstants.ENV_GRADLE_OPTS, appendTmpDir(buildGradleOpts(), getBuildTempDirectory()));
    env.put(GradleRunnerConstants.ENV_TEAMCITY_BUILD_INIT_PATH, buildInitScriptClassPath());
    env.put(GradleRunnerConstants.ENV_INCREMENTAL_PARAM, getIncrementalMode());

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
    final String envGradleOpts = getEnvironmentVariables().get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    final String runnerGradleOpts = getRunnerParameters().get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    final String runnerJavaArguments = getJavaArgs();

    if (!StringUtil.isEmpty(runnerJavaArguments)) {
      return runnerJavaArguments;
    } else if (!StringUtil.isEmpty(runnerGradleOpts)) {
      return runnerGradleOpts;
    } else {
      return StringUtil.emptyIfNull(envGradleOpts);
    }
  }

  private String getIncrementalMode() {
    boolean incrementalOptionEnabled = Boolean.valueOf(getRunnerParameters().get(GradleRunnerConstants.IS_INCREMENTAL));
    boolean internalFullBuildOverride = !IncrementalBuild.isEnabled();
    if (incrementalOptionEnabled) {
      if (internalFullBuildOverride) {
        return GradleRunnerConstants.ENV_INCREMENTAL_VALUE_SKIP;
      } else {
        return GradleRunnerConstants.ENV_INCREMENTAL_VALUE_PROCEED;
      }
    } else {
      return  Boolean.FALSE.toString();
    }
  }

  private String getJavaHome() throws RunBuildException {
    String javaHome = JavaRunnerUtil.findJavaHome(getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME),
                                                  getBuildParameters().getAllParameters(),
                                                  AgentRuntimeProperties.getCheckoutDir(getRunnerParameters()));
    if (javaHome == null) throw new RunBuildException("Unable to find Java home");
    javaHome = FileUtil.getCanonicalFile(new File(javaHome)).getPath();
    return javaHome;
  }

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

  private String getJavaArgs() {
    return ConfigurationParamsUtil.getJavaArgs(getRunnerParameters());
  }

  private List<String> getParams()
  {
    List<String> params = new ArrayList<String>();
    insertInitScript(params);

    params.addAll(ConfigurationParamsUtil.getGradleParams(getRunnerParameters()));

    params.add("-Dorg.gradle.daemon=false");

    if (ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(), GradleRunnerConstants.DEBUG))
      params.add("-d");

    if (ConfigurationParamsUtil.isParameterEnabled(getRunnerParameters(), GradleRunnerConstants.STACKTRACE))
      params.add("-s");

    if (getRunnerParameters().containsKey(GradleRunnerConstants.PATH_TO_BUILD_FILE)) {
      params.add("-b");
      params.add(getRunnerParameters().get(GradleRunnerConstants.PATH_TO_BUILD_FILE));
    }

    String gradleTasks = ConfigurationParamsUtil.getGradleTasks(getRunnerParameters());
    if (gradleTasks.length() > 0)
      params.addAll(Arrays.asList(gradleTasks.split(" ")));

    return params;
  }

  private void insertInitScript(List<String> params)
  {
    final String scriptPath = ConfigurationParamsUtil.getGradleInitScript(getRunnerParameters());
    File initScript;

    if (scriptPath.length() > 0) {
      initScript = new File(scriptPath);
    } else {
      File pluginsDirectory = getBuild().getAgentConfiguration().getAgentPluginsDirectory();
      File runnerPluginDir = new File(pluginsDirectory, GradleRunnerConstants.RUNNER_TYPE);
      initScript = new File(runnerPluginDir, GradleRunnerConstants.INIT_SCRIPT_SUFFIX);
    }

    params.add("--init-script");
    params.add(initScript.getAbsolutePath());
  }

  private String buildInitScriptClassPath() throws RunBuildException {
    try {
      final File serviceMessagesLib = new File(ClasspathUtil.getClasspathEntry(ServiceMessage.class));
      final File runtimeUtil = new File(ClasspathUtil.getClasspathEntry(ComparisonFailureUtil.class));
      final File gradleRunnerConstants = new File(ClasspathUtil.getClasspathEntry(GradleRunnerConstants.class));
      final File gradleMessageLib = new File(ClasspathUtil.getClasspathEntry(GradleBuildProblem.class));

      return serviceMessagesLib.getAbsolutePath() +
                       File.pathSeparator + runtimeUtil.getAbsolutePath() +
                       File.pathSeparator + gradleMessageLib.getAbsolutePath() +
                       File.pathSeparator + gradleRunnerConstants.getAbsolutePath();

    } catch (IOException e) {
      throw new RunBuildException("Failed to create init script classpath", e);
    }
  }

  @NotNull
  @Override
  public List<ProcessListener> getListeners() {
    if (null == myListenerList) {
      myListenerList = Collections.<ProcessListener>singletonList(new GradleLoggingListener(getLogger()));
    }
    return myListenerList;
  }

}

