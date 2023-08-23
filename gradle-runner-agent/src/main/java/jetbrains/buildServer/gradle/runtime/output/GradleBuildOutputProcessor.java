package jetbrains.buildServer.gradle.runtime.output;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jetbrains.buildServer.gradle.runtime.listening.BuildEventListener;
import jetbrains.buildServer.gradle.runtime.listening.event.*;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.messages.serviceMessages.CompilationFinished;
import jetbrains.buildServer.messages.serviceMessages.CompilationStarted;
import jetbrains.buildServer.messages.serviceMessages.Message;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Processes output of the build process
 */
public class GradleBuildOutputProcessor implements BuildEventListener {

  /**
   * In string "> Task :compileJava" the last whitespace index is 6.
   * So we can parse task name from it.
   * But if the input string is "> Task :compileJava FAILED",
   * the last whitespace index is greater than 6, so we should consider it while parsing task name.
   */
  private static final int TASK_NAME_LAST_WHITESPACE_INDEX = 6;

  private final GradleToolingLogger logger;
  private volatile String currentTask = null;
  private final Map<String, StringBuilder> tasksErrorOutput;
  private final List<BuildFailedTaskData> failedTasks;

  public GradleBuildOutputProcessor(GradleToolingLogger logger) {
    this.logger = logger;
    tasksErrorOutput = new ConcurrentHashMap<>();
    failedTasks = new ArrayList<>();
  }

  @Override
  public void onEvent(@NotNull BuildEvent event) {
    if (event instanceof TaskStartedEvent) {
      tasksErrorOutput.put(event.getMessage(), new StringBuilder());
    } else if (event instanceof TaskFinishedEvent) {
      TaskFinishedEvent taskFinishedEvent = (TaskFinishedEvent) event;
      if (taskFinishedEvent.getResult() instanceof FailureResult) {
        FailureResult failureResult = (FailureResult) taskFinishedEvent.getResult();
        BuildFailedTaskData failedTask = new BuildFailedTaskData(taskFinishedEvent.getMessage(), failureResult.getFailureKind());
        failedTasks.add(failedTask);
      }
    } else if (event instanceof TaskOutputEvent) {
      TaskOutputEvent outputEvent = (TaskOutputEvent) event;
      append(event.getMessage(), outputEvent.getOutputType());
    } else if (event instanceof BuildFinishedEvent) {
      BuildFinishedEvent buildFinishedEvent = (BuildFinishedEvent) event;
      switch (buildFinishedEvent.getResult()) {
        case SUCCEEDED:
          return;
        case FAILED:
          collectFailureMessages();
          processFailedTasks();
      }
    }
  }

  private void append(@NotNull String line, @NotNull OutputType outputType) {
    if (line.isEmpty()) return;

    if (line.startsWith("> Task :")) {
      currentTask = parseTaskName(line);
    }
    else if (line.startsWith("> Configure") ||
             line.startsWith("FAILURE: ") ||
             line.startsWith("CONFIGURE SUCCESSFUL") ||
             line.startsWith("BUILD SUCCESSFUL") ||
             line.endsWith("storing the configuration cache.")) {
      currentTask = null;
    }

    if (currentTask != null && OutputType.STD_ERR == outputType) {
      appendToTaskOutput(line);
    }
  }

  @Nullable
  private String parseTaskName(@NotNull String line) {
    try {
      int lastSpaceIndex = line.lastIndexOf(' ');
      return lastSpaceIndex > TASK_NAME_LAST_WHITESPACE_INDEX
             ? line.substring("> Task ".length(), lastSpaceIndex).trim()
             : line.substring("> Task ".length()).trim();
    } catch (Throwable t) {
      logger.warn("Couldn't parse Gradle task name from line: " + line);
      return null;
    }
  }

  private void appendToTaskOutput(@NotNull String line) {
    tasksErrorOutput.getOrDefault(currentTask, new StringBuilder()).append(line);
  }

  private void collectFailureMessages() {
    failedTasks.forEach(failedTask -> {
      failedTask.setFailureMessages(getTaskMessages(failedTask.getTaskPath()));
    });
  }

  @NotNull
  private List<String> getTaskMessages(@NotNull String taskPath) {
    return Optional.ofNullable(tasksErrorOutput.get(taskPath))
                   .map(taskOutput -> Arrays.stream(taskOutput.toString().split("\\r?\\n"))
                                            .collect(Collectors.toList()))
                   .orElseGet(Collections::emptyList);
  }

  private void processFailedTasks() {
    failedTasks.forEach(failedTask -> {
      String taskPath = failedTask.getTaskPath();
      Collection<String> errors = failedTask.getFailureMessages();
      if (failedTask.getFailureKind() == FailureKind.COMPILATION) {
        reportCompilationError(taskPath, errors);
      }
    });
  }

  private void reportCompilationError(@NotNull String taskPath,
                                      @NotNull Collection<String> errors) {
    logger.lifecycle(internalize(new CompilationStarted(taskPath)).asString());
    errors.forEach(error -> logger.lifecycle(internalize(new Message(error, "ERROR", null)).asString()));
    logger.lifecycle(internalize(new CompilationFinished(taskPath)).asString());
  }

  @NotNull
  private ServiceMessage internalize(@NotNull final ServiceMessage message) {
    message.addTag(DefaultMessagesInfo.TAG_INTERNAL);
    return message;
  }
}
