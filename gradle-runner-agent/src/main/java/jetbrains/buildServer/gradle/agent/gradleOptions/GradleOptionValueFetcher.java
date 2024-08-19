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

import static jetbrains.buildServer.gradle.agent.util.GradleCommandLineUtil.extractEqualSignSeparatedParamValue;

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

  @NotNull
  private Optional<String> checkGradleUserHomeDirectory(@NotNull GradleOptionValueFetchingParameters parameters) {
    if (!parameters.getGradleUserHome().isPresent()) {
      return Optional.empty();
    }

    File gradleProperties = new File(parameters.getGradleUserHome().get(), GRADLE_PROPERTIES_FILENAME);

    return checkGradleProperties(gradleProperties, parameters.getGradlePropertiesOptionNames());
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
}
