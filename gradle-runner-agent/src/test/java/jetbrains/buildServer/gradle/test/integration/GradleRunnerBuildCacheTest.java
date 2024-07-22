package jetbrains.buildServer.gradle.test.integration;

import java.util.List;
import java.util.Random;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED;
import static org.testng.Assert.assertTrue;

public class GradleRunnerBuildCacheTest extends GradleRunnerServiceMessageTest {

  public static final String BUILD_CMD = "clean test --build-cache --info";

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_UseTestTaskFromCache_When_TestTaskJvmArgsAreChanged(final String gradleVersion) throws Exception {
    // arrange
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_E_NAME, BUILD_CMD, null);
    config.setGradleVersion(gradleVersion);
    myTeamCitySystemProps.put("gradle.test.jvmargs", getRandomJvmArg());

    // act: first run
    List<String> buildLog1 = run(config).getAllMessages();

    // assert
    assertTrue(buildLog1.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", buildLog1));

    // act: second run
    myTeamCitySystemProps.put("gradle.test.jvmargs", getRandomJvmArg());
    List<String> buildLog2 = run(config).getAllMessages();

    // assert
    assertTrue(buildLog2.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", buildLog2));
    assertTrue(buildLog2.stream().anyMatch(it -> it.equals("> Task :test FROM-CACHE")));
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_SetJvmArgsDirectrly_When_JvmArgsProviderIsDisabled(final String gradleVersion) throws Exception {
    // arrange
    myTeamCityConfigParameters.put(GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED, "true");
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_E_NAME, BUILD_CMD, null);
    config.setGradleVersion(gradleVersion);
    myTeamCitySystemProps.put("gradle.test.jvmargs", getRandomJvmArg());

    // act: first run
    List<String> buildLog1 = run(config).getAllMessages();

    // assert
    assertTrue(buildLog1.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", buildLog1));

    // act: second run
    myTeamCitySystemProps.put("gradle.test.jvmargs", getRandomJvmArg());
    List<String> buildLog2 = run(config).getAllMessages();

    // assert
    assertTrue(buildLog2.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", buildLog2));
    assertTrue(buildLog2.stream().anyMatch(it -> it.equals("Task ':test' is not up-to-date because:")));
    assertTrue(buildLog2.stream().anyMatch(it -> it.contains("Value of input property 'systemProperties' has changed for task ':test'")));
  }

  private String getRandomJvmArg() {
    return "-Dproperty.name=" + Long.valueOf(new Random().nextLong()).toString();
  }
}

