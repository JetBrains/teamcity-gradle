package jetbrains.buildServer.gradle.runtime;

import java.io.File;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil;
import jetbrains.buildServer.gradle.runtime.listening.BuildEventListener;
import jetbrains.buildServer.gradle.runtime.listening.BuildLifecycleListener;
import jetbrains.buildServer.gradle.runtime.listening.GradleBuildLifecycleListener;
import jetbrains.buildServer.gradle.runtime.listening.event.BuildStartedEventImpl;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLoggerImpl;
import jetbrains.buildServer.gradle.runtime.output.GradleBuildOutputProcessor;
import jetbrains.buildServer.gradle.runtime.service.GradleBuildConfigurator;
import jetbrains.buildServer.gradle.runtime.service.jvmargs.GradleJvmArgsMerger;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR;
import static jetbrains.buildServer.gradle.runtime.service.TeamCityBuildParametersResolver.getTcBuildParametersFile;

public class TeamCityGradleLauncher {

  public static void main(String[] args) {
    final Map<String, String> gradleEnv = new HashMap<>(System.getenv());
    try {
      gradleEnv.put("TEAMCITY_BUILD_INIT_PATH", GradleBuildConfigurator.getInitScriptClasspath());
    } catch (IOException e) {
      System.err.println("Couldn't launch Gradle via Tooling API: error while trying to build init script classpath");
      return;
    }

    String gradleParamsFilePath = getSystemEnvValue(gradleEnv, GradleRunnerConstants.GRADLE_PARAMS_FILE_ENV_KEY);
    if (gradleParamsFilePath == null) {
      return;
    }
    final List<String> gradleParams = readParams(gradleParamsFilePath);
    if (gradleParams == null) {
      return;
    }

    String jvmArgsFilePath = getSystemEnvValue(gradleEnv, GradleRunnerConstants.GRADLE_JVM_PARAMS_FILE_ENV_KEY);
    if (jvmArgsFilePath == null) {
      return;
    }
    final List<String> tcJvmArgs = readParams(jvmArgsFilePath);
    if (tcJvmArgs == null) {
      return;
    }

    String gradleTasksPath = getSystemEnvValue(gradleEnv, GradleRunnerConstants.GRADLE_TASKS_FILE_ENV_KEY);
    if (gradleTasksPath == null) {
      return;
    }
    final List<String> gradleTasks = readParams(gradleTasksPath);
    if (gradleTasks == null) {
      return;
    }

    final String workingDir = gradleEnv.get(GradleRunnerConstants.WORKING_DIRECTORY_ENV_KEY);
    if (workingDir == null) {
      System.err.println("Parameter " + GradleRunnerConstants.WORKING_DIRECTORY_ENV_KEY
                         + " must be present in environment variables.");
      return;
    }

    File tcBuildParametersFile = getTcBuildParametersFile(gradleEnv);
    Properties teamCityBuildParameters = getTeamCityBuildParameters(tcBuildParametersFile);
    if (teamCityBuildParameters == null) {
      return;
    }
    String buildTempDir = teamCityBuildParameters.getProperty("teamcity.build.tempDir", "");
    if (buildTempDir.isEmpty()) {
      System.err.println("Parameter teamcity.build.tempDir must be set in teamcity.build.parameters.");
      return;
    }
    String buildNumber = teamCityBuildParameters.getProperty("build.number", "");

    final Boolean useWrapper = Boolean.valueOf(gradleEnv.get(GradleRunnerConstants.USE_WRAPPER_ENV_KEY));
    final String gradleHome = gradleEnv.get(GradleRunnerConstants.GRADLE_HOME_ENV_KEY);
    final String gradleWrapperProperties = gradleEnv.get(GradleRunnerConstants.GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY);

    final GradleConnector connector;
    try {
      connector = GradleBuildConfigurator.prepareConnector(workingDir, useWrapper, gradleWrapperProperties, gradleHome);
    } catch (Exception e) {
      System.err.println(e.getMessage());
      return;
    }

    boolean isDebugModeEnabled = gradleParams.stream().anyMatch(task -> task.equals("-d"));
    GradleToolingLogger logger = new GradleToolingLoggerImpl(isDebugModeEnabled);
    GradleJvmArgsMerger jvmArgsMerger = new GradleJvmArgsMerger(logger);
    String taskOutputDir = buildTempDir + File.separator + BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR;
    BuildContext buildContext = new BuildContext(tcBuildParametersFile.getAbsolutePath(), taskOutputDir, gradleParamsFilePath, jvmArgsFilePath, gradleTasksPath);
    List<BuildEventListener > eventListeners = new ArrayList<>();
    eventListeners.add(new GradleBuildOutputProcessor(logger, buildContext));
    BuildLifecycleListener buildLifecycleListener = new GradleBuildLifecycleListener(logger, eventListeners, buildContext);

    try (ProjectConnection connection = connector.connect()) {
      Optional<BuildEnvironment> buildEnvironment = getBuildEnvironment(connection, logger);
      List<String> gradleProjectJvmArgs = buildEnvironment.map(env -> env.getJava().getJvmArguments()).orElseGet(Collections::emptyList);
      Collection<String> jvmArgsForOverriding = !tcJvmArgs.isEmpty()
                                                ? jvmArgsMerger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs)
                                                : Collections.emptyList();

      BuildLauncher launcher = GradleBuildConfigurator.prepareBuildExecutor(gradleEnv, gradleParams, jvmArgsForOverriding, gradleTasks,
                                                                            buildLifecycleListener, logger, buildNumber, connection);

      String buildStartedMessage = composeBuildStartedMessage(buildNumber, gradleTasks, gradleParams, jvmArgsForOverriding, buildEnvironment.orElse(null));
      buildLifecycleListener.onStart(new BuildStartedEventImpl(System.currentTimeMillis(), buildStartedMessage));

      launcher.run(new ResultHandler<Void>() {
        @Override
        public void onComplete(Void unused) {
          buildLifecycleListener.onSuccess();
        }

        @Override
        public void onFailure(GradleConnectionException e) {
          buildLifecycleListener.onFail();
          throw e;
        }
      });
    }
  }

  @Nullable
  private static Properties getTeamCityBuildParameters(@NotNull File tcBuildParameters) {
    try {
      return GradleRunnerFileUtil.readProperties(tcBuildParameters);
    } catch (IOException e) {
      System.err.println("Couldn't read properties from: " + tcBuildParameters.getAbsolutePath());
      return null;
    }
  }

  @Nullable
  private static String getSystemEnvValue(@NotNull Map<String, String> systemEnv,
                                          @NotNull String systemEnvKey) {
    String value = systemEnv.get(systemEnvKey);
    if (value == null) {
      System.err.println("Source filename is not present in System.getenv(). Source filename: " + systemEnvKey);
      return null;
    }
    return value;
  }

  @Nullable
  private static List<String> readParams(@NotNull String paramsFilePath) {
    try {
      return GradleRunnerFileUtil.readParams(paramsFilePath);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      return null;
    }
  }

  @NotNull
  private static Optional<BuildEnvironment> getBuildEnvironment(@NotNull ProjectConnection connection,
                                                                @NotNull GradleToolingLogger logger) {
    try {
      return Optional.of(connection.getModel(BuildEnvironment.class));
    } catch (Throwable t) {
      logger.debug("Failed to obtain build environment from Gradle: " + t);
    }
    return Optional.empty();
  }

  private static String composeBuildStartedMessage(@NotNull String buildNumber,
                                                   @NotNull Collection<String> gradleTasks,
                                                   @NotNull Collection<String> gradleParams,
                                                   @NotNull Collection<String> overridedJvmArgs,
                                                   @Nullable BuildEnvironment buildEnvironment) {
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Starting Gradle in TeamCity build ").append(buildNumber).append(System.lineSeparator());
    messageBuilder.append("Gradle tasks: ").append(String.join(" ", gradleTasks)).append(System.lineSeparator());
    messageBuilder.append("Gradle arguments: ").append(String.join(" ", gradleParams));

    if (buildEnvironment != null) {
      try {
        String version = buildEnvironment.getGradle().getGradleVersion();
        String javaHome = buildEnvironment.getJava().getJavaHome().getAbsolutePath();
        String jvmArgsStr = !overridedJvmArgs.isEmpty()
                            ? String.join(" ", overridedJvmArgs)
                            : String.join(" ", buildEnvironment.getJava().getJvmArguments());
        messageBuilder.append(System.lineSeparator())
                      .append("Gradle version: ").append(version).append(System.lineSeparator())
                      .append("Gradle java home: ").append(javaHome).append(System.lineSeparator())
                      .append("Gradle jvm arguments: ").append(jvmArgsStr);
      } catch (Throwable ignore) {}
    }

    return messageBuilder.toString();
  }
}
