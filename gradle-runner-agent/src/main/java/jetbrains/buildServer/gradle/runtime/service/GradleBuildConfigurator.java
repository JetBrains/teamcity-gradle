package jetbrains.buildServer.gradle.runtime.service;

import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jetbrains.buildServer.ComparisonFailureUtil;
import jetbrains.buildServer.agent.ClasspathUtil;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.runtime.listening.BuildLifecycleListener;
import jetbrains.buildServer.gradle.runtime.listening.GradleToolingApiProgressListener;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.gradle.runtime.output.GradleOutputWrapper;
import jetbrains.buildServer.gradle.runtime.output.OutputType;
import jetbrains.buildServer.gradle.runtime.output.TestOutputParser;
import jetbrains.buildServer.gradle.runtime.service.commandLine.CommandLineParametersProcessor;
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

  private final CommandLineParametersProcessor commandLineParametersProcessor;
  private final GradleToolingLogger logger;

  public GradleBuildConfigurator(CommandLineParametersProcessor commandLineParametersProcessor,
                                 GradleToolingLogger logger) {
    this.commandLineParametersProcessor = commandLineParametersProcessor;
    this.logger = logger;
  }

  @NotNull
  public GradleConnector prepareConnector(@NotNull String workingDirectoryPath,
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
  public BuildLauncher prepareBuildExecutor(@NotNull Map<String, String> env,
                                            @NotNull Collection<String> gradleParams,
                                            @NotNull Collection<String> overridedJvmArgs,
                                            @NotNull Collection<String> gradleTasks,
                                            @NotNull BuildLifecycleListener buildListener,
                                            @NotNull String buildNumber,
                                            @NotNull ProjectConnection connection) {
    BuildLauncher launcher = connection.newBuild();

    if (!overridedJvmArgs.isEmpty()) {
      launcher.addJvmArguments(overridedJvmArgs);
    }

    Set<String> unsupportedArgs = commandLineParametersProcessor.obtainUnsupportedArguments(gradleParams);
    if (!unsupportedArgs.isEmpty()) {
      logger.warn("Not all of the Gradle command line options are supported by the Gradle Tooling API." +
                  "\nPlease find more information in the javadoc for the LongRunningOperation class.");
      unsupportedArgs.forEach(arg -> logger.warn("The argument is not supported by the Gradle Tooling API and will not be used: " + arg));
    }

    launcher.forTasks(gradleTasks.toArray(new String[0]));
    launcher.addProgressListener(new GradleToolingApiProgressListener(buildListener, logger, buildNumber), OperationType.TASK);
    launcher.addArguments(gradleParams.stream().filter(arg -> !unsupportedArgs.contains(arg)).collect(Collectors.toList()));
    launcher.setEnvironmentVariables(env);
    launcher.setStandardOutput(new GradleOutputWrapper(buildListener, OutputType.STD_OUT));
    launcher.setStandardError(new GradleOutputWrapper(buildListener, OutputType.STD_ERR));

    return launcher;
  }

  @NotNull
  public String getInitScriptClasspath() throws IOException {
    return new File(ClasspathUtil.getClasspathEntry(ServiceMessage.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(ComparisonFailureUtil.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(GradleRunnerConstants.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(TestOutputParser.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(SortedProperties.class)).getAbsolutePath()
           + File.pathSeparator + new File(ClasspathUtil.getClasspathEntry(GsonBuilder.class)).getAbsolutePath();
  }
}
