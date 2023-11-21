package jetbrains.buildServer.gradle.runtime.service.commandLine;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

public class CommandLineParametersProcessor {

  /**
   * Obtain arguments unsupported by the Gradle Tooling API.
   * If unsupported arguments are passed to Gradle Tooling API, it will throw an exception and the build will fail.
   *
   * @param tasksAndParams gradle tasks and arguments configured in a built project
   */
  @NotNull
  public Set<String> obtainUnsupportedArguments(@NotNull Collection<String> tasksAndParams) {
    Options unsupported = GradleToolingCommandLineOptionsProvider.getUnsupportedOptions();

    return tasksAndParams.stream()
                  .filter(it -> it.startsWith("-") || it.startsWith("--"))
                  .filter(it -> unsupported.hasOption(it))
                  .collect(Collectors.toSet());
  }
}
