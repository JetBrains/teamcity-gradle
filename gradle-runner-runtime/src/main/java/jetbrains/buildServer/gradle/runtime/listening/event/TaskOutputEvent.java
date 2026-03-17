package jetbrains.buildServer.gradle.runtime.listening.event;

import jetbrains.buildServer.gradle.runtime.output.OutputType;
import org.jetbrains.annotations.NotNull;

public interface TaskOutputEvent extends BuildEvent {

  @NotNull
  OutputType getOutputType();
}
