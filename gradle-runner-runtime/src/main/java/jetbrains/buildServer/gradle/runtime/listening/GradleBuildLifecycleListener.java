package jetbrains.buildServer.gradle.runtime.listening;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Stream;
import jetbrains.buildServer.gradle.runtime.BuildContext;
import jetbrains.buildServer.gradle.runtime.listening.event.BuildEvent;
import jetbrains.buildServer.gradle.runtime.listening.event.BuildFinishedEventImpl;
import jetbrains.buildServer.gradle.runtime.listening.event.BuildResult;
import jetbrains.buildServer.gradle.runtime.listening.event.TaskOutputEvent;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.gradle.agent.propertySplit.SplitPropertiesFilenameBuilder.buildStaticPropertiesFilename;

public class GradleBuildLifecycleListener implements BuildLifecycleListener {

  private final GradleToolingLogger myLogger;
  private final Collection<BuildEventListener> myEventListeners;
  private final BuildContext myBuildContext;

  public GradleBuildLifecycleListener(GradleToolingLogger logger,
                                      Collection<BuildEventListener> eventListeners,
                                      BuildContext buildContext) {
    myLogger = logger;
    myEventListeners = eventListeners;
    myBuildContext = buildContext;
  }

  @Override
  public void onStart(@NotNull BuildEvent event) {
    myLogger.lifecycle(event.getMessage());
    createTaskOutputDir();
  }

  @Override
  public void onTaskOutput(@NotNull TaskOutputEvent event) {
    myEventListeners.forEach(it -> it.onEvent(event));
  }

  @Override
  public void onStatusChange(@NotNull BuildEvent event) {
    myEventListeners.forEach(it -> it.onEvent(event));
  }

  @Override
  public void onSuccess() {
    myEventListeners.forEach(it -> it.onEvent(new BuildFinishedEventImpl(System.currentTimeMillis(), "Build completed successfully", BuildResult.SUCCEEDED)));
    deleteTemporaryFiles();
  }

  @Override
  public void onFail() {
    myEventListeners.forEach(it -> it.onEvent(new BuildFinishedEventImpl(System.currentTimeMillis(), "Build failed", BuildResult.FAILED)));
    deleteTemporaryFiles();
  }

  private void createTaskOutputDir() {
    File taskOutputDir = new File(myBuildContext.getTaskOutputDir());
    if (taskOutputDir.exists()) {
      try {
        delete(taskOutputDir);
      } catch (IOException e) {
        String msg = String.format("Unable to start build. Couldn't delete task output directory: path=%s", myBuildContext.getTaskOutputDir());
        myLogger.warn(msg);
        throw new RuntimeException(msg, e);
      }
    }

    if (!taskOutputDir.mkdirs()) {
      String msg = String.format("Couldn't create task output directory: path=%s", myBuildContext.getTaskOutputDir());
      myLogger.warn(msg);
      throw new RuntimeException(msg);
    }
  }

  private void delete(@NotNull File file) throws IOException {
    Path path = file.toPath();
    if (!Files.exists(path)) return;

    try (Stream<Path> walk = Files.walk(path)) {
      walk.sorted(Comparator.reverseOrder())
          .forEach(p -> {
            try {
              Files.delete(p);
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (UncheckedIOException e) {
      throw e.getCause();
    }
  }

  private void deleteTemporaryFiles() {
    String staticFilePath = buildStaticPropertiesFilename(myBuildContext.getTcBuildParametersPath());

    Stream.of(staticFilePath,
              myBuildContext.getGradleParamsTempFilePath(),
              myBuildContext.getJvmArgsTempFilePath(),
              myBuildContext.getGradleTasksTempFilePath()).forEach(filePath -> {
      File source = new File(filePath);
      if (!source.exists()) {
        myLogger.warn("Couldn't delete file. The file doesn't exist: " + filePath);
        return;
      }

      if (!source.delete()) {
        myLogger.warn("Couldn't delete file: " + filePath);
      }
    });

    deleteTaskOutputDir();
  }

  private void deleteTaskOutputDir() {
    File taskOutputDir = new File(myBuildContext.getTaskOutputDir());
    try {
      delete(taskOutputDir);
    }
    catch (IOException e) {
      myLogger.warn(String.format("Couldn't delete task output directory: path=%s, message: " + e.getMessage(), myBuildContext.getTaskOutputDir()));
    }
  }
}
