package jetbrains.buildServer.gradle.runtime.listening.event;

import jetbrains.buildServer.gradle.runtime.output.OutputType;
import org.jetbrains.annotations.NotNull;

public class TaskOutputEventImpl extends AbstractBuildEvent implements TaskOutputEvent {

  private final OutputType myOutputType;

  public TaskOutputEventImpl(long eventTimestamp, @NotNull String message, @NotNull OutputType outputType) {
    super(null, eventTimestamp, message);
    myOutputType = outputType;
  }

  @Override
  @NotNull
  public OutputType getOutputType() {
    return myOutputType;
  }
}
