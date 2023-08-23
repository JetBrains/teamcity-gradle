package jetbrains.buildServer.gradle.runtime.service;

import java.util.StringJoiner;
import jetbrains.buildServer.gradle.runtime.listening.event.*;
import org.gradle.tooling.events.OperationDescriptor;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.task.TaskFinishEvent;
import org.gradle.tooling.events.task.TaskOperationResult;
import org.gradle.tooling.events.task.TaskProgressEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Converts events received from Gradle Tooling API to a TeamCity domain
 */
public class GradleProgressEventConverter {

  @Nullable
  public static BuildEvent createTaskNotificationEvent(@NotNull ProgressEvent event,
                                                       @NotNull String operationId) {
    if (event instanceof TaskProgressEvent) {
      return convertTaskProgressEvent((TaskProgressEvent)event, operationId);
    }
    return null;
  }

  @Nullable
  private static BuildEvent convertTaskProgressEvent(@NotNull TaskProgressEvent event,
                                                     @NotNull String operationId) {
    String eventId = createEventId(event.getDescriptor(), operationId);
    Long eventTime = event.getEventTime();
    String message = event.getDescriptor().getName();

    if (event instanceof org.gradle.tooling.events.task.TaskStartEvent) {
      return new TaskStartedEventImpl(eventId, eventTime, message);
    }

    if (event instanceof org.gradle.tooling.events.task.TaskFinishEvent) {
      TaskFinishEvent finishEvent = (TaskFinishEvent) event;
      TaskOperationResult result = finishEvent.getResult();
      EventResult eventResult = convertTaskProgressEventResult(result);
      if (eventResult != null) {
        return new TaskFinishedEventImpl(eventId, eventTime, message, eventResult);
      }
    }

    return null;
  }

  @Nullable
  private static EventResult convertTaskProgressEventResult(@NotNull OperationResult result) {
    if (result instanceof org.gradle.tooling.events.SuccessResult) {
      return new SuccessResultImpl();
    }
    if (result instanceof org.gradle.tooling.events.FailureResult) {
      org.gradle.tooling.events.FailureResult failureResult = (org.gradle.tooling.events.FailureResult) result;
      return new FailureResultImpl(detectFailureKind(failureResult));
    }
    if (result instanceof org.gradle.tooling.events.SkippedResult) {
      return new SkippedResultImpl();
    }

    return null;
  }

  @NotNull
  private static FailureKind detectFailureKind(@NotNull org.gradle.tooling.events.FailureResult failureResult) {
    if (failureResult.getFailures().stream().anyMatch(it -> isCompilationError(it.getDescription()))) {
      return FailureKind.COMPILATION;
    }
    return FailureKind.UNDEFINED;
  }

  private static boolean isCompilationError(@NotNull String description) {
    return description.contains("Compilation failed")
           || description.contains("Compilation error")
           || description.contains("compilation error")
           || description.contains("compiler failed");
  }

  @NotNull
  private static String createEventId(@NotNull OperationDescriptor descriptor, @NotNull String operationId) {
    StringJoiner joiner = new StringJoiner(" > ");
    joiner.add("[" + operationId + "]");
    OperationDescriptor currentDescriptor = descriptor;
    while (currentDescriptor != null) {
      joiner.add("[" + currentDescriptor.getDisplayName() + "]");
      currentDescriptor = currentDescriptor.getParent();
    }
    return joiner.toString();
  }
}
