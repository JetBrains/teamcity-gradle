package jetbrains.buildServer.gradle.runtime.listening.event;

import org.jetbrains.annotations.NotNull;

public interface BuildFinishedEvent extends BuildEvent {

  @NotNull
  BuildResult getResult();
}