package jetbrains.buildServer.gradle.agent.gradleOptions;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil;
import org.jetbrains.annotations.NotNull;

public class GradleOptionValueFetcher {

  private static final String GRADLE_PROPERTIES_FILENAME = "gradle.properties";

  /**
   * Fetch a gradle option value from the building project.
   * Fetching an option is implemented considering strict priority in several places.
   * Here they are in descending order of priority:
   * 1. Command line args (e.g.: --configuration-cache, --no-configuration-cache)
   * 2. gradle.properties from Gradle User Home directory.
   * The fact that the directory could be overridden in the command line arguments is taken into account.
   * 3. gradle.properties from project directory.
   *
   * @return option value, if it has been detected
   * empty {@link Optional}, if no information about the option is found
   */
  @NotNull
  public Optional<String> fetchOptionValue(@NotNull GradleOptionValueFetchingParameters parameters) {
    Optional<String> optionFromCommandLine = checkCommandLineArgs(parameters);
    if (optionFromCommandLine.isPresent()) {
      return optionFromCommandLine;
    }

    Optional<String> optionFromGradleUserHomeDir = checkGradleUserHomeDirectory(parameters);
    if (optionFromGradleUserHomeDir.isPresent()) {
      return optionFromGradleUserHomeDir;
    }

    Optional<String> optionFromProjectDir = checkProjectDirectory(parameters);
    if (optionFromProjectDir.isPresent()) {
      return optionFromProjectDir;
    }

    return Optional.empty();
  }

  @NotNull
  private Optional<String> checkCommandLineArgs(@NotNull GradleOptionValueFetchingParameters parameters) {
    switch (parameters.getGradleOptionType()) {
      case BOOLEAN:
        Set<String> tasksAndParams = Stream.concat(parameters.getGradleTasks().stream(), parameters.getGradleParams().stream()).collect(Collectors.toSet());
        for (String it : parameters.getOptionNames()) {
          if (tasksAndParams.contains(it)) {
            return Optional.of(Boolean.TRUE.toString());
          }
        }
        for (String it : parameters.getOptionDisablingNames()) {
          if (tasksAndParams.contains(it)) {
            return Optional.of(Boolean.FALSE.toString());
          }
        }
        return Optional.empty();
      case KEY_VALUE:
        List<String> tasksAndParamsList = Stream.concat(parameters.getGradleTasks().stream(), parameters.getGradleParams().stream()).collect(Collectors.toList());
        for (int i = 0; i < tasksAndParamsList.size(); i++) {
          String item = tasksAndParamsList.get(i);
          for (String it : parameters.getOptionNames()) {
            if (item.startsWith(it + "=")) {
              return Optional.of(extractEqualSignSeparatedParamValue(item));
            }
            if (item.equals(it) && i + 1 < tasksAndParamsList.size()) {
              return Optional.of(tasksAndParamsList.get(i + 1));
            }
          }
        }
        return Optional.empty();
      default:
        throw new IllegalStateException("Unsupported GradleOptionType: " + parameters.getGradleOptionType());
    }
  }

  @NotNull
  private Optional<String> checkProjectDirectory(@NotNull GradleOptionValueFetchingParameters parameters) {
    File gradleProperties = new File(parameters.getProjectDirectory(), GRADLE_PROPERTIES_FILENAME);
    return checkGradleProperties(gradleProperties, parameters.getGradlePropertiesOptionNames());
  }

  /**
   * If a user has overridden Gradle User Home via the system property or the command line argument,
   * the overridden value will have a higher priority over the default GRADLE_USER_HOME.
   */
  @NotNull
  private Optional<String> checkGradleUserHomeDirectory(@NotNull GradleOptionValueFetchingParameters parameters) {
    Optional<File> overriddenGradleUserHome = tryToGetOverriddenGradleUserHome(parameters.getGradleTasks(), parameters.getGradleParams());
    if (overriddenGradleUserHome.isPresent()) {
      File gradleProperties = new File(overriddenGradleUserHome.get(), GRADLE_PROPERTIES_FILENAME);
      return checkGradleProperties(gradleProperties, parameters.getGradlePropertiesOptionNames());
    }

    if (!parameters.getGradleUserHome().isPresent()) {
      return Optional.empty();
    }
    File gradleProperties = new File(parameters.getGradleUserHome().get(), GRADLE_PROPERTIES_FILENAME);

    return checkGradleProperties(gradleProperties, parameters.getGradlePropertiesOptionNames());
  }

  /**
   * It is possible to pass to Gradle both of: system property and command line argument.
   * E.g.: gradlew build -Dgradle.user.home=/CustomGUH --gradle-user-home=/CustomGUH2.
   * In this case, the command line arg has higher priority.
   * If both of: short command line arg (e.g.: -g=/Path) and long command line arg (e.g.: --gradle-user-home=/Path) are passed, Gradle will throw an exception.
   */
  @NotNull
  private Optional<File> tryToGetOverriddenGradleUserHome(@NotNull List<String> gradleTasks,
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
  private Optional<String> checkGradleProperties(@NotNull File gradleProperties,
                                                 @NotNull Collection<String> gradlePropertiesOptionNames) {
    if (!gradleProperties.exists() || !gradleProperties.isFile()) {
      return Optional.empty();
    }

    Properties gradlePropertiesContent;
    try {
      gradlePropertiesContent = GradleRunnerFileUtil.readProperties(gradleProperties);
    } catch (IOException ignore) {
      return Optional.empty();
    }

    for (String optionName : gradlePropertiesOptionNames) {
      if (gradlePropertiesContent.containsKey(optionName)) {
        String optionValue = (String) gradlePropertiesContent.get(optionName);
        return Optional.of(optionValue);
      }
    }

    return Optional.empty();
  }

  @NotNull
  private String extractEqualSignSeparatedParamValue(@NotNull String param) {
    String[] divided = param.split("=");
    return divided.length > 1 ? divided[1] : "";
  }
}
