package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.io.File;
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
  private final File buildTempDir;
  @NotNull
  private final Map<String, String> runnerParameters;
  @NotNull
  private final File pluginsDirectory;
  @NotNull
  private final String gradleOpts;
  @NotNull
  private final List<String> gradleTasks;
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
  private final File checkoutDirectory;
  @NotNull
  private final File workingDirectory;
  @NotNull
  private final List<String> initialGradleParams;
  @NotNull
  private final String exePath;
  @Nullable
  private final GradleLaunchModeSelectionResult launchModeSelectionResult;

  private GradleCommandLineComposerParameters(@NotNull Map<String, String> env,
                                              @NotNull File buildTempDir,
                                              @NotNull Map<String, String> runnerParameters,
                                              @NotNull File pluginsDirectory,
                                              @NotNull String gradleOpts,
                                              @NotNull List<String> gradleTasks,
                                              boolean configurationCacheEnabled,
                                              @NotNull Map<String, String> configParameters,
                                              @NotNull BuildProgressLogger logger,
                                              @NotNull BuildRunnerContext runnerContext,
                                              @NotNull String javaHome,
                                              @NotNull File checkoutDirectory,
                                              @NotNull File workingDirectory,
                                              @NotNull List<String> initialGradleParams,
                                              @NotNull String exePath,
                                              @Nullable GradleLaunchModeSelectionResult launchModeSelectionResult) {
    this.env = env;
    this.buildTempDir = buildTempDir;
    this.runnerParameters = runnerParameters;
    this.pluginsDirectory = pluginsDirectory;
    this.gradleOpts = gradleOpts;
    this.gradleTasks = gradleTasks;
    this.configurationCacheEnabled = configurationCacheEnabled;
    this.configParameters = configParameters;
    this.logger = logger;
    this.runnerContext = runnerContext;
    this.javaHome = javaHome;
    this.checkoutDirectory = checkoutDirectory;
    this.workingDirectory = workingDirectory;
    this.initialGradleParams = initialGradleParams;
    this.exePath = exePath;
    this.launchModeSelectionResult = launchModeSelectionResult;
  }

  @NotNull
  public Map<String, String> getEnv() {
    return env;
  }

  @NotNull
  public File getBuildTempDir() {
    return buildTempDir;
  }

  @NotNull
  public Map<String, String> getRunnerParameters() {
    return runnerParameters;
  }

  @NotNull
  public File getPluginsDirectory() {
    return pluginsDirectory;
  }

  @NotNull
  public String getGradleOpts() {
    return gradleOpts;
  }

  @NotNull
  public List<String> getGradleTasks() {
    return gradleTasks;
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
  public File getCheckoutDirectory() {
    return checkoutDirectory;
  }

  @NotNull
  public File getWorkingDirectory() {
    return workingDirectory;
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
    private File buildTempDir;
    private Map<String, String> runnerParameters;
    private File pluginsDirectory;
    private String gradleOpts;
    private List<String> gradleTasks;
    private boolean configurationCacheEnabled;
    private Map<String, String> configParameters;
    private BuildProgressLogger logger;
    private BuildRunnerContext runnerContext;
    private String javaHome;
    private File checkoutDirectory;
    private File workingDirectory;
    private List<String> initialGradleParams;
    private String exePath;
    private GradleLaunchModeSelectionResult launchModeSelectionResult;

    private Builder() {
    }

    public Builder withEnv(@NotNull Map<String, String> env) {
      this.env = env;
      return this;
    }

    public Builder withBuildTempDir(@NotNull File buildTempDir) {
      this.buildTempDir = buildTempDir;
      return this;
    }

    public Builder withRunnerParameters(@NotNull Map<String, String> runnerParameters) {
      this.runnerParameters = runnerParameters;
      return this;
    }

    public Builder withPluginsDirectory(@NotNull File pluginsDirectory) {
      this.pluginsDirectory = pluginsDirectory;
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

    public Builder withCheckoutDirectory(@NotNull File checkoutDirectory) {
      this.checkoutDirectory = checkoutDirectory;
      return this;
    }

    public Builder withWorkingDirectory(@NotNull File workingDirectory) {
      this.workingDirectory = workingDirectory;
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
      return new GradleCommandLineComposerParameters(env, buildTempDir, runnerParameters, pluginsDirectory, gradleOpts,
                                                     gradleTasks, configurationCacheEnabled, configParameters, logger, runnerContext,
                                                     javaHome, checkoutDirectory, workingDirectory, initialGradleParams, exePath,
                                                     launchModeSelectionResult);
    }
  }
}
