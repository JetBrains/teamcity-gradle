package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.agent.util.GradleCommandLineUtil.extractEqualSignSeparatedParamValue;

public class GradleUserHomeDetector {

  private static final Logger LOG = Logger.getInstance(GradleUserHomeDetector.class);

  /**
   * Detect the value of Gradle User Home.
   * There are several places where this value can be set.
   * Below, they are listed in descending order of priority.
   * Ultimately, the build will use the value with the highest priority.
   *
   * 1. Command line option (--gradle-user-home, -g).
   * 2. System property in the command line (-Dgradle.user.home).
   * 3. Environment variables. Gradle User Home can be overridden through the GRADLE_USER_HOME environment variable.
   * 4. Default location if not specified elsewhere. By default, it is set to USER_HOME/.gradle, where USER_HOME is the user's home directory.
   */
  @NotNull
  public Optional<File> detect(@NotNull List<String> gradleTasks,
                               @NotNull List<String> gradleParams,
                               @NotNull Map<String, String> env,
                               @Nullable GradleConnector projectConnector) {
    Optional<File> overriddenInCommandLine = tryToGetFromCommandLine(gradleTasks, gradleParams);
    if (overriddenInCommandLine.isPresent()) {
      return overriddenInCommandLine;
    }

    Optional<File> overriddenInEvironmentVariables = tryToGetFromEnvironmentVariables(env);
    if (overriddenInEvironmentVariables.isPresent()) {
      return overriddenInEvironmentVariables;
    }

    return Optional.ofNullable(projectConnector).map(this::tryToDetectDefault).orElse(Optional.empty());
  }

  /**
   * It is possible to pass to Gradle both of: system property and command line argument.
   * E.g.: gradlew build -Dgradle.user.home=/CustomGUH --gradle-user-home=/CustomGUH2.
   * In this case, the command line arg has higher priority.
   * If both of: short command line arg (e.g.: -g=/Path) and long command line arg (e.g.: --gradle-user-home=/Path) are passed, Gradle will throw an exception.
   */
  @NotNull
  private Optional<File> tryToGetFromCommandLine(@NotNull List<String> gradleTasks,
                                                 @NotNull List<String> gradleParams) {
    String gradleUserHomeCLArg = null;
    String gradleUserHomeSystemProperty = null;
    List<String> tasksAndParams = Stream.concat(gradleTasks.stream(), gradleParams.stream()).collect(Collectors.toList());

    for (int i = 0; i < tasksAndParams.size(); i++) {
      String item = tasksAndParams.get(i);
      if (item.startsWith("--gradle-user-home=") || item.startsWith("-g=")) {
        gradleUserHomeCLArg = extractEqualSignSeparatedParamValue(item);
        break;
      }
      if (item.equals("--gradle-user-home") || item.equals("-g") && i + 1 < tasksAndParams.size()) {
        gradleUserHomeCLArg = tasksAndParams.get(i + 1);
        break;
      }
      if (item.startsWith("-Dgradle.user.home=")) {
        gradleUserHomeSystemProperty = extractEqualSignSeparatedParamValue(item);
      }
    }

    if (gradleUserHomeCLArg != null) {
      return Optional.of(new File(gradleUserHomeCLArg));
    }

    if (gradleUserHomeSystemProperty != null) {
      return Optional.of(new File(gradleUserHomeSystemProperty));
    }

    return Optional.empty();
  }

  private Optional<File> tryToGetFromEnvironmentVariables(@NotNull Map<String, String> env) {
    return Optional.ofNullable(env.get("GRADLE_USER_HOME")).map(File::new);
  }

  private Optional<File> tryToDetectDefault(@NotNull GradleConnector projectConnector) {
    try (ProjectConnection connection = projectConnector.connect()) {
      BuildEnvironment buildEnvironment = connection.getModel(BuildEnvironment.class);
      return Optional.ofNullable(buildEnvironment.getGradle().getGradleUserHome());
    } catch (Throwable t) {
      LOG.warnAndDebugDetails("Unable to detect Gradle User Home", t);
      return Optional.empty();
    }
  }
}
