package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import org.jetbrains.annotations.NotNull;

public class GradleSimpleCommandLineComposer implements GradleCommandLineComposer {

  private final GradleTasksComposer tasksComposer;

  public GradleSimpleCommandLineComposer(GradleTasksComposer tasksComposer) {
    this.tasksComposer = tasksComposer;
  }

  @Override
  @NotNull
  public GradleLaunchMode getApplicableLaunchMode() {
    return GradleLaunchMode.COMMAND_LINE;
  }

  @Override
  @NotNull
  public ProgramCommandLine composeCommandLine(@NotNull GradleCommandLineComposerParameters parameters) {
    final List<String> gradleParameters = new ArrayList<>(parameters.getInitialGradleParams());
    gradleParameters.addAll(tasksComposer.getGradleParameters(GradleLaunchMode.COMMAND_LINE, parameters.getRunnerParameters(),
                                                              parameters.getGradleUserDefinedParams(), parameters.getPluginsDirectory()));
    gradleParameters.addAll(parameters.getGradleTasks());

    return new SimpleProgramCommandLine(parameters.getEnv(), parameters.getWorkingDirectory().getPath(), parameters.getExePath(), gradleParameters);
  }
}
