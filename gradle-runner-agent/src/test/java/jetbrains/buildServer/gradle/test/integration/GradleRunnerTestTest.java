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
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.serverSide.BuildTypeOptions;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.Test;

import static jetbrains.buildServer.util.FileUtil.getExtension;
import static jetbrains.buildServer.util.FileUtil.getNameWithoutExtension;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 8, 2010
 */
public class GradleRunnerTestTest extends GradleRunnerServiceMessageTest {

  private static final int PROJECT_D_TEST_COUNT = 39;

  private void testTest(String project, String command, String seqFile, String gradleVersion) throws Exception {
    testTest(project, command, seqFile, gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  private void testTest(String project, String command, String seqFile, String gradleVersion, String pattern) throws Exception {
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(project, command, seqFile);
    gradleRunConfiguration.setPatternStr(pattern);
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void failedAndSkippedJUnitTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_C_NAME, "clean test", "failedProjectCJUnitSequence.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void failedAndSkippedTestNGTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_C_NAME, "clean testng", versionSpecific("failedProjectCTestNGSequence.txt", gradleVersion), gradleVersion);
  }

  private String versionSpecific(@NotNull final String fileName, @NotNull final String gradleVersion) {
    if (VersionComparatorUtil.compare(getGradleVersionFromPath(gradleVersion), "2.0") > 0) {
      return getNameWithoutExtension(fileName) + "_2." + getExtension(fileName);
    }
    return fileName;
  }

  @Test(dataProvider = "gradle-version-provider")
  public void testOutputReportingTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test", "testOutputSequence.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void stdOutputSuppressTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test -Dteamcity.ignoreTestStdOut=true", "testStdOutSuppressed.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void stdErrSuppressTest(final String gradleVersion) throws Exception {
    testTest(PROJECT_E_NAME, "clean test -Dteamcity.ignoreTestStdErr=true", "testStdErrSuppressed.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
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

  // concurrent test close does not work after version 4.4 // TODO fix
  @Test(dataProvider = "gradle-version-provider<4.4")
  public void parallelTestNgTests(final String gradleVersion) throws RunBuildException, IOException {
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(PROJECT_F_NAME,
                                                                                     "clean test",null);
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
    gatherMessage.validateTestFlows(15);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void bigErrorMessage(final String gradleVersion) throws Exception {
    testTest(PROJECT_I_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=2048 -Dteamcity.gradle.minAttachedTestException=-1", "failedProjectITest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void bigErrorExpectedAndActual(final String gradleVersion) throws Exception {
    testTest(PROJECT_J_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=100 -Dteamcity.gradle.minAttachedTestException=-1", "failedProjectJTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void attacheFailMessage(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.tempDir", myTempFiles.createTempDir().getPath());

    testTest(PROJECT_L_NAME, "clean test -Dteamcity.gradle.stacktrace.maxLength=100", "failedProjectLTest.txt", gradleVersion,
             "##teamcity\\[(test|message|publishArtifacts)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=4.4")
  public void customTestFramework(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("gradle.test.jvmargs", "-Dtest.property.alpha=ignored\n" +
                                                     "-Dtest.property.bravo=ignored");
    testTest(PROJECT_M_NAME, "clean custom", "failedProjectMTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void tmpDirectoryTest(String gradleVersion) throws Exception {
    File tempDirectory = new File(myTempFiles.getCurrentTempDir(), "my_test_directory");
    tempDirectory.mkdirs();
    myTempFiles.registerAsTempFile(tempDirectory);

    myTeamCitySystemProps.put("teamcity.build.tempDir", tempDirectory.getPath());

    testTest(PROJECT_N_NAME, "clean test", "projectNTest.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=4.7")
  @TestFor(issues = "TW-60728")
  public void testDisplayName(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.testNameFormat", "displayName");
    testTest(PROJECT_O_NAME, "clean custom", "testProjectOTestDisplayName.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=4.7")
  @TestFor(issues = "TW-60728")
  public void testWithoutDisplayName(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.testNameFormat", "name");
    testTest(PROJECT_O_NAME, "clean custom", "testProjectOTestMethodName.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=4.7")
  public void manyLinesOfOutput(final String gradleVersion) throws Exception {
    testTest(PROJECT_P_NAME, "clean custom", "testProjectP.txt", gradleVersion);
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsWithEnabledSetting(final String gradleVersion) throws Exception {
    myBuildTypeOptionValue.put(BuildTypeOptions.BT_SUPPORT_TEST_RETRY, Boolean.TRUE);
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=2", "testMultiProjectEWithEnabledSetting.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsOnlySubmoduleWithPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -b projectA/build.gradle -PmaxRetriesProperty=2", "testMultiProjectEOnlySubmoduleWithPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsOnlySubmoduleWithoutPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -b projectB/build.gradle -PmaxRetriesProperty=2", "testMultiProjectEOnlySubmoduleWithoutPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsAllModulesWithOnePlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=2", "testMultiProjectEAllModulesWithOnePlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  @TestFor(issues = "TW-64037")
  public void testRerunTestsAllModulesDisabledPlugin(final String gradleVersion) throws Exception {
    testTest(MULTI_PROJECT_E_NAME, "clean test -PmaxRetriesProperty=0", "testMultiProjectEAllModulesDisabledPlugin.txt", gradleVersion,
             "##teamcity\\[(test|message|testRetrySupport)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-last-version-provider")
  public void testIgnoreDefaultDistributionSuiteNames(final String gradleVersion) throws Exception {
    testTest(PROJECT_Q_NAME, "clean test", "testIgnoreDefaultDistributionSuiteNames.txt", gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-last-version-provider")
  public void testIgnoreCustomSuiteNames(final String gradleVersion) throws Exception {
    myTeamCityConfigParameters.put("teamcity.internal.gradle.ignoredSuiteFormat", "(ignored)|(42)");
    testTest(PROJECT_Q_NAME, "clean test", "testIgnoreCustomSuiteNames.txt", gradleVersion, "##teamcity\\[test(.*?)(?<!\\|)\\]");
  }
}
