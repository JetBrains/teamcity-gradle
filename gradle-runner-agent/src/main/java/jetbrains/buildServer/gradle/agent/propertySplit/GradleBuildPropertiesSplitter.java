package jetbrains.buildServer.gradle.agent.propertySplit;

import java.io.File;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import org.jetbrains.annotations.NotNull;

/**
 * Splits properties used in Gradle build into two parts:
 * static, which doesn't change from build to build, and dynamic (the original property file).
 * This is required in order to implement Gradle's configuration cache feature.
 */
public interface GradleBuildPropertiesSplitter {

  SplitablePropertyFile getType();

  void split(@NotNull Map<String, String> environment,
             @NotNull File buildTempDir) throws RunBuildException;
}
