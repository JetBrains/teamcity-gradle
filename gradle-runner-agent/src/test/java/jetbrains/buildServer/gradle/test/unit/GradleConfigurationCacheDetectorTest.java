package jetbrains.buildServer.gradle.test.unit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.gradle.agent.GradleConfigurationCacheDetector;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class GradleConfigurationCacheDetectorTest {

  private static final String GRADLE_PROPERTIES_CONTENT = "version=2023.3.3\n" +
                                                          "#this.is.commented.param=value\n" +
                                                          "this is just a text" +
                                                          "cusTomPARAM=VaL" +
                                                          "\n" +
                                                          "%s" + // a place for the CC flag
                                                          "\n" +
                                                          "org.gradle.jvmargs=-Xmx500m -XX:+HeapDumpOnOutOfMemoryError -XX:+UseParallelGC -XX:MaxMetaspaceSize=500m\n" +
                                                          "org.gradle.daemon=true\n" +
                                                          "org.gradle.parallel=true\n" +
                                                          "org.gradle.workers.max=3\n" +
                                                          "org.gradle.caching=true\n" +
                                                          "\n" +
                                                          "\n";

  private final TempFiles tempFiles = new TempFiles();
  private File projectDir;
  private File gradleUserHomeDir;

  @BeforeMethod
  public void beforeTest() throws IOException {
    projectDir = tempFiles.createTempDir();
    gradleUserHomeDir = tempFiles.createTempDir();
  }

  @DataProvider
  public Object[][] ccEnabledCommandLineProvider() {
    return new Object[][]{
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--configuration-cache", "--parallel")
      },
      {
        Arrays.asList("clean", "build", "--configuration-cache"),
        Arrays.asList("--continue", "--parallel")
      },
      {
        Arrays.asList("--configuration-cache"),
        Collections.emptyList()
      },
      {
        Collections.emptyList(),
        Arrays.asList("--configuration-cache"),
      },
    };
  }
  @Test(dataProvider = "ccEnabledCommandLineProvider")
  public void should_DetectConfigurationCacheEnabled_When_PassedThroughCommandLine_And_DisabledInOtherPlaces(List<String> gradleTasks,
                                                                                                             List<String> gradleParams) throws IOException {
    // arrange
    String gradlePropertiesContent = String.format(GRADLE_PROPERTIES_CONTENT, "org.gradle.configuration-cache=false");
    createGradleProperties(projectDir, gradlePropertiesContent);
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertTrue(isEnabled.get());
  }

  @DataProvider
  public Object[][] ccDisabledCommandLineProvider() {
    return new Object[][]{
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--no-configuration-cache", "--continue", "--parallel")
      },
      {
        Arrays.asList("build", "--no-configuration-cache"),
        Arrays.asList("--continue", "--parallel")
      },
      {
        Arrays.asList("--no-configuration-cache"),
        Collections.emptyList()
      },
      {
        Collections.emptyList(),
        Arrays.asList("--no-configuration-cache"),
      },
    };
  }
  @Test(dataProvider = "ccDisabledCommandLineProvider")
  public void should_DetectConfigurationCacheDisabled_When_PassedThroughCommandLine_And_EnabledInOtherPlaces(List<String> gradleTasks,
                                                                                                             List<String> gradleParams) throws IOException {
    // arrange
    String gradlePropertiesContent = String.format(GRADLE_PROPERTIES_CONTENT, "org.gradle.configuration-cache=true");
    createGradleProperties(projectDir, gradlePropertiesContent);
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertFalse(isEnabled.get());
  }

  @Test
  public void should_ReturnEmptyResult_When_ThereIsNoInfoAboutConfigurationCacheInAllThePlaces() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertFalse(isEnabled.isPresent());
  }

  @Test
  public void should_ReturnEmptyResult_When_GradlePropertiesContainsCommentedInfoAboutConfigurationCache() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    createGradleProperties(projectDir, "#org.gradle.configuration-cache=false");
    createGradleProperties(gradleUserHomeDir, "#org.gradle.configuration-cache=true");

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertFalse(isEnabled.isPresent());
  }

  @DataProvider
  public Object[][] ccGradlePropertiesInGradleUserHomeEnabledProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=true" },
      { "org.gradle.unsafe.configuration-cache=true" },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInGradleUserHomeEnabledProvider")
  public void should_DetectConfigurationCacheEnabled_When_SetInGradleUserHome_And_DisabledInProjectDir(String gradleUserHomeGradlePropertiesContent) throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Collections.emptyList();
    createGradleProperties(projectDir, "org.gradle.configuration-cache=false");
    createGradleProperties(gradleUserHomeDir, gradleUserHomeGradlePropertiesContent);

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertTrue(isEnabled.get());
  }

  @DataProvider
  public Object[][] ccGradlePropertiesInGradleUserHomeDisabledProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=false" },
      { "org.gradle.unsafe.configuration-cache=false" },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInGradleUserHomeDisabledProvider")
  public void should_DetectConfigurationCacheDisabled_When_SetInGradleUserHome_And_EnabledInProjectDir(String gradleUserHomeGradlePropertiesContent) throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Collections.emptyList();
    createGradleProperties(projectDir, "org.gradle.configuration-cache=true");
    createGradleProperties(gradleUserHomeDir, gradleUserHomeGradlePropertiesContent);

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertFalse(isEnabled.get());
  }

  @DataProvider
  public Object[][] ccOverriddenViaCLArgGradleUserHomeProvider() {
    return new Object[][]{
      {
        "clean build",
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty} --gradle-user-home={gUHOverriddenViaCLArg}"
      },
      {
        "clean build",
        "--gradle-user-home={gUHOverriddenViaCLArg} -Dgradle.user.home={gUHOverriddenViaSystemProperty}"
      },
      {
        "--gradle-user-home={gUHOverriddenViaCLArg} -Dgradle.user.home={gUHOverriddenViaSystemProperty}",
        "--continue"
      },
      {
        "clean build",
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty} --gradle-user-home {gUHOverriddenViaCLArg}"
      },
      {
        "clean build",
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty} -g {gUHOverriddenViaCLArg}"
      },
      {
        "clean build",
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty} -g={gUHOverriddenViaCLArg}"
      },
    };
  }
  @Test(dataProvider = "ccOverriddenViaCLArgGradleUserHomeProvider")
  public void should_DetectConfigurationCacheEnabled_When_EnabledInCommandLineArg_And_DisabledInOtherPlaces(String gradleTasksStr,
                                                                                                            String gradleParamsStr) throws IOException {
    // arrange
    createGradleProperties(projectDir, "org.gradle.configuration-cache=false");
    createGradleProperties(gradleUserHomeDir, "org.gradle.configuration-cache=false");
    File gradleUserHomeForSystemPropertyDir = tempFiles.createTempDir();
    createGradleProperties(gradleUserHomeForSystemPropertyDir, "org.gradle.configuration-cache=false");
    File gradleUserHomeForCLArgDir = tempFiles.createTempDir();
    createGradleProperties(gradleUserHomeForCLArgDir, "org.gradle.configuration-cache=true");

    List<String> gradleTasks = Arrays.asList(
      gradleTasksStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .replaceAll("\\{gUHOverriddenViaCLArg\\}", gradleUserHomeForCLArgDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));
    List<String> gradleParams = Arrays.asList(
      gradleParamsStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .replaceAll("\\{gUHOverriddenViaCLArg\\}", gradleUserHomeForCLArgDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertTrue(isEnabled.get());
  }

  @DataProvider
  public Object[][] ccOverriddenViaSystemPropertyGradleUserHomeProvider() {
    return new Object[][]{
      {
        "clean build",
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty}"
      },
      {
        "-Dgradle.user.home={gUHOverriddenViaSystemProperty}",
        "continue"
      },
    };
  }
  @Test(dataProvider = "ccOverriddenViaSystemPropertyGradleUserHomeProvider")
  public void should_DetectConfigurationCacheEnabled_When_DisabledInSystemProperty_And_EnabledInOtherPlaces(String gradleTasksStr,
                                                                                                            String gradleParamsStr) throws IOException {
    // arrange
    createGradleProperties(projectDir, "org.gradle.configuration-cache=true");
    createGradleProperties(gradleUserHomeDir, "org.gradle.configuration-cache=true");
    File gradleUserHomeForSystemPropertyDir = tempFiles.createTempDir();
    createGradleProperties(gradleUserHomeForSystemPropertyDir, "org.gradle.configuration-cache=false");

    List<String> gradleTasks = Arrays.asList(
      gradleTasksStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));
    List<String> gradleParams = Arrays.asList(
      gradleParamsStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                     .split(" "));

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(gradleTasks, gradleParams, gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertFalse(isEnabled.get());
  }


  @DataProvider
  public Object[][] ccGradlePropertiesInProjectDirProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=true", true },
      { "org.gradle.unsafe.configuration-cache=true", true },
      { "org.gradle.configuration-cache=false", false },
      { "org.gradle.unsafe.configuration-cache=false", false },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInProjectDirProvider")
  public void should_DetectConfigurationCache_When_SetOnlyInProjectDirGradleProperties(String ccValue,
                                                                                       Boolean expected) throws IOException {
    // arrange
    createGradleProperties(projectDir, ccValue);

    // act
    Optional<Boolean> isEnabled = GradleConfigurationCacheDetector.isConfigurationCacheEnabled(Collections.emptyList(), Collections.emptyList(), gradleUserHomeDir, projectDir);

    // assert
    assertTrue(isEnabled.isPresent());
    assertEquals(isEnabled.get(), expected);
  }

  private File createGradleProperties(File parentDir,
                                      String content) throws IOException {
    File properties = new File(parentDir, "gradle.properties");
    try (final OutputStream os = Files.newOutputStream(properties.toPath())) {
      os.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return properties;
  }
}
