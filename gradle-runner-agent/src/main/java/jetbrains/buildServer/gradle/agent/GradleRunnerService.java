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
import java.util.stream.Collectors;
import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.agent.IncrementalBuild;
import jetbrains.buildServer.agent.ToolCannotBeFoundException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildPropertiesSplitter;
import jetbrains.buildServer.gradle.agent.propertySplit.SplitablePropertyFile;
import jetbrains.buildServer.gradle.runtime.TeamCityGradleLauncher;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeOptions;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.impl.Lazy;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.gradle.tooling.BuildLauncher;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;

public class GradleRunnerService extends BuildServiceAdapter
{
  private final String exePath;
  private final String wrapperName;
  private final Lazy<List<ProcessListener>> listeners;
  private final Map<SplitablePropertyFile, GradleBuildPropertiesSplitter> propertySplitters;

  public GradleRunnerService(final String exePath,
                             final String wrapperName,
                             final Map<SplitablePropertyFile, GradleBuildPropertiesSplitter> propertySplitters) {
    this.exePath = exePath;
    this.wrapperName = wrapperName;
    this.propertySplitters = propertySplitters;
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
    File gradleExe;
    File gradleHome = null;
    File gradleWrapperProperties = null;
    String exePath = "gradle";
    File workingDirectory = getWorkingDirectory();

    if (!useWrapper) {
      if (getRunnerContext().isVirtualContext()) {
        getLogger().message("Step is running in a virtual context, skip detecting GRADLE_HOME");
      } else {
        gradleHome = getGradleHome();
        gradleExe = new File(gradleHome, this.exePath);
        exePath = gradleExe.getAbsolutePath();

        if (!gradleHome.exists())
          throw new RunBuildException("Gradle home path (" + gradleHome + ") is invalid.");
        if (!gradleExe.exists())
          throw new RunBuildException("Gradle home path (" + gradleHome + ") does not contain a Gradle installation.  Cannot find "+
                                      this.exePath + ".");
      }
    } else {
      String relativeGradleWPath = ConfigurationParamsUtil.getGradleWPath(getRunnerParameters());

      gradleExe = new File(workingDirectory, relativeGradleWPath + File.separator + wrapperName);
      exePath = gradleExe.getAbsolutePath();
      if (!gradleExe.exists())
        throw new RunBuildException("Gradle wrapper script " + wrapperName + " can not be found at " +
                                    gradleExe.getAbsolutePath() + "\n" +
                                    "Please, provide path to wrapper script in build configuration settings.");

      gradleWrapperProperties = new File(workingDirectory,
                                         relativeGradleWPath + File.separator + GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION);
      if (!gradleWrapperProperties.exists()) {
        throw new RunBuildException("gradle-wrapper.properties couldn't be found at " +
                                    gradleWrapperProperties.getAbsolutePath() + "\n" +
                                    "Please, provide correct path to wrapper in build configuration settings.");
      }
    }

    if (SystemInfo.isUnix) {
      params.add(exePath);
      exePath = "bash";
    }

    List<String> gradleTasks = getGradleTasks();

    Map<String, String> env = getEnvironments(workingDirectory, useWrapper, gradleHome, gradleWrapperProperties);

    GradleLaunchMode launchMode = GradleLaunchModeSelector.selectMode(workingDirectory, useWrapper, getConfigParameters(), gradleHome, gradleWrapperProperties);
    switch (launchMode) {
      case GRADLE_TOOLING_API:
        return prepareToolingApi(env, workingDirectory, gradleTasks);
      case GRADLE:
        params.addAll(getParams(GradleLaunchMode.GRADLE, gradleTasks));
        return new SimpleProgramCommandLine(env, workingDirectory.getPath(), exePath, params);

      default:
        throw new RunBuildException("Unable to detect Gradle version.\n" +
                                    "Try to use Gradle 1.0 or newer");
    }
  }

  private ProgramCommandLine prepareToolingApi(@NotNull Map<String, String> env,
                                               @NotNull File workingDirectory,
                                               @NotNull List<String> gradleTasks) throws RunBuildException {

    final File buildTempDir = getBuildTempDirectory();

    for (GradleBuildPropertiesSplitter splitter : propertySplitters.values()) {
      splitter.split(env, buildTempDir);
    }

    final File envFile = new File(buildTempDir, GRADLE_LAUNCHER_ENV_FILE);
    storeParamsMap(buildTempDir, env, envFile);

    final List<String> gradleParams = getParams(GradleLaunchMode.GRADLE_TOOLING_API, Collections.emptyList());
    final File gradleParamsFile = new File(buildTempDir, GRADLE_PARAMS_FILE);
    storeParams(buildTempDir, gradleParams, gradleParamsFile);

    final List<String> jvmArgs = StringUtil.splitHonorQuotes(buildGradleOpts(), GRADLE_TASKS_DELIMITER).stream()
                                           .map(String::trim)
                                           .collect(Collectors.toCollection(ArrayList::new));
    getTempDir(buildTempDir).ifPresent(tmpDir -> jvmArgs.add("-Djava.io.tmpdir=" + tmpDir));
    final File jvmParamsFile = new File(buildTempDir, GRADLE_JVM_PARAMS_FILE);
    storeParams(buildTempDir, jvmArgs, jvmParamsFile);

    final File gradleTasksFile = new File(buildTempDir, GRADLE_TASKS_FILE);
    storeParams(buildTempDir, gradleTasks, gradleTasksFile);

    final Map<String, String> envs = new HashMap<>(System.getenv());
    envs.put(GRADLE_LAUNCHER_ENV_FILE_ENV_KEY, envFile.getAbsolutePath());
    envs.put(GRADLE_PARAMS_FILE_ENV_KEY, gradleParamsFile.getAbsolutePath());
    envs.put(GRADLE_JVM_PARAMS_FILE_ENV_KEY, jvmParamsFile.getAbsolutePath());
    envs.put(GRADLE_TASKS_FILE_ENV_KEY, gradleTasksFile.getAbsolutePath());

    final Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put(GRADLE_RUNNER_READ_ALL_CONFIG_PARAM,
                         Boolean.valueOf(ConfigurationParamsUtil.isParameterEnabled(getConfigParameters(), GRADLE_RUNNER_READ_ALL_CONFIG_PARAM)).toString());
    Optional.ofNullable(System.getProperty(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY))
            .ifPresent(tcBuildParametersFilePath -> systemProperties.put(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY, tcBuildParametersFilePath));

    final String javaHome = getRunnerContext().isVirtualContext()
                      ? getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME)
                      : getJavaHome();

    return new JavaCommandLineBuilder()
      .withJavaHome(javaHome, getRunnerContext().isVirtualContext())
      .withBaseDir(getCheckoutDirectory().getAbsolutePath())
      .withWorkingDir(workingDirectory.getAbsolutePath())
      .withSystemProperties(systemProperties)
      .withEnvVariables(envs)
      .withClassPath(composeToolingApiProcessClasspath())
      .withMainClass(TeamCityGradleLauncher.class.getCanonicalName())
      .build();
  }

  private void storeParamsMap(@NotNull File buildTempDir,
                              @NotNull Map<String, String> params,
                              @NotNull File targetFile) throws RunBuildException {
    try {
      GradleRunnerFileUtil.storeParams(buildTempDir, params, targetFile);
    } catch (IOException e) {
      throw new RunBuildException("Couldn't create temp file while trying to prepare Gradle Tooling API build.\n" +
                                  "Destination file: " + targetFile.getAbsolutePath(), e);
    }
  }

  private void storeParams(@NotNull File buildTempDir,
                           @NotNull Collection<String> params,
                           @NotNull File targetFile) throws RunBuildException {
    try {
      GradleRunnerFileUtil.storeParams(buildTempDir, params, targetFile);
    } catch (IOException e) {
      throw new RunBuildException("Couldn't create temp file while trying to prepare Gradle Tooling API build.\n" +
                                  "Destination file: " + targetFile.getAbsolutePath(), e);
    }
  }

  private String composeToolingApiProcessClasspath() throws RunBuildException {
    final StringBuilder classPath = new StringBuilder();
    try {
      classPath.append(getClasspathElement(TeamCityGradleLauncher.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(BuildLauncher.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(GradleRunnerConstants.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(ServiceMessage.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(com.google.gson.Gson.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(LoggerFactory.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(StaticLoggerBinder.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(AbstractLoggerAdapter.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(LoggerContextAccessor.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(ClasspathUtil.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(ComparisonFailureUtil.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(jetbrains.buildServer.util.FileUtil.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(com.intellij.openapi.util.io.FileUtil.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(com.intellij.openapi.diagnostic.Logger.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(JDOMException.class));

    } catch (IOException e) {
      throw new RunBuildException("Failed to create init script classpath", e);
    }

    return classPath.toString();
  }

  private String getClasspathElement(Class<?> utilClass) throws IOException, RunBuildException {
    final String utilPath = ClasspathUtil.getClasspathEntry(utilClass);
    if (utilPath == null) throw new RunBuildException("Failed to define classpath for: " + utilClass.getCanonicalName());
    return new File(utilPath).getAbsolutePath();
  }

  private Map<String, String> getEnvironments(@NotNull File workingDirectory,
                                              @NotNull Boolean useWrapper,
                                              @Nullable final File gradleHome,
                                              @Nullable final File gradleWrapperProperties) throws RunBuildException {
    final Map<String,String> env = new HashMap<>(getEnvironmentVariables());
    if (gradleHome != null) env.put(GRADLE_HOME_ENV_KEY, gradleHome.getAbsolutePath());
    env.put("GRADLE_EXIT_CONSOLE", "true");

    if (!getRunnerContext().isVirtualContext()) {
      env.put(JavaRunnerConstants.JAVA_HOME, getJavaHome());
    }

    env.put(GradleRunnerConstants.ENV_GRADLE_OPTS, appendTmpDir(buildGradleOpts(), getBuildTempDirectory()));
    env.put(GradleRunnerConstants.ENV_INCREMENTAL_PARAM, getIncrementalMode());
    env.put(GradleRunnerConstants.ENV_SUPPORT_TEST_RETRY, getBuild().getBuildTypeOptionValue(BuildTypeOptions.BT_SUPPORT_TEST_RETRY).toString());
    env.put(GradleRunnerConstants.TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH, getBuildParameters().getSystemProperties().getOrDefault("teamcity.build.parallelTests.excludesFile", ""));

    if (gradleWrapperProperties != null) env.put(GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY, gradleWrapperProperties.getAbsolutePath());

    env.put(WORKING_DIRECTORY_ENV_KEY, workingDirectory.getAbsolutePath());
    env.put(USE_WRAPPER_ENV_KEY, useWrapper.toString());

    return env;
  }

  private String appendTmpDir(@NotNull final String s, @NotNull final File tempDir) {
    Optional<String> temp = getTempDir(tempDir);
    if (temp.isPresent()) {
      return s + " \"-Djava.io.tmpdir=" + temp.get() + "\"";
    }
    return s;
  }

  private Optional<String> getTempDir(@NotNull final File tempDir) {
    try {
      return Optional.of(tempDir.getCanonicalPath());
    } catch (IOException e) {
      Loggers.AGENT.warnAndDebugDetails("Failed patch temp dir for Gradle runtime environment: " + e, e);
      return Optional.empty();
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
  private List<String> getParams(@NotNull GradleLaunchMode gradleLaunchMode,
                                 @NotNull List<String> gradleTasks) {

    final List<String> params = new ArrayList<>();

    params.addAll(getInitScriptParams(gradleLaunchMode));
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

    for (String task: gradleTasks) {
      params.add(task);
    }

    return params;
  }

  private List<String> getGradleTasks() {
    String gradleTasks = ConfigurationParamsUtil.getGradleTasks(getRunnerParameters());
    return gradleTasks.length() > 0
           ? StringUtil.splitHonorQuotes(gradleTasks, GRADLE_TASKS_DELIMITER).stream()
                       .map(String::trim)
                       .collect(Collectors.toList())
           : Collections.emptyList();
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

  private List<String> getInitScriptParams(@NotNull GradleLaunchMode gradleLaunchMode) {
    final String scriptPath = ConfigurationParamsUtil.getGradleInitScript(getRunnerParameters());
    File initScript;

    if (!scriptPath.isEmpty()) {
      initScript = new File(scriptPath);
    } else {
      File pluginsDirectory = getBuild().getAgentConfiguration().getAgentPluginsDirectory();
      File runnerPluginDir = new File(pluginsDirectory, GradleRunnerConstants.RUNNER_TYPE);
      String initScriptName = gradleLaunchMode == GradleLaunchMode.GRADLE
                              ? INIT_SCRIPT_NAME
                              : INIT_SCRIPT_SINCE_8_NAME;
      initScript = new File(new File(runnerPluginDir, INIT_SCRIPT_DIR), initScriptName);
    }

    return Arrays.asList("--init-script", initScript.getAbsolutePath());
  }

  @NotNull
  @Override
  public List<ProcessListener> getListeners() {
    return listeners.getValue();
  }

}

