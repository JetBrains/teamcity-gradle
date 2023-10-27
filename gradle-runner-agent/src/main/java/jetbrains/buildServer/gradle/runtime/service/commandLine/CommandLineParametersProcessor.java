package jetbrains.buildServer.gradle.runtime.service.commandLine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

public class CommandLineParametersProcessor {

  /**
   * Obtain arguments unsupported by the Gradle Tooling API.
   * If this is not done, Tooling API throws an exception and a build fails.
   *
   * @param gradleArgs arguments configured in a built project
   */
  @NotNull
  public Set<String> obtainUnsupportedArguments(@NotNull Collection<String> gradleArgs) {
    Options unsupported = GradleToolingCommandLineOptionsProvider.getUnsupportedOptions();
    Set<String> result = new HashSet<>();

    gradleArgs.forEach(p -> {
      if (unsupported.hasLongOption(p) || unsupported.hasOption(p)) {
        result.add(p);
      }
    });

    return result;
  }
}
