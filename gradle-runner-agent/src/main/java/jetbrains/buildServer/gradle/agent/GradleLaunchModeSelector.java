package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.runtime.service.DistributionFactoryExtension;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.internal.DefaultGradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Selects the mode in which the build will run: with Gradle directly or with Gradle Tooling API
 */
public class GradleLaunchModeSelector {

  private static final String GRADLE_TOOLING_API_VERSION_FROM = "8.0";

  @NotNull
  public static GradleLaunchModeSelectionResult selectMode(@NotNull File workingDirectory,
                                                           @NotNull Boolean useWrapper,
                                                           @NotNull Map<String, String> configurationParameters,
                                                           @Nullable File gradleHome,
                                                           @Nullable File gradleWrapperProperties) throws RunBuildException {
    String configuredLaunchMode = ConfigurationParamsUtil.getGradleLaunchMode(configurationParameters);

    // currently we default to launching Gradle build in the old way when the launch mode is not configured with the appropriate config param
    if (configuredLaunchMode.isEmpty()) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
    }

    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_GRADLE_LAUNCH_MODE)) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
    }
    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE)) {
      String reason = String.format("%s configuration parameter is set to %s",
                                    GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM,
                                    GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE_TOOLING_API, reason);
    }

    GradleConnector connector = GradleConnector.newConnector();
    connector.forProjectDirectory(workingDirectory);

    if (useWrapper) {
      if (gradleWrapperProperties == null) {
        throw new RunBuildException("Couldn't select launch mode. " +
                                    "gradle-wrapper.properties must be present in the project when Gradle Wrapper build mode selected");
      }
      DistributionFactoryExtension.setWrappedDistribution(connector, gradleWrapperProperties.getAbsolutePath());
    } else {
      if (gradleHome == null) {
        throw new RunBuildException("Couldn't select launch mode. " +
                                    "gradleHome must be present in the project when build mode with Gradle Home selected");
      }
      connector.useInstallation(gradleHome);
    }

    try (ProjectConnection connection = connector.connect()) {
      BuildEnvironment buildEnvironment;
      try {
        buildEnvironment = connection.getModel(BuildEnvironment.class);
      } catch (Throwable t) {
        return new GradleLaunchModeSelectionResult(GradleLaunchMode.UNDEFINED, null);
      }

      String gradleVersion = buildEnvironment.getGradle().getGradleVersion();
      return getByGradleVersion(gradleVersion, configuredLaunchMode);
    }
  }

  @NotNull
  public static GradleLaunchModeSelectionResult getByGradleVersion(@Nullable String gradleVersionStr,
                                                                   @NotNull String configuredLaunchMode) {
    if (gradleVersionStr == null) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.UNDEFINED, null);
    }

    DefaultGradleVersion gradleVersion;
    try {
      gradleVersion = DefaultGradleVersion.version(gradleVersionStr);
    } catch (IllegalArgumentException e) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.UNDEFINED, null);
    }

    return VersionComparatorUtil.compare(gradleVersion.getVersion(), GRADLE_TOOLING_API_VERSION_FROM) >= 0
           ? new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE_TOOLING_API, composeLaunchingViaToolingApiReason(configuredLaunchMode))
           : new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
  }

  private static String composeLaunchingViaToolingApiReason(@NotNull String configuredLaunchMode) {
    StringBuilder result = new StringBuilder();
    result.append("Gradle version is 8.0+ ").append(GRADLE_TOOLING_API_VERSION_FROM);
    if (!configuredLaunchMode.isEmpty()) {
      result.append(" and ")
            .append(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM)
            .append(" configuration parameter")
            .append(" is set to ")
            .append(configuredLaunchMode);
    }
    return result.toString();
  }
}
