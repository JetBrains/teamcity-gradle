package jetbrains.buildServer.gradle.runtime.service.commandLine;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

public class CommandLineParametersProcessor {

  /**
   * Obtain arguments unsupported by the Gradle Tooling API.
   * If unsupported arguments are passed to Gradle Tooling API, it will throw an exception and the build will fail.
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
