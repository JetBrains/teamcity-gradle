package jetbrains.buildServer.gradle.agent;

import com.intellij.openapi.util.TCSystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession;
import jetbrains.buildServer.agent.runner.MultiCommandBuildSessionFactory;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersionDetector;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerBuildSessionFactory implements MultiCommandBuildSessionFactory
{
  private static final Info info = new Info();

  private final GradleCommandLineComposerHolder composerHolder;
  private final GradleTasksComposer tasksComposer;
  private final GradleLaunchModeSelector gradleLaunchModeSelector;
  private final GradleConfigurationCacheDetector gradleConfigurationCacheDetector;
  private final CommandLineParametersProcessor commandLineParametersProcessor;
  private final GradleUserHomeManager gradleUserHomeManager;

  public GradleRunnerBuildSessionFactory(GradleCommandLineComposerHolder composerHolder,
                                         GradleTasksComposer tasksComposer,
                                         GradleLaunchModeSelector gradleLaunchModeSelector,
                                         GradleConfigurationCacheDetector gradleConfigurationCacheDetector,
                                         CommandLineParametersProcessor commandLineParametersProcessor,
                                         GradleUserHomeManager gradleUserHomeManager) {
    this.composerHolder = composerHolder;
    this.tasksComposer = tasksComposer;
    this.gradleLaunchModeSelector = gradleLaunchModeSelector;
    this.gradleConfigurationCacheDetector = gradleConfigurationCacheDetector;
    this.commandLineParametersProcessor = commandLineParametersProcessor;
    this.gradleUserHomeManager = gradleUserHomeManager;
  }

  @NotNull
  public MultiCommandBuildSession createSession(@NotNull BuildRunnerContext runnerContext)
   {
    GradleRunnerContext gradleRunnerContext = new GradleRunnerContext(runnerContext);
    GradleVersionDetector gradleVersionDetector = new GradleVersionDetector(gradleRunnerContext);
    GradleCommandLineProvider gradleCommandLineProvider = new GradleCommandLineProvider(
      gradleRunnerContext,
      composerHolder,
      tasksComposer,
      gradleLaunchModeSelector,
      gradleConfigurationCacheDetector,
      commandLineParametersProcessor,
      gradleUserHomeManager
    );
    return new GradleRunnerBuildSession(gradleRunnerContext, gradleVersionDetector, gradleCommandLineProvider);
   }

   @NotNull public AgentBuildRunnerInfo getBuildRunnerInfo()
   {
      return info;
   }

   private static class Info implements AgentBuildRunnerInfo
   {
      private final boolean isOSSupported;

      public Info() {
        isOSSupported = TCSystemInfo.isWindows || TCSystemInfo.isUnix;
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
