package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.gradle.GradleRunnerConstants.*
import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer

class GradleCliV2CommandLineComposer(tasksComposer: GradleTasksComposer) : GradleCliCommandLineComposerBase(tasksComposer) {
    override fun getLaunchMode(): GradleLaunchMode = GradleLaunchMode.COMMAND_LINE_V2

    override fun getEnvironmentVariables(parameters: GradleCommandLineComposerParameters): Map<String, String> {
        val additionalEnv = getEnvVariablesFromSystemProperties(parameters) + getEnvVariablesFromConfigParameters(parameters)

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
            TEST_NAME_FORMAT_CONFIG_PARAM to "TEAMCITY_TEST_NAME_FORMAT",
            IGNORED_SUITE_FORMAT_CONFIG_PARAM to "TEAMCITY_IGNORE_SUITE_FORMAT_FORMAT",
        ).forEach { (systemPropertyName, envVariableName) ->
            parameters.configParameters[systemPropertyName]?.let { value ->
                yield(envVariableName to value)
            }
        }
    }
}
