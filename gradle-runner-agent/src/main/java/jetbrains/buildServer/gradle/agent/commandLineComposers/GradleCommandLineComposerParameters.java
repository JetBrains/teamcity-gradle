package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelectionResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleCommandLineComposerParameters {
  @NotNull
  private final Map<String, String> env;
  @NotNull
  private final Path buildTempDir;
  @NotNull
  private final Map<String, String> runnerParameters;
  @NotNull
  private final Path pluginsDir;
  @NotNull
  private final String gradleOpts;
  @NotNull
  private final List<String> gradleTasks;
  @NotNull
  private final List<String> gradleUserDefinedParams;
  private final boolean configurationCacheEnabled;
  @NotNull
  private final Map<String, String> configParameters;
  @NotNull
  private final BuildProgressLogger logger;
  @NotNull
  private final BuildRunnerContext runnerContext;
  @NotNull
  private final String javaHome;
  @NotNull
  private final Path checkoutDir;
  @NotNull
  private final Path workingDir;
  @NotNull
  private final List<String> initialGradleParams;
  @NotNull
  private final String exePath;
  @Nullable
  private final GradleLaunchModeSelectionResult launchModeSelectionResult;

  private GradleCommandLineComposerParameters(@NotNull Map<String, String> env,
                                              @NotNull Path buildTempDir,
                                              @NotNull Map<String, String> runnerParameters,
                                              @NotNull Path pluginsDir,
                                              @NotNull String gradleOpts,
                                              @NotNull List<String> gradleTasks,
                                              @NotNull List<String> gradleUserDefinedParams,
                                              boolean configurationCacheEnabled,
                                              @NotNull Map<String, String> configParameters,
                                              @NotNull BuildProgressLogger logger,
                                              @NotNull BuildRunnerContext runnerContext,
                                              @NotNull String javaHome,
                                              @NotNull Path checkoutDir,
                                              @NotNull Path workingDir,
                                              @NotNull List<String> initialGradleParams,
                                              @NotNull String exePath,
                                              @Nullable GradleLaunchModeSelectionResult launchModeSelectionResult) {
    this.env = env;
    this.buildTempDir = buildTempDir;
    this.runnerParameters = runnerParameters;
    this.pluginsDir = pluginsDir;
    this.gradleOpts = gradleOpts;
    this.gradleTasks = gradleTasks;
    this.gradleUserDefinedParams = gradleUserDefinedParams;
    this.configurationCacheEnabled = configurationCacheEnabled;
    this.configParameters = configParameters;
    this.logger = logger;
    this.runnerContext = runnerContext;
    this.javaHome = javaHome;
    this.checkoutDir = checkoutDir;
    this.workingDir = workingDir;
    this.initialGradleParams = initialGradleParams;
    this.exePath = exePath;
    this.launchModeSelectionResult = launchModeSelectionResult;
  }

  @NotNull
  public Map<String, String> getEnv() {
    return env;
  }

  @NotNull
  public Path getBuildTempDir() {
    return buildTempDir;
  }

  @NotNull
  public Map<String, String> getRunnerParameters() {
    return runnerParameters;
  }

  @NotNull
  public Path getPluginsDir() {
    return pluginsDir;
  }

  @NotNull
  public String getGradleOpts() {
    return gradleOpts;
  }

  @NotNull
  public List<String> getGradleTasks() {
    return gradleTasks;
  }

  @NotNull
  public List<String> getGradleUserDefinedParams() {
    return gradleUserDefinedParams;
  }

  public boolean isConfigurationCacheEnabled() {
    return configurationCacheEnabled;
  }

  @NotNull
  public Map<String, String> getConfigParameters() {
    return configParameters;
  }

  @NotNull
  public BuildProgressLogger getLogger() {
    return logger;
  }

  @NotNull
  public BuildRunnerContext getRunnerContext() {
    return runnerContext;
  }

  @NotNull
  public String getJavaHome() {
    return javaHome;
  }

  @NotNull
  public Path getCheckoutDir() {
    return checkoutDir;
  }

  @NotNull
  public Path getWorkingDir() {
    return workingDir;
  }

  @NotNull
  public List<String> getInitialGradleParams() {
    return initialGradleParams;
  }

  @NotNull
  public String getExePath() {
    return exePath;
  }

  @NotNull
  public Optional<GradleLaunchModeSelectionResult> getLaunchModeSelectionResult() {
    return Optional.ofNullable(launchModeSelectionResult);
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private Map<String, String> env;
    private Path buildTempDir;
    private Map<String, String> runnerParameters;
    private Path pluginsDir;
    private String gradleOpts;
    private List<String> gradleTasks;
    private List<String> gradleUserDefinedParams;
    private boolean configurationCacheEnabled;
    private Map<String, String> configParameters;
    private BuildProgressLogger logger;
    private BuildRunnerContext runnerContext;
    private String javaHome;
    private Path checkoutDir;
    private Path workingDir;
    private List<String> initialGradleParams;
    private String exePath;
    private GradleLaunchModeSelectionResult launchModeSelectionResult;

    private Builder() {
    }

    public Builder withEnv(@NotNull Map<String, String> env) {
      this.env = env;
      return this;
    }

    public Builder withBuildTempDir(@NotNull Path buildTempDir) {
      this.buildTempDir = toNormalizedAbsolutePath(buildTempDir);
      return this;
    }

    public Builder withRunnerParameters(@NotNull Map<String, String> runnerParameters) {
      this.runnerParameters = runnerParameters;
      return this;
    }

    public Builder withPluginsDir(@NotNull Path pluginsDir) {
      this.pluginsDir = toNormalizedAbsolutePath(pluginsDir);
      return this;
    }

    public Builder withGradleOpts(@NotNull String gradleOpts) {
      this.gradleOpts = gradleOpts;
      return this;
    }

    public Builder withGradleTasks(@NotNull List<String> gradleTasks) {
      this.gradleTasks = gradleTasks;
      return this;
    }

    public Builder withGradleUserDefinedParams(@NotNull List<String> gradleUserDefinedParams) {
      this.gradleUserDefinedParams = gradleUserDefinedParams;
      return this;
    }

    public Builder withConfigurationCacheEnabled(boolean configurationCacheEnabled) {
      this.configurationCacheEnabled = configurationCacheEnabled;
      return this;
    }

    public Builder withConfigParameters(@NotNull Map<String, String> configParameters) {
      this.configParameters = configParameters;
      return this;
    }

    public Builder withLogger(@NotNull BuildProgressLogger logger) {
      this.logger = logger;
      return this;
    }

    public Builder withRunnerContext(@NotNull BuildRunnerContext runnerContext) {
      this.runnerContext = runnerContext;
      return this;
    }

    public Builder withJavaHome(@NotNull String javaHome) {
      this.javaHome = javaHome;
      return this;
    }

    public Builder withCheckoutDir(@NotNull Path checkoutDir) {
      this.checkoutDir = toNormalizedAbsolutePath(checkoutDir);
      return this;
    }

    public Builder withWorkingDir(@NotNull Path workingDir) {
      this.workingDir = toNormalizedAbsolutePath(workingDir);
      return this;
    }

    public Builder withInitialGradleParams(@NotNull List<String> initialGradleParams) {
      this.initialGradleParams = initialGradleParams;
      return this;
    }

    public Builder withExePath(@NotNull String exePath) {
      this.exePath = exePath;
      return this;
    }

    public Builder withLaunchModeSelectionResult(@Nullable GradleLaunchModeSelectionResult launchModeSelectionResult) {
      this.launchModeSelectionResult = launchModeSelectionResult;
      return this;
    }

    @NotNull
    public GradleCommandLineComposerParameters build() {
      return new GradleCommandLineComposerParameters(env, buildTempDir, runnerParameters, pluginsDir, gradleOpts,
                                                     gradleTasks, gradleUserDefinedParams, configurationCacheEnabled, configParameters, logger, runnerContext,
                                                     javaHome, checkoutDir, workingDir, initialGradleParams, exePath,
                                                     launchModeSelectionResult);
    }

    @NotNull
    private Path toNormalizedAbsolutePath(@NotNull Path path) {
      return path.toAbsolutePath().normalize();
    }
  }
}
