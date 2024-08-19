package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.util.concurrent.TimeUnit;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.internal.consumer.DefaultGradleConnector;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerToolingApiTest extends BaseGradleRunnerTest {

  protected GradleConnector configureGradleConnector(@NotNull File workingDirectory,
                                                     @NotNull File gradleHome) {
    GradleConnector connector = GradleConnector.newConnector();
    connector.forProjectDirectory(workingDirectory);

    connector.useInstallation(gradleHome);

    if (connector instanceof DefaultGradleConnector) {
      ((DefaultGradleConnector)connector).daemonMaxIdleTime(1, TimeUnit.MINUTES);
    }

    return connector;
  }
}
