package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.agent.IncrementalBuild;
import jetbrains.buildServer.agent.ToolCannotBeFoundException;
import jetbrains.buildServer.agent.runner.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerParameters;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.messages.ErrorData;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeOptions;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.impl.Lazy;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.internal.DefaultGradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;

public class GradleRunnerService extends BuildServiceAdapter
{
  private final String exePath;
  private final String wrapperName;
  private final Lazy<List<ProcessListener>> listeners;
  private final GradleCommandLineComposerHolder composerHolder;
  private final GradleTasksComposer tasksComposer;
  private final GradleLaunchModeSelector gradleLaunchModeSelector;
  private final GradleConfigurationCacheDetector gradleConfigurationCacheDetector;
  private final CommandLineParametersProcessor commandLineParametersProcessor;
  private final GradleVersionDetector gradleVersionDetector;

  public GradleRunnerService(final String exePath,
                             final String wrapperName,
                             final GradleCommandLineComposerHolder composerHolder,
                             final GradleTasksComposer tasksComposer,
                             final GradleLaunchModeSelector gradleLaunchModeSelector,
                             final GradleConfigurationCacheDetector gradleConfigurationCacheDetector,
                             final CommandLineParametersProcessor commandLineParametersProcessor,
                             final GradleVersionDetector gradleVersionDetector) {
    this.exePath = exePath;
    this.wrapperName = wrapperName;
    this.composerHolder = composerHolder;
    this.tasksComposer = tasksComposer;
    this.gradleLaunchModeSelector = gradleLaunchModeSelector;
    this.gradleConfigurationCacheDetector = gradleConfigurationCacheDetector;
    this.commandLineParametersProcessor = commandLineParametersProcessor;
    this.gradleVersionDetector = gradleVersionDetector;
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

      gradleWrapperProperties = getGradleWrapperProperties(workingDirectory, relativeGradleWPath);
      if (!gradleWrapperProperties.exists()) {
        getLogger().warning("gradle-wrapper.properties couldn't be found at " + gradleWrapperProperties.getAbsolutePath());
      }
    }

    if (SystemInfo.isUnix) {
      params.add(exePath);
      exePath = "bash";
    }

    List<String> gradleTasks = tasksComposer.getGradleTasks(getRunnerParameters());

    Map<String, String> env = getEnvironments(workingDirectory, useWrapper, gradleHome, gradleWrapperProperties);

    if (useWrapper && !gradleWrapperProperties.exists()) {
      return composerHolder.getCommandLineComposer(GradleLaunchMode.COMMAND_LINE).composeCommandLine(
        getComposerParameters(env, gradleTasks, null, workingDirectory, params, exePath, null));
    }

    GradleConnector projectConnector = getGradleConnector(workingDirectory, useWrapper, gradleHome, gradleWrapperProperties);
    File gradleUserHome = Optional.ofNullable(projectConnector).flatMap(this::getGradleUserHome).orElse(null);
    DefaultGradleVersion gradleVersion = gradleVersionDetector.detect(projectConnector, getLogger()).orElse(null);
    List<String> userDefinedParams = ConfigurationParamsUtil.getGradleParams(getRunnerParameters());
    boolean configurationCacheEnabled = gradleConfigurationCacheDetector.isConfigurationCacheEnabled(getLogger(), gradleTasks, userDefinedParams, gradleUserHome, workingDirectory, gradleVersion);
    boolean configurationCacheProblemsIgnored = gradleConfigurationCacheDetector.areConfigurationCacheProblemsIgnored(getLogger(), gradleTasks, userDefinedParams, gradleUserHome, workingDirectory, gradleVersion);
    Set<String> unsupportedByToolingArgs = commandLineParametersProcessor.obtainUnsupportedArguments(Stream.concat(gradleTasks.stream(), userDefinedParams.stream()).collect(Collectors.toList()));
    GradleLaunchModeSelectionResult selectionResult = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                                             .withLogger(getLogger())
                                                                                                                             .withConfigurationParameters(getConfigParameters())
                                                                                                                             .withGradleVersion(gradleVersion)
                                                                                                                             .withConfigurationCacheEnabled(configurationCacheEnabled)
                                                                                                                             .withConfigurationCacheProblemsIgnored(configurationCacheProblemsIgnored)
                                                                                                                             .withUnsupportedByToolingArgs(unsupportedByToolingArgs)
                                                                                                                             .build());

    GradleCommandLineComposerParameters composerParameters =
      getComposerParameters(env, gradleTasks, configurationCacheEnabled, workingDirectory, params, exePath, selectionResult);

    return composerHolder.getCommandLineComposer(selectionResult.getLaunchMode()).composeCommandLine(composerParameters);
  }

  private GradleCommandLineComposerParameters getComposerParameters(@NotNull Map<String, String> env,
                                                                    @NotNull List<String> gradleTasks,
                                                                    @Nullable Boolean configurationCacheEnabled,
                                                                    @NotNull File workingDirectory,
                                                                    @NotNull List<String> params,
                                                                    @NotNull String exePath,
                                                                    @Nullable GradleLaunchModeSelectionResult launchModeSelectionResult) throws RunBuildException {
    return GradleCommandLineComposerParameters.builder()
                                              .withEnv(env)
                                              .withBuildTempDir(getBuildTempDirectory())
                                              .withRunnerParameters(getRunnerParameters())
                                              .withPluginsDirectory(getBuild().getAgentConfiguration().getAgentPluginsDirectory())
                                              .withGradleOpts(buildGradleOpts())
                                              .withGradleTasks(gradleTasks)
                                              .withConfigurationCacheEnabled(Optional.ofNullable(configurationCacheEnabled).orElse(false))
                                              .withConfigParameters(getConfigParameters())
                                              .withLogger(getLogger())
                                              .withRunnerContext(getRunnerContext())
                                              .withJavaHome(getJavaHome())
                                              .withCheckoutDirectory(getCheckoutDirectory())
                                              .withWorkingDirectory(workingDirectory)
                                              .withInitialGradleParams(params)
                                              .withExePath(exePath)
                                              .withLaunchModeSelectionResult(launchModeSelectionResult)
                                              .build();
  }

  @NotNull
  private File getGradleWrapperProperties(@NotNull File workingDirectory,
                                          @NotNull String relativeGradleWPath) {
    String wrapperPropertiesPath = ConfigurationParamsUtil.getGradleWrapperPropertiesPath(getConfigParameters());
    if (StringUtil.isNotEmpty(wrapperPropertiesPath)) {
      return new File(workingDirectory, wrapperPropertiesPath + File.separator + GRADLE_WRAPPER_PROPERTIES_FILENAME);
    }

    return new File(workingDirectory, relativeGradleWPath + File.separator + GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION);
  }

  @NotNull
  private Optional<File> getGradleUserHome(@NotNull GradleConnector projectConnector) {
    try (ProjectConnection connection = projectConnector.connect()) {
      BuildEnvironment buildEnvironment = connection.getModel(BuildEnvironment.class);
      return Optional.ofNullable(buildEnvironment.getGradle().getGradleUserHome());
    } catch (Throwable t) {
      getLogger().warning("Unable to detect Gradle User Home: " + t.getMessage());
      return Optional.empty();
    }
  }

  @Nullable
  private GradleConnector getGradleConnector(@NotNull File workingDirectory,
                                             @NotNull Boolean useWrapper,
                                             @Nullable File gradleHome,
                                             @Nullable File gradleWrapperProperties) {
    try {
      return GradleToolingConnectorFactory.instantiate(workingDirectory, useWrapper, gradleHome, gradleWrapperProperties);
    } catch (Throwable t) {
      getLogger().warning("Unable to obtain project connector: " + t.getMessage());
      return null;
    }
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
  @Override
  public List<ProcessListener> getListeners() {
    return listeners.getValue();
  }

}