package jetbrains.buildServer.gradle.agent.tasks;

import java.util.List;
import org.jetbrains.annotations.NotNull;

public interface GradleTasksPostProcessor {

  void process(@NotNull List<String> gradleTasks);
}
