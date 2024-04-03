package jetbrains.buildServer.gradle.agent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Optional;
import jetbrains.buildServer.agent.BuildProgressLogger;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.gradle.util.internal.DefaultGradleVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleVersionDetector {

  @NotNull
  public Optional<DefaultGradleVersion> detect(@Nullable GradleConnector projectConnector, @NotNull BuildProgressLogger logger) {
    return Optional.ofNullable(projectConnector)
                   .flatMap(connector -> getGradleVersion(connector, logger));
  }

  @NotNull
  private Optional<DefaultGradleVersion> getGradleVersion(@NotNull GradleConnector projectConnector,
                                                          @NotNull BuildProgressLogger logger) {
    try (ProjectConnection connection = projectConnector.connect()) {
      BuildEnvironment buildEnvironment = connection.getModel(BuildEnvironment.class);

      String gradleVersionStr = buildEnvironment.getGradle().getGradleVersion();
      if (gradleVersionStr == null) {
        logger.warning("Couldn't detect the Gradle version in the project: null value");
        return Optional.empty();
      }

      return Optional.of(DefaultGradleVersion.version(gradleVersionStr));
    } catch (Throwable t) {
      logger.warning("Couldn't detect the Gradle version in the project: " + t.getMessage());
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      t.printStackTrace(new PrintStream(out, true));
      logger.debug(out.toString());
      return Optional.empty();
    }
  }
}
