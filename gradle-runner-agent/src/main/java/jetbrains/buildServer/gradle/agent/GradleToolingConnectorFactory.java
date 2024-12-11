package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.runtime.service.DistributionFactoryExtension;
import org.gradle.tooling.GradleConnector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleToolingConnectorFactory {

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

    return connector;
  }
}
