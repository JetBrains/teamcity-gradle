package jetbrains.buildServer.gradle.test.toolingApiTests.integration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import jetbrains.buildServer.agent.IncrementalBuild;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/25/11, 2:36 PM
 */
public class GradleRunnerDepBasedTestTestForToolingApi extends GradleRunnerServiceMessageTestForToolingApi {

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.put(GradleRunnerConstants.IS_INCREMENTAL, Boolean.TRUE.toString());
    System.clearProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    super.tearDown();
    myRunnerParams.clear();
    System.clearProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testSingleDependency(String gradleVersion) throws Exception {

    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestSingleDep.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }


  @Test(dataProvider = "gradle-version-provider>=8")
  public void testDoubleDependency(String gradleVersion) throws Exception {

    final String changedFilesPath = createFileWithChanges("src/main/java/my/module/GreeterRoot.java:ADD:1");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestDoubleDep.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testNoDependency(String gradleVersion) throws Exception {

    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", createFileWithChanges("projectD/src/main/java/my/module/GreeterD.java:ADD:1"));
    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestZeroDep.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testNothingTouched(String gradleVersion) throws Exception {
    final String changedFilesPath = createFileWithChanges("");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);

    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testPersonalChange(String gradleVersion) throws Exception {
    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1\n" +
                                                          "projectA/src/main/java/my/module/GreeterA.java:ADD:<personal>");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestPersonalChange.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void testSkipOption(String gradleVersion) throws Exception {

    System.setProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM, "false");

    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }


  @Test(dataProvider = "gradle-version-provider>=8")
  public void testChangeNotInSourceSet(String gradleVersion) throws Exception {

    final String changedFilesPath = createFileWithChanges("projectA/build.gradle:ADD:1");
    myTeamCitySystemProps.put("teamcity.build.changedFiles.file", changedFilesPath);

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "toolingApi/DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleVersion(gradleVersion);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  private String createFileWithChanges(final String changesList) throws IOException {
    File changedFilesFile = myTempFiles.createTempFile(changesList);
    return changedFilesFile.getAbsolutePath().replaceAll("\\\\", "/");
  }
}
