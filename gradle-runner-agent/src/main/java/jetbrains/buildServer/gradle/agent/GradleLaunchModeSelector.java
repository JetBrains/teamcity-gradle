package jetbrains.buildServer.gradle.agent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import jetbrains.buildServer.agent.BuildProgressLogger;
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
  public GradleLaunchModeSelectionResult selectMode(@NotNull Parameters parameters) {
    String configuredLaunchMode = ConfigurationParamsUtil.getGradleLaunchMode(parameters.getConfigurationParameters());
    GradleLaunchModeSelectionResult defaultMode = GradleLaunchModeSelectionResult.builder().withLaunchMode(GradleLaunchMode.COMMAND_LINE).build();
    DefaultGradleVersion gradleVersion = Optional.ofNullable(parameters.getProjectConnector())
                                                 .flatMap(connector -> getGradleVersion(connector, parameters.getLogger()))
                                                 .orElse(null);

    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_COMMAND_LINE_LAUNCH_MODE)) {
      return defaultMode;
    }

    if (configuredLaunchMode.equals(GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE)) {
      return GradleLaunchModeSelectionResult.builder()
                                            .withLaunchMode(GradleLaunchMode.TOOLING_API)
                                            .withReason(composeLaunchingViaToolingApiReason(configuredLaunchMode, false, false))
                                            .build();
    }

    return tryToIdentifyModeIndirectly(parameters, configuredLaunchMode, gradleVersion)
      .orElse(defaultMode);
  }

  private Optional<GradleLaunchModeSelectionResult> tryToIdentifyModeIndirectly(@NotNull Parameters parameters,
                                                                                @NotNull String configuredLaunchMode,
                                                                                @Nullable DefaultGradleVersion gradleVersion) {
    BuildProgressLogger logger = parameters.getLogger();
    if (!isVersionToolingApiCompatible(gradleVersion)) {
      return Optional.empty();
    }

    if (!parameters.isConfigurationCacheEnabled()) {
      return Optional.empty();
    }

    if (parameters.isConfigurationCacheProblemsIgnored()) {
      logger.warning(
        "The \"--configuration-cache-problems\" command line argument or the \"org.gradle.configuration-cache.problems\" property is set to \"warn\". " +
        "This causes the build to run in legacy mode with the non-functional configuration-cache when the Tooling API (required to utilize configuration-cache) detects a problem.\n" +
        "If you want to ensure the build uses configuration-cache, switch the argument/property to the default \"fail\" value.");
      return Optional.empty();
    }

    if (!parameters.getUnsupportedByToolingArgs().isEmpty()) {
      String message = String.format("This build can only be run in legacy mode. " +
                                     "Using configuration-cache requires launching the build via the Gradle Tooling API that is not compatible with the following Gradle argument(s): %s.\n" +
                                     "Remove the unsupported argument(s) or disable the configuration-cache.",
                                     String.join(", ", parameters.getUnsupportedByToolingArgs()));
      logger.warning(message);
      return Optional.empty();
    }

    return Optional.of(GradleLaunchModeSelectionResult.builder()
                                                      .withLaunchMode(GradleLaunchMode.TOOLING_API)
                                                      .withReason(composeLaunchingViaToolingApiReason(configuredLaunchMode, true, true))
                                                      .build());
  }

  private boolean isVersionToolingApiCompatible(@Nullable DefaultGradleVersion gradleVersion) {
    if (gradleVersion == null) {
      return false;
    }
    return VersionComparatorUtil.compare(gradleVersion.getVersion(), GRADLE_TOOLING_API_VERSION_FROM) >= 0;
  }

  @NotNull
  private String composeLaunchingViaToolingApiReason(@NotNull String configuredLaunchMode,
                                                     boolean reportVersionToolingCompatible,
                                                     boolean reportConfigurationCacheEnabled) {
    List<String> result = new ArrayList<>();

    if (!configuredLaunchMode.isEmpty()) {
      result.add(new StringBuilder().append("\"").append(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM).append("\"")
                                    .append(" configuration parameter")
                                    .append(" is set to ")
                                    .append("\"").append(configuredLaunchMode).append("\"").toString());
    }

    if (reportVersionToolingCompatible) {
      result.add(new StringBuilder().append("Gradle version is ").append(GRADLE_TOOLING_API_VERSION_FROM).append("+").toString());
    }

    if (reportConfigurationCacheEnabled) {
      result.add("Gradle's configuration-cache is enabled");
    }

    return !result.isEmpty() ? String.join(", ", result) : "unknown reason";
  }

  @NotNull
  private Optional<DefaultGradleVersion> getGradleVersion(@NotNull GradleConnector projectConnector,
                                                          @NotNull BuildProgressLogger logger) {
    try (ProjectConnection connection = projectConnector.connect()) {
      BuildEnvironment buildEnvironment = connection.getModel(BuildEnvironment.class);

      String gradleVersionStr = buildEnvironment.getGradle().getGradleVersion();
      if (gradleVersionStr == null) {
        logger.warning("Couldn't detect the Gradle version in the project: null value");
        return Optional.empty();
      }

      return Optional.of(DefaultGradleVersion.version(gradleVersionStr));
    } catch (Throwable t) {
      logger.warning("Couldn't detect the Gradle version in the project: " + t.getMessage());
      return Optional.empty();
    }
  }

  public static class Parameters {
    @NotNull
    private final BuildProgressLogger logger;
    @NotNull
    private final Map<String, String> configurationParameters;
    @Nullable
    private final GradleConnector projectConnector;
    private final boolean configurationCacheEnabled;
    private final boolean configurationCacheProblemsIgnored;
    @NotNull
    private final Set<String> unsupportedByToolingArgs;

    private Parameters(@NotNull BuildProgressLogger logger,
                       @NotNull Map<String, String> configurationParameters,
                       @Nullable GradleConnector projectConnector,
                       boolean configurationCacheEnabled,
                       boolean configurationCacheProblemsIgnored,
                       @NotNull Set<String> unsupportedByToolingArgs) {
      this.logger = logger;
      this.configurationParameters = Collections.unmodifiableMap(configurationParameters);
      this.projectConnector = projectConnector;
      this.configurationCacheEnabled = configurationCacheEnabled;
      this.configurationCacheProblemsIgnored = configurationCacheProblemsIgnored;
      this.unsupportedByToolingArgs = Collections.unmodifiableSet(unsupportedByToolingArgs);
    }

    @NotNull
    public BuildProgressLogger getLogger() {
      return logger;
    }

    @NotNull
    public Map<String, String> getConfigurationParameters() {
      return configurationParameters;
    }

    @Nullable
    public GradleConnector getProjectConnector() {
      return projectConnector;
    }

    public boolean isConfigurationCacheEnabled() {
      return configurationCacheEnabled;
    }

    public boolean isConfigurationCacheProblemsIgnored() {
      return configurationCacheProblemsIgnored;
    }

    @NotNull
    public Set<String> getUnsupportedByToolingArgs() {
      return unsupportedByToolingArgs;
    }

    @NotNull
    public static Builder builder() {
      return new Builder();
    }

    public static final class Builder {
      private BuildProgressLogger logger;
      private Map<String, String> configurationParameters;
      private GradleConnector projectConnector;
      private boolean configurationCacheEnabled;
      private boolean configurationCacheProblemsIgnored;
      private Set<String> unsupportedByToolingArgs;

      private Builder() {
      }

      public Builder withLogger(@NotNull BuildProgressLogger logger) {
        this.logger = logger;
        return this;
      }

      public Builder withConfigurationParameters(@NotNull Map<String, String> configurationParameters) {
        this.configurationParameters = configurationParameters;
        return this;
      }

      public Builder withProjectConnector(@Nullable GradleConnector projectConnector) {
        this.projectConnector = projectConnector;
        return this;
      }

      public Builder withConfigurationCacheEnabled(boolean configurationCacheEnabled) {
        this.configurationCacheEnabled = configurationCacheEnabled;
        return this;
      }

      public Builder withConfigurationCacheProblemsIgnored(boolean configurationCacheProblemsIgnored) {
        this.configurationCacheProblemsIgnored = configurationCacheProblemsIgnored;
        return this;
      }

      public Builder withUnsupportedByToolingArgs(Set<String> unsupportedByToolingArgs) {
        this.unsupportedByToolingArgs = unsupportedByToolingArgs;
        return this;
      }

      @NotNull
      public Parameters build() {
        return new Parameters(logger, configurationParameters, projectConnector, configurationCacheEnabled, configurationCacheProblemsIgnored,
                              unsupportedByToolingArgs);
      }
    }
  }
}
