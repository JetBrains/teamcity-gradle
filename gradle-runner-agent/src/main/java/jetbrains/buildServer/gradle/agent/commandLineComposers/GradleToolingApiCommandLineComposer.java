package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.agent.runner.JavaCommandLineBuilder;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleDaemonEnhancementClassesProvider;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelectionResult;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildPropertiesSplitter;
import jetbrains.buildServer.gradle.agent.propertySplit.SplitablePropertyFile;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.gradle.runtime.LauncherParameters;
import jetbrains.buildServer.gradle.runtime.TeamCityGradleLauncher;
import jetbrains.buildServer.gradle.runtime.service.GradleBuildConfigurator;
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
import static jetbrains.buildServer.gradle.agent.ConfigurationParamsUtil.getBooleanOrDefault;

public class GradleToolingApiCommandLineComposer implements GradleCommandLineComposer {
  @NotNull
  private final Map<SplitablePropertyFile, GradleBuildPropertiesSplitter> propertySplitters;
  @NotNull
  private final GradleTasksComposer tasksComposer;

  public GradleToolingApiCommandLineComposer(@NotNull List<GradleBuildPropertiesSplitter> propertySplitters, @NotNull GradleTasksComposer tasksComposer) {
    this.propertySplitters = propertySplitters.stream().collect(Collectors.toMap(GradleBuildPropertiesSplitter::getType, Function.identity()));
    this.tasksComposer = tasksComposer;
  }

  @Override
  @NotNull
  public GradleLaunchMode getLaunchMode() {
    return GradleLaunchMode.TOOLING_API;
  }

  @Override
  @NotNull
  public ProgramCommandLine compose(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException {
    parameters.getLogger().message("Gradle will be launched via Tooling API, reason: " +
                                   parameters.getLaunchModeSelectionResult().map(GradleLaunchModeSelectionResult::getReason).orElse(null));

    boolean isVirtualContext = parameters.getRunnerContext().isVirtualContext();
    String actualJavaHome = isVirtualContext ? parameters.getRunnerParameters().get(JavaRunnerConstants.TARGET_JDK_HOME) : parameters.getJavaHome();

    return new JavaCommandLineBuilder()
      .withJavaHome(actualJavaHome, isVirtualContext)
      .withBaseDir(parameters.getCheckoutDir().toString())
      .withWorkingDir(parameters.getWorkingDir().toString())
      .withSystemProperties(composeSystemProperties(parameters))
      .withEnvVariables(composeEnvs(parameters))
      .withClassPath(composeClasspath(parameters))
      .withJvmArgs(composeJvmArgs(parameters))
      .withMainClass(TeamCityGradleLauncher.class.getCanonicalName())
      .build();
  }

  @NotNull
  private Map<String, String> composeSystemProperties(@NotNull GradleCommandLineComposerParameters parameters) {
    Map<String, String> props = new HashMap<>();
    props.put(
      GRADLE_RUNNER_READ_ALL_CONFIG_PARAM,
      readAllBuildParamsRequired(parameters.isConfigurationCacheEnabled(), parameters.getConfigParameters(), parameters.getLogger()).toString()
    );
    props.put(
      GRADLE_RUNNER_ALLOW_JVM_ARGS_OVERRIDING_CONFIG_PARAM,
      getBooleanOrDefault(parameters.getConfigParameters(), GRADLE_RUNNER_ALLOW_JVM_ARGS_OVERRIDING_CONFIG_PARAM, true).toString()
    );
    Optional
      .ofNullable(System.getProperty(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY))
      .ifPresent(it -> props.put(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY, it));

    return props;
  }

  @NotNull
  private Map<String, String> composeEnvs(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException {
    Path buildTempDir = parameters.getBuildTempDir();

    for (GradleBuildPropertiesSplitter splitter : propertySplitters.values()) {
      splitter.split(parameters.getEnv(), buildTempDir.toFile());
    }

    Map<String, String> env = new HashMap<>(parameters.getEnv());
    env.put(GRADLE_PARAMS_FILE_ENV_KEY, storeLauncherParamsToTempFile(tasksComposer.getGradleParameters(
      getLaunchMode(),
      parameters.getRunnerParameters(),
      parameters.getGradleUserDefinedParams(),
      parameters.getPluginsDir().toFile()
    ), buildTempDir, GRADLE_PARAMS_FILE));
    env.put(GRADLE_JVM_PARAMS_FILE_ENV_KEY, storeLauncherParamsToTempFile(composeGradleJvmArgs(parameters), buildTempDir, GRADLE_JVM_PARAMS_FILE));
    env.put(GRADLE_TASKS_FILE_ENV_KEY, storeLauncherParamsToTempFile(parameters.getGradleTasks(), buildTempDir, GRADLE_TASKS_FILE));

    Map<String, String> configParams = parameters.getConfigParameters();
    configureGradleDaemonClasspathEnhancement(env, configParams);
    configureTestTaskJvmArgumentsProvider(env, configParams);
    configureInitScriptClasspath(env, parameters.getLogger()); // workaround for https://github.com/gradle/gradle/issues/32939
    configureLauncherShutdownTimeout(env, configParams); // workaround for https://github.com/gradle/gradle/issues/34491, delete when issue fixed

    return env;
  }

  @NotNull
  private static List<String> composeGradleJvmArgs(@NotNull GradleCommandLineComposerParameters parameters) {
    List<String> gradleJvmArgs = StringUtil
      .splitHonorQuotes(parameters.getGradleOpts(), GRADLE_TASKS_DELIMITER).stream()
      .map(String::trim)
      .collect(Collectors.toCollection(ArrayList::new));

    gradleJvmArgs.add("-Djava.io.tmpdir=" + parameters.getBuildTempDir());
    return gradleJvmArgs;
  }

  @NotNull
  private String storeLauncherParamsToTempFile(@NotNull List<String> params, @NotNull Path buildTempDir, @NotNull String fileName) throws RunBuildException {
    Path targetFile;
    try {
      targetFile = buildTempDir.resolve(fileName);
      LauncherParameters.fromValues(params).writeToFile(targetFile);
      return targetFile.toString();
    } catch (IOException e) {
      throw new RunBuildException("Failed to store Gradle launcher parameters to file " + fileName + " in " + buildTempDir, e);
    }
  }

  private void configureGradleDaemonClasspathEnhancement(@NotNull final Map<String, String> env, @NotNull final Map<String, String> configParams) {
    if (!configParams.containsKey(GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH) || Boolean.parseBoolean(configParams.get(GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH))) {
      env.put(GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY, GradleDaemonEnhancementClassesProvider.provide());
    }
  }

  private void configureTestTaskJvmArgumentsProvider(@NotNull final Map<String, String> env, @NotNull final Map<String, String> configParams) {
    if (configParams.containsKey(GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED) && Boolean.parseBoolean(configParams.get(GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED))) {
      env.put(TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_KEY, "true");
    }
  }

  private void configureInitScriptClasspath(@NotNull Map<String, String> env, @NotNull BuildProgressLogger logger) {
    try {
      env.put(TEAMCITY_BUILD_INIT_PATH, GradleBuildConfigurator.getInitScriptClasspath());
    } catch (IOException e) {
      logger.message("Couldn't launch Gradle via Tooling API: error while trying to build init script classpath");
    }
  }

  private void configureLauncherShutdownTimeout(@NotNull Map<String, String> env, @NotNull Map<String, String> configParams) {
    boolean timeoutEnabled = Optional
      .ofNullable(configParams.get(GRADLE_RUNNER_TOOLING_API_LAUNCHER_SHUTDOWN_TIMEOUT_ENABLED))
      .map(Boolean::parseBoolean)
      .orElse(true);

    if (timeoutEnabled) {
      String timeoutSec = Optional.ofNullable(configParams.get(GRADLE_RUNNER_TOOLING_API_LAUNCHER_SHUTDOWN_TIMEOUT_SEC)).orElse("60");
      env.put(GRADLE_TOOLING_API_LAUNCHER_SHUTDOWN_TIMEOUT_SEC_ENV_KEY, timeoutSec);
    }
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
  private String composeClasspath(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException {
    try {
      return Stream
        .concat(
          getGradlePluginClasspathEntries(),
          getCommonAgentLibsClasspathEntries(parameters)
        )
        .map(Path::toString)
        .collect(Collectors.joining(File.pathSeparator));
    } catch (RuntimeException e) {
      throw new RunBuildException("Failed to create Tooling API launcher classpath", e);
    }
  }

  @NotNull
  private Stream<Path> getGradlePluginClasspathEntries() {
    return Stream
      .of(
        TeamCityGradleLauncher.class,
        BuildLauncher.class,
        GradleRunnerConstants.class,
        OptionGroup.class
      )
      .map(this::getClasspathEntry);
  }

  // libs from <buildAgentDir>/lib won't be accessible in case of Docker-in-Docker / Docker Wormhole builds
  // see https://youtrack.jetbrains.com/issue/TW-87034
  @NotNull
  private Stream<Path> getCommonAgentLibsClasspathEntries(@NotNull GradleCommandLineComposerParameters parameters) {
    boolean relocateLibs = parameters.getRunnerContext().isVirtualContext() &&
                           getBooleanOrDefault(parameters.getConfigParameters(), GRADLE_RUNNER_PLACE_LIBS_FOR_TOOLING_IN_TEMP_DIR, true);

    Path libsTempDir = parameters.getBuildTempDir().resolve("agentLibs");
    try {
      if (relocateLibs) Files.createDirectories(libsTempDir);
    } catch (IOException e) {
      throw new RuntimeException("Failed to create temp directory for common agent libs at " + libsTempDir, e);
    }

    return Stream
      .of(
        ServiceMessage.class,
        com.google.gson.Gson.class,
        LoggerFactory.class,
        StaticLoggerBinder.class,
        AbstractLoggerAdapter.class,
        LoggerContextAccessor.class,
        ClasspathUtil.class,
        ComparisonFailureUtil.class,
        FileUtil.class,
        com.intellij.openapi.util.io.FileUtil.class,
        com.intellij.openapi.diagnostic.Logger.class,
        JDOMException.class
      )
      .map(this::getClasspathEntry)
      .map(originalPath -> relocateLibs ? moveLibrary(originalPath, libsTempDir) : originalPath);
  }

  @NotNull
  private Path moveLibrary(@NotNull Path libraryPath, @NotNull Path targetDir) {
    Path targetPath;
    try {
      targetPath = targetDir.resolve(libraryPath.getFileName());
      if (!Files.exists(targetPath)) {
        if (Files.isRegularFile(libraryPath)) {
          FileUtil.copy(libraryPath.toFile(), targetPath.toFile());
        } else {
          FileUtil.copyDir(libraryPath.toFile(), targetPath.toFile());
        }
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to move library " + libraryPath + " to " + targetDir, e);
    }

    return targetPath;
  }

  @NotNull
  private Path getClasspathEntry(@NotNull Class<?> someClass) {
    try {
      String entry = ClasspathUtil.getClasspathEntry(someClass);
      if (entry == null) throw new IllegalStateException("No path found");
      return Paths.get(entry).toAbsolutePath();
    } catch (Exception e) {
      throw new RuntimeException("Failed to define classpath entry for: " + someClass.getCanonicalName(), e);
    }
  }

  @NotNull
  private List<String> composeJvmArgs(@NotNull GradleCommandLineComposerParameters parameters) {
    return Optional
      .ofNullable(parameters.getConfigParameters().get(GRADLE_RUNNER_TOOLING_API_LAUNCHER_JVM_ARGS))
      .map(StringUtil::splitHonorQuotes)
      .orElse(Collections.emptyList());
  }
}
