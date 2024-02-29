package jetbrains.buildServer.gradle.test.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.commandLine.GradleToolingCommandLineOptionsProvider;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class GradleRunnerCommandLineArgumentsTest extends GradleRunnerServiceMessageTest {

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_SupportedLongArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> supported = new ArrayList<>(GradleToolingCommandLineOptionsProvider
                                               .getLongOptionsNames(GradleToolingCommandLineOptionsProvider.getSupportedOptions().getOptions())
                                               .stream()
                                               .filter(arg -> !arg.equals("--watch-fs") && !arg.equals("--continuous"))
                                               // Continuous build doesn't work w/o watch-fs. watch-fs leads to test failures when watching the file system is not supported
                                               .collect(Collectors.toList()));

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
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("The build will be launched via Gradle Tooling API")), "The build should be launched via Tooling API\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Unable to launch the build via Gradle Tooling API to make it configuration cache compatible")), "Supported arguments should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Caused by: org.gradle.cli.CommandLineArgumentException: Unknown command-line option")), "Supported arguments should not cause an exception\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_NotFail_When_SupportedShortArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> supported = new ArrayList<>(GradleToolingCommandLineOptionsProvider
                                               .getShortOptionsNames(GradleToolingCommandLineOptionsProvider.getSupportedOptions().getOptions())
                                               .stream()
                                               .filter(arg -> !arg.equals("-t"))
                                               // Continuous build doesn't work w/o watch-fs. watch-fs leads to test failures when watching the file system is not supported
                                               .collect(Collectors.toList()));

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
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("The build will be launched via Gradle Tooling API")), "The build should be launched via Tooling API\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Unable to launch the build via Gradle Tooling API to make it configuration cache compatible")), "Supported arguments should not be filtered out\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("Caused by: org.gradle.cli.CommandLineArgumentException: Unknown command-line option")), "Supported arguments should not cause an exception\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_LaunchBuildInOldWay_When_UnsupportedLongArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> unsupported = new ArrayList<>(GradleToolingCommandLineOptionsProvider.getLongOptionsNames(GradleToolingCommandLineOptionsProvider.getUnsupportedOptions().getOptions()));

    for (String unsupportedArg : unsupported) {
      String buildCmd = "clean build --configuration-cache " + prefillArgWithValue(unsupportedArg, gradleVersion, PROJECT_WITH_GENERATED_TASKS_B_NAME);

      final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
      config.setGradleVersion(gradleVersion);
      myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

      // act
      System.out.println("Checking cmd: " + buildCmd);
      List<String> messages = run(config).getAllMessages();

      // assert
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("The build will be launched via Gradle Tooling API")), "Expected: build to be launched in old way\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("This build can only be run in legacy mode. " +
                                                                    "Using configuration-cache requires launching the build via the Gradle Tooling API that is not compatible with the following Gradle argument(s): " + unsupportedArg)),
                 "Expected: unsupported args are logged\nFull log:\n" + StringUtil.join("\n", messages));
    }
  }

  @Test(dataProvider = "gradle-version-provider>=8")
  public void should_LaunchBuildInOldWay_When_UnsupportedShortArgumentsArePassed(final String gradleVersion) throws Exception {
    // arrange
    List<String> unsupported = new ArrayList<>(GradleToolingCommandLineOptionsProvider.getShortOptionsNames(GradleToolingCommandLineOptionsProvider.getUnsupportedOptions().getOptions()));

    for (String unsupportedArg : unsupported) {
      String buildCmd = "clean build --configuration-cache " + prefillArgWithValue(unsupportedArg, gradleVersion, PROJECT_WITH_GENERATED_TASKS_B_NAME);

      final GradleRunConfiguration config = new GradleRunConfiguration(PROJECT_WITH_GENERATED_TASKS_B_NAME, buildCmd, null);
      config.setGradleVersion(gradleVersion);
      myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, "auto");

      // act
      System.out.println("Checking cmd: " + buildCmd);
      List<String> messages = run(config).getAllMessages();

      // assert
      assertTrue(messages.stream().noneMatch(line -> line.startsWith("The build will be launched via Gradle Tooling API")), "Expected: build to be launched in old way\nFull log:\n" + StringUtil.join("\n", messages));
      assertTrue(messages.stream().anyMatch(line -> line.startsWith("This build can only be run in legacy mode. " +
                                                                    "Using configuration-cache requires launching the build via the Gradle Tooling API that is not compatible with the following Gradle argument(s): " + unsupportedArg)),
                 "Expected: unsupported args are logged\nFull log:\n" + StringUtil.join("\n", messages));
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
    if (arg.equals("--priority")) {
      return arg + "=normal";
    }
    if (arg.equals("--foreground")) {
      return arg + " --stop";
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

