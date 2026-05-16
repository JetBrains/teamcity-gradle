package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine
import jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY
import jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH
import jetbrains.buildServer.gradle.GradleRunnerConstants.TEAMCITY_BUILD_INIT_PATH
import jetbrains.buildServer.gradle.agent.GradleDaemonEnhancementClassesProvider
import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.propertySplit.InitScriptParametersConstants
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer
import jetbrains.buildServer.gradle.runtime.service.GradleBuildConfigurator
import java.io.IOException

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

        val env = HashMap<String, String>(parameters.env)
        getEnv(parameters).forEach {
            env.putIfAbsent(it.first, it.second)
        }
        val env1 = getEnvironmentForInitScript(parameters, env)
        return SimpleProgramCommandLine(
            env1,
            parameters.workingDir.toString(),
            parameters.exePath,
            gradleParameters
        )
    }

    private fun getEnv(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
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
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY,
        ).forEach { parameterName ->
            parameters.configParameters[parameterName]?.let { value ->
                yield(parameterName to value)
            }
        }
    }

    private fun getEnvironmentForInitScript(parameters: GradleCommandLineComposerParameters, env: HashMap<String, String>): Map<String, String> {
        if (getLaunchMode() != GradleLaunchMode.COMMAND_LINE_V2) return parameters.env

        if (isGradleDaemonClasspathEnhancementEnabled(parameters)) {
            env[GRADLE_DAEMON_ENHANCEMENT_CLASSES_ENV_KEY] = GradleDaemonEnhancementClassesProvider.provide()
        }

        try {
            env[TEAMCITY_BUILD_INIT_PATH] = GradleBuildConfigurator.getInitScriptClasspath()
        } catch (e: IOException) {
            parameters.logger.message("Couldn't configure Gradle init script classpath: ${e.message}")
        }

        return env
    }

    private fun isGradleDaemonClasspathEnhancementEnabled(parameters: GradleCommandLineComposerParameters): Boolean {
        return parameters.configParameters[GRADLE_RUNNER_ENHANCE_GRADLE_DAEMON_CLASSPATH]?.toBooleanStrictOrNull() ?: true
    }

    private fun getSystemPropertiesForInitScript(parameters: GradleCommandLineComposerParameters): Sequence<Pair<String, String>> = sequence {
        listOf(
            InitScriptParametersConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY,
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_TASK_JVM_ARG_PROVIDER_DISABLED,
        ).forEach { parameterName ->
            parameters.configParameters[parameterName]?.let { value ->
                yield(parameterName to value)
            }
        }
    }

    private fun Pair<String, String>.getArgumentFromSystemProperty() = "-D${first}=${second}"
}
