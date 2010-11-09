package jetbrains.buildServer.gradle.test.integration;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerService;
import jetbrains.buildServer.messages.ErrorData;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 11, 2010
 */
public class

  GradleRunnerWrapperTest extends GradleRunnerServiceMessageTest {

  @Test
  public void simpleWrapperTest() throws RunBuildException {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    GradleRunConfiguration config = new GradleRunConfiguration("wrappedProjectA", "clean build", "wrappedProjASequence.txt");
    config.setPatternStr("^Downloading(.*)|^Unzipping(.*)|##teamcity\\[(.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }

  @Test
  public void incompatibleStartupScriptAPI() throws RunBuildException {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    GradleRunConfiguration config = new GradleRunConfiguration("wrappedProjectB", "clean build", "wrappedProjBSequence.txt");
    config.setPatternStr("^Downloading(.*)|^Unzipping(.*)|##teamcity\\[(.*?)(?<!\\|)\\]");

    runAndAssertInternalError(config);
  }

  @Test
  public void startupScriptNotSupportedTest() throws RunBuildException {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    GradleRunConfiguration config = new GradleRunConfiguration("wrappedProjectC", "clean test", "wrappedProjCSequence.txt");
    config.setPatternStr("^Downloading(.*)|^Unzipping(.*)|##teamcity\\[(.*?)(?<!\\|)\\]");

    runAndAssertInternalError(config);
  }


  private void runAndAssertInternalError(final GradleRunConfiguration config) throws RunBuildException {
    final Mockery ctx = initContext(config.getProject(), config.getCommand(),
                                    config.getGradleHome());

    final ServiceMessageReceiver gatherMessage = new ServiceMessageReceiver("Gather service messages");
    gatherMessage.setPattern(config.getPatternStr());
    final String wrongVersionStr = GradleRunnerService.GradleVersionErrorsListener .WRONG_GRADLE_VERSION;

    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(reportWarning);
      allowing(myMockLogger).error(with(any(String.class))); will(reportError);
      oneOf(myMockLogger).internalError(ErrorData.BUILD_RUNNER_ERROR_TYPE, wrongVersionStr, null);
    }};

    runTest(gatherServiceMessage, ctx);

    String[] sequence = readReportSequence(config.getSequenceFileName());
    assertMessageSequence(gatherMessage.getMessages(), sequence);
    ctx.assertIsSatisfied();
  }


}
