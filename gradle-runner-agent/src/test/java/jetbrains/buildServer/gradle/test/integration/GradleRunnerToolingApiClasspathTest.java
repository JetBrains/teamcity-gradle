package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class GradleRunnerToolingApiClasspathTest extends GradleRunnerServiceMessageTest {
  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.clear();
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_Locate_Libs_Under_Temp_Dir_When_LaunchedViaToolingApiInDockerContainer(final String gradleVersion) throws Exception {
    // arrange
    myVirtualContext = true;
    String buildCmd = "clean build";
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    final GradleRunConfiguration config = new GradleRunConfiguration(WRAPPED_PROJECT_A_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    File expectedParentDir = new File(myTempDir, "agentLibs");
    String classpathLine = "Gradle init script classpath: ";

    // act
    List<String> messages = run(config).getAllMessages();
    String classpath = messages.stream()
                               .filter(it -> it.startsWith(classpathLine))
                               .map(it -> it.substring(classpathLine.length() - 1))
                               .findFirst()
                               .orElse(null);

    // assert
    String messagesStr = StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + messagesStr);
    assertNotNull(classpath);
    List<File> classPathElements = Arrays.stream(classpath.split(File.pathSeparator))
                                         .map(it -> new File(it.trim()))
                                         .filter(it -> it.getName().contains("gson")
                                                         || it.getName().contains("serviceMessages")
                                                         || it.getName().contains("runtime-util")
                                                         || it.getName().contains("common") && !it.getName().contains("gradle"))
                                         .collect(Collectors.toList());
    assertEquals(classPathElements.size(), 4);
    for (File classpathElement : classPathElements) {
      assertEquals(classpathElement.getParent(), expectedParentDir.getAbsolutePath());
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_Not_Locate_Libs_Under_Temp_Dir_When_LaunchedViaToolingApiInDockerContainer_And_FeatureToggleIsOff(final String gradleVersion) throws Exception {
    // arrange
    myVirtualContext = true;
    String buildCmd = "clean build";
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    final GradleRunConfiguration config = new GradleRunConfiguration(WRAPPED_PROJECT_A_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_PLACE_LIBS_FOR_TOOLING_IN_TEMP_DIR, "false");

    // act
    List<String> messages = run(config).getAllMessages();
    String classpath = messages.stream()
                               .filter(it -> it.startsWith("Gradle init script classpath"))
                               .findFirst()
                               .orElse(null);

    // assert
    String messagesStr = StringUtil.join("\n", messages);
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + messagesStr);
    assertNotNull(classpath);
    List<String> classPathElements = Arrays.asList(classpath.split(File.pathSeparator));
    assertTrue(classPathElements.stream().noneMatch(it -> it.contains("agentLibs")));
  }
}