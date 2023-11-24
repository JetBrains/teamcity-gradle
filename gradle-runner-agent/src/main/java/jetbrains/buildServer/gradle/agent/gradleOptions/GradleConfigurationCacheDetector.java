package jetbrains.buildServer.gradle.agent.gradleOptions;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleConfigurationCacheDetector {

  private final GradleOptionValueFetcher gradleOptionValueFetcher;

  public GradleConfigurationCacheDetector(GradleOptionValueFetcher gradleOptionValueFetcher) {
    this.gradleOptionValueFetcher = gradleOptionValueFetcher;
  }

  public boolean isConfigurationCacheEnabled(@NotNull BuildProgressLogger logger,
                                             @NotNull List<String> gradleTasks,
                                             @NotNull List<String> gradleParams,
                                             @Nullable File gradleUserHome,
                                             @NotNull File projectDirectory) {
    try {
      return gradleOptionValueFetcher.fetchOptionValue(
        GradleOptionValueFetchingParameters.builder()
                                           .withGradleTasks(gradleTasks)
                                           .withGradleParams(gradleParams)
                                           .withGradleUserHome(gradleUserHome)
                                           .withProjectDirectory(projectDirectory)
                                           .withGradleOptionType(GradleOptionType.BOOLEAN)
                                           .withOptionNames(Arrays.asList("--configuration-cache"))
                                           .withOptionDisablingNames(Arrays.asList("--no-configuration-cache"))
                                           .withGradlePropertiesOptionNames(Arrays.asList("org.gradle.configuration-cache", "org.gradle.unsafe.configuration-cache"))
                                           .build())
                                     .map(Boolean::parseBoolean)
        .orElse(false);
    } catch (Throwable t) {
      logger.warning("An error occurred while checking if the configuration cache feature is enabled: " + t.getMessage());
      return false;
    }
  }

  public boolean areConfigurationCacheProblemsIgnored(@NotNull BuildProgressLogger logger,
                                                      @NotNull List<String> gradleTasks,
                                                      @NotNull List<String> gradleParams,
                                                      @Nullable File gradleUserHome,
                                                      @NotNull File projectDirectory) {
    try {
      return gradleOptionValueFetcher.fetchOptionValue(
        GradleOptionValueFetchingParameters.builder()
                                           .withGradleTasks(gradleTasks)
                                           .withGradleParams(gradleParams)
                                           .withGradleUserHome(gradleUserHome)
                                           .withProjectDirectory(projectDirectory)
                                           .withGradleOptionType(GradleOptionType.KEY_VALUE)
                                           .withOptionNames(Arrays.asList("--configuration-cache-problems"))
                                           .withGradlePropertiesOptionNames(Arrays.asList("org.gradle.configuration-cache.problems", "org.gradle.unsafe.configuration-cache-problems"))
                                           .build())
        .map(value -> value.equalsIgnoreCase("warn"))
        .orElse(false);
    } catch (Throwable t) {
      logger.warning("An error occurred while checking if configuration cache problems are ignored: " + t.getMessage());
      return false;
    }
  }
}
