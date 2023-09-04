package jetbrains.buildServer.gradle.runtime;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.ResultHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR;

public class TeamCityGradleLauncher {

  public static void main(String[] args) {
    final Map<String, String> systemEnv = System.getenv();

    String envFilePath = getSystemEnvValue(systemEnv, GradleRunnerConstants.GRADLE_LAUNCHER_ENV_FILE);
    if (envFilePath == null) {
      return;
    }
    final Map<String, String> gradleEnv = readParamsMap(envFilePath);
    if (gradleEnv == null) {
      return;
    }
    try {
      gradleEnv.put("TEAMCITY_BUILD_INIT_PATH", GradleBuildConfigurator.getInitScriptClasspath());
    } catch (IOException e) {
      System.err.println("Couldn't launch Gradle via Tooling API: error while trying to build init script classpath");
      return;
    }

    String gradleParamsFilePath = getSystemEnvValue(systemEnv, GradleRunnerConstants.GRADLE_PARAMS_FILE);
    if (gradleParamsFilePath == null) {
      return;
    }
    final List<String> gradleParams = readParams(gradleParamsFilePath);
    if (gradleParams == null) {
      return;
    }

    String jvmArgsFilePath = getSystemEnvValue(systemEnv, GradleRunnerConstants.GRADLE_JVM_PARAMS_FILE);
    if (jvmArgsFilePath == null) {
      return;
    }
    final List<String> jvmArgs = readParams(jvmArgsFilePath);
    if (jvmArgs == null) {
      return;
    }

    String gradleTasksPath = getSystemEnvValue(systemEnv, GradleRunnerConstants.GRADLE_TASKS_FILE);
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

    String tcBuildParametersPath = GradleBuildConfigurator.getTeamCityBuildParametersPath(gradleEnv);
    Properties teamCityBuildParameters = getTeamCityBuildParameters(tcBuildParametersPath);
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
    String taskOutputDir = buildTempDir + File.separator + BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR;
    BuildContext buildContext = new BuildContext(tcBuildParametersPath, taskOutputDir, envFilePath, gradleParamsFilePath, jvmArgsFilePath, gradleTasksPath);
    List<BuildEventListener > eventListeners = new ArrayList<>();
    eventListeners.add(new GradleBuildOutputProcessor(logger, buildContext));
    BuildLifecycleListener buildLifecycleListener = new GradleBuildLifecycleListener(logger, eventListeners, buildContext);

    try (ProjectConnection connection = connector.connect()) {
      BuildLauncher launcher = GradleBuildConfigurator.prepareBuildExecutor(gradleEnv, gradleParams, jvmArgs, gradleTasks, buildLifecycleListener,
                                                                            logger, buildNumber, connection);

      String buildStartedMessage = String.format("%s %s", "Starting Gradle in TeamCity build", buildNumber);
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
  private static Properties getTeamCityBuildParameters(@NotNull String tcBuildParametersPath) {
    try {
      return GradleRunnerFileUtil.readProperties(new File(tcBuildParametersPath));
    } catch (IOException e) {
      System.err.println("Couldn't read properties from: " + tcBuildParametersPath);
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
  private static Map<String, String> readParamsMap(@NotNull String paramsFilePath) {
    try {
      return GradleRunnerFileUtil.readParamsMap(paramsFilePath);
    } catch (IOException e) {
      System.err.println(e.getMessage());
      return null;
    }
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
}
