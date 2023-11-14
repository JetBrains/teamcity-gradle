package jetbrains.buildServer.gradle.agent;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleLaunchModeSelectionResult {

  @NotNull
  private final GradleLaunchMode launchMode;
  @Nullable
  private final String reason;

  public GradleLaunchModeSelectionResult(@NotNull GradleLaunchMode launchMode,
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
}
