package jetbrains.buildServer.gradle.agent.propertySplit;

/**
 * Build parameters used in init_since_8.gradle.
 * If you need to use some parameter in init_since_8.gradle:
 * 1. This parameter must be kept in static file with this postfix: {@link jetbrains.buildServer.gradle.GradleRunnerConstants#SPLIT_PROPERTY_STATIC_POSTFIX}
 * 2. This parameter must be predefined by the default value if it could be null.
 */
public class InitScriptParametersConstants {

  public static final String TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY = "gradle.test.jvmargs";
  public static final String TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY = "teamcity.build.stacktraceLogDir";
  public static final String TEAMCITY_BUILD_CHANGED_FILES_KEY = "teamcity.build.changedFiles.file";
  public static final String TEAMCITY_BUILD_TEMP_DIR_KEY = "teamcity.build.tempDir";
  public static final String TEAMCITY_BASE_DIR_KEY = "base.dir";

  // Configuration parameters used in the init script
  public static final String TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY = "teamcity.internal.gradle.testNameFormat";
  public static final String TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY = "teamcity.internal.gradle.ignoredSuiteFormat";
  public static final String TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY = "teamcity.internal.gradle.useTestRetryPlugin";
}
