package jetbrains.buildServer.gradle.runtime.listening;

import jetbrains.buildServer.gradle.runtime.listening.event.BuildEvent;
import jetbrains.buildServer.gradle.runtime.listening.event.TaskOutputEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Listens to build lifecycle events
 */
public interface BuildLifecycleListener {

  void onStart(@NotNull BuildEvent event);

  void onTaskOutput(@NotNull TaskOutputEvent event);

  void onStatusChange(@NotNull BuildEvent event);

  void onSuccess();

  void onFail();
}
