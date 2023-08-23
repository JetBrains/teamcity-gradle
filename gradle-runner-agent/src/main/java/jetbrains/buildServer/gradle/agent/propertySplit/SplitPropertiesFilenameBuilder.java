package jetbrains.buildServer.gradle.agent.propertySplit;

import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.SPLIT_PROPERTY_STATIC_POSTFIX;

/**
 * Builds filename for static part of the split property file.
 */
public class SplitPropertiesFilenameBuilder {

  @NotNull
  public static String buildStaticPropertiesFilename(@NotNull String filePath) {
    return filePath + SPLIT_PROPERTY_STATIC_POSTFIX;
  }
}
