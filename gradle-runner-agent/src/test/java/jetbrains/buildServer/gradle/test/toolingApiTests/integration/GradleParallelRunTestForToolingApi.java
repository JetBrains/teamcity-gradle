package jetbrains.buildServer.gradle.test.toolingApiTests.integration;

import java.io.IOException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.test.integration.FlowServiceMessageReceiver;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * Date: 4/2/13, 1:16 PM
 */
public class GradleParallelRunTestForToolingApi extends GradleRunnerServiceMessageTestForToolingApi {

  @Test(dataProvider = "gradle-version-provider>=8")
  public void parallelProjectsTest(final String gradleVersion) throws RunBuildException, IOException {

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_C_NAME,
                                                                                     "clean test --parallel",null);
    gradleRunConfiguration.setPatternStr("##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
    gradleRunConfiguration.setGradleVersion(gradleVersion);

    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleVersion());
    final FlowServiceMessageReceiver flowMessageReceiver = new FlowServiceMessageReceiver();
    flowMessageReceiver.setPattern(gradleRunConfiguration.getPatternStr());

    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(flowMessageReceiver);
      allowing(myMockLogger).warning(with(any(String.class))); will(reportWarning);
      allowing(myMockLogger).error(with(any(String.class))); will(reportError);
    }};

    runTest(gatherServiceMessage, ctx);
    flowMessageReceiver.validateTestFlows(13);
  }
}
