package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/17/12, 4:15 PM
 */
public class GradleVersionErrorsListenerTest extends GradleRunnerServiceMessageTest {

  @Test(dataProvider = "gradle-version-provider")
  public void failedAndSkippedJUnitTest(final String gradleVersion) throws Exception {
    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                         new File(myProjectRoot, "src/test/resources/testProjects/buildNumber.properties").getAbsolutePath());
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration("wrappedProjectD", "-i --continue clean test",
                                                                                     "test_error_output_Sequence.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Override
  protected Mockery initContext(final String projectName, final String gradleParams, final String gradleVersion) throws IOException {
    final Mockery mockery = super.initContext(projectName, gradleParams,
                                              gradleVersion);

    final Expectations dontExpectInternalErrors = new Expectations() {{
      never(myMockLogger).internalError(with(any(String.class)), with(any(String.class)), with(any(Throwable.class)));
    }};
    mockery.checking(dontExpectInternalErrors);
    return mockery;
  }

}

