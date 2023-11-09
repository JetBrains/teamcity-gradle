package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleConfigurationCacheDetector {

  private static final String GRADLE_PROPERTIES_FILENAME = "gradle.properties";

  /**
   * Checking for configuration cache is implemented considering strict priority in several places.
   * Here they are in descending order of priority:
   * 1. Command line args (--configuration-cache, --no-configuration-cache)
   * 2. gradle.properties from Gradle User Home directory.
   * The fact that the directory could be overridden in the command line arguments is taken into account.
   * 3. gradle.properties from project directory.
   *
   * @return true - if we have detected that CC is enabled, false - if we detected that CC is disabled,
   * empty {@link Optional} if we haven't found any info about configuration cache
   */
  @NotNull
  public static Optional<Boolean> isConfigurationCacheEnabled(@NotNull List<String> gradleTasks,
                                                              @NotNull List<String> gradleParams,
                                                              @Nullable File gradleUserHome,
                                                              @NotNull File projectDirectory) {
    Optional<Boolean> isEnabledInCommandLine = checkCommandLineArgs(gradleTasks, gradleParams);
    if (isEnabledInCommandLine.isPresent()) {
      return isEnabledInCommandLine;
    }

    Optional<Boolean> isEnabledInGradleUserHomeDir = checkGradleUserHomeDirectory(gradleTasks, gradleParams, gradleUserHome);
    if (isEnabledInGradleUserHomeDir.isPresent()) {
      return isEnabledInGradleUserHomeDir;
    }

    Optional<Boolean> isEnabledInProjectDir = checkProjectDirectory(projectDirectory);
    if (isEnabledInProjectDir.isPresent()) {
      return isEnabledInProjectDir;
    }

    return Optional.empty();
  }

  @NotNull
  private static Optional<Boolean> checkCommandLineArgs(@NotNull List<String> gradleTasks,
                                                        @NotNull List<String> gradleParams) {
    Set<String> tasksAndParams = Stream.concat(gradleTasks.stream(), gradleParams.stream()).collect(Collectors.toSet());

    if (tasksAndParams.contains("--configuration-cache")) {
      return Optional.of(Boolean.TRUE);
    }

    if (tasksAndParams.contains("--no-configuration-cache")) {
      return Optional.of(Boolean.FALSE);
    }

    return Optional.empty();
  }

  @NotNull
  private static Optional<Boolean> checkProjectDirectory(@NotNull File projectDirectory) {
    File gradleProperties = new File(projectDirectory, GRADLE_PROPERTIES_FILENAME);
    return checkGradleProperties(gradleProperties);
  }

  /**
   * If a user has overridden Gradle User Home via the system property or the command line argument,
   * the overridden value will have a higher priority over the default GRADLE_USER_HOME.
   */
  @NotNull
  private static Optional<Boolean> checkGradleUserHomeDirectory(@NotNull List<String> gradleTasks,
                                                                @NotNull List<String> gradleParams,
                                                                @Nullable File gradleUserHomeDirectory) {
    Optional<File> overriddenGradleUserHome = tryToGetOverriddenGradleUserHome(gradleTasks, gradleParams);
    if (overriddenGradleUserHome.isPresent()) {
      File gradleProperties = new File(overriddenGradleUserHome.get(), GRADLE_PROPERTIES_FILENAME);
      return checkGradleProperties(gradleProperties);
    }

    if (gradleUserHomeDirectory == null) {
      return Optional.empty();
    }
    File gradleProperties = new File(gradleUserHomeDirectory, GRADLE_PROPERTIES_FILENAME);

    return checkGradleProperties(gradleProperties);
  }

  /**
   * It is possible to pass to Gradle both of: system property and command line argument.
   * E.g.: gradlew build -Dgradle.user.home=/CustomGUH --gradle-user-home=/CustomGUH2.
   * In this case, the command line arg has higher priority.
   * If both of: short command line arg (e.g.: -g=/Path) and long command line arg (e.g.: --gradle-user-home=/Path) are passed, Gradle will throw an exception.
   */
  @NotNull
  private static Optional<File> tryToGetOverriddenGradleUserHome(@NotNull List<String> gradleTasks,
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

  @NotNull
  private static Optional<Boolean> checkGradleProperties(@NotNull File gradleProperties) {
    if (!gradleProperties.exists() || !gradleProperties.isFile()) {
      return Optional.empty();
    }

    Properties gradlePropertiesContent;
    try {
      gradlePropertiesContent = GradleRunnerFileUtil.readProperties(gradleProperties);
    } catch (IOException ignore) {
      return Optional.empty();
    }

    if (gradlePropertiesContent.containsKey("org.gradle.configuration-cache")) {
      String ccValue = (String) gradlePropertiesContent.get("org.gradle.configuration-cache");
      return Optional.of(Boolean.valueOf(ccValue));
    }

    if (gradlePropertiesContent.containsKey("org.gradle.unsafe.configuration-cache")) {
      String ccValue = (String) gradlePropertiesContent.get("org.gradle.unsafe.configuration-cache");
      return Optional.of(Boolean.valueOf(ccValue));
    }

    return Optional.empty();
  }

  @NotNull
  private static String extractEqualSignSeparatedParamValue(@NotNull String param) {
    String[] divided = param.split("=");
    return divided.length > 1 ? divided[1] : "";
  }
}
