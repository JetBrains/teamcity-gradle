package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.RunBuildException;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * Date: 4/2/13, 1:16 PM
 */
public class GradleParallelRunTest extends GradleRunnerServiceMessageTest {

  @Test(dataProvider = "gradle-version-provider")
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
