package jetbrains.buildServer.gradle.test.integration;

import java.util.List;
import java.util.Random;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_READ_ALL_CONFIG_PARAM;
import static org.testng.Assert.assertTrue;

public class GradleRunnerConfigurationCacheTest extends GradleRunnerServiceMessageTest {

  public static final String CONFIGURATION_CACHE_CMD = "--configuration-cache";
  public static final String BUILD_CMD = "clean compileJava compileTest";

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldReuseConfigurationCache(final String gradleVersion) throws Exception {
    // given
    myTeamCitySystemProps.put("build.number", String.valueOf(new Random().nextInt()));
    final GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_B_NAME, CONFIGURATION_CACHE_CMD + " " + BUILD_CMD + " --stacktrace", null);
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("##tc-property.*");

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));

    // when: second run
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused and build is successful
    final String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("> Configure project ")), buildFullLog);
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("##tc-property ")), buildFullLog);

    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotReuseConfigurationCacheWhenStaticParameterChanges(final String gradleVersion) throws Exception {
    // given
    String customParam = "Custom Parameter";
    myTeamCitySystemProps.put("custom.static.property", customParam);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_STATIC_PROPERTY_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run with reading the dynamic parameter from teamcity.build.parameters
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused and build is successful
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("> Configure project ")), buildFullLog);
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("##tc-property ")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);

    // when: changing custom property and run build again
    customParam = "Custom Parameter Changed";
    myTeamCitySystemProps.put("system.custom.static.property", customParam);
    config = new GradleRunConfiguration(PROJECT_WITH_STATIC_PROPERTY_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);
    messages = run(config).getAllMessages();

    // then: configuration cache couldn't be reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Calculating task graph as configuration cache cannot be reused")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.contains("teamcity.build.parameters.static' has changed")), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotReuseConfigurationCacheWhenReadingDynamicParameters(final String gradleVersion) throws Exception {
    // given
    myTeamCitySystemProps.put("build.number", String.valueOf(new Random().nextInt()));
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_DYNAMIC_PROPERTIES_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run with reading the dynamic parameter from teamcity.build.parameters
    messages = run(config).getAllMessages();

    // then: configuration cache couldn't be reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Calculating task graph as configuration cache cannot be reused")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.contains("teamcity.build.parameters' has changed")), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldReadDynamicPropertyOnDemandOnly(String gradleVersion) throws Exception {
    // given: dynamic property that used in the gradle project
    // build.number is TC's property that changes from build to build
    myTeamCitySystemProps.put("build.number", String.valueOf(new Random().nextInt()));
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
                                                               "clean build printBuildNumber",
                                                               null);
    config.setPatternStr("##build-num(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    List<String> messages = run(config).getAllMessages();

    // then: by default, we don't read dynamic properties because of configuration-cache feature
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD FAILED")), "Expected: BUILD FAILED\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.contains("Could not get unknown property 'build.number'")), buildFullLog);

    // when: setting up the corresponding configuration parameter and trying again
    myTeamCityConfigParameters.put(GRADLE_RUNNER_READ_ALL_CONFIG_PARAM, "true");
    messages = run(config).getAllMessages();

    // then: property has successfully been printed
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##build-num: ")), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldAccessSystemPropertyWithoutConfigurationCache(String gradleVersion) throws Exception {
    // given: preconfigured system property that used in the gradle project
    myTeamCitySystemProps.put("my_custom_property", "My Custom Property Value");
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
                                                               "clean build printSystemProperty printSystemPropertyFromProject",
                                                               "printPropertiesConfigurationCache.txt");
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);

    // when / then: system property is available in the project
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotAccessSystemPropertyFromProjectWithConfigurationCacheEnabled(String gradleVersion) throws Exception {
    // given: preconfigured system property that used in the gradle project
    myTeamCitySystemProps.put("my_custom_property", "My Custom Property Value");
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
                                                               "clean build printSystemPropertyFromProject" + " " + CONFIGURATION_CACHE_CMD,
                                                               null);
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    final List<String> messages = run(config).getAllMessages();

    // then: using properties during task execution with configuration cache enabled is unsupported by Gradle
    // See https://docs.gradle.org/8.2/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD FAILED")), "Expected: BUILD FAILED\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache problems found in this build")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.contains("invocation of 'Task.project' at execution time is unsupported")), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotAccessSystemPropertyWithConfigurationCacheEnabled(String gradleVersion) throws Exception {
    // given: preconfigured system property that used in the gradle project
    myTeamCitySystemProps.put("my_custom_property", "My Custom Property Value");
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
                                                               "clean build printSystemProperty" + " " + CONFIGURATION_CACHE_CMD,
                                                               null);
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    final List<String> messages = run(config).getAllMessages();

    // then: using properties during task execution with configuration cache enabled is unsupported by Gradle
    // See https://docs.gradle.org/8.2/userguide/configuration_cache.html#config_cache:requirements:use_project_during_execution
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD FAILED")), "Expected: BUILD FAILED\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.contains("Could not get unknown property 'my_custom_property'")), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldRunTestsWithConfigurationCache(final String gradleVersion) throws Exception {
    // given
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_E_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));

    // when: second run / then: tests ran with configuration cache reused
    config = new GradleRunConfiguration(PROJECT_E_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, "testsWithConfigurationCache");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("(##teamcity\\[(.*?)(?<!\\|)\\])|(Reusing configuration cache)");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldRunJupiterTestsWithConfigurationCache(final String gradleVersion) throws Exception {
    // given
    GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_B_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));

    // when: second run / then: tests ran with configuration cache reused
    config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_B_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, "testsWithConfigurationCacheJupiter");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("(##teamcity\\[importData (.*?)(?<!\\|)\\])|(Reusing configuration cache)");
    runAndCheckServiceMessages(config);
  }
}

