

package jetbrains.buildServer.gradle.test.toolingApiTests.integration;

import jetbrains.buildServer.gradle.GradleRunnerConstants;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public class GradleRunnerCompileTestForToolingApi extends GradleRunnerServiceMessageTestForToolingApi {

  public static final String BUILD_CMD = "clean compileJava compileTest";
  private static final boolean isJre8 = System.getProperty("java.specification.version").contains("1.8");
  private static final boolean isJre7 = System.getProperty("java.specification.version").contains("1.7");
  private static final boolean isJre6 = System.getProperty("java.specification.version").contains("1.6");
  private static final boolean isJre5 = System.getProperty("java.specification.version").contains("1.5");
  private static final String COMPILATION_BLOCK_PROPS_MSGS_PATTERN = "##teamcity\\[(message|compilation|block)(.*?)(?<!\\|)\\]|##tc-property.*";
  private static final String COMPILATION_BLOCK_PROPS_MSGS_FLOW_PATTERN = "##teamcity\\[(message|compilation|block|flow)(.*?)(?<!\\|)\\]|##tc-property.*";
  private static final String COMPILATION_MSGS_PATTERN = "##teamcity\\[(message|compilation)(.*?)(?<!\\|)\\]";

  @Test(dataProvider = "gradle-version-provider>=8")
  public void successfulCompileTest(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("property_alpha", "value_alpha");
    myTeamCitySystemProps.put("property.bravo", "value bravo");
    myTeamCitySystemProps.put("property charlie", "value charlie");

    GradleRunConfiguration config = new GradleRunConfiguration(MULTI_PROJECT_A_NAME, BUILD_CMD + " printProperties", "mProjectABlockSequence.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  // JUnit version 5 doesn't work on Gradle version 2.0. The build file option is not supported in Gradle 9+, so this test setup is not possible on 9+.
  @Test(dataProvider = "gradle-version-provider=8.x")
  public void configureOnDemand(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_A_NAME, BUILD_CMD + " -b sub-module1/build.gradle", "compileDemandMultiProjectABlockSequence.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedCompileTest(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = null;
    // Compilation errors output differs on different javac versions
    if (isJre5) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_5.txt");
    } else if (isJre6) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_6.txt");
    } else {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "toolingApi/failedCompilationSequence1_7.txt");
    }
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedParallelCompileTest8AndHigher(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("MultiProjectD", "clean build --parallel", "toolingApi/failedCompilationParallel.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##teamcity\\[(message)(.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void failedKotlinCompileTest(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("projectKotlinBroken", "clean build", "toolingApi/failedKotlinCompilation.txt");
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

  // This test is not relevant for Gradle 9+ because the build file argument was deprecated
  @Test(dataProvider = "gradle-version-provider=8.x")
  public void pathToBuildGradleTest(final String gradleVersion)  throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("subdir", "clean build", "toolingApi/projectABlockSequence.txt");
    myRunnerParams.put(GradleRunnerConstants.PATH_TO_BUILD_FILE, "projectA/run.gradle");
    try {
      config.setGradleVersion(gradleVersion);
      config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_FLOW_PATTERN);
      runAndCheckServiceMessages(config);
    } finally {
      myRunnerParams.remove(GradleRunnerConstants.PATH_TO_BUILD_FILE);
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void buildProblemTest(final String gradleVersion)  throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_BROKEN_NAME, "build", "buildProblemTest.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##teamcity\\[buildProblem (.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }
}
