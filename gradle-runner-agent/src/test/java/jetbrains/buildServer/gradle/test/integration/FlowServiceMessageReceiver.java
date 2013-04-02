package jetbrains.buildServer.gradle.test.integration;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jmock.api.Invocation;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
* Created by Nikita.Skvortsov
* Date: 4/2/13, 1:17 PM
*/
public class FlowServiceMessageReceiver extends GradleRunnerServiceMessageTest.ServiceMessageReceiver {
  private final ConcurrentHashMap<String, List<String>> myFlows = new ConcurrentHashMap<String, List<String>>();
  private final Pattern flowIdPattern = Pattern.compile("flowId='(.*?)'");

  public FlowServiceMessageReceiver() {
    super("Gather service messages");
  }

  @Override
  public Object invoke(final Invocation invocation) throws Throwable {
    BaseGradleRunnerTest.reportMessage.invoke(invocation);
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
    for (Map.Entry<String, List<String>> flow : myFlows.entrySet()) {
      stream.println("== flow:" + flow.getKey());
      for (String msg : flow.getValue()) {
        stream.println("=== " + msg);
      }
    }
  }

  private void addToFlow(final String group, final String text) {
    List<String> list =  new LinkedList<String>();
    List<String> old = myFlows.putIfAbsent(group, list);
    if (null != old) {
      list = old;
    }
    list.add(text);
  }

  public Map<String,List<String>> getFlows() {
    return myFlows;
  }

  protected void validateTestFlows(int totalTestCount) {
      int testCount = 0;
      Pattern testMsg = Pattern.compile("##teamcity\\[(\\w+?)(Started|Finished|Ignored)\\s+name='(.*?)'.*(?<!\\|)\\]");

      assertTrue(myFlows.size() > 0, "Have zero flows to validate!");

      for (Map.Entry<String, List<String>> flow : myFlows.entrySet()) {

        List<String> tests = new ArrayList<String>((flow.getValue().size() / 2) + 1);

        for(String msg : flow.getValue()) {
          Matcher parser = testMsg.matcher(msg);
          assertTrue(parser.find(), "Failed to parse service message: " + msg);

          final boolean isFinished = "Finished".equals(parser.group(2));
          final boolean isStarted = "Started".equals(parser.group(2));
          final boolean isIgnored = "Ignored".equals(parser.group(2));

          String fullName = parser.group(1) + parser.group(3);
          if (isFinished || isIgnored) {
            final boolean isTest = "test".equals(parser.group(1));
            if (isTest) {
              testCount++;
            }
            if (isFinished) {
              assertTrue(tests.remove(fullName), "Have 'Finished' message without matching 'Started' : " + msg);
            }
          } else {
            if(isStarted) {
              tests.add(fullName);
            }
          }
        }
        assertTrue(tests.size() == 0, "Some 'Started' messages are not closed: " + tests.toString());
      }
      assertEquals(testCount, totalTestCount, "Wrong number of tests reported");
    }

}
