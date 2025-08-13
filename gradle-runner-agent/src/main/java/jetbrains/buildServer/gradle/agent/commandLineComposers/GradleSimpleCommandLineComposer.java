package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import org.jetbrains.annotations.NotNull;

public class GradleSimpleCommandLineComposer implements GradleCommandLineComposer {
  @NotNull
  private final GradleTasksComposer tasksComposer;

  public GradleSimpleCommandLineComposer(@NotNull GradleTasksComposer tasksComposer) {
    this.tasksComposer = tasksComposer;
  }

  @Override
  @NotNull
  public GradleLaunchMode getLaunchMode() {
    return GradleLaunchMode.COMMAND_LINE;
  }

  @Override
  @NotNull
  public ProgramCommandLine compose(@NotNull GradleCommandLineComposerParameters parameters) {
    List<String> gradleParameters = new ArrayList<>(parameters.getInitialGradleParams());
    gradleParameters.addAll(tasksComposer.getGradleParameters(
      getLaunchMode(),
      parameters.getRunnerParameters(),
      parameters.getGradleUserDefinedParams(),
      parameters.getPluginsDir().toFile()
    ));
    gradleParameters.addAll(parameters.getGradleTasks());

    return new SimpleProgramCommandLine(parameters.getEnv(), parameters.getWorkingDir().toString(), parameters.getExePath(), gradleParameters);
  }
}
