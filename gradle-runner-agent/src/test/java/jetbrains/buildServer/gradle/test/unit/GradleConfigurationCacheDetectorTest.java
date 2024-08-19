package jetbrains.buildServer.gradle.test.unit;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleOptionValueFetcher;
import org.gradle.util.internal.DefaultGradleVersion;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;

public class GradleConfigurationCacheDetectorTest {

  private final GradleConfigurationCacheDetector configurationCacheDetector = new GradleConfigurationCacheDetector(new GradleOptionValueFetcher());

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
  private Mockery context;
  private BuildProgressLogger logger;

  @BeforeMethod
  public void beforeTest() throws IOException {
    projectDir = tempFiles.createTempDir();
    gradleUserHomeDir = tempFiles.createTempDir();

    context = new Mockery() {{
      setImposteriser(ClassImposteriser.INSTANCE);
    }};

    logger = context.mock(BuildProgressLogger.class);
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
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");
    createGradleProperties(projectDir, gradlePropertiesContent);
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertTrue(isEnabled);
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
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");
    createGradleProperties(projectDir, gradlePropertiesContent);
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertFalse(isEnabled);
  }

  @Test
  public void should_ReturnFalse_When_ThereIsNoInfoAboutConfigurationCacheInAllThePlaces() {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertFalse(isEnabled);
  }

  @Test
  public void should_ReturnFalse_When_GradlePropertiesContainsCommentedInfoAboutConfigurationCache() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    createGradleProperties(projectDir, "#org.gradle.configuration-cache=false");
    createGradleProperties(gradleUserHomeDir, "#org.gradle.configuration-cache=true");
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertFalse(isEnabled);
  }

  @DataProvider
  public Object[][] ccGradlePropertiesInGradleUserHomeEnabledProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=true", "8.2" },
      { "org.gradle.unsafe.configuration-cache=true", "8.0" },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInGradleUserHomeEnabledProvider")
  public void should_DetectConfigurationCacheEnabled_When_SetInGradleUserHome_And_DisabledInProjectDir(String gradleUserHomeGradlePropertiesContent,
                                                                                                       String gradleVersionStr) throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Collections.emptyList();
    createGradleProperties(projectDir, "org.gradle.configuration-cache=false");
    createGradleProperties(gradleUserHomeDir, gradleUserHomeGradlePropertiesContent);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version(gradleVersionStr);

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertTrue(isEnabled);
  }

  @DataProvider
  public Object[][] ccGradlePropertiesInGradleUserHomeDisabledProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=false", "8.2" },
      { "org.gradle.unsafe.configuration-cache=false", "8.0" },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInGradleUserHomeDisabledProvider")
  public void should_DetectConfigurationCacheDisabled_When_SetInGradleUserHome_And_EnabledInProjectDir(String gradleUserHomeGradlePropertiesContent,
                                                                                                       String gradleVersionStr) throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Collections.emptyList();
    createGradleProperties(projectDir, "org.gradle.configuration-cache=true");
    createGradleProperties(gradleUserHomeDir, gradleUserHomeGradlePropertiesContent);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version(gradleVersionStr);

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertFalse(isEnabled);
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
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    List<String> gradleTasks = Arrays.asList(
      gradleTasksStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .replaceAll("\\{gUHOverriddenViaCLArg\\}", gradleUserHomeForCLArgDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));
    List<String> gradleParams = Arrays.asList(
      gradleParamsStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .replaceAll("\\{gUHOverriddenViaCLArg\\}", gradleUserHomeForCLArgDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeForCLArgDir, projectDir, gradleVersion);

    // assert
    assertTrue(isEnabled);
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
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    List<String> gradleTasks = Arrays.asList(
      gradleTasksStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                    .split(" "));
    List<String> gradleParams = Arrays.asList(
      gradleParamsStr.replaceAll("\\{gUHOverriddenViaSystemProperty\\}", gradleUserHomeForSystemPropertyDir.getAbsolutePath().replace("\\", "/"))
                     .split(" "));

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, gradleTasks, gradleParams, gradleUserHomeForSystemPropertyDir, projectDir, gradleVersion);

    // assert
    assertFalse(isEnabled);
  }


  @DataProvider
  public Object[][] ccGradlePropertiesInProjectDirProvider() {
    return new Object[][]{
      { "org.gradle.configuration-cache=true", "8.2", true },
      { "org.gradle.unsafe.configuration-cache=true", "8.0", true },
      { "org.gradle.configuration-cache=false", "8.2", false },
      { "org.gradle.unsafe.configuration-cache=false", "8.0", false },
    };
  }
  @Test(dataProvider = "ccGradlePropertiesInProjectDirProvider")
  public void should_DetectConfigurationCache_When_SetOnlyInProjectDirGradleProperties(String ccValue,
                                                                                       String gradleVersionStr,
                                                                                       boolean expected) throws IOException {
    // arrange
    createGradleProperties(projectDir, ccValue);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version(gradleVersionStr);

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, Collections.emptyList(), Collections.emptyList(), gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertEquals(isEnabled, expected);
  }

  @Test
  public void should_NotDetectConfigurationCache_When_WrongPropertyKeyIsUsedForGradleProperties() throws IOException {
    // arrange
    createGradleProperties(projectDir, "org.gradle.configuration-cache=true");
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.0.2");

    // act
    boolean isEnabled = configurationCacheDetector.isConfigurationCacheEnabled(logger, Collections.emptyList(), Collections.emptyList(), gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    Assert.assertFalse(isEnabled);
  }

  @DataProvider
  public Object[][] ccProblemsDisabledCommandLineProvider() {
    return new Object[][]{
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--configuration-cache-problems", "warn", "--parallel"),
        true
      },
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--configuration-cache-problems=warn", "--parallel"),
        true
      },
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--configuration-cache-problems", "warn", "--continue", "--parallel"),
        true
      },
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--parallel", "--configuration-cache-problems", "warn"),
        true
      },
      {
        Arrays.asList("clean", "build", "--configuration-cache-problems=warn"),
        Arrays.asList("--continue", "--parallel"),
        true
      },
      {
        Arrays.asList("--configuration-cache-problems=warn", "clean", "build"),
        Collections.emptyList(),
        true
      },
      {
        Arrays.asList("test", "--configuration-cache-problems=warn", "clean", "build"),
        Collections.emptyList(),
        true
      },
      {
        Collections.emptyList(),
        Arrays.asList("clean", "build", "--configuration-cache-problems", "warn"),
        true,
      },

      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--configuration-cache-problems", "fail", "--parallel"),
        false
      },
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--continue", "--configuration-cache-problems=fail", "--parallel"),
        false
      },
      {
        Arrays.asList("clean", "build"),
        Arrays.asList("--configuration-cache-problems", "fail", "--continue", "--parallel"),
        false
      },
    };
  }
  @Test(dataProvider = "ccProblemsDisabledCommandLineProvider")
  public void should_DetectConfigurationProblemsIgnored_When_PassedThroughCommandLine(List<String> gradleTasks,
                                                                                      List<String> gradleParams,
                                                                                      boolean expected) throws IOException {
    // arrange
    String gradlePropertiesContent = GRADLE_PROPERTIES_CONTENT + "org.gradle.configuration-cache.problems=fail";
    createGradleProperties(projectDir, gradlePropertiesContent);
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean ccProblemsIgnored = configurationCacheDetector.areConfigurationCacheProblemsIgnored(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertEquals(ccProblemsIgnored, expected);
  }

  @Test
  public void should_DetectConfigurationProblemsIgnored_When_DefinedInGradleUserHome() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    String gradlePropertiesContent = GRADLE_PROPERTIES_CONTENT + "org.gradle.configuration-cache.problems=warn";
    createGradleProperties(projectDir, "");
    createGradleProperties(gradleUserHomeDir, gradlePropertiesContent);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean ccProblemsIgnored = configurationCacheDetector.areConfigurationCacheProblemsIgnored(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertTrue(ccProblemsIgnored);
  }

  @Test
  public void should_DetectConfigurationProblemsIgnored_When_DefinedInProject() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    String gradlePropertiesContent = GRADLE_PROPERTIES_CONTENT + "org.gradle.configuration-cache.problems=warn";
    createGradleProperties(gradleUserHomeDir, "");
    createGradleProperties(projectDir, gradlePropertiesContent);
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean ccProblemsIgnored = configurationCacheDetector.areConfigurationCacheProblemsIgnored(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertTrue(ccProblemsIgnored);
  }

  @Test
  public void should_NotDetectConfigurationProblemsIgnored_When_NotDefinedInAnyPlace() throws IOException {
    // arrange
    List<String> gradleTasks = Arrays.asList("clean", "build");
    List<String> gradleParams = Arrays.asList("--continue");
    createGradleProperties(gradleUserHomeDir, "");
    createGradleProperties(projectDir, "");
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version("8.2");

    // act
    boolean ccProblemsIgnored = configurationCacheDetector.areConfigurationCacheProblemsIgnored(logger, gradleTasks, gradleParams, gradleUserHomeDir, projectDir, gradleVersion);

    // assert
    assertFalse(ccProblemsIgnored);
  }

  @DataProvider
  public Object[][] gradleConfigCacheVersionsProvider() {
    return new Object[][]{
      {"6.6", false},
      {"6.6.1", false},
      {"6.7", false},
      {"6.7.1", false},
      {"6.8", false},
      {"6.8.1", false},
      {"6.8.2", false},
      {"6.8.3", false},
      {"6.9", false},
      {"6.9.1", false},
      {"6.9.2", false},
      {"6.9.3", false},
      {"6.9.4", false},
      {"7.0", false},
      {"7.0.1", false},
      {"7.0.2", false},
      {"7.1", false},
      {"7.1.1", false},
      {"7.2", false},
      {"7.3", false},
      {"7.3.1", false},
      {"7.3.2", false},
      {"7.3.3", false},
      {"7.4", false},
      {"7.4.1", false},
      {"7.4.2", false},
      {"7.5", false},
      {"7.5.1", false},
      {"7.6", false},
      {"7.6.1", false},
      {"7.6.2", false},
      {"7.6.3", false},
      {"7.6.4", false},
      {"8.0", false},
      {"8.0.1", false},
      {"8.0.2", false},
      {"8.1", true},
      {"8.1.1", true},
      {"8.2", true},
      {"8.2.1", true},
      {"8.3", true},
      {"8.4", true},
      {"8.5", true},
      {"8.6", true},
      {"8.7", true},
    };
  }

  @Test(dataProvider = "gradleConfigCacheVersionsProvider")
  public void should_DetectConfigurationCacheStableVersions(String gradleVersionStr, boolean expected) {
    // arrange
    DefaultGradleVersion gradleVersion = DefaultGradleVersion.version(gradleVersionStr);

    // act
    boolean result = configurationCacheDetector.isVersionWithStableConfigCache(gradleVersion);

    // assert
    assertEquals(result, expected);
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
