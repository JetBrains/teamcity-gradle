package jetbrains.buildServer.gradle.runtime.listening;

import jetbrains.buildServer.gradle.runtime.listening.event.BuildEvent;
import org.jetbrains.annotations.NotNull;

public interface BuildEventListener {

  void onEvent(@NotNull BuildEvent event);
}
