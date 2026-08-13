package jetbrains.buildServer.gradle.test.unit;

import jetbrains.buildServer.gradle.agent.propertySplit.TeamCityBuildPropertiesGradleSplitter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class TeamCityBuildPropertiesGradleSplitterTest {

  private final TeamCityBuildPropertiesGradleSplitter splitter = new TeamCityBuildPropertiesGradleSplitter();

  @DataProvider
  public Object[][] propertyKeyDataProvider() {
    return new Object[][]{
      { "teamcity.build.id", true },
      { "teamcity.version", true },
      { "teamcity.idea.home", true },
      { "dep.buildType.teamcity.idea.home", true },
      { "dep.q.teamcity.idea.home", true },
      { "teamcity.custom.parameter", true },
      { "build.number", true },
      { "build.vcs.number", true },
      { "build.vcs.number.1", true },
      { "build.vcs.number.GradleDemandMultiProjectA_SshGitGit_RefsHeadsMaster", true },

      { "teamcity.build.changedFiles.file", false },
      { "teamcity.build.checkoutDir", false },
      { "teamcity.build.tempDir", false },
      { "teamcity.build.workingDir", false },
      { "teamcity.build.properties.file", false },
      { "teamcity.buildConfName", false },
      { "teamcity.buildType.id", false },
      { "dep.buildType.teamcity.buildType.id", false },
      { "dep.q.teamcity.buildType.id", false },
      { "teamcity.configuration.properties.file", false },
      { "teamcity.runner.properties.file", false },
      { "teamcity.projectName", false },
      { "teamcity.tests.recentlyFailedTests.file", false },
      { "agent.home.dir", false },
      { "agent.name", false },

      { "my.custom.param", false },
      { "my.custom.param", false },
      { "system.parameter", false },
    };
  }

  @Test(dataProvider = "propertyKeyDataProvider")
  public void shouldComputeDynamicProperty(String propertyKey, boolean isDynamic) {
    // when
    boolean result = splitter.isDynamicProperty(propertyKey);

    // then
    assertEquals(result, isDynamic);
  }
}
