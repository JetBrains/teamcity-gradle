package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.ComparisonFailureUtil
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.ClasspathUtil
import jetbrains.buildServer.gradle.GradleRunnerConstants.*
import jetbrains.buildServer.gradle.agent.GradleDaemonEnhancementClassesProvider
import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer
import java.io.File

class GradleCliV2CommandLineComposer(tasksComposer: GradleTasksComposer) : GradleCliCommandLineComposerBase(tasksComposer) {
    override fun getLaunchMode(): GradleLaunchMode = GradleLaunchMode.COMMAND_LINE_V2

    override fun getEnvironmentVariables(parameters: GradleCommandLineComposerParameters): Map<String, String> {
        val additionalEnv =
            getEnvVariablesFromSystemProperties(parameters) +
            getEnvVariablesFromConfigParameters(parameters) +
            getDaemonEnhancementClassesEnvVariable(parameters) +
            getInitScriptClassPathEnvVariable(parameters)

        return buildMap {
            putAll(parameters.env)
            additionalEnv.forEach {
                putIfAbsent(it.first, it.second)
            }
        }
    }

    private fun getEnvVariablesFromSystemProperties(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
        listOf(
            BUILD_TEMP_DIR_SYSTEM_PROPERTY_KEY to "TEAMCITY_BUILD_TEMP_DIR",
            CHANGED_FILES_FILE_SYSTEM_PROPERTY_KEY to "TEAMCITY_CHANGED_FILES_FILE",
            GRADLE_TEST_COVERAGE_JVM_ARGS_SYSTEM_PROPERTY_KEY to "TEAMCITY_GRADLE_TEST_COVERAGE_JVM_ARGS",
            STACKTRACE_LOG_DIR_SYSTEM_PROPERTY_KEY to "TEAMCITY_STACKTRACE_LOG_DIR"
        ).forEach { (systemPropertyName, envVariableName) ->
            parameters.runnerContext.buildParameters.systemProperties[systemPropertyName]?.let { value ->
                yield(envVariableName to value)
            }
        }
    }

    private fun getEnvVariablesFromConfigParameters(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
        listOf(
            GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES_CONFIG_PARAM to "TEAMCITY_DO_NOT_POPULATE_GRADLE_PROPERTIES",
            USE_TEST_RETRY_PLUGIN_CONFIG_PARAM to "TEAMCITY_USE_TEST_RETRY_PLUGIN",
            TEST_TASK_JVM_ARG_PROVIDER_DISABLED_CONFIG_PARAM to "TEAMCITY_TEST_TASK_JVM_ARGUMENTS_PROVIDER_DISABLED",
            TEST_NAME_FORMAT_CONFIG_PARAM to "TEAMCITY_TEST_NAME_FORMAT",
            IGNORED_SUITE_FORMAT_CONFIG_PARAM to "TEAMCITY_IGNORE_SUITE_FORMAT_FORMAT",
        ).forEach { (systemPropertyName, envVariableName) ->
            parameters.configParameters[systemPropertyName]?.let { value ->
                yield(envVariableName to value)
            }
        }
    }

    private fun getDaemonEnhancementClassesEnvVariable(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
        val isGradleDaemonClasspathEnhancementEnabled =
            parameters.configParameters[GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH]?.toBooleanStrictOrNull() ?: true

        if (isGradleDaemonClasspathEnhancementEnabled) {
            yield(GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY to GradleDaemonEnhancementClassesProvider.provide())
        }
    }

    private fun getInitScriptClassPathEnvVariable(parameters: GradleCommandLineComposerParameters): Pair<String, String> =
        TEAMCITY_INIT_SCRIPT_CLASSPATH_ENV_VAR to getInitScriptClasspath(parameters.logger)

    private fun getInitScriptClasspath(logger: BuildProgressLogger): String {
        return listOf(
            ComparisonFailureUtil::class.java,  // runtime-util
        )
            .mapNotNull { getCanonicalPathForClass(it, logger) }
            .joinToString(File.pathSeparator)
    }

    private fun getCanonicalPathForClass(javaClass: Class<*>, logger: BuildProgressLogger): String? {
        val classpathEntry = ClasspathUtil.getClasspathEntry(javaClass)
        if (classpathEntry == null) {
            logger.debug("Couldn't get classpath entry for ${javaClass.canonicalName}")
            return null
        }

        return try {
            File(classpathEntry).canonicalPath
        } catch (e: Throwable) {
            logger.debug("Couldn't get classpath entry for ${javaClass.canonicalName}: ${e.message}")
            null
        }
    }
}
