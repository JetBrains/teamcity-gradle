package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.runtime.service.DistributionFactoryExtension;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.TOOLING_PREPARATION_PHASE_DAEMON_MAX_IDLE_TIME_ENABLED;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.TOOLING_PREPARATION_PHASE_DAEMON_MAX_IDLE_TIME_MS;
import static jetbrains.buildServer.gradle.agent.ConfigurationParamsUtil.getBooleanOrDefault;

public class GradleToolingConnectorFactory {

  private static final boolean DAEMON_MAX_IDLE_TIME_ENABLED_DEFAULT = true;
  private static final int DAEMON_MAX_IDLE_TIME_DEFAULT_MS = 1000;

  @NotNull
  public static GradleConnector instantiate(@NotNull File workingDirectory,
                                            @NotNull Boolean useWrapper,
                                            @Nullable File gradleHome,
                                            @Nullable File gradleWrapperProperties,
                                            @NotNull Map<String, String> configParams) throws RunBuildException {
    GradleConnector connector = GradleConnector.newConnector();
    connector.forProjectDirectory(workingDirectory);

    if (useWrapper) {
      if (gradleWrapperProperties == null) {
        throw new RunBuildException("gradle-wrapper.properties must be present in the project when Gradle Wrapper build mode selected");
      }
      DistributionFactoryExtension.setWrappedDistribution(connector, gradleWrapperProperties.getAbsolutePath());
    } else {
      if (gradleHome == null) {
        throw new RunBuildException("gradleHome must be present in the project when build mode with Gradle Home selected");
      }
      connector.useInstallation(gradleHome);
    }

    boolean daemonMaxIdleTimeEnabled = getBooleanOrDefault(configParams, TOOLING_PREPARATION_PHASE_DAEMON_MAX_IDLE_TIME_ENABLED, DAEMON_MAX_IDLE_TIME_ENABLED_DEFAULT);
    int daemonMaxIdleTimeMs = Optional.ofNullable(configParams.get(TOOLING_PREPARATION_PHASE_DAEMON_MAX_IDLE_TIME_MS))
                                      .map(Integer::valueOf)
                                      .orElse(DAEMON_MAX_IDLE_TIME_DEFAULT_MS);
    if (daemonMaxIdleTimeEnabled) {
      setDaemonMaxIdleTime(connector, daemonMaxIdleTimeMs);
    }

    return connector;
  }

  private static void setDaemonMaxIdleTime(@NotNull GradleConnector connector,
                                           int daemonMaxIdleTime) {
    if (connector instanceof DefaultGradleConnector) {
      ((DefaultGradleConnector)connector).daemonMaxIdleTime(daemonMaxIdleTime, TimeUnit.MILLISECONDS);
    }
  }
}
