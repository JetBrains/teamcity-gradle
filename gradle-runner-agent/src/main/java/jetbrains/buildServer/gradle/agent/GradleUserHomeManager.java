package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.diagnostic.Logger;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildRunnerContext;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED;
import static jetbrains.buildServer.gradle.agent.util.GradleCommandLineUtil.extractEqualSignSeparatedParamValue;

public class GradleUserHomeManager {

  private static final Logger LOG = Logger.getInstance(GradleUserHomeManager.class);
  private static final String GRADLE_USER_HOME_DIR = "gradle.user.home";

  public boolean isGradleUserHomeOverrideNeeded(@NotNull BuildRunnerContext buildRunnerContext,
                                                boolean isDependencyCacheEnabled) {
    boolean isConfigParamEnabled = Optional.ofNullable(buildRunnerContext.getConfigParameters().get(GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED))
                                           .map(Boolean::valueOf)
                                           .orElse(true);

    return isConfigParamEnabled && isDependencyCacheEnabled && buildRunnerContext.isVirtualContext();
  }

  /**
   * Removes the argument for overriding GRADLE_USER_HOME from Gradle task or command-line arguments list.
   * We should specifically remove the command line argument: -g or --gradle-user-home,
   * because the system property -Dgradle.user.home will have a lower priority if both are present.
   * @param items user-defined command line: gradle tasks or command line arguments
   * @return user-defined command line without GRADLE_USER_HOME command-line arguments
   */
  public List<String> removeGradleUserHomeArgument(@NotNull List<String> items) {
    List<String> result = new ArrayList<>();

    for (int i = 0; i < items.size(); i++) {
      String item = items.get(i);

      // Skip arguments starting with "--gradle-user-home=" or "-g="
      // because these arguments already contain the value.
      if (item.startsWith("--gradle-user-home=") || item.startsWith("-g=")) {
        continue;
      }

      // Skip "--gradle-user-home" or "-g" along with its next value
      // because the next argument contains the value for this parameter.
      if (item.equals("--gradle-user-home") || item.equals("-g")) {
        i++;
        continue;
      }

      // Add all other items to the new collection
      result.add(item);
    }

    return result;
  }

  @NotNull
  public File getGradleUserHomeAgentLocal(@NotNull BuildAgentConfiguration agentConfiguration) {
    File localGradleCacheDirectory = GradleRunnerCacheDirectoryProvider.getGradleRunnerCacheDirectory(agentConfiguration);
    File gradleUserHomeLocal = new File(localGradleCacheDirectory, GRADLE_USER_HOME_DIR);

    if (!gradleUserHomeLocal.exists()) {
      gradleUserHomeLocal.mkdirs();
    }

    return gradleUserHomeLocal;
  }

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
  public Optional<File> detectGradleUserHome(@NotNull List<String> gradleTasks,
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
