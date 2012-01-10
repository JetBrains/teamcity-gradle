/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.Reporter;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public abstract class GradleRunnerServiceMessageTest extends BaseGradleRunnerTest {


  protected static final String SEQENCE_FILES_ENCODING = "utf-8";
  protected static final String DEFAULT_MSG_PATTERN = "##teamcity\\[(.*?)(?<!\\|)\\]";

  protected void assertMessageSequence(final List<String> actual, final String[] expected) {
    Iterator<String> actualIterator = actual.iterator();
    for (String expectation : expected) {
      assertTrue(actualIterator.hasNext(), "Actual sequence too short.");
      final String nextActual = actualIterator.next();
      assertTrue(nextActual.contains(expectation), "Unexpected message in sequence. Expected to contain [" + expectation  + "] but was [" + nextActual + "]");
    }
  }

  protected String[] readReportSequence(final String sequenceName) throws RunBuildException {
    final File sequenceFile = new File (myProjectRoot, REPORT_SEQ_DIR + File.separator + sequenceName);
    String expected;
    try {
      expected = new String(FileUtil.loadFileText(sequenceFile, SEQENCE_FILES_ENCODING));
    } catch (IOException e) {
      throw new RunBuildException(e);
    }

    return expected.split("[\n\r]{2}|\n(?=!\r)|\r(?=!\n)");

  }

  protected void writeReportSequence(final String sequenceName, List<String> data) throws RunBuildException {
    final File sequenceFile = new File (myProjectRoot, REPORT_SEQ_DIR + File.separator + sequenceName);
    FileUtil.writeFile(sequenceFile, StringUtil.join(data, "\r\n"));
  }

  protected void runAndCheckServiceMessages(final GradleRunConfiguration gradleRunConfiguration)
    throws RunBuildException {
    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleHome());

    final ServiceMessageReceiver gatherMessage = new ServiceMessageReceiver("Gather service messages");
    gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());

    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(reportWarning);
      allowing(myMockLogger).error(with(any(String.class))); will(reportError);
    }};

    runTest(gatherServiceMessage, ctx);

    String[] sequence = readReportSequence(gradleRunConfiguration.getSequenceFileName());
    assertMessageSequence(gatherMessage.getMessages(), sequence);
  }

  protected void runAndWriteServiceMessages(final GradleRunConfiguration gradleRunConfiguration, final boolean writeToFile) throws RunBuildException {
    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleHome());

    final ServiceMessageReceiver gatherMessage = new ServiceMessageReceiver("Gather service messages");
    if (writeToFile) {
      gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());
    } else {
      gatherMessage.setPattern(".*");
    }

    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).error(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).internalError(with(any(String.class)),
                                           with(any(String.class)),
                                           with(any(Throwable.class))); will(gatherMessage);
    }};

    runTest(gatherServiceMessage, ctx);

    if (writeToFile) {
      writeReportSequence(gradleRunConfiguration.getSequenceFileName(), gatherMessage.getMessages());
    } else {
      gatherMessage.printTrace();
      for (String s : gatherMessage.getMessages()) {
        Reporter.log(s);
      }
    }
    fail("Writing a report always causes test failure");
  }

  public static class GradleRunConfiguration {
    private final String myProject;
    private final String myCommand;
    private final String mySequenceFileName;
    private String myGradleHome;
    private String myPatternStr;

    public GradleRunConfiguration(final String project, final String command, final String sequenceFileName) {
      myProject = project;
      myCommand = command;
      mySequenceFileName = sequenceFileName;
      myGradleHome = System.getProperty(PROPERTY_GRADLE_RUNTIME);
      myPatternStr = DEFAULT_MSG_PATTERN;
    }

    public String getProject() {
      return myProject;
    }

    public String getCommand() {
      return myCommand;
    }

    public String getSequenceFileName() {
      return mySequenceFileName;
    }

    public String getGradleHome() {
      return myGradleHome;
    }

    public void setGradleHome(String home) {
      myGradleHome = home;
    }

    public String getPatternStr() {
      return myPatternStr;
    }

    public void setPatternStr(String pattern) {
      myPatternStr = pattern;
    }
  }


  protected static class ServiceMessageReceiver extends CustomAction {
    protected final List<String> messages = new LinkedList<String>();
    protected Pattern myPattern = Pattern.compile("##teamcity\\[(.*?)(?<!\\|)\\]");

    public ServiceMessageReceiver(String description) {
      super(description);
    }

    public Object invoke(final Invocation invocation) throws Throwable {
      reportMessage.invoke(invocation);
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String line = param == null ? "" : param.toString();
          final Matcher matcher = myPattern.matcher(line);
          while(matcher.find()) {
            // convert to unix line ending
            String escapedMessage = matcher.group(0).replaceAll("\\|r\\|n","|n");
            messages.add(escapedMessage);
          }
        }
      }
      return null;
    }

    public List<String> getMessages() {
      return messages;
    }

    public void setPattern(String patternStr) {
      myPattern = Pattern.compile(patternStr);
    }

    public void printTrace(PrintStream stream) {
      for (String str : getMessages()) {
        stream.println(str);
      }
    }

    public void printTrace() {
      printTrace(System.out);
    }
  }
}
