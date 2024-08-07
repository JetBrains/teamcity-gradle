package jetbrains.buildServer.gradle.test.unit;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelector;
import org.gradle.util.internal.DefaultGradleVersion;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class GradleLaunchModeSelectorTest {

  private final GradleLaunchModeSelector gradleLaunchModeSelector = new GradleLaunchModeSelector();
  private Mockery context;
  private BuildProgressLogger logger;

  @BeforeMethod
  public void setUp() {
    context = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};

    logger = context.mock(BuildProgressLogger.class);
    context.checking(new Expectations() {{
      allowing(logger).warning(with(any(String.class)));
    }});
  }

  @DataProvider
  public Object[][] gradleVersionProvider() {
    return new Object[][]{
      { "0.9", GradleLaunchMode.COMMAND_LINE},
      { "1.0", GradleLaunchMode.COMMAND_LINE},
      { "1.1", GradleLaunchMode.COMMAND_LINE},
      { "2.0", GradleLaunchMode.COMMAND_LINE},
      { "2.6", GradleLaunchMode.COMMAND_LINE},
      { "2.7", GradleLaunchMode.COMMAND_LINE},
      { "3.0", GradleLaunchMode.COMMAND_LINE},
      { "4.0", GradleLaunchMode.COMMAND_LINE},
      { "4.7", GradleLaunchMode.COMMAND_LINE},
      { "4.8", GradleLaunchMode.COMMAND_LINE},
      { "4.9", GradleLaunchMode.COMMAND_LINE},
      { "5.0", GradleLaunchMode.COMMAND_LINE},
      { "5.5", GradleLaunchMode.COMMAND_LINE},
      { "6.0", GradleLaunchMode.COMMAND_LINE},
      { "7.4", GradleLaunchMode.COMMAND_LINE},
      { "7.9", GradleLaunchMode.COMMAND_LINE},

      { "8.0", GradleLaunchMode.TOOLING_API},
      { "8.0.1", GradleLaunchMode.TOOLING_API},
      { "8.1", GradleLaunchMode.TOOLING_API},
      { "8.1.1", GradleLaunchMode.TOOLING_API},
      { "9.1.3", GradleLaunchMode.TOOLING_API},
    };
  }
  @Test(dataProvider = "gradleVersionProvider")
  public void should_ReturnExpectedLaunchMode_When_CorrectGradleVersionPassed(String gradleVersion, GradleLaunchMode expectedMode) {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(DefaultGradleVersion.version(gradleVersion))
                                                                                                     .withConfigurationCacheEnabled(true)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, expectedMode);
  }

  @Test
  public void should_ReturnToolingApiLaunchMode_When_VersionIsConfigurationCacheCompatible_And_ConfigurationCacheIsEnabled() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(DefaultGradleVersion.version("8.0"))
                                                                                                     .withConfigurationCacheEnabled(true)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, GradleLaunchMode.TOOLING_API);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_VersionIsConfigurationCacheCompatible_And_ConfigurationCacheIsDisabled() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(DefaultGradleVersion.version("8.0"))
                                                                                                     .withConfigurationCacheEnabled(false)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, GradleLaunchMode.COMMAND_LINE);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_VersionIsConfigurationCacheCompatible_And_ConfigurationCacheIsEnabled_And_ConfigurationCacheProblemsDisabled() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(DefaultGradleVersion.version("8.0"))
                                                                                                     .withConfigurationCacheEnabled(true)
                                                                                                     .withConfigurationCacheProblemsIgnored(true)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, GradleLaunchMode.COMMAND_LINE);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_VersionIsConfigurationCacheCompatible_And_ConfigurationCacheIsEnabled_And_UnsupportedArgsPassed() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");
    Set<String> unsupported = new HashSet<>();
    unsupported.add("--daemon");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(DefaultGradleVersion.version("8.0"))
                                                                                                     .withConfigurationCacheEnabled(true)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(unsupported)
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, GradleLaunchMode.COMMAND_LINE);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_GradleVersionNotDetected() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(null)
                                                                                                     .withConfigurationCacheEnabled(false)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, GradleLaunchMode.COMMAND_LINE);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_CorrespondingConfigurationParameterIsAbsent() {
    // arrange
    Map<String, String> configurationParameters = Collections.emptyMap();
    GradleLaunchMode expectedMode = GradleLaunchMode.COMMAND_LINE;

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(null)
                                                                                                     .withConfigurationCacheEnabled(false)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, expectedMode);
  }

  @Test
  public void should_ReturnGradleLaunchMode_When_CorrespondingConfigurationParameterSet() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    GradleLaunchMode expectedMode = GradleLaunchMode.TOOLING_API;

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(null)
                                                                                                     .withConfigurationCacheEnabled(false)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // arrange
    assertEquals(result, expectedMode);
  }

  @Test
  public void should_ReturnToolingApiLaunchMode_When_CorrespondingConfigurationParameterSet() {
    // arrange
    Map<String, String> configurationParameters = new HashMap<>();
    configurationParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_COMMAND_LINE_LAUNCH_MODE);
    GradleLaunchMode expectedMode = GradleLaunchMode.COMMAND_LINE;

    // act
    GradleLaunchMode result = gradleLaunchModeSelector.selectMode(GradleLaunchModeSelector.Parameters.builder()
                                                                                                     .withLogger(logger)
                                                                                                     .withConfigurationParameters(configurationParameters)
                                                                                                     .withGradleVersion(null)
                                                                                                     .withConfigurationCacheEnabled(false)
                                                                                                     .withConfigurationCacheProblemsIgnored(false)
                                                                                                     .withUnsupportedByToolingArgs(Collections.emptySet())
                                                                                                     .build()).getLaunchMode();

    // assert
    assertEquals(result, expectedMode);
  }
}
