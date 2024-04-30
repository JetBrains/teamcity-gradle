package jetbrains.buildServer.gradle.agent.commandLineComposers;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import org.jetbrains.annotations.NotNull;

public class GradleCommandLineComposerHolder {

  private final Map<GradleLaunchMode, GradleCommandLineComposer> launchModeToComposer;

  public GradleCommandLineComposerHolder(List<GradleCommandLineComposer> commandLineComposerList) {
    launchModeToComposer = commandLineComposerList.stream()
                                                  .collect(Collectors.toMap(it -> it.getApplicableLaunchMode(), Function.identity()));
  }

  @NotNull
  public GradleCommandLineComposer getCommandLineComposer(@NotNull GradleLaunchMode launchMode) throws RunBuildException {
    if (!launchModeToComposer.containsKey(launchMode)) {
      throw new RunBuildException("Unsupported mode: " + launchMode);
    }

    return launchModeToComposer.get(launchMode);
  }
}
