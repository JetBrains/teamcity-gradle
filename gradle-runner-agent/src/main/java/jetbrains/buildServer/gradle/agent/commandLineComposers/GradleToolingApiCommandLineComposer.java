package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.agent.runner.JavaCommandLineBuilder;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleDaemonEnhancementClassesProvider;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelectionResult;
import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildPropertiesSplitter;
import jetbrains.buildServer.gradle.agent.propertySplit.SplitablePropertyFile;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.gradle.runtime.service.GradleBuildConfigurator;
import jetbrains.buildServer.gradle.runtime.TeamCityGradleLauncher;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.apache.commons.cli.OptionGroup;
import org.apache.logging.log4j.core.LoggerContextAccessor;
import org.apache.logging.log4j.spi.AbstractLoggerAdapter;
import org.gradle.tooling.BuildLauncher;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY;
import static jetbrains.buildServer.gradle.agent.ConfigurationParamsUtil.getBooleanOrDefault;

public class GradleToolingApiCommandLineComposer implements GradleCommandLineComposer {

  private final Map<SplitablePropertyFile, GradleBuildPropertiesSplitter> propertySplitters;
  private final GradleTasksComposer tasksComposer;

  public GradleToolingApiCommandLineComposer(List<GradleBuildPropertiesSplitter> propertySplitters,
                                             GradleTasksComposer tasksComposer) {
    this.propertySplitters = propertySplitters.stream().collect(Collectors.toMap(it -> it.getType(), Function.identity()));
    this.tasksComposer = tasksComposer;
  }

  @Override
  @NotNull
  public GradleLaunchMode getApplicableLaunchMode() {
    return GradleLaunchMode.TOOLING_API;
  }

  @Override
  @NotNull
  public ProgramCommandLine composeCommandLine(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException {
    parameters.getLogger().message("The build will be launched via Gradle Tooling API because: " +
                                   parameters.getLaunchModeSelectionResult().map(GradleLaunchModeSelectionResult::getReason).orElse(null));

    final File buildTempDir = parameters.getBuildTempDir();

    for (GradleBuildPropertiesSplitter splitter : propertySplitters.values()) {
      splitter.split(parameters.getEnv(), buildTempDir);
    }

    final List<String> gradleParams = tasksComposer.getGradleParameters(GradleLaunchMode.TOOLING_API, parameters.getRunnerParameters(),
                                                                        parameters.getGradleUserDefinedParams(), parameters.getPluginsDirectory());
    final File gradleParamsFile = new File(buildTempDir, GRADLE_PARAMS_FILE);
    storeParams(buildTempDir, gradleParams, gradleParamsFile);

    final List<String> jvmArgs = StringUtil.splitHonorQuotes(parameters.getGradleOpts(), GRADLE_TASKS_DELIMITER).stream()
                                           .map(String::trim)
                                           .collect(Collectors.toCollection(ArrayList::new));
    getTempDir(buildTempDir).ifPresent(tmpDir -> jvmArgs.add("-Djava.io.tmpdir=" + tmpDir));
    final File jvmParamsFile = new File(buildTempDir, GRADLE_JVM_PARAMS_FILE);
    storeParams(buildTempDir, jvmArgs, jvmParamsFile);

    final File gradleTasksFile = new File(buildTempDir, GRADLE_TASKS_FILE);
    storeParams(buildTempDir, parameters.getGradleTasks(), gradleTasksFile);

    final Map<String, String> envs = new HashMap<>(parameters.getEnv());
    envs.put(GRADLE_PARAMS_FILE_ENV_KEY, gradleParamsFile.getAbsolutePath());
    envs.put(GRADLE_JVM_PARAMS_FILE_ENV_KEY, jvmParamsFile.getAbsolutePath());
    envs.put(GRADLE_TASKS_FILE_ENV_KEY, gradleTasksFile.getAbsolutePath());
    if (gradleDaemonClasspathEnhancementEnabled(parameters.getConfigParameters())) {
      envs.put(GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY, GradleDaemonEnhancementClassesProvider.provide());
    }
    if (testTaskJvmArgumentsProviderDisabled(parameters.getConfigParameters())) {
      envs.put(TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_KEY, "true");
    }

    final Map<String, String> systemProperties = new HashMap<>();
    systemProperties.put(GRADLE_RUNNER_READ_ALL_CONFIG_PARAM, readAllBuildParamsRequired(parameters.isConfigurationCacheEnabled(),
                                                                                         parameters.getConfigParameters(), parameters.getLogger()).toString());
    systemProperties.put(GRADLE_RUNNER_ALLOW_JVM_ARGS_OVERRIDING_CONFIG_PARAM,
                         getBooleanOrDefault(parameters.getConfigParameters(), GRADLE_RUNNER_ALLOW_JVM_ARGS_OVERRIDING_CONFIG_PARAM, true).toString());
    Optional.ofNullable(System.getProperty(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY))
            .ifPresent(tcBuildParametersFilePath -> systemProperties.put(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY, tcBuildParametersFilePath));

    final String actualJavaHome = parameters.getRunnerContext().isVirtualContext()
                            ? parameters.getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME)
                            : parameters.getJavaHome();

    try {
      // workaround for https://github.com/gradle/gradle/issues/32939
      envs.put(TEAMCITY_BUILD_INIT_PATH, GradleBuildConfigurator.getInitScriptClasspath());
    } catch (IOException e) {
      parameters.getLogger().message("Couldn't launch Gradle via Tooling API: error while trying to build init script classpath ");
    }

    return new JavaCommandLineBuilder()
      .withJavaHome(actualJavaHome, parameters.getRunnerContext().isVirtualContext())
      .withBaseDir(parameters.getCheckoutDirectory().getAbsolutePath())
      .withWorkingDir(parameters.getWorkingDirectory().getAbsolutePath())
      .withSystemProperties(systemProperties)
      .withEnvVariables(envs)
      .withClassPath(composeToolingApiProcessClasspath(buildTempDir, parameters.getRunnerContext(), parameters.getConfigParameters()))
      .withMainClass(TeamCityGradleLauncher.class.getCanonicalName())
      .build();
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

  private Optional<String> getTempDir(@NotNull final File tempDir) {
    try {
      return Optional.of(tempDir.getCanonicalPath());
    } catch (IOException e) {
      Loggers.AGENT.warnAndDebugDetails("Failed patch temp dir for Gradle runtime environment: " + e, e);
      return Optional.empty();
    }
  }

  @NotNull
  private Boolean gradleDaemonClasspathEnhancementEnabled(@NotNull final Map<String, String> configParams) {
    if (configParams.containsKey(GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH)) {
      return Boolean.valueOf(configParams.get(GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH));
    }

    return true;
  }

  private boolean testTaskJvmArgumentsProviderDisabled(@NotNull final Map<String, String> configParams) {
    if (configParams.containsKey(GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED)) {
      return Boolean.parseBoolean(configParams.get(GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED));
    }

    return false;
  }

  @NotNull
  private Boolean readAllBuildParamsRequired(final boolean configurationCacheEnabled,
                                             @NotNull final Map<String, String> configParams,
                                             @NotNull final BuildProgressLogger logger) {
    if (configParams.containsKey(GRADLE_RUNNER_READ_ALL_CONFIG_PARAM)) {
      return Boolean.valueOf(configParams.get(GRADLE_RUNNER_READ_ALL_CONFIG_PARAM));
    }

    if (!configurationCacheEnabled) {
      return true;
    }

    logger.message(
      "This Gradle step uses a configuration cache.\n" +
      "To ensure the configuration cache operates as expected, parameters whose values always change from build to build (for example, build.id or build.number) " +
      "will be loaded only on demand. \n" +
      "You can still obtain values of these properties using direct references (for example, project.teamcity[\"build.number\"]), " +
      "but the project.findProperty(\"build.number\") or project[\"build.number\"] yields no results.");
    return false;
  }

  @NotNull
  private String composeToolingApiProcessClasspath(@NotNull final File buildTempDirectory,
                                                   @NotNull final BuildRunnerContext runnerContext,
                                                   @NotNull final Map<String, String> configParameters) throws RunBuildException {
    final StringBuilder classPath = new StringBuilder();
    try {
      classPath.append(getClasspathElement(TeamCityGradleLauncher.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(BuildLauncher.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(GradleRunnerConstants.class))
               .append(File.pathSeparator)
               .append(getClasspathElement(OptionGroup.class))
               .append(File.pathSeparator)
               .append(prepareClasspathForCommonAgentLibs(buildTempDirectory, runnerContext, configParameters));

    } catch (IOException e) {
      throw new RunBuildException("Failed to create init script classpath", e);
    }

    return classPath.toString();
  }

  // libs from <buildAgentDir>/lib won't be accessible in case of Docker-in-Docker / Docker Wormhole builds
  // see https://youtrack.jetbrains.com/issue/TW-87034
  @NotNull
  private String prepareClasspathForCommonAgentLibs(@NotNull final File buildTempDirectory,
                                                    @NotNull final BuildRunnerContext runnerContext,
                                                    @NotNull final Map<String, String> configParameters) throws RunBuildException, IOException {
    final List<Class<?>> classesFromCommonAgentLibs = Arrays.asList(
      ServiceMessage.class,
      com.google.gson.Gson.class,
      LoggerFactory.class,
      StaticLoggerBinder.class,
      AbstractLoggerAdapter.class,
      LoggerContextAccessor.class,
      ClasspathUtil.class,
      ComparisonFailureUtil.class,
      jetbrains.buildServer.util.FileUtil.class,
      com.intellij.openapi.util.io.FileUtil.class,
      com.intellij.openapi.diagnostic.Logger.class,
      JDOMException.class
    );
    final StringBuilder classPath = new StringBuilder();
    final File agentLibs = new File(buildTempDirectory, "agentLibs");
    agentLibs.mkdirs();
    final boolean changeLocation = runnerContext.isVirtualContext() &&
                                   getBooleanOrDefault(configParameters, GRADLE_RUNNER_PLACE_LIBS_FOR_TOOLING_IN_TEMP_DIR, true);

    for (int i = 0; i < classesFromCommonAgentLibs.size(); i++) {
      final Class<?> classFromAgentLib = classesFromCommonAgentLibs.get(i);
      final String originalLibPath = getClasspathElement(classFromAgentLib);

      if (changeLocation) {
        classPath.append(changeLibLocation(agentLibs, originalLibPath));
      } else {
        classPath.append(originalLibPath);
      }

      if (i < classesFromCommonAgentLibs.size() - 1) {
        classPath.append(File.pathSeparator);
      }
    }

    return classPath.toString();
  }

  @NotNull
  private String changeLibLocation(@NotNull File newLocation,
                                   @NotNull String originalLibPath) throws IOException {
    final File originalLib = new File(originalLibPath);
    final File relocatedLib = new File(newLocation, originalLib.getName());

    if (originalLib.isFile()) {
      if (!relocatedLib.exists()) {
        FileUtil.copy(originalLib, relocatedLib);
      }
    } else {
      FileUtil.copyDir(originalLib, relocatedLib);
    }

    return relocatedLib.getAbsolutePath();
  }

  private String getClasspathElement(Class<?> utilClass) throws IOException, RunBuildException {
    final String utilPath = ClasspathUtil.getClasspathEntry(utilClass);
    if (utilPath == null) throw new RunBuildException("Failed to define classpath for: " + utilClass.getCanonicalName());
    return new File(utilPath).getAbsolutePath();
  }
}
