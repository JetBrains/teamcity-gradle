package jetbrains.buildServer.gradle.agent.propertySplit;

import jetbrains.buildServer.gradle.GradleRunnerConstants;

/**
 * Build parameters used in init_since_8.gradle.
 * If you need to use some parameter in init_since_8.gradle:
 * 1. This parameter must be kept in static file with postfix `.static`.
 * 2. This parameter must be predefined by the default value if it could be null.
 */
public class InitScriptParametersConstants {

  public static final String TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY = "gradle.test.jvmargs";
  public static final String TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY = "teamcity.build.stacktraceLogDir";
  public static final String TEAMCITY_BUILD_CHANGED_FILES_KEY = "teamcity.build.changedFiles.file";
  public static final String TEAMCITY_BUILD_TEMP_DIR_KEY = "teamcity.build.tempDir";

  // Configuration parameters used in the init script
  public static final String TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY = "teamcity.internal.gradle.testNameFormat";
  public static final String TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY = "teamcity.internal.gradle.ignoredSuiteFormat";
  public static final String TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY = "teamcity.internal.gradle.useTestRetryPlugin";
  public static final String TEAMCITY_CONFIGURATION_BUILD_BRANCH_KEY = "teamcity.build.branch";

  public static final String TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY = GradleRunnerConstants.TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY;
  public static final String TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY = GradleRunnerConstants.TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY;
  public static final String GRADLE_RUNNER_READ_ALL_CONFIG_PARAM = GradleRunnerConstants.GRADLE_RUNNER_READ_ALL_CONFIG_PARAM;
  public static final String GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES = GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES;

  public static final String GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY = GradleRunnerConstants.GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY;
}
