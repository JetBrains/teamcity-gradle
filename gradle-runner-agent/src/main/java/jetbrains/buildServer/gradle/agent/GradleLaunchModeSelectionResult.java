package jetbrains.buildServer.gradle.agent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleLaunchModeSelectionResult {

  @NotNull
  private final GradleLaunchMode launchMode;
  @Nullable
  private final String reason;

  private GradleLaunchModeSelectionResult(@NotNull GradleLaunchMode launchMode,
                                          @Nullable String reason) {
    this.launchMode = launchMode;
    this.reason = reason;
  }

  @NotNull
  public GradleLaunchMode getLaunchMode() {
    return launchMode;
  }

  @Nullable
  public String getReason() {
    return reason;
  }

  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private GradleLaunchMode launchMode;
    private String reason;

    private Builder() {
    }

    public Builder withLaunchMode(@NotNull GradleLaunchMode launchMode) {
      this.launchMode = launchMode;
      return this;
    }

    public Builder withReason(@Nullable String reason) {
      this.reason = reason;
      return this;
    }

    @NotNull
    public GradleLaunchModeSelectionResult build() {
      return new GradleLaunchModeSelectionResult(launchMode, reason);
    }
  }
}
