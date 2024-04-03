package jetbrains.buildServer.gradle.agent.gradleOptions;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.gradle.util.internal.DefaultGradleVersion;
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
                                             @NotNull File projectDirectory,
                                             @Nullable DefaultGradleVersion gradleVersion) {
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
                                           .withGradlePropertiesOptionNames(getConfigurationCachePropertyNames(gradleVersion,
                                                                                                               "org.gradle.configuration-cache",
                                                                                                               "org.gradle.unsafe.configuration-cache"))
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
                                                      @NotNull File projectDirectory,
                                                      @Nullable DefaultGradleVersion gradleVersion) {
    try {
      return gradleOptionValueFetcher.fetchOptionValue(
        GradleOptionValueFetchingParameters.builder()
                                           .withGradleTasks(gradleTasks)
                                           .withGradleParams(gradleParams)
                                           .withGradleUserHome(gradleUserHome)
                                           .withProjectDirectory(projectDirectory)
                                           .withGradleOptionType(GradleOptionType.KEY_VALUE)
                                           .withOptionNames(Arrays.asList("--configuration-cache-problems"))
                                           .withGradlePropertiesOptionNames(getConfigurationCachePropertyNames(gradleVersion,
                                                                                                               "org.gradle.configuration-cache.problems",
                                                                                                               "org.gradle.unsafe.configuration-cache-problems"))
                                           .build())
        .map(value -> value.equalsIgnoreCase("warn"))
        .orElse(false);
    } catch (Throwable t) {
      logger.warning("An error occurred while checking if configuration cache problems are ignored: " + t.getMessage());
      return false;
    }
  }

  /**
   * The configuration cache feature in Gradle was introduced in version 6.6. See https://docs.gradle.org/6.6/release-notes.html#configuration-caching.
   * From this version onward, to configure the cache via the gradle.properties file, you have to add the following line:
   * org.gradle.unsafe.configuration-cache=true.
   *
   * With the release of version 8.1, the configuration cache feature became stable, and a new option was introduced to enable the cache:
   * org.gradle.configuration-cache=true (without the "unsafe" prefix). See https://docs.gradle.org/8.1/userguide/upgrading_version_8.html#configuration_caching_options_renamed
   *
   * So we determine whether the configuration cache is enabled based on the version used.
   *
   * The key "org.gradle.unsafe.configuration-cache" is still available in the latest version (currently 8.7), although there are plans to remove it in the future.
   * The key "org.gradle.configuration-cache" didn't work before version 8.1.
   * However, when it was present in gradle.properties, there were messages in the build log indicating that the configuration cache was detected, which could be misleading.
   */
  public boolean isVersionWithStableConfigCache(@NotNull DefaultGradleVersion gradleVersion) {
    return VersionComparatorUtil.compare(gradleVersion.getVersion(), "8.1") >= 0;
  }

  private Collection<String> getConfigurationCachePropertyNames(@Nullable DefaultGradleVersion gradleVersion,
                                                                @NotNull String stablePropertyName,
                                                                @NotNull String unstablePropertyName) {
    if (gradleVersion == null) {
      return Arrays.asList(stablePropertyName, unstablePropertyName);
    }

    return isVersionWithStableConfigCache(gradleVersion)
           ? Arrays.asList(stablePropertyName, unstablePropertyName)
           : Arrays.asList(unstablePropertyName);
  }
}
