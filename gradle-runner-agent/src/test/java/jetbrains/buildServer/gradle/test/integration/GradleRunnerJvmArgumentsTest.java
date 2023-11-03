package jetbrains.buildServer.gradle.test.integration;

import java.util.List;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.testng.SkipException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class GradleRunnerJvmArgumentsTest extends GradleRunnerServiceMessageTest {

  @BeforeMethod
  public void beforeTest() {
    final String jdk11 = System.getenv("JDK_11");
    if (jdk11 != null) {
      myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, jdk11);
      return;
    }
    final String jdk17 = System.getenv("JDK_17");
    if (jdk17 != null) {
      myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, jdk17);
      return;
    }

    throw new SkipException("This test only works with JDK 9+");
  }

  @AfterMethod
  public void afterTest() {
    myRunnerParams.remove(JavaRunnerConstants.TARGET_JDK_HOME);
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_JdkInternalModulesAreUsed(final String gradleVersion) throws Exception {
    // arrange
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_JDK_INTERNAL_MODULE_NAME, "build internalThings", null);
    config.setGradleVersion(gradleVersion);

    // act
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().anyMatch(line -> line.contains("--add-opens java.base/jdk.internal.module=ALL-UNNAMED")), "Expected: --add-opens to be applied\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().noneMatch(line -> line.contains("cannot access class jdk.internal.module.ModuleInfoWriter")), "Expected: --add-opens to be applied\nFull log:\n" + StringUtil.join("\n", messages));
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_DuplicatedAddOpensOptionsArePassed(final String gradleVersion) throws Exception {
    // arrange
    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_JDK_INTERNAL_MODULE_NAME, "build internalThings", null);
    config.setGradleVersion(gradleVersion);
    String jvmArgs = "--add-opens java.base/jdk.internal.module=ALL-UNNAMED --add-opens java.base/java.util=ALL-UNNAMED --add-opens=java.base/jdk.internal.module=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED";
    myRunnerParams.put(JavaRunnerConstants.JVM_ARGS_KEY, jvmArgs);

    // act
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
  }
}

