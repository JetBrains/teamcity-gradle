

package jetbrains.buildServer.gradle.test.integration;

import jetbrains.buildServer.gradle.GradleRunnerConstants;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public class GradleRunnerCompileTest extends GradleRunnerServiceMessageTest {

  public static final String BUILD_CMD = "clean compileJava compileTest";
  private static final boolean isJre8 = System.getProperty("java.specification.version").contains("1.8");
  private static final boolean isJre7 = System.getProperty("java.specification.version").contains("1.7");
  private static final boolean isJre6 = System.getProperty("java.specification.version").contains("1.6");
  private static final boolean isJre5 = System.getProperty("java.specification.version").contains("1.5");
  private static final String COMPILATION_BLOCK_PROPS_MSGS_PATTERN = "##teamcity\\[(message|compilation|block)(.*?)(?<!\\|)\\]|##tc-property.*";
  private static final String COMPILATION_BLOCK_PROPS_MSGS_FLOW_PATTERN = "##teamcity\\[(message|compilation|block|flow)(.*?)(?<!\\|)\\]|##tc-property.*";
  private static final String COMPILATION_MSGS_PATTERN = "##teamcity\\[(message|compilation)(.*?)(?<!\\|)\\]";

  @Test(dataProvider = "gradle-version-provider")
  public void successfulCompileTest(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("property_alpha", "value_alpha");
    myTeamCitySystemProps.put("property.bravo", "value bravo");
    myTeamCitySystemProps.put("property charlie", "value charlie");

    GradleRunConfiguration config = new GradleRunConfiguration(MULTI_PROJECT_A_NAME, BUILD_CMD + " printProperties", "mProjectABlockSequence.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  // junit version 5 doesn't work on gradle version 2.0
  @Test(dataProvider = "gradle-version-provider>2.0")
  public void configureOnDemand(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_A_NAME, BUILD_CMD + " -b sub-module1/build.gradle", "compileDemandMultiProjectABlockSequence.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void failedCompileTest(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = null;
    // Compilation errors output differs on different javac versions
    if (isJre5) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_5.txt");
    } else if (isJre6) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_6.txt");
    } else if (isJre7 || isJre8) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_7.txt");
    } else {
      fail("Compiler test requires JRE version 5,6,7 or 8 to run; Current version: " + System.getProperty("java.specification.version"));
    }
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "8 > gradle-version-provider >= 3.0")
  public void failedParallelCompileTestLess8(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("MultiProjectD", "clean build --parallel", "failedCompilationParallel.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##teamcity\\[message text='[^:](.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedParallelCompileTest8AndHigher(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("MultiProjectD", "clean build --parallel", "failedCompilationParallel.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##teamcity\\[(message)(.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void failedKotlinCompileTest(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("projectKotlinBroken", "clean build", "failedKotlinCompilation.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldDetectCompilationErrorsWithHugeAmountOfTasksAndStdErrOutput(final String gradleVersion) throws Exception {
    // given
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_A_NAME, "clean build --continue", "testsWithHugeAmountOfTasksAndStdErrOutput.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##tc-error-output.*|##teamcity\\[(message|compilation)(.*?)(?<!\\|)\\]");

    // when / then
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void pathToBuildGradleTest(final String gradleVersion)  throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("subdir", "clean build", "projectABlockSequence.txt");
    myRunnerParams.put(GradleRunnerConstants.PATH_TO_BUILD_FILE, "projectA/run.gradle");
    try {
      config.setGradleVersion(gradleVersion);
      config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_FLOW_PATTERN);
      runAndCheckServiceMessages(config);
    } finally {
      myRunnerParams.remove(GradleRunnerConstants.PATH_TO_BUILD_FILE);
    }
  }

  @Test(dataProvider = "gradle-version-provider")
  public void buildProblemTest(final String gradleVersion)  throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_BROKEN_NAME, "build", "buildProblemTest.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##teamcity\\[buildProblem (.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }
}