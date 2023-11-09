package jetbrains.buildServer.gradle.test.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelector;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class GradleLaunchModeSelectorTest {

  @DataProvider
  public Object[][] gradleVersionProvider() {
    return new Object[][]{
      { "0.9", GradleLaunchMode.GRADLE },
      { "1.0", GradleLaunchMode.GRADLE },
      { "1.1", GradleLaunchMode.GRADLE },
      { "2.0", GradleLaunchMode.GRADLE },
      { "2.6", GradleLaunchMode.GRADLE },
      { "2.7", GradleLaunchMode.GRADLE },
      { "3.0", GradleLaunchMode.GRADLE },
      { "4.0", GradleLaunchMode.GRADLE },
      { "4.7", GradleLaunchMode.GRADLE },
      { "4.8", GradleLaunchMode.GRADLE },
      { "4.9", GradleLaunchMode.GRADLE },
      { "5.0", GradleLaunchMode.GRADLE },
      { "5.5", GradleLaunchMode.GRADLE },
      { "6.0", GradleLaunchMode.GRADLE },
      { "7.4", GradleLaunchMode.GRADLE },
      { "7.9", GradleLaunchMode.GRADLE },

      { "8.0", GradleLaunchMode.GRADLE_TOOLING_API },
      { "8.0.1", GradleLaunchMode.GRADLE_TOOLING_API },
      { "8.1", GradleLaunchMode.GRADLE_TOOLING_API },
      { "8.1.1", GradleLaunchMode.GRADLE_TOOLING_API },
      { "9.1.3", GradleLaunchMode.GRADLE_TOOLING_API },

      { "0", GradleLaunchMode.UNDEFINED },
      { "!", GradleLaunchMode.UNDEFINED },
      { "81", GradleLaunchMode.UNDEFINED },
      { "ver", GradleLaunchMode.UNDEFINED },
      { "������", GradleLaunchMode.UNDEFINED },
      { "!", GradleLaunchMode.UNDEFINED },
      { null, GradleLaunchMode.UNDEFINED },
    };
  }

  @Test(dataProvider = "gradleVersionProvider")
  public void shouldReturnGradleLaunchModeAsExpected(String gradleVersion, GradleLaunchMode expectedMode) {
    // when
    GradleLaunchMode result = GradleLaunchModeSelector.getByGradleVersion(gradleVersion, "").getLaunchMode();

    // then
    assertEquals(result, expectedMode);
  }

  @Test
  public void shouldReturnGradleLaunchModeWhenCorrespondingConfigurationParameterIsAbsent() {
    // given
    Map<String, String> configurationParameters = Collections.emptyMap();
    GradleLaunchMode expectedMode = GradleLaunchMode.GRADLE;

    // when
    GradleLaunchMode result = GradleLaunchModeSelector.selectMode(configurationParameters, null).getLaunchMode();

    // then
    assertEquals(result, expectedMode);
  }

  @Test
  public void shouldReturnGradleLaunchModeWhenCorrespondingConfigurationParameterSet() {
    // given
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    GradleLaunchMode expectedMode = GradleLaunchMode.GRADLE_TOOLING_API;

    // when
    GradleLaunchMode result = GradleLaunchModeSelector.selectMode(configurationParameters, null).getLaunchMode();

    // then
    assertEquals(result, expectedMode);
  }

  @Test
  public void shouldReturnToolingAPiLaunchModeWhenCorrespondingConfigurationParameterSet() {
    // given
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_GRADLE_LAUNCH_MODE);
    GradleLaunchMode expectedMode = GradleLaunchMode.GRADLE;

    // when
    GradleLaunchMode result = GradleLaunchModeSelector.selectMode(configurationParameters, null).getLaunchMode();

    // then
    assertEquals(result, expectedMode);
  }
}
