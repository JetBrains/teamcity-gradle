package jetbrains.buildServer.gradle.runtime.listening;

import java.io.File;
import java.util.Collection;
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
    deleteTemporaryFiles();
    myEventListeners.forEach(it -> it.onEvent(new BuildFinishedEventImpl(System.currentTimeMillis(), "Build completed successfully", BuildResult.SUCCEEDED)));
  }

  @Override
  public void onFail() {
    deleteTemporaryFiles();
    myEventListeners.forEach(it -> it.onEvent(new BuildFinishedEventImpl(System.currentTimeMillis(), "Build failed", BuildResult.FAILED)));
  }

  private void deleteTemporaryFiles() {
    String staticFilePath = buildStaticPropertiesFilename(myBuildContext.getTcBuildParametersPath());

    Stream.of(staticFilePath,
              myBuildContext.getEnvTempFilePath(),
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
  }
}
