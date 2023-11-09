package jetbrains.buildServer.gradle.agent;

import java.util.Map;
import java.util.Optional;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
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
  public static GradleLaunchModeSelectionResult selectMode(@NotNull Map<String, String> configurationParameters,
                                                           @Nullable GradleConnector projectConnector) {
    String configuredLaunchMode = ConfigurationParamsUtil.getGradleLaunchMode(configurationParameters);

    // currently we default to launching Gradle build in the old way when the launch mode is not configured with the appropriate config param
    if (configuredLaunchMode.isEmpty()) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
    }

    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_GRADLE_LAUNCH_MODE)) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
    }
    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE)) {
      return new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE_TOOLING_API, composeLaunchingViaToolingApiReason(configuredLaunchMode, false));
    }

    return Optional.ofNullable(projectConnector)
                   .flatMap(connector -> getGradleVersion(connector))
                   .map(gradleVersion -> getByGradleVersion(gradleVersion, configuredLaunchMode))
                   .orElse(new GradleLaunchModeSelectionResult(GradleLaunchMode.UNDEFINED, null));
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
           ? new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE_TOOLING_API, composeLaunchingViaToolingApiReason(configuredLaunchMode, true))
           : new GradleLaunchModeSelectionResult(GradleLaunchMode.GRADLE, null);
  }

  @NotNull
  private static String composeLaunchingViaToolingApiReason(@NotNull String configuredLaunchMode,
                                                            boolean gradleVersionToolingCompatible) {
    StringBuilder result = new StringBuilder();
    if (!configuredLaunchMode.isEmpty()) {
      result.append("\"").append(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM).append("\"")
            .append(" configuration parameter")
            .append(" is set to ")
            .append("\"").append(configuredLaunchMode).append("\"");
    }

    if (gradleVersionToolingCompatible) {
      result.append(" and ")
            .append("Gradle version is ").append(GRADLE_TOOLING_API_VERSION_FROM).append("+");
    }

    return result.length() > 0 ? result.toString() : "unknown reason";
  }

  @NotNull
  private static Optional<String> getGradleVersion(@NotNull GradleConnector projectConnector) {
    try (ProjectConnection connection = projectConnector.connect()) {
      BuildEnvironment buildEnvironment;
      try {
        buildEnvironment = connection.getModel(BuildEnvironment.class);
      } catch (Throwable t) {
        return Optional.empty();
      }

      return Optional.of(buildEnvironment.getGradle().getGradleVersion());
    }
  }
}
