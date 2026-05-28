package jetbrains.buildServer.gradle.test.integration;

import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES_CONFIG_PARAM;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_READ_ALL_CONFIG_PARAM;
import static org.testng.Assert.assertTrue;

public class GradleRunnerConfigurationCacheTest extends GradleRunnerServiceMessageTest {

  public static final String CONFIGURATION_CACHE_CMD = "--configuration-cache";
  public static final String BUILD_CMD = "clean compileJava compileTest";

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldReuseConfigurationCacheWhenConfigurationPhaseInputsAreUnchanged(final String gradleVersion) throws Exception {
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
  public void shouldNotReuseConfigurationCacheWhenAStaticParameterAccessedThroughProjectExtChanges(final String gradleVersion) throws Exception {
    // given: a static property that is used in the Gradle project
    String staticPropValue = "My Custom Property Value";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printSystemPropertyFromProjectExt " + CONFIGURATION_CACHE_CMD,
            null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run, no parameters are changed
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused and build is successful
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);

    // when: third run, the custom parameter value is changed
    staticPropValue = "My Custom Property Value Changed";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printSystemPropertyFromProjectExt " + CONFIGURATION_CACHE_CMD,
            null);
    config.setGradleVersion(gradleVersion);
    messages = run(config).getAllMessages();

    // then: configuration cache couldn't be reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line ->
                    line.startsWith("Calculating task graph as configuration cache cannot be reused because TeamCity build properties collection has changed.")),
            "Expected a log line explaining why the cache could not be reused.\n" + buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotReuseConfigurationCacheWhenAStaticParameterAccessedThroughProjectExtTeamcityChanges(final String gradleVersion) throws Exception {
    // given: a static property that is used in the Gradle project
    String staticPropValue = "My Custom Property Value";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
      "clean build printSystemPropertyFromProjectExtTeamcity " + CONFIGURATION_CACHE_CMD,
      null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run, no parameters are changed
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused and build is successful
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);

    // when: third run, the custom parameter value is changed
    staticPropValue = "My Custom Property Value Changed";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
      "clean build printSystemPropertyFromProjectExtTeamcity " + CONFIGURATION_CACHE_CMD,
      null);
    config.setGradleVersion(gradleVersion);
    messages = run(config).getAllMessages();

    // then: configuration cache couldn't be reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line ->
        line.startsWith("Calculating task graph as configuration cache cannot be reused because TeamCity build properties collection has changed.")),
      "Expected a log line explaining why the cache could not be reused.\n" + buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldNotReuseConfigurationCacheWhenADynamicParameterAccessedThroughProjectExtTeamcityChanges(final String gradleVersion) throws Exception {
    // given: a dynamic property that is used in the Gradle project
    String dynamicPropValue = String.valueOf(new Random().nextInt());
    myTeamCitySystemProps.put("build.number", dynamicPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printBuildNumberFromProjectExtTeamcity " + CONFIGURATION_CACHE_CMD,
            null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run, no parameters are changed
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused and build is successful
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);

    // when: third run, the dynamic parameter value is changed
    dynamicPropValue = String.valueOf(new Random().nextInt());
    myTeamCitySystemProps.put("build.number", dynamicPropValue);
    config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printBuildNumberFromProjectExtTeamcity " + CONFIGURATION_CACHE_CMD,
            null);
    config.setGradleVersion(gradleVersion);
    messages = run(config).getAllMessages();

    // then: configuration cache couldn't be reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line ->
                    line.startsWith("Calculating task graph as configuration cache cannot be reused because TeamCity build property 'build.number' has changed.")),
            "Expected a log line explaining why the cache could not be reused.\n" + buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void dynamicAndStaticPropertiesShouldBeAvailableOnProjectExtAndProjectTeamcityWhenCCIsDisabled(String gradleVersion) throws Exception {
    // given: a dynamic property and a static property that are used in the Gradle project
    String dynamicPropValue = String.valueOf(new Random().nextInt());
    myTeamCitySystemProps.put("build.number", dynamicPropValue);
    String staticPropValue = "My Custom Property Value";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printBuildNumber printSystemProperty",
            null);
    config.setPatternStr("##(?:build-num|system-property)(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    List<String> messages = run(config).getAllMessages();

    // then: both dynamic and static properties are available both through project.ext and through project.ext.teamcity
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##build-num from project.ext: " + dynamicPropValue)), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##build-num from project.ext.teamcity: " + dynamicPropValue)), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##system-property from project.ext: " + staticPropValue)), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##system-property from project.ext.teamcity: " + staticPropValue)), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void staticPropertiesShouldBeAvailableOnProjectExtAndProjectTeamcityWhenCCIsEnabled(String gradleVersion) throws Exception {
    // given: a static property that is used in the Gradle project
    String staticPropValue = "My Custom Property Value";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printSystemProperty",
            null);
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    List<String> messages = run(config).getAllMessages();

    // then: static properties are available both through project.ext and through project.ext.teamcity
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##system-property from project.ext: " + staticPropValue)), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##system-property from project.ext.teamcity: " + staticPropValue)), buildFullLog);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void dynamicPropertiesShouldBeAvailableOnlyOnProjectTeamcityWhenCCIsEnabled(String gradleVersion) throws Exception {
    // given: a dynamic property and a static property that are used in the Gradle project
    String dynamicPropValue = String.valueOf(new Random().nextInt());
    myTeamCitySystemProps.put("build.number", dynamicPropValue);
    String staticPropValue = "My Custom Property Value";
    myTeamCitySystemProps.put("my_custom_property", staticPropValue);
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_READING_PROPERTIES_NAME,
            "clean build printBuildNumber printSystemProperty" + " " + CONFIGURATION_CACHE_CMD,
            null);
    config.setPatternStr("##(?:build-num|system-property)(.*)");
    config.setGradleVersion(gradleVersion);

    // when
    List<String> messages = run(config).getAllMessages();

    // then: both dynamic and static properties are available both through project.ext and through project.ext.teamcity
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##build-num from project.ext: not found")), buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("##build-num from project.ext.teamcity: " + dynamicPropValue)), buildFullLog);
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
    config = new GradleRunConfiguration(PROJECT_E_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, "testsWithConfigurationCache.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("(##teamcity\\[(.*?)(?<!\\|)\\])|(Reusing configuration cache)");
    runAndCheckServiceMessages(config);
  }

  // The JUnit platform Gradle plugin is not compatible with Gradle 9+
  @Test(dataProvider = "gradle-version-provider=8.x")
  public void shouldRunJupiterTestsWithConfigurationCache(final String gradleVersion) throws Exception {
    // given
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_OBSOLETE_JUNIT_PLUGIN, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));

    // when: second run / then: tests ran with configuration cache reused
    config = new GradleRunConfiguration(PROJECT_WITH_OBSOLETE_JUNIT_PLUGIN, "clean build" + " " + CONFIGURATION_CACHE_CMD, "testsWithConfigurationCacheJupiter.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr("(##teamcity\\[importData (.*?)(?<!\\|)\\])|(Reusing configuration cache)");
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void shouldReuseConfigurationCacheWhenTheDoNotPopulateGradlePropertiesFlagIsSetDespiteParametersChange(final String gradleVersion) throws Exception {
    // given
    myTeamCitySystemProps.put("build.number", String.valueOf(1));
    myTeamCitySystemProps.put("foo", String.valueOf(1));
    myTeamCityConfigParameters.put(GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES_CONFIG_PARAM, "true");
    GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_E_NAME, "clean build" + " " + CONFIGURATION_CACHE_CMD, null);
    config.setGradleVersion(gradleVersion);

    // when: first run
    List<String> messages = run(config).getAllMessages();

    // then: configuration cache has been stored
    String buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Configuration cache entry stored.")), buildFullLog);

    // when: second run
    myTeamCitySystemProps.put("build.number", String.valueOf(2));
    myTeamCitySystemProps.put("foo", String.valueOf(2));
    messages = run(config).getAllMessages();

    // then: configuration cache has been reused
    buildFullLog = "Full log:\n" + StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\n" + buildFullLog);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Reusing configuration cache.")), buildFullLog);
  }
}
