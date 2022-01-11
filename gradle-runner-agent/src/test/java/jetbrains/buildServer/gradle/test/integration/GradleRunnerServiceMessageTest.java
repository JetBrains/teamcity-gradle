/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;

import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public abstract class GradleRunnerServiceMessageTest extends BaseGradleRunnerTest {
  protected static final String SEQENCE_FILES_ENCODING = "utf-8";
  protected static final String DEFAULT_MSG_PATTERN = "##teamcity\\[(.*?)(?<!\\|)\\]";

  protected void assertServiceMessages(final List<String> actual, final String[] expected) {
    assertEquals(actual.size(), expected.length, "Sequences differ in size. " + getAsString(actual, expected));
    List<String> processedActual = preprocessMessages(actual);
    for (String expectedMsg : expected) {
      assertTrue(processedActual.remove(expectedMsg), "Could not find " + expectedMsg + " in actual sequence: "
                                                      + getAsString(processedActual, expected));
    }
  }

  private List<String> preprocessMessages(List<String> messages) {
    List<String> result = new ArrayList<String>(messages.size());
    for (final String message : messages) {
      String resultMessage = message.replaceAll("flowId='[^']+'", "flowId='##Flow_ID##'"); // drop flow id
      resultMessage = resultMessage.replaceAll("parent='[^']+'", "parent='##ParentFlow_ID##'"); // drop flow id
      resultMessage = resultMessage.replaceAll("(name|compiler)='(.*?)( UP-TO-DATE)?'", "$1='$2'"); // drop up-to-date marks
      resultMessage = resultMessage.replaceAll("duration='\\d+'", "duration='##Duration##'"); // drop test durations
      resultMessage = resultMessage.replaceAll("details='java.lang.AssertionError.*?[^|]'", "details='##Assert_Stacktrace##'"); // drop test durations
      resultMessage = resultMessage.replaceAll("\\\\", "/"); // normalize paths if any
      resultMessage = resultMessage.replaceAll( "(file:(/)?)?" + myCoDir.getAbsolutePath().replaceAll("\\\\", "/"), "##Checkout_directory##"); // substitute temp checkout dir
      resultMessage = resultMessage.replaceAll("wrapper/dists/gradle-([0-9.]+)-bin/[^/]+", "wrapper/dists/gradle-$1-bin/##HASH##");
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[testMetadata.*?)value='(.*?)/[0-9]+\\.log'", "$1value='$2/##NUMBER##.log'"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[testMetadata.*?value='(.*?))/[0-9]+/",
                                               "$1/##NUMBER##/"); // drop build number directory
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?)/[0-9]+\\.log =>", "$1/##NUMBER##.log =>"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?=>.*?)/[0-9]+'", "$1/##NUMBER##'"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?)/[0-9]+/",
                                               "$1/##NUMBER##/"); // drop build number directory

      String tempDir = myTeamCitySystemProps.get("teamcity.build.tempDir");
      if (tempDir != null) {
        resultMessage = resultMessage.replaceAll(tempDir.replaceAll("\\\\", "/"),
                                                 "##Build_temp_directory##"); // drop build tmp directory
      }
      result.add(resultMessage);
    }
    return result;
  }

  protected String getAsString(final Collection<String> actual, final String[] expected) {
    return "\nActual messages:\n" + StringUtil.join("\n", actual)
           + "\n\nExpected messages: " + StringUtil.join(expected, "\n");
  }

  protected String[] readReportSequence(final String sequenceName) throws RunBuildException {
    final File sequenceFile = new File (ourProjectRoot, REPORT_SEQ_DIR + File.separator + sequenceName);
    String expected;
    try {
      expected = new String(FileUtil.loadFileText(sequenceFile, SEQENCE_FILES_ENCODING));
    } catch (IOException e) {
      throw new RunBuildException(e);
    }
    return expected.split("\\r\\n?|\\n");
  }

  protected void writeReportSequence(final File sequenceFile, List<String> data) throws IOException {
    FileUtil.writeFileAndReportErrors(sequenceFile, StringUtil.join(preprocessMessages(data), "\r\n"));
  }

  protected void runAndCheckServiceMessages(@NotNull final GradleRunConfiguration gradleRunConfiguration) throws IOException, RunBuildException {

    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleVersion());

    final String sequenceName = gradleRunConfiguration.getSequenceFileName();
    final File sequenceFile = new File (ourProjectRoot, REPORT_SEQ_DIR + File.separator + sequenceName);
    final ServiceMessageReceiver gatherMessage = new ServiceMessageReceiver("Gather service messages");
    gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());

    if (sequenceFile.exists()) {
      final Expectations gatherServiceMessage = new Expectations() {{
        allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
        allowing(myMockLogger).warning(with(any(String.class))); will(gatherMessage);
        allowing(myMockLogger).error(with(any(String.class))); will(reportError);
      }};

      runTest(gatherServiceMessage, ctx);

      String[] sequence = readReportSequence(sequenceName);
      if (sequence.length != gatherMessage.getMessages().size()) {
        System.out.println("Process output:");
        for (String line: gatherMessage.getAllMessages()) System.out.println(line);
      }
      assertServiceMessages(gatherMessage.getMessages(), sequence);
    } else {
      final Expectations gatherServiceMessage = new Expectations() {{
        allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
        allowing(myMockLogger).warning(with(any(String.class))); will(gatherMessage);
        allowing(myMockLogger).error(with(any(String.class))); will(gatherMessage);
        allowing(myMockLogger).internalError(with(any(String.class)),
                                             with(any(String.class)),
                                             with(any(Throwable.class))); will(gatherMessage);
      }};

      runTest(gatherServiceMessage, ctx);
      writeReportSequence(sequenceFile, gatherMessage.getMessages());
      System.out.println("Process output:");
      for (String line: gatherMessage.getAllMessages()) System.out.println(line);
      fail("Writing a report always causes test failure");
    }
  }

  public static class GradleRunConfiguration {
    private final String myProject;
    private final String myCommand;
    private final String mySequenceFileName;
    private String myGradleVersion;
    private String myPatternStr;

    public GradleRunConfiguration(final String project, final String command, final String sequenceFileName) {
      myProject = project;
      myCommand = command;
      mySequenceFileName = sequenceFileName;
      myGradleVersion = System.getProperty(PROPERTY_GRADLE_RUNTIME);
      myPatternStr = DEFAULT_MSG_PATTERN;
    }

    public String getProject() {
      return myProject;
    }

    public String getCommand() {
      return myCommand;
    }

    public String getSequenceFileName() {
      return myGradleVersion.startsWith("gradle-") && VersionComparatorUtil.compare(getGradleVersionFromPath(myGradleVersion), "3") >= 0
             ? FileUtil.getNameWithoutExtension(mySequenceFileName) + ".3." + FileUtil.getExtension(mySequenceFileName)
             : mySequenceFileName;
    }

    public String getGradleVersion() {
      return myGradleVersion;
    }

    public void setGradleVersion(String version) {
      myGradleVersion = version;
    }

    public String getPatternStr() {
      return myPatternStr;
    }

    public void setPatternStr(String pattern) {
      myPatternStr = pattern;
    }
  }


  protected static class ServiceMessageReceiver extends CustomAction {
    protected final List<String> messages = new ArrayList<String>();
    protected final List<String> output = new ArrayList<String>();
    protected Pattern myPattern = Pattern.compile("##teamcity\\[(.*?)(?<!\\|)\\]");

    public ServiceMessageReceiver(String description) {
      super(description);
    }

    public Object invoke(final Invocation invocation) throws Throwable {
      reportMessage.invoke(invocation);
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String line = param == null ? "" : param.toString();
          output.add(line);
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

    public List<String> getAllMessages() {
      return output;
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
