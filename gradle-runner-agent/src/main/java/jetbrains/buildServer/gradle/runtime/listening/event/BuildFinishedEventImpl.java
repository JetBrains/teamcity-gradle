package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public class BuildFinishedEventImpl extends AbstractBuildEvent implements BuildFinishedEvent {

  private final BuildResult myBuildResult;

  public BuildFinishedEventImpl(long eventTimestamp, @NotNull String message, @NotNull BuildResult buildResult) {
    super(null, eventTimestamp, message);
    myBuildResult = buildResult;
  }

  @NotNull
  @Override
  public BuildResult getResult() {
    return myBuildResult;
  }
}
