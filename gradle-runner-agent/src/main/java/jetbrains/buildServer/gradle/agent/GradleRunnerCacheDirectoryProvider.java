package jetbrains.buildServer.gradle.agent;

import java.io.File;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerCacheDirectoryProvider {

  public static final String GRADLE_CACHE_DIR = "jetbrains.gradle.runner";

  @NotNull
  public static File getGradleRunnerCacheDirectory(@NotNull BuildAgentConfiguration agentConfiguration) {
    return agentConfiguration.getCacheDirectory(GRADLE_CACHE_DIR);
  }
}
