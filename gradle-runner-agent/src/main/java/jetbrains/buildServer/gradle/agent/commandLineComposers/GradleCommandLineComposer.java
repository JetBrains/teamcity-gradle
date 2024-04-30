package jetbrains.buildServer.gradle.agent.commandLineComposers;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import org.jetbrains.annotations.NotNull;

public interface GradleCommandLineComposer {

  @NotNull
  GradleLaunchMode getApplicableLaunchMode();

  @NotNull
  ProgramCommandLine composeCommandLine(@NotNull GradleCommandLineComposerParameters parameters) throws RunBuildException;
}
