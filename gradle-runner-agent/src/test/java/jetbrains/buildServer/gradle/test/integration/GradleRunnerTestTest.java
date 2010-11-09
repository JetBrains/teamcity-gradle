package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 8, 2010
 */
public class GradleRunnerTestTest extends GradleRunnerServiceMessageTest {

  private static final int PROJECT_D_TEST_COUNT = 39;

  private void testTest(String project, String command, String seqFile, String gradleHomePath) throws RunBuildException {
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(project, command, seqFile);
    gradleRunConfiguration.setPatternStr("##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
    gradleRunConfiguration.setGradleHome(gradleHomePath);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void failedAndSkippedJUnitTest(final String gradleHomePath) throws RunBuildException {
    testTest(PROJECT_C_NAME, "clean test", "failedProjectCJUnitSequence.txt", gradleHomePath);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void failedAndSkippedTestNGTest(final String gradleHomePath) throws RunBuildException {
    testTest(PROJECT_C_NAME, "clean testng", "failedProjectCTestNGSequence.txt", gradleHomePath);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void parallelTestSuiteTest(final String gradleHomePath) throws RunBuildException {
    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     new File(myProjectRoot, "src/test/resources/testProjects/testJvmArgs.properties").getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(PROJECT_D_NAME,
                                                                                     "clean testParallel",null);
    gradleRunConfiguration.setPatternStr("##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
    gradleRunConfiguration.setGradleHome(gradleHomePath);
    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleHome());
    final FlowServiceMessageReceiver gatherMessage = new FlowServiceMessageReceiver();
    gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());
    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(reportWarning);
    }};

    runTest(gatherServiceMessage, ctx);
    validateTestFlows(gatherMessage.getFlows(), PROJECT_D_TEST_COUNT);
  }

  protected void validateTestFlows(Map<String, List<String>> flows, int totalTestCount) {
    int testCount = totalTestCount;
    Pattern testMsg = Pattern.compile("##teamcity\\[(\\w+?)(Started|Finished)\\s+name='(.*?)'.*(?<!\\|)\\]");

    assertTrue(flows.size() > 0, "Have zero flows to validate!");

    for (Map.Entry<String, List<String>> flow : flows.entrySet()) {

      List<String> tests = new ArrayList<String>((flow.getValue().size() / 2) + 1);

      for(String msg : flow.getValue()) {
        Matcher parser = testMsg.matcher(msg);
        assertTrue(parser.find(), "Failed to parse service message: " + msg);
        boolean finished = "Finished".equals(parser.group(2));
        String fullName = parser.group(1) + parser.group(3);
        if (finished) {
          assertTrue(tests.remove(fullName), "Have 'Finished' message without matching 'Started' : " + msg);
          final boolean isTest = "test".equals(parser.group(1));
          if (isTest) {
            testCount--;
          }
        } else {
          tests.add(fullName);
        }
      }
      assertTrue(tests.size() == 0, "Some 'Started' messages are not closed: " + tests.toString());
    }
    assertTrue(testCount == 0, "Wrong number of tests reported: " + (totalTestCount - testCount));
  }

  public static class FlowServiceMessageReceiver extends ServiceMessageReceiver {
    private final ConcurrentHashMap<String, List<String>> flows = new ConcurrentHashMap<String, List<String>>();
    private final Pattern flowIdPattern = Pattern.compile("flowId='(.*?)'");

    public FlowServiceMessageReceiver() {
      super("Gather service messages");
    }

    @Override
    public Object invoke(final Invocation invocation) throws Throwable {
      reportMessage.invoke(invocation);
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String text = param.toString();
          final Matcher matcher = myPattern.matcher(text);
          final Matcher flowMatcher = flowIdPattern.matcher(text);
          while(matcher.find()) {
            messages.add(matcher.group(0));
            if (flowMatcher.find()) {
              addToFlow(flowMatcher.group(1), text);
            }
          }
        }
      }
      return null;
    }

    @Override
    public void printTrace(PrintStream stream) {
      for (String str : getMessages()) {
        stream.println(str);
      }
      stream.println("==== Flows ====");
      for (Map.Entry<String, List<String>> flow :flows.entrySet()) {
        stream.println("== flow:" + flow.getKey());
        for (String msg : flow.getValue()) {
          stream.println("=== " + msg);
        }
      }
    }

    private void addToFlow(final String group, final String text) {
      List<String> list =  new LinkedList<String>();
      List<String> old = flows.putIfAbsent(group, list);
      if (null != old) {
        list = old;
      }
      list.add(text);
    }

    public Map<String,List<String>> getFlows() {
      return flows;
    }

  }
}
