package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine
import jetbrains.buildServer.gradle.agent.propertySplit.InitScriptParametersConstants
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer

abstract class GradleCliCommandLineComposerBase(private val tasksComposer: GradleTasksComposer) : GradleCommandLineComposer {
    override fun compose(parameters: GradleCommandLineComposerParameters): ProgramCommandLine {
        val gradleParameters = buildList {
            addAll(parameters.initialGradleParams)
            addAll(
                tasksComposer.getGradleParameters(
                    getLaunchMode(),
                    parameters.runnerParameters,
                    parameters.gradleUserDefinedParams,
                    parameters.pluginsDir.toFile()
                )
            )
            addAll(getSystemPropertiesForInitScript(parameters).map { it.getArgumentFromSystemProperty() })
            addAll(parameters.gradleTasks)
        }

        return SimpleProgramCommandLine(
            parameters.env,
            parameters.workingDir.toString(),
            parameters.exePath,
            gradleParameters
        )
    }

    private fun getSystemPropertiesForInitScript(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
        listOf(
            InitScriptParametersConstants.TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY,
            InitScriptParametersConstants.TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY,
            InitScriptParametersConstants.TEAMCITY_BUILD_CHANGED_FILES_KEY,
            InitScriptParametersConstants.TEAMCITY_BUILD_TEMP_DIR_KEY,
        ).forEach { propertyName ->
            parameters.runnerContext.buildParameters.systemProperties[propertyName]?.let { value ->
                yield(propertyName to value)
            }
        }

        listOf(
            InitScriptParametersConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY,
        ).forEach { parameterName ->
            parameters.configParameters[parameterName]?.let { value ->
                yield(parameterName to value)
            }
        }
    }

    private fun Pair<String, String>.getArgumentFromSystemProperty() = "-D${first}=${second}"
}
