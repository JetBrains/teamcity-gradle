

package jetbrains.buildServer.gradle.test.toolingApiTests.integration;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.test.integration.FlowServiceMessageReceiver;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeOptions;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static jetbrains.buildServer.util.FileUtil.getExtension;
import static jetbrains.buildServer.util.FileUtil.getNameWithoutExtension;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 8, 2010
 */
public class GradleRunnerTestTestForToolingApi extends GradleRunnerServiceMessageTestForToolingApi {

  private static final int PROJECT_D_TEST_COUNT = 39;

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedAndSkippedJUnitTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_C_NAME, "clean junit", "failedProjectCJUnitSequence.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedAndSkippedTestNGTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_C_NAME, "clean testng", versionSpecific("failedProjectCTestNGSequence.txt", gradleVersion), gradleVersion);
  }

  private String versionSpecific(@NotNull final String fileName, @NotNull final String gradleVersion) {
    if (VersionComparatorUtil.compare(getGradleVersionFromPath(gradleVersion), "2.0") > 0) {
      return getNameWithoutExtension(fileName) + "_2." + getExtension(fileName);
    }
    return fileName;
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testOutputReportingTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test", "testOutputSequence.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void stdOutputSuppressTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test -Dteamcity.ignoreTestStdOut=true", "testStdOutSuppressed.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void stdErrSuppressTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test -Dteamcity.ignoreTestStdErr=true", "testStdErrSuppressed.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void parallelTestSuiteTest(final String gradleVersion) throws RunBuildException, IOException {
    myTeamCitySystemProps.put("gradle.test.jvmargs", "-Dtest.property.alpha=valueAlpha\n" +
                                                     "-Dtest.property.bravo=valueBravo");

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(PROJECT_D_NAME,
                                                                                     "clean testParallel",null);
    gradleRunConfiguration.setPatternStr("##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    final Mockery ctx = initContext(gradleRunConfiguration.getProject(), gradleRunConfiguration.getCommand(),
                                    gradleRunConfiguration.getGradleVersion());
    final FlowServiceMessageReceiver gatherMessage = new FlowServiceMessageReceiver();
    gatherMessage.setPattern(gradleRunConfiguration.getPatternStr());
    final Expectations gatherServiceMessage = new Expectations() {{
      allowing(myMockLogger).message(with(any(String.class))); will(gatherMessage);
      allowing(myMockLogger).warning(with(any(String.class))); will(reportWarning);
    }};

    runTest(gatherServiceMessage, ctx);
    gatherMessage.validateTestFlows(PROJECT_D_TEST_COUNT);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void bigErrorMessage(final String gradleVersion) throws Exception {
    testTest(PROJECT_I_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=2048 -Dteamcity.gradle.minAttachedTestException=-1", "failedProjectITest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void bigErrorExpectedAndActual(final String gradleVersion) throws Exception {
    testTest(PROJECT_J_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=100 -Dteamcity.gradle.minAttachedTestException=-1", "failedProjectJTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void attacheFailMessage(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.tempDir", myTempFiles.createTempDir().getPath());

    testTest(PROJECT_L_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=100", "failedProjectLTest.txt", gradleVersion,
             "##teamcity\\[(test|message|publishArtifacts)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void customTestFramework(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("gradle.test.jvmargs", "-Dtest.property.alpha=ignored\n" +
                                                     "-Dtest.property.bravo=ignored");
    testTest(PROJECT_M_NAME, "clean custom", "failedProjectMTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void tmpDirectoryTest(String gradleVersion) throws Exception {
    File tempDirectory = new File(myTempFiles.getCurrentTempDir(), "my_test_directory");
    tempDirectory.mkdirs();
    myTempFiles.registerAsTempFile(tempDirectory);

    myTeamCitySystemProps.put("teamcity.build.tempDir", tempDirectory.getPath());

    testTest(PROJECT_N_NAME, "clean test", "projectNTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-60728")
  public void testDisplayName(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.testNameFormat", "displayName");
    testTest(PROJECT_O_NAME, "clean custom", "testProjectOTestDisplayName.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-60728")
  public void testWithoutDisplayName(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.testNameFormat", "name");
    testTest(PROJECT_O_NAME, "clean custom", "testProjectOTestMethodName.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void manyLinesOfOutput(final String gradleVersion) throws Exception {
    testTest(PROJECT_P_NAME, "clean custom", "testProjectP.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsWithEnabledSetting(final String gradleVersion) throws Exception {
    myBuildTypeOptionValue.put(BuildTypeOptions.BT_SUPPORT_TEST_RETRY, Boolean.TRUE);
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=2", "testMultiProjectEWithEnabledSetting.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsOnlySubmoduleWithPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, ":projectA:clean :projectA:test -PmaxRetriesProperty=2", "toolingApi/testMultiProjectEOnlySubmoduleWithPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsOnlySubmoduleWithoutPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, ":projectB:clean :projectB:test -PmaxRetriesProperty=2", "testMultiProjectEOnlySubmoduleWithoutPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsAllModulesWithOnePlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=2", "toolingApi/testMultiProjectEAllModulesWithOnePlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsAllModulesDisabledPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=0", "testMultiProjectEAllModulesDisabledPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testIgnoreDefaultDistributionSuiteNames(final String gradleVersion) throws Exception {
    testTest(PROJECT_Q_NAME, "clean test", "testIgnoreDefaultDistributionSuiteNames.txt", gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testIgnoreCustomSuiteNames(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.ignoredSuiteFormat", "(ignored)|(42)");
    testTest(PROJECT_Q_NAME, "clean test", "testIgnoreCustomSuiteNames.txt", gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testEscapingServiceMessage(final String gradleVersion) throws Exception {
    final String test = SystemInfo.isWindows ? "my.SimpleTest" : "my.PrintTest";
    final String expected = SystemInfo.isWindows ? "testEscapingServiceMessage.win.txt" : "testEscapingServiceMessage.unix.txt";
    testTest(PROJECT_PRINT_NAME, "clean test --tests " + test, expected, gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testComparisonServiceMessage(final String gradleVersion) throws Exception {
    testTest(PROJECT_PRINT_NAME, "clean test --tests my.ComparisonTest", "toolingApi/testComparisonServiceMessage.txt", gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void splitLongOutput(final String gradleVersion) throws Exception {
    testTest(PROJECT_T_NAME, "clean test -Dteamcity.gradle.message.maxLength=100", "splitLongOutput.txt", gradleVersion, "##teamcity\\[testStd(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void splitLongOutputWithoutServiceMessages(final String gradleVersion) throws Exception {
    testTest(PROJECT_T_NAME, "clean test -Dteamcity.gradle.message.maxLength=100 -Dteamcity.gradle.message.parser.type=disabled", "splitLongOutputWithoutServiceMessages.txt", gradleVersion, "##teamcity\\[testStd(.*?)(?<!\\|)\\]");
  }
}
