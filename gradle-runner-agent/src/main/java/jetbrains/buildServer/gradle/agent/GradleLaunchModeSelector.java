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
  public static GradleLaunchMode selectMode(@NotNull File workingDirectory,
                                            @NotNull Boolean useWrapper,
                                            @NotNull Map<String, String> configurationParameters,
                                            @Nullable File gradleHome,
                                            @Nullable File gradleWrapperProperties) throws RunBuildException {
    String configuredLaunchMode = ConfigurationParamsUtil.getGradleLaunchMode(configurationParameters);

    // currently we default to launching Gradle build in the old way when the launch mode is not configured with the appropriate config param
    if (configuredLaunchMode.isEmpty()) {
      return GradleLaunchMode.GRADLE;
    }

    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_GRADLE_LAUNCH_MODE)) {
      return GradleLaunchMode.GRADLE;
    }
    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE)) {
      return GradleLaunchMode.GRADLE_TOOLING_API;
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
        return GradleLaunchMode.UNDEFINED;
      }

      String gradleVersion = buildEnvironment.getGradle().getGradleVersion();
      return getByGradleVersion(gradleVersion);
    }
  }

  @NotNull
  public static GradleLaunchMode getByGradleVersion(@Nullable String gradleVersionStr) {
    if (gradleVersionStr == null) {
      return GradleLaunchMode.UNDEFINED;
    }

    DefaultGradleVersion gradleVersion;
    try {
      gradleVersion = DefaultGradleVersion.version(gradleVersionStr);
    } catch (IllegalArgumentException e) {
      return GradleLaunchMode.UNDEFINED;
    }

    return VersionComparatorUtil.compare(gradleVersion.getVersion(), GRADLE_TOOLING_API_VERSION_FROM) >= 0
           ? GradleLaunchMode.GRADLE_TOOLING_API
           : GradleLaunchMode.GRADLE;
  }
}
