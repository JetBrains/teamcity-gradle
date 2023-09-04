package jetbrains.buildServer.gradle.runtime.service;

import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.runtime.listening.BuildLifecycleListener;
import jetbrains.buildServer.gradle.runtime.listening.GradleToolingApiProgressListener;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.gradle.runtime.output.GradleOutputWrapper;
import jetbrains.buildServer.gradle.runtime.output.OutputType;
import jetbrains.buildServer.gradle.runtime.output.TestOutputParser;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessage;
import jetbrains.buildServer.util.SortedProperties;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.events.OperationType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Helps to configure Gradle Tooling API build
 */
public class GradleBuildConfigurator {

  @NotNull
  public static GradleConnector prepareConnector(@NotNull String workingDirectoryPath,
                                                 @NotNull Boolean useWrapper,
                                                 @Nullable String gradleWrapperPropertiesPath,
                                                 @Nullable String gradleHomePath) {
    File workingDirectory = new File(workingDirectoryPath);

    GradleConnector connector = GradleConnector.newConnector();
    connector.forProjectDirectory(workingDirectory);

    if (useWrapper) {
      if (gradleWrapperPropertiesPath == null) {
        throw new RuntimeException("Parameter " + GradleRunnerConstants.GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY
                                   + " must be present in environment variables.");
      }
      File gradleWrapperProperties = new File(gradleWrapperPropertiesPath);
      if (!gradleWrapperProperties.exists()) {
        throw new RuntimeException("File doesn't exist: " + gradleWrapperPropertiesPath);
      }

      DistributionFactoryExtension.setWrappedDistribution(connector, gradleWrapperProperties.getAbsolutePath());
    } else {
      if (gradleHomePath == null) {
        throw new RuntimeException("Parameter " + GradleRunnerConstants.GRADLE_HOME_ENV_KEY
                                   + " must be present in environment variables.");
      }
      File gradleHome = new File(gradleHomePath);
      if (!gradleHome.exists()) {
        throw new RuntimeException("File doesn't exist: " + gradleHomePath);
      }

      connector.useInstallation(gradleHome);
    }

    return connector;
  }

  @NotNull
  public static BuildLauncher prepareBuildExecutor(@NotNull Map<String, String> env,
                                                   @NotNull List<String> gradleParams,
                                                   @NotNull List<String> jvmArgs,
                                                   @NotNull List<String> gradleTasks,
                                                   @NotNull BuildLifecycleListener buildListener,
                                                   @NotNull GradleToolingLogger logger,
                                                   @NotNull String buildNumber,
                                                   @NotNull ProjectConnection connection) {
    BuildLauncher launcher = connection.newBuild();

    launcher.forTasks(gradleTasks.toArray(new String[0]));
    launcher.addProgressListener(new GradleToolingApiProgressListener(buildListener, logger, buildNumber), OperationType.TASK);
    launcher.addArguments(gradleParams);
    launcher.addJvmArguments(jvmArgs);
    launcher.setEnvironmentVariables(env);
    launcher.setStandardOutput(new GradleOutputWrapper(buildListener, OutputType.STD_OUT));
    launcher.setStandardError(new GradleOutputWrapper(buildListener, OutputType.STD_ERR));

    return launcher;
  }

  @NotNull
  public static String getInitScriptClasspath() throws IOException {
    return new File(ClasspathUtil.getClasspathEntry(ServiceMessage.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(ComparisonFailureUtil.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(GradleRunnerConstants.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(TestOutputParser.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(SortedProperties.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(GsonBuilder.class)).getAbsolutePath();
  }
}
