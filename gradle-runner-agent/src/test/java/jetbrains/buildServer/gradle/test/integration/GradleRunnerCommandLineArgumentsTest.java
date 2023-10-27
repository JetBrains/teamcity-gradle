package jetbrains.buildServer.gradle.test.integration;

import java.util.ArrayList;
import java.util.List;
import jetbrains.buildServer.gradle.runtime.service.commandLine.GradleToolingCommandLineOptionsProvider;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class GradleRunnerCommandLineArgumentsTest extends GradleRunnerServiceMessageTest {

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_SupportedLongArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> supported = new ArrayList<>(GradleToolingCommandLineOptionsProvider
                                               .getLongOptionsNames(GradleToolingCommandLineOptionsProvider.getSupportedOptions().getOptions()));

    for (String supportedArg : supported) {
      // act
      String buildCmd = "generatedTask1 " + prefillArgWithValue(supportedArg, gradleVersion, PROJECT_WITH_GENERATED_TASKS_B_NAME);
      System.out.println("Checking command: " + buildCmd);
      final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
      config.setGradleVersion(gradleVersion);
      List<String> messages = run(config).getAllMessages();

      // assert
      if (!isLoggingOption(supportedArg)) {
        assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
      }
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API and will not be used")), "Supported arguments should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Caused by: org.gradle.cli.CommandLineArgumentException: Unknown command-line option")), "Supported arguments should not cause an exception\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_SupportedShortArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> supported = new ArrayList<>(GradleToolingCommandLineOptionsProvider
                                               .getShortOptionsNames(GradleToolingCommandLineOptionsProvider.getSupportedOptions().getOptions()));

    for (String supportedArg : supported) {
      // act
      String buildCmd = "generatedTask1 " + prefillArgWithValue(supportedArg, gradleVersion, PROJECT_WITH_GENERATED_TASKS_B_NAME);
      System.out.println("Checking command: " + buildCmd);
      final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
      config.setGradleVersion(gradleVersion);
      List<String> messages = run(config).getAllMessages();

      // assert
      if (!isLoggingOption(supportedArg)) {
        assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
      }
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API and will not be used")), "Supported arguments should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Caused by: org.gradle.cli.CommandLineArgumentException: Unknown command-line option")), "Supported arguments should not cause an exception\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_UnsupportedLongArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> unsupported = new ArrayList<>(GradleToolingCommandLineOptionsProvider.getLongOptionsNames(GradleToolingCommandLineOptionsProvider.getUnsupportedOptions().getOptions()));
    String buildCmd = "clean build " + String.join(" ", unsupported);

    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);

    // act
    System.out.println("Checking cmd: " + buildCmd);
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
    for (String unsupportedArg : unsupported) {
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API and will not be used: " + unsupportedArg)),
                 "Unsupported arguments should be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_UnsupportedShortArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> unsupported = new ArrayList<>(GradleToolingCommandLineOptionsProvider.getShortOptionsNames(GradleToolingCommandLineOptionsProvider.getUnsupportedOptions().getOptions()));
    String buildCmd = "clean build " + String.join(" ", unsupported);

    final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);

    // act
    System.out.println("Checking cmd: " + buildCmd);
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
    for (String unsupportedArg : unsupported) {
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API and will not be used: " + unsupportedArg)),
                 "Unsupported arguments should be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_Fail_When_UnsupportedGradleArgumentsArePassedThatWeDontKnow(final String gradleVersion) throws Exception {
    // arrange
    String buildCmd = "clean build --unknown-arg";
    final GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_A_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);

    // act
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD FAILED")), "Expected: BUILD FAILED\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("Caused by: org.gradle.cli.CommandLineArgumentException: Unknown command-line option '--unknown-arg'")), "Should fail with an exception due to unknown unsupported arg\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API and will not be used")), "Unknown by us unsupported args should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFilterOutGradleTasks_When_TasksArePassedAsArguments(final String gradleVersion) throws Exception {
    // arrange
    String buildCmd = ":sub-module1:clean sub-module1:test compileJava --parallel -b build.gradle --stacktrace";
    final GradleRunConfiguration config = new GradleRunConfiguration(DEMAND_MULTI_PROJECT_A_NAME, buildCmd, null);
    config.setGradleVersion(gradleVersion);

    // act
    List<String> messages = run(config).getAllMessages();

    // assert
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("BUILD SUCCESSFUL")), "Expected: BUILD SUCCESSFUL\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().noneMatch(line -> line.startsWith("The argument is not supported by the Gradle Tooling API")), "Gradle tasks should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("> Task :sub-module1:clean")), "Task must be executed\nFull log:\n" + StringUtil.join("\n", messages));
    assertTrue(messages.stream().anyMatch(line -> line.startsWith("> Task :sub-module1:test")), "Task must be executed\nFull log:\n" + StringUtil.join("\n", messages));
  }

  private String prefillArgWithValue(String arg,
                                     String gradleVersion,
                                     String projectName) {
    if (arg.equals("--configuration-cache-problems")) {
      return arg + "=warn";
    }
    if (arg.equals("--max-workers")) {
      return arg + "=1";
    }
    if (arg.equals("--console")) {
      return arg + "=auto";
    }
    if (arg.equals("--warning-mode")) {
      return arg + "=all";
    }
    if (arg.equals("--update-locks")) {
      return arg + " org.example:test-api";
    }
    if (arg.equals("--project-cache-dir")) {
      return arg + " " + getWorkingDir(getGradleVersion(gradleVersion), projectName);
    }
    if (arg.equals("--project-prop")) {
      return arg + " key=value";
    }
    if (arg.equals("-P")) {
      return arg + "key=value";
    }
    if (arg.equals("-x") || arg.equals("--exclude-task")) {
      return arg + " generatedTask1";
    }
    if (arg.equals("-F") || arg.equals("--dependency-verification")) {
      return arg + "=off";
    }
    if (arg.equals("-M") || arg.equals("--write-verification-metadata")) {
      return arg + " sha256";
    }
    if (arg.equals("-g") || arg.equals("--gradle-user-home")) {
      return arg + " homeTestDir";
    }
    if (arg.equals("-p") || arg.equals("--project-dir")) {
      return arg + " " + getWorkingDir(getGradleVersion(gradleVersion), projectName);
    }
    if (arg.equals("--system-prop")) {
      return arg + " key=value";
    }
    if (arg.equals("-D")) {
      return arg + "key=value";
    }
    if (arg.equals("-b") || arg.equals("--build-file")) {
      return arg + "=build.gradle";
    }
    if (arg.equals("--include-build")) {
      return arg + " " + getWorkingDir(getGradleVersion(gradleVersion), projectName);
    }
    if (arg.equals("-c") || arg.equals("--settings-file")) {
      return arg + "=settings.gradle";
    }
    if (arg.equals("-I") || arg.equals("--init-script")) {
      return arg + " " + getInitScript().getAbsolutePath();
    }

    return arg;
  }

  private boolean isLoggingOption(String arg) {
    return arg.equals("--warn") ||
           arg.equals("-w") ||
           arg.equals("--debug") ||
           arg.equals("-d") ||
           arg.equals("--quiet") ||
           arg.equals("-q");
  }
}

