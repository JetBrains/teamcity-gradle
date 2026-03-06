package jetbrains.buildServer.gradle.agent.propertySplit;

import org.jetbrains.annotations.NotNull;

/**
 * Builds filename for static part of the split property file.
 */
public class SplitPropertiesFilenameBuilder {
  private static final String SPLIT_PROPERTY_STATIC_POSTFIX = ".static";

  @NotNull
  public static String buildStaticPropertiesFilename(@NotNull String filePath) {
    return filePath + SPLIT_PROPERTY_STATIC_POSTFIX;
  }
}
