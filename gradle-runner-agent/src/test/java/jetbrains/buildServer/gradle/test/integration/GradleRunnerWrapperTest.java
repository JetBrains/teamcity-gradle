package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.util.List;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.test.GradleTestUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 11, 2010
 */
public class GradleRunnerWrapperTest extends GradleRunnerServiceMessageTest {
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.clear();
  }

  @Test
  public void simpleWrapperTest() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    GradleRunConfiguration config = new GradleRunConfiguration(WRAPPED_PROJECT_A_NAME, "clean", "wrappedProjASequence_clean.txt");
    config.setPatternStr("^Downloading(.*)|^Unzipping(.*)|##teamcity\\[(.*?)(?<!\\|)\\]");
    config.setGradleVersion("1.0");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_Launch_Build_Via_Command_Line_When_GradleWrapperPropertiesIsLocatedInNonStandardDirectory(final String gradleVersion) throws Exception {
    // arrange
    String buildCmd = "clean build";
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_WRAPPER_PROPERTIES_NON_STANDARD_LOCATION, buildCmd, null);
    config.setGradleVersion(gradleVersion);
    Mockery ctx = initContext(config.getProject(), config.getCommand(), config.getGradleVersion());
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    File initScript = new File(ourProjectRoot, GradleTestUtil.REL_SCRIPT_DIR + "/" + GradleRunnerConstants.INIT_SCRIPT_NAME);
    myRunnerParams.put(GradleRunnerConstants.GRADLE_INIT_SCRIPT, initScript.getAbsolutePath());

    // act
    List<String> messages = run(config, ctx).getAllMessages();

    // assert
    String messagesStr = StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + messagesStr);
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("Gradle will be launched via Tooling API")),
               "Expected: build to be launched via Tooling API\nFull log:\n" + messagesStr);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_Take_Custom_Location_When_GradleWrapperPropertiesIsLocatedInNonStandardDirectory(final String gradleVersion) throws Exception {
    // arrange
    String buildCmd = "clean build";
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_WRAPPER_PROPERTIES_NON_STANDARD_LOCATION, buildCmd, null);
    config.setGradleVersion(gradleVersion);
    Mockery ctx = initContext(config.getProject(), config.getCommand(), config.getGradleVersion());
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_WRAPPER_PROPERTIES_PATH_CONFIG_PARAM, "wrapper");

    // act
    List<String> messages = run(config, ctx).getAllMessages();

    // assert
    String messagesStr = StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + messagesStr);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Gradle will be launched via Tooling API")),
               "Expected: build to be launched via Tooling API\nFull log:\n" + messagesStr);
  }
}