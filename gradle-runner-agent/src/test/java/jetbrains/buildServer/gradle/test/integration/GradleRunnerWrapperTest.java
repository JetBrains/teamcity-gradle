

package jetbrains.buildServer.gradle.test.integration;

import jetbrains.buildServer.gradle.GradleRunnerConstants;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
}