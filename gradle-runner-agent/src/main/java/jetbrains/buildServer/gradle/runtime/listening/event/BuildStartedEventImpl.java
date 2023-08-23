package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public class BuildStartedEventImpl extends AbstractBuildEvent implements BuildStartedEvent {

  public BuildStartedEventImpl(long eventTimestamp, @NotNull String message) {
    super(null, eventTimestamp, message);
  }
}
