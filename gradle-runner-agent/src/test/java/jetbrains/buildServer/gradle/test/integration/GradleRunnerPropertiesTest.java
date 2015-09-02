package jetbrains.buildServer.gradle.test.integration;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * date: 01.09.2015.
 */
public class GradleRunnerPropertiesTest extends GradleRunnerServiceMessageTest {
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.clear();
  }

  @Test(dataProvider = "gradle-version-provider")
  public void simpleTeamCityPropertiesTest(String gradleVersion) throws Exception {
    mySystemProps.put("test_teamcity_property", "test_value");
    GradleRunConfiguration config = new GradleRunConfiguration("projectA", "clean printTcProperty", "testTeamcityProperty.txt");
    config.setPatternStr("##tc-property(.*)");
    config.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void simpleSystemPropertiesTest(String gradleVersion) throws Exception {
    mySystemProps.put("test_system_property", "test_value");
    GradleRunConfiguration config = new GradleRunConfiguration("projectA", "clean printSystemProperty", "testSystemProperty.txt");
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void overrideSystemPropertiesInCommandlineTest(String gradleVersion) throws Exception {
    final String testKey = "test_system_property";
    final String testValue = "test_value";
    final String otherValue = "other_value";

    mySystemProps.put(testKey, testValue);
    GradleRunConfiguration config = new GradleRunConfiguration("projectA", "clean printSystemProperty -D" + testKey + "=" + otherValue, "overrideSystemProperty.txt");
    config.setPatternStr("##system-property(.*)");
    config.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(config);
  }
}
