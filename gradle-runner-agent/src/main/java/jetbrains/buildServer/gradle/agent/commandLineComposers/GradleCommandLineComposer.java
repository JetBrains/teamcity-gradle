package jetbrains.buildServer.gradle.agent.commandLineComposers;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import org.jetbrains.annotations.NotNull;

public interface GradleCommandLineComposer {

  @NotNull
  GradleLaunchMode getLaunchMode();

  @NotNull
  ProgramCommandLine compose(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException;
}
