package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.util.SystemInfo;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildPropertiesSplitter;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerServiceFactory implements CommandLineBuildServiceFactory
{
  public static final String WIN_GRADLE_EXE = "bin/gradle.bat";
  public static final String WIN_GRADLEW = "gradlew.bat";
  public static final String UNIX_GRADLE_EXE = "bin/gradle";
  public static final String UNIX_GRADLEW = "gradlew";

  private static final Info info = new Info();

  private final List<GradleBuildPropertiesSplitter> propertySplitters;
  private final GradleLaunchModeSelector gradleLaunchModeSelector;
  private final GradleConfigurationCacheDetector gradleConfigurationCacheDetector;
  private final CommandLineParametersProcessor commandLineParametersProcessor;
  private final GradleVersionDetector gradleVersionDetector;

  public GradleRunnerServiceFactory(List<GradleBuildPropertiesSplitter> propertySplitters,
                                    GradleLaunchModeSelector gradleLaunchModeSelector,
                                    GradleConfigurationCacheDetector gradleConfigurationCacheDetector,
                                    CommandLineParametersProcessor commandLineParametersProcessor,
                                    GradleVersionDetector gradleVersionDetector) {
    this.propertySplitters = propertySplitters;
    this.gradleLaunchModeSelector = gradleLaunchModeSelector;
    this.gradleConfigurationCacheDetector = gradleConfigurationCacheDetector;
    this.commandLineParametersProcessor = commandLineParametersProcessor;
    this.gradleVersionDetector = gradleVersionDetector;
  }

  @NotNull public CommandLineBuildService createService()
   {
     final String exePath;
     final String wrapperName;

    if (SystemInfo.isWindows) {
      exePath = WIN_GRADLE_EXE;
      wrapperName = WIN_GRADLEW;
    } else if (SystemInfo.isUnix) {
      exePath = UNIX_GRADLE_EXE;
      wrapperName = UNIX_GRADLEW;
    } else {
      throw new RuntimeException("OS not supported");
    }

    return new GradleRunnerService(exePath,
                                   wrapperName,
                                   propertySplitters.stream().collect(Collectors.toMap(it -> it.getType(), Function.identity())),
                                   gradleLaunchModeSelector,
                                   gradleConfigurationCacheDetector,
                                   commandLineParametersProcessor,
                                   gradleVersionDetector);
   }

   @NotNull public AgentBuildRunnerInfo getBuildRunnerInfo()
   {
      return info;
   }

   private static class Info implements AgentBuildRunnerInfo
   {
      private final boolean isOSSupported;

      public Info() {
        isOSSupported = SystemInfo.isWindows || SystemInfo.isUnix;
        if (!isOSSupported) {
          Loggers.AGENT.warn("Gradle runner plugin does not support current OS. Gradle build runner will not be available.");
        }
      }

      @NotNull public String getType()
      {
         return GradleRunnerConstants.RUNNER_TYPE;
      }

      public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfiguration)
      {
         return isOSSupported;
      }
   }
}