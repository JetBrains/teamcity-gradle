package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.List;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.agent.IncrementalBuild;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * Date: 7/25/11, 2:36 PM
 */
public class GradleRunnerDepBasedTestTest extends GradleRunnerServiceMessageTest {

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.put(GradleRunnerConstants.IS_INCREMENTAL, Boolean.TRUE.toString());
  }

  @Test(dataProvider = "gradle-path-provider")
  public void testSingleDependency(String gradleHome) throws Exception {

    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestSingleDep.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }


  @Test(dataProvider = "gradle-path-provider")
  public void testDoubleDependency(String gradleHome) throws Exception {

    final String changedFilesPath = createFileWithChanges("src/main/java/my/module/GreeterRoot.java:ADD:1");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestDoubleDep.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void testNoDependency(String gradleHome) throws Exception {

    final String changedFilesPath = createFileWithChanges("projectD/src/main/java/my/module/GreeterD.java:ADD:1");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestZeroDep.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void testNothingTouched(String gradleHome) throws Exception {
    final String changedFilesPath = createFileWithChanges("");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);

    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void testPersonalChange(String gradleHome) throws Exception {
    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1\n" +
                                                          "projectA/src/main/java/my/module/GreeterA.java:ADD:<personal>");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestPersonalChange.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }

  @Test(dataProvider = "gradle-path-provider")
  public void testSkipOption(String gradleHome) throws Exception {

    System.setProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM, "false");

    final String changedFilesPath = createFileWithChanges("projectC/src/main/java/my/module/GreeterC.java:ADD:1");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                     runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }


  @Test(dataProvider = "gradle-path-provider")
  public void testChangeNotInSourceSet(String gradleHome) throws Exception {

    final String changedFilesPath = createFileWithChanges("projectA/build.gradle:ADD:1");
    final File runtimePropsTemplate = new File(myCoDir, "testDepsBasedTestRun.properties");

    final File runtimePropsFile = addChangedFilesToRuntimeProps(changedFilesPath, runtimePropsTemplate);

    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV,
                       runtimePropsFile.getAbsolutePath());

    final GradleRunConfiguration gradleRunConfiguration = new GradleRunConfiguration(MULTI_PROJECT_B_NAME,
                                                                                     "clean",
                                                                                     "DepBasedTestFullBuild.txt");
    gradleRunConfiguration.setGradleHome(gradleHome);
    runAndCheckServiceMessages(gradleRunConfiguration);
  }



  private String createFileWithChanges(final String changesList) throws IOException {
    File changedFilesFile = myTempFiles.createTempFile(changesList);
    return changedFilesFile.getAbsolutePath().replaceAll("\\\\", "/");
  }

  private File addChangedFilesToRuntimeProps(final String changedFilesPath, final File runtimePropsFile) throws IOException {

    final List<String> properties = FileUtil.readFile(runtimePropsFile);
    for (int i = 0; i < properties.size(); i++) {
      String property = properties.get(i);
      if (property.contains("__changedFiles_file__")) {
        final String changedFilesProperty = property.replaceFirst("__changedFiles_file__",
                                                                  changedFilesPath);
        properties.set(i, changedFilesProperty);
      }
    }
    final File resultFile = myTempFiles.createTempFile(StringUtil.join("\n", properties));
    return resultFile;
  }
}
