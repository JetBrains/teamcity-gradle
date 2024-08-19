package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheManager;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerServiceFactory implements CommandLineBuildServiceFactory
{
  public static final String WIN_GRADLE_EXE = "bin/gradle.bat";
  public static final String WIN_GRADLEW = "gradlew.bat";
  public static final String UNIX_GRADLE_EXE = "bin/gradle";
  public static final String UNIX_GRADLEW = "gradlew";

  private static final Info info = new Info();

  private final GradleCommandLineComposerHolder composerHolder;
  private final GradleTasksComposer tasksComposer;
  private final GradleLaunchModeSelector gradleLaunchModeSelector;
  private final GradleConfigurationCacheDetector gradleConfigurationCacheDetector;
  private final CommandLineParametersProcessor commandLineParametersProcessor;
  private final GradleVersionDetector gradleVersionDetector;
  private final GradleUserHomeDetector gradleUserHomeDetector;
  private final GradleDependencyCacheManager gradleDependencyCacheManager;

  public GradleRunnerServiceFactory(GradleCommandLineComposerHolder composerHolder,
                                    GradleTasksComposer tasksComposer,
                                    GradleLaunchModeSelector gradleLaunchModeSelector,
                                    GradleConfigurationCacheDetector gradleConfigurationCacheDetector,
                                    CommandLineParametersProcessor commandLineParametersProcessor,
                                    GradleVersionDetector gradleVersionDetector,
                                    GradleUserHomeDetector gradleUserHomeDetector,
                                    GradleDependencyCacheManager gradleDependencyCacheManager) {
    this.composerHolder = composerHolder;
    this.tasksComposer = tasksComposer;
    this.gradleLaunchModeSelector = gradleLaunchModeSelector;
    this.gradleConfigurationCacheDetector = gradleConfigurationCacheDetector;
    this.commandLineParametersProcessor = commandLineParametersProcessor;
    this.gradleVersionDetector = gradleVersionDetector;
    this.gradleUserHomeDetector = gradleUserHomeDetector;
    this.gradleDependencyCacheManager = gradleDependencyCacheManager;
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
                                   composerHolder,
                                   tasksComposer,
                                   gradleLaunchModeSelector,
                                   gradleConfigurationCacheDetector,
                                   commandLineParametersProcessor,
                                   gradleVersionDetector,
                                   gradleUserHomeDetector,
                                   gradleDependencyCacheManager);
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