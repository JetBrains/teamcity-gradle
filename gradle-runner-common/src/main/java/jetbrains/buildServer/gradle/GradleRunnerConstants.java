package jetbrains.buildServer.gradle;

import java.io.File;

public class GradleRunnerConstants
{
  public static final String RUNNER_TYPE = "gradle-runner";
  public static final String DISPLAY_NAME = "Gradle";

  public static final String GRADLE_INIT_SCRIPT = "ui.gradleRunner.gradle.init.script";
  public static final String GRADLE_HOME = "ui.gradleRunner.gradle.home";
  public static final String GRADLE_TASKS = "ui.gradleRunner.gradle.tasks.names";
  public static final String GRADLE_PARAMS = "ui.gradleRunner.additional.gradle.cmd.params";
  public static final String STACKTRACE = "ui.gradleRunner.gradle.stacktrace.enabled";
  public static final String DEBUG = "ui.gradleRunner.gradle.debug.enabled";
  public static final String GRADLE_WRAPPER_FLAG = "ui.gradleRunner.gradle.wrapper.useWrapper";
  public static final String GRADLE_WRAPPER_PATH = "ui.gradleRunner.gradle.wrapper.path";
  public static final String IS_INCREMENTAL = "ui.gradleRunner.gradle.incremental";
  public static final String PATH_TO_BUILD_FILE = "ui.gradleRUnner.gradle.build.file";

  // incremental options run
  public static final String ENV_INCREMENTAL_PARAM = "TEAMCITY_GRADLE_INCREMENTAL_MODE";

  // internal configuration parameters
  public static final String GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM = "teamcity.internal.gradle.runner.launch.mode";
  public static final String GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE = "gradle-tooling-api";
  public static final String GRADLE_RUNNER_COMMAND_LINE_LAUNCH_MODE = "gradle";
  /**
   * This param defaults to true. But if Gradle's configuration-cache feature is enabled, it switches to false.
   * That means, we won't read all the data from teamcity.build.parameters file.
   * The content of this file changes from build to build. So if we read all the content of the file, Gradle's configuration-cache will not work properly.
   */
  public static final String GRADLE_RUNNER_READ_ALL_CONFIG_PARAM = "teamcity.internal.gradle.runner.read.all.params";
  public static final String GRADLE_RUNNER_ALLOW_JVM_ARGS_OVERRIDING_CONFIG_PARAM = "teamcity.internal.gradle.runner.allow.jvm.args.overriding";
  /**
   * A relative path to the gradle-wrapper.properties without a filename.
   * In case the file is located under <PROJECT_DIR>/wrapper/custom-location/gradle-wrapper.properties,
   * the value should be: /wrapper/custom-location
   */
  public static final String GRADLE_RUNNER_WRAPPER_PROPERTIES_PATH_CONFIG_PARAM = "teamcity.internal.gradle.runner.wrapperPropertiesPath";
  public static final String GRADLE_RUNNER_PLACE_LIBS_FOR_TOOLING_IN_TEMP_DIR = "teamcity.internal.gradle.runner.placeLibsForToolingApiInTempDir";
  /**
   * Defaults to true
   */
  public static final String GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH = "teamcity.internal.gradle.runner.enhanceGradleDaemonClasspath";
  /**
   * Defaults to false
   */
  public static final String GRADLE_RUNNER_TEST_TASK_JVM_ARG_PROVIDER_DISABLED = "teamcity.internal.gradle.runner.testTaskJvmArgumentsProviderDisabled";

  public static final String ENV_INCREMENTAL_VALUE_SKIP = "skip_incremental";
  public static final String ENV_INCREMENTAL_VALUE_PROCEED = "do_incremental";
  public static final String ENV_GRADLE_OPTS = "GRADLE_OPTS";
  public static final String ENV_SUPPORT_TEST_RETRY = "TEAMCITY_SUPPORT_TEST_RETRY";
  public static final String TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH = "TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH";
  public static final String TEAMCITY_RISK_TESTS_ARTIFACT_PATH = "TEAMCITY_RISK_TESTS_ARTIFACT_PATH";
  public static final String TEAMCITY_BUILD_INIT_PATH = "TEAMCITY_BUILD_INIT_PATH";

  public static final String INIT_SCRIPT_DIR = "scripts";
  public static final String INIT_SCRIPT_NAME = "init.gradle";
  public static final String INIT_SCRIPT_SINCE_8_NAME = "init_since_8.gradle";
  public static final String GRADLE_BUILD_PROBLEM_TYPE = "gradleBuildProblem";

  public static final String GRADLE_WRAPPER_PROPERTIES_FILENAME = "gradle-wrapper.properties";
  public static final String GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION = "gradle" + File.separator + "wrapper" + File.separator + GRADLE_WRAPPER_PROPERTIES_FILENAME;

  public static final String GRADLE_HOME_ENV_KEY = "GRADLE_HOME";
  public static final String GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY = "GRADLE_WRAPPED_DISTRIBUTION_PATH";
  public static final String WORKING_DIRECTORY_ENV_KEY = "WORKING_DIRECTORY";
  public static final String USE_WRAPPER_ENV_KEY = "USE_WRAPPER";

  public static final char GRADLE_TASKS_DELIMITER = ' ';

  public static final String GRADLE_PARAMS_FILE = "teamcity.gradle.config.parameters";
  public static final String GRADLE_JVM_PARAMS_FILE = "teamcity.gradle.jvm.parameters";
  public static final String GRADLE_TASKS_FILE = "teamcity.gradle.tasks";

  public static final String GRADLE_PARAMS_FILE_ENV_KEY = "TEAMCITY_GRADLE_CONFIG_PARAMETERS";
  public static final String GRADLE_JVM_PARAMS_FILE_ENV_KEY = "TEAMCITY_GRADLE_JVM_PARAMETERS";
  public static final String GRADLE_TASKS_FILE_ENV_KEY = "TEAMCITY_GRADLE_TASKS";
  public static final String GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY = "GRADLE_DAEMON_ENHANCEMENT_CLASSES";
  public static final String TEST_TASK_JVM_ARG_PROVIDER_DISABLED_ENV_KEY = "TEST_TASK_JVM_ARG_PROVIDER_DISABLED";

  public static final String TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY = "teamcity.build.properties.file";
  public static final String TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY = "TEAMCITY_BUILD_PROPERTIES_FILE";

  public static final String SPLIT_PROPERTY_STATIC_POSTFIX = ".static";
  public static final String BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR = "task-output";
}