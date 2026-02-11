package jetbrains.buildServer.gradle.agent.propertySplit;

/**
 * Build parameters used in init_since_8.gradle.
 * If you need to use some parameter in init_since_8.gradle:
 * 1. This parameter must be kept in static file with this postfix: {@link jetbrains.buildServer.gradle.GradleRunnerConstants#SPLIT_PROPERTY_STATIC_POSTFIX}
 * 2. This parameter must be predefined by the default value if it could be null.
 */
public enum InitScriptParametersConstants {
    TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY("gradle.test.jvmargs", ""),
    TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY("teamcity.build.stacktraceLogDir", ""),
    TEAMCITY_BUILD_CHANGED_FILES_KEY("teamcity.build.changedFiles.file", ""),
    TEAMCITY_BUILD_TEMP_DIR_KEY( "teamcity.build.tempDir", ""),
    TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY("teamcity.internal.gradle.testNameFormat", ""),
    TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY("teamcity.internal.gradle.ignoredSuiteFormat", ""),
    TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY("teamcity.internal.gradle.useTestRetryPlugin", "true"),
    TEAMCITY_CONFIGURATION_BUILD_BRANCH_KEY("teamcity.build.branch", "");


    private  final String key;
    private final String envKey;
    private final String defaultValue;

     InitScriptParametersConstants(String key, String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
        this.envKey = key.toUpperCase().replace('.', '_');
    }

    public String getKey() {
         return key;
    }

    public String getEnvKey() {
         return envKey;
    }

    public String getDefaultValue() {
        return defaultValue;
    }
}
