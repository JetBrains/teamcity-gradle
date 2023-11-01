package jetbrains.buildServer.gradle.runtime.service.commandLine;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.jetbrains.annotations.NotNull;

public final class GradleToolingCommandLineOptionsProvider {

  private static final Options OPTIONS;
  private static final Options UNSUPPORTED_OPTIONS;

  public static Options getSupportedOptions() {
    return OPTIONS;
  }

  public static Options getUnsupportedOptions() {
    return UNSUPPORTED_OPTIONS;
  }

  public static List<String> getShortOptionsNames(@NotNull Collection<Option> options) {
    return options.stream()
                  .map(it -> it.getOpt())
                  .filter(it -> it != null)
                  .map(it -> "-" + it)
                  .collect(Collectors.toList());
  }

  public static List<String> getLongOptionsNames(@NotNull Collection<Option> options) {
    return options.stream()
                  .map(it -> it.getLongOpt())
                  .filter(it -> it != null)
                  .map(it -> "--" + it)
                  .collect(Collectors.toList());
  }

  static {
    // These options aren't supported via Gradle Tooling API,
    // https://github.com/gradle/gradle/blob/v6.2.0/subprojects/tooling-api/src/main/java/org/gradle/tooling/LongRunningOperation.java#L149-L154
    UNSUPPORTED_OPTIONS = new Options()
      // Debugging options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_debugging
      .addOption(Option.builder("?").longOpt("?").desc("Shows the help message").build())
      .addOption(Option.builder("h").longOpt("help").desc("Shows the help message").build())
      .addOption(Option.builder("v").longOpt("version").desc("Print version info").build())
      // Daemon options, see https://docs.gradle.org/current/userguide/command_line_interface.html#gradle_daemon_options
      .addOption(Option.builder().longOpt("daemon").desc("Uses the Gradle Daemon to run the build. Starts the Daemon if not running.").build())
      .addOption(Option.builder().longOpt("no-daemon").desc("Do not use the Gradle daemon to run the build. Useful occasionally if you have configured Gradle to always run with the daemon by default.").build())
      .addOption(Option.builder().longOpt("status").desc("Shows status of running and recently stopped Gradle Daemon(s).").build())
      .addOption(Option.builder().longOpt("stop").desc("Stops the Gradle Daemon if it is running.").build())
      .addOption(Option.builder().longOpt("foreground").desc("Starts the Gradle Daemon in the foreground.").build())
      // Performance options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_performance
      .addOption(Option.builder().longOpt("priority").desc("Specifies the scheduling priority for the Gradle daemon and all processes launched by it. Values are 'normal' (default) or 'low'.").hasArg().build());

    // These options are deprecated
    OptionGroup DEPRECATED_OPTIONS = new OptionGroup()
      .addOption(Option.builder("b").longOpt("build-file").desc("Specify the build file").hasArg().build())
      .addOption(Option.builder("c").longOpt("settings-file").desc("Specify the settings file").hasArg().build());

    // Debugging options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_debugging
    OptionGroup DEBUGGING_OPTIONS = new OptionGroup()
      .addOption(Option.builder("S").longOpt("full-stacktrace").desc("Print out the full (very verbose) stacktrace for all exceptions.").build())
      .addOption(Option.builder("s").longOpt("stacktrace").desc("Print out the stacktrace for all exceptions.").build())
      .addOption(Option.builder().longOpt("scan").desc("Creates a build scan. Gradle will emit a warning if the build scan plugin has not been applied.").build())
      .addOption(Option.builder().longOpt("no-scan").desc("Disables the creation of a build scan.").build());

    // Performance options, see https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_performance
    OptionGroup PERFORMANCE_OPTIONS = new OptionGroup()
      .addOption(Option.builder().longOpt("build-cache").desc("Enables the Gradle build cache. Gradle will try to reuse outputs from previous builds.").build())
      .addOption(Option.builder().longOpt("no-build-cache").desc("Disables the Gradle build cache.").build())
      .addOption(Option.builder().longOpt("configuration-cache").desc("Enables the configuration cache. Gradle will try to reuse the build configuration from previous builds.").build())
      .addOption(Option.builder().longOpt("no-configuration-cache").desc("Disables the configuration cache.").build())
      .addOption(Option.builder().longOpt("configuration-cache-problems").desc("Configures how the configuration cache handles problems (fail or warn). Defaults to fail.").hasArg().build())
      .addOption(Option.builder().longOpt("configure-on-demand").desc("Configure necessary projects only. Gradle will attempt to reduce configuration time for large multi-project builds.").build())
      .addOption(Option.builder().longOpt("no-configure-on-demand").desc("Disables the use of configuration on demand.").build())
      .addOption(Option.builder().longOpt("max-workers").desc("Configure the number of concurrent workers Gradle is allowed to use.").hasArg().build())
      .addOption(Option.builder().longOpt("parallel").desc("Build projects in parallel. Gradle will attempt to determine the optimal number of executor threads to use.").build())
      .addOption(Option.builder().longOpt("no-parallel").desc("Disables parallel execution to build projects.").build())
      .addOption(Option.builder().longOpt("profile").desc("Profile build execution time and generates a report in the <build_dir>/reports/profile directory.").build())
      .addOption(Option.builder().longOpt("watch-fs").desc("Enables watching the file system for changes, allowing data about the file system to be re-used for the next build.").build())
      .addOption(Option.builder().longOpt("no-watch-fs").desc("Disables watching the file system.").build());

    // Logging options, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_logging
    OptionGroup LOGGING_OPTIONS =  new OptionGroup()
      .addOption(Option.builder("q").longOpt("quiet").desc("Log errors only.").build())
      .addOption(Option.builder("w").longOpt("warn").desc("Set log level to warning.").build())
      .addOption(Option.builder("i").longOpt("info").desc("Set log level to information.").build())
      .addOption(Option.builder("d").longOpt("debug").desc("Log in debug mode (includes normal stacktrace).").build())
      .addOption(Option.builder().longOpt("console").desc("Specifies which type of console output to generate. Values are 'plain', 'auto' (default), 'rich' or 'verbose'.").hasArg().build())
      .addOption(Option.builder().longOpt("warning-mode").desc("Specifies which mode of warnings to generate. Values are 'all', 'fail', 'summary'(default) or 'none'.").hasArg().build());

    // Execution options, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_execution_options
    OptionGroup EXECUTION_OPTIONS = new OptionGroup()
      .addOption(Option.builder().longOpt("include-build").desc("Include the specified build in the composite.").hasArg().build())
      .addOption(Option.builder().longOpt("offline").desc("Execute the build without accessing network resources.").build())
      .addOption(Option.builder("U").longOpt("refresh-dependencies").desc("Refresh the state of dependencies.").build())
      .addOption(Option.builder().longOpt("continue").desc("Continue task execution after a task failure.").build())
      .addOption(Option.builder("m").longOpt("dry-run").desc("Run the builds with all task actions disabled.").build())
      .addOption(Option.builder("t").longOpt("continuous").desc("Continuous Build allows you to automatically re-execute the requested tasks when task inputs change.").build())
      .addOption(Option.builder().longOpt("write-locks").desc("Persists dependency resolution for locked configurations, ignoring existing locking information if it exists.").build())
      .addOption(Option.builder().longOpt("update-locks").desc("Perform a partial update of the dependency lock, letting passed in module notations change version.").hasArg().build())
      .addOption(Option.builder("a").longOpt("no-rebuild").desc("Do not rebuild project dependencies.").build());

    // Environment options, https://docs.gradle.org/current/userguide/command_line_interface.html#environment_options
    OptionGroup ENVIRONMENT_OPTIONS = new OptionGroup()
      .addOption(Option.builder("g").longOpt("gradle-user-home").desc("Specifies the gradle user home directory.").hasArg().build())
      .addOption(Option.builder("p").longOpt("project-dir").desc("Specifies the start directory for Gradle. Defaults to current directory.").hasArg().build())
      .addOption(Option.builder().longOpt("project-cache-dir").desc("Specify the project-specific cache directory. Defaults to .gradle in the root project directory.").hasArg().build())
      .addOption(Option.builder("D").longOpt("system-prop").desc("Set system property of the JVM (e.g. -Dmyprop=myvalue).").hasArg().build())
      .addOption(Option.builder("I").longOpt("init-script").desc("Specify an initialization script.").hasArg().build())
      .addOption(Option.builder("P").longOpt("project-prop").desc("Set project property for the build script (e.g. -Pmyprop=myvalue).").hasArg().build());

    // Executing tasks, https://docs.gradle.org/current/userguide/command_line_interface.html#sec:command_line_executing_tasks
    OptionGroup EXECUTING_TASKS_OPTIONS = new OptionGroup()
      .addOption(Option.builder("x").longOpt("exclude-task").desc("Specify a task to be excluded from execution.").hasArg().build())
      .addOption(Option.builder().longOpt("rerun-tasks").desc("Ignore previously cached task results.").build());

    // https://docs.gradle.org/current/userguide/dependency_verification.html
    OptionGroup VERIFICATION_OPTIONS = new OptionGroup()
      .addOption(Option.builder("F").longOpt("dependency-verification").desc("Configures the dependency verification mode (strict, lenient or off)").hasArg().build())
      .addOption(Option.builder("M").longOpt("write-verification-metadata").desc("Generates checksums for dependencies used in the project (comma-separated list).").hasArg().build())
      .addOption(Option.builder().longOpt("refresh-keys").desc("Refresh the public keys used for dependency verification.").build())
      .addOption(Option.builder().longOpt("export-keys").desc("Exports the public keys used for dependency verification.").build());

    OPTIONS = new Options()
      .addOptionGroup(DEBUGGING_OPTIONS)
      .addOptionGroup(PERFORMANCE_OPTIONS)
      .addOptionGroup(LOGGING_OPTIONS)
      .addOptionGroup(EXECUTION_OPTIONS)
      .addOptionGroup(ENVIRONMENT_OPTIONS)
      .addOptionGroup(EXECUTING_TASKS_OPTIONS)
      .addOptionGroup(VERIFICATION_OPTIONS)
      .addOptionGroup(DEPRECATED_OPTIONS);
  }
}
