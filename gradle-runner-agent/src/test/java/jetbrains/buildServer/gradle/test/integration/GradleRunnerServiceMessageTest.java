

package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
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

  protected void assertServiceMessages(ServiceMessageReceiver message, final String[] expected) {
    final String[] actual = message.getMessages().toArray(new String[0]);
    if (actual.length != expected.length) {
      assertEquals(actual, expected, "Sequences differ in size.\n" +
                                     "Sequences differ in size. " + getAsString(Arrays.asList(actual), expected) + "\n" +
                                     "Full log:\n" + StringUtil.join("\n", message.getAllMessages()));
    }
    List<String> processedActual = preprocessMessages(Arrays.asList(actual));
    for (String expectedMsg : expected) {
      if (!processedActual.remove(expectedMsg)) {
        assertEquals(processedActual, Arrays.asList(expected), "Could not find " + expectedMsg + " in actual sequence: "
                                                               + getAsString(processedActual, expected) + "\n" +
                                                               "Full log:\n" + StringUtil.join("\n", message.getAllMessages()));
      }
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
      resultMessage = resultMessage.replaceAll("details='org.junit.ComparisonFailure.*?[^|]'", "details='##Assert_Stacktrace##'"); // drop test durations
      resultMessage = resultMessage.replaceAll("\\\\", "/"); // normalize paths if any
      resultMessage = resultMessage.replaceAll( "(file:(/)?)?" + myCoDir.getAbsolutePath().replaceAll("\\\\", "/"), "##Checkout_directory##"); // substitute temp checkout dir
      resultMessage = resultMessage.replace("##Checkout_directory##/" + GradleRunnerConstants.INIT_SCRIPT_SINCE_8_NAME, "##Checkout_directory##"); // substitute temp checkout dir
      resultMessage = resultMessage.replace("##Checkout_directory##/" + GradleRunnerConstants.INIT_SCRIPT_NAME, "##Checkout_directory##"); // substitute temp checkout dir
      resultMessage = resultMessage.replaceAll("wrapper/dists/gradle-([0-9.]+)-bin/[^/]+", "wrapper/dists/gradle-$1-bin/##HASH##");
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[testMetadata.*?)value='(.*?)/[0-9]+\\.log'", "$1value='$2/##NUMBER##.log'"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[testMetadata.*?value='(.*?))/[0-9]+/",
                                               "$1/##NUMBER##/"); // drop build number directory
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?)/[0-9]+\\.log =>", "$1/##NUMBER##.log =>"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?=>.*?)/[0-9]+'", "$1/##NUMBER##'"); // drop file number
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[publishArtifacts.*?)/[0-9]+/",
                                               "$1/##NUMBER##/"); // drop build number directory
      resultMessage = resultMessage.replaceAll("^(##teamcity\\[buildProblem.*?) identity='[0-9\\-]+'(.*?) description='.*?'\\]",
                                               "$1 identity='##NUMBER##'$2 description='#DESCRIPTION']"); // clean buildProblem

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

  protected ServiceMessageReceiver run(@NotNull final GradleRunConfiguration gradleRunConfiguration) throws IOException, RunBuildException {
    return run(gradleRunConfiguration, initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                                   gradleRunConfiguration.getGradleVersion()));
  }

  protected ServiceMessageReceiver run(@NotNull final GradleRunConfiguration gradleRunConfiguration, @NotNull final Mockery ctx) throws IOException, RunBuildException {
    final ServiceMessageReceiver gatherMessage = new ServiceMessageReceiver("Gather service messages");
    gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());

    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).error(with(any(String.class))); will(reportError);
    }};

    runTest(gatherServiceMessage, ctx);
    return gatherMessage;
  }

  protected void runAndCheckServiceMessages(@NotNull final GradleRunConfiguration gradleRunConfiguration) throws IOException, RunBuildException {

    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleVersion());

    final String sequenceName = gradleRunConfiguration.getSequenceFileName(new File(ourProjectRoot, REPORT_SEQ_DIR));
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
      gatherMessage.getAllMessages().stream()
                   .filter(line -> line.contains("init.gradle:"))
                   .findFirst().ifPresent(error -> fail(error));

      String[] sequence = readReportSequence(sequenceName);
      if (sequence.length != gatherMessage.getMessages().size()) {
        System.out.println("Process output:");
        for (String line: gatherMessage.getAllMessages()) System.out.println(line);
      }
      assertServiceMessages(gatherMessage, sequence);
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
      gatherMessage.getAllMessages().stream()
                   .filter(line -> line.contains("init.gradle:"))
                   .findFirst().ifPresent(error -> fail(error));

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

    public String getSequenceFileName(@NotNull final File dir) {
      if (myGradleVersion.startsWith("gradle-")) {
        if (VersionComparatorUtil.compare(getGradleVersionFromPath(myGradleVersion), "8") >= 0) {
          final String file = FileUtil.getNameWithoutExtension(mySequenceFileName) + ".8." + FileUtil.getExtension(mySequenceFileName);
          //if (new File(dir, file).exists()) return file;
          return file;
        }
        if (VersionComparatorUtil.compare(getGradleVersionFromPath(myGradleVersion), "3") >= 0) {
          final String file = FileUtil.getNameWithoutExtension(mySequenceFileName) + ".3." + FileUtil.getExtension(mySequenceFileName);
          if (new File(dir, file).exists()) return file;
        }
      }
      return mySequenceFileName;
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

  protected void testTest(String project, String command, String seqFile, String gradleVersion) throws Exception {
    testTest(project, command, seqFile, gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  protected void testTest(String project, String command, String seqFile, String gradleVersion, String pattern) throws Exception {
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(project, command, seqFile);
    gradleRunConfiguration.setPatternStr(pattern);
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }
}