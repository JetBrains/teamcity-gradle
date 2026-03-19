package jetbrains.buildServer.gradle.agent.gradleExecution

import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.gradle.agent.*
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerParameters
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector
import jetbrains.buildServer.gradle.agent.obsolete.GradleConnectorProvider
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersion
import java.util.*
import java.util.stream.Collectors
import java.util.stream.Stream

class GradleCommandLineProvider(
    private val gradleRunnerContext: GradleRunnerContext,
    private val composerHolder: GradleCommandLineComposerHolder,
    private val tasksComposer: GradleTasksComposer,
    private val gradleLaunchModeSelector: GradleLaunchModeSelector,
    private val gradleConfigurationCacheDetector: GradleConfigurationCacheDetector,
    private val commandLineParametersProcessor: CommandLineParametersProcessor,
    private val gradleUserHomeManager: GradleUserHomeManager
) {
    private val build = gradleRunnerContext.build
    private val logger = build.buildLogger
    private val workingDirectory = gradleRunnerContext.buildRunnerContext.workingDirectory

    fun getGradleCommandLine(isUnix: Boolean, detectedGradleVersion: GradleVersion?, connectorProvider: GradleConnectorProvider): ProgramCommandLine {
        val gradleTasks = tasksComposer.getGradleTasks(gradleRunnerContext.buildRunnerContext.runnerParameters)
        val userDefinedParams = ConfigurationParamsUtil.getGradleParams(gradleRunnerContext.buildRunnerContext.runnerParameters)

        if (gradleRunnerContext.isWrapperPropertiesFileMissing) {
            return composerHolder.getCommandLineComposer(GradleLaunchMode.COMMAND_LINE).compose(
                getComposerParameters(
                    gradleRunnerContext.environmentVariables,
                    gradleTasks,
                    userDefinedParams,
                    null,
                    gradleRunnerContext.gradleExecutablePath,
                    isUnix,
                    null
                )
            )
        }

        val projectConnector = connectorProvider.getConnector()
        val gradleUserHome = gradleUserHomeManager.detectGradleUserHome(gradleTasks, userDefinedParams, gradleRunnerContext.environmentVariables, projectConnector).orElse(null)
        val configurationCacheEnabled = gradleConfigurationCacheDetector.isConfigurationCacheEnabled(
            logger, gradleTasks, userDefinedParams, gradleUserHome, workingDirectory, detectedGradleVersion
        )
        val configurationCacheProblemsIgnored = gradleConfigurationCacheDetector.areConfigurationCacheProblemsIgnored(
            logger, gradleTasks, userDefinedParams, gradleUserHome, workingDirectory, detectedGradleVersion
        )
        val unsupportedByToolingArgs = commandLineParametersProcessor.obtainUnsupportedArguments(
            Stream.concat(gradleTasks.stream(), userDefinedParams.stream()).collect(Collectors.toList())
        )

        val selectionResult = gradleLaunchModeSelector.selectMode(
            GradleLaunchModeSelector.Parameters
                .builder()
                .withLogger(logger)
                .withConfigurationParameters(gradleRunnerContext.buildRunnerContext.configParameters)
                .withGradleVersion(detectedGradleVersion)
                .withConfigurationCacheEnabled(configurationCacheEnabled)
                .withConfigurationCacheProblemsIgnored(configurationCacheProblemsIgnored)
                .withUnsupportedByToolingArgs(unsupportedByToolingArgs)
                .build()
        )

        val composerParameters = getComposerParameters(
            gradleRunnerContext.environmentVariables,
            gradleTasks,
            userDefinedParams,
            configurationCacheEnabled,
            gradleRunnerContext.gradleExecutablePath,
            isUnix,
            selectionResult
        )

        return composerHolder.getCommandLineComposer(selectionResult.launchMode).compose(composerParameters)
    }

    private fun getComposerParameters(
        env: Map<String, String>,
        gradleTasks: List<String>,
        gradleUserDefinedParams: List<String>,
        configurationCacheEnabled: Boolean?,
        gradleExecPath: String,
        isUnix: Boolean,
        launchModeSelectionResult: GradleLaunchModeSelectionResult?
    ): GradleCommandLineComposerParameters {
        val executablePath: String
        val commandLineParameters: List<String>
        if (isUnix) {
            executablePath = "bash"
            commandLineParameters = listOf(gradleExecPath)
        } else {
            executablePath = gradleExecPath
            commandLineParameters = ArrayList()
        }
        return GradleCommandLineComposerParameters.builder()
            .withEnv(env)
            .withBuildTempDir(build.buildTempDirectory.toPath())
            .withRunnerParameters(gradleRunnerContext.buildRunnerContext.runnerParameters)
            .withPluginsDir(build.agentConfiguration.agentPluginsDirectory.toPath())
            .withGradleOpts(gradleRunnerContext.gradleOptions)
            .withGradleTasks(gradleTasks)
            .withGradleUserDefinedParams(gradleUserDefinedParams)
            .withConfigurationCacheEnabled(Optional.ofNullable(configurationCacheEnabled).orElse(false))
            .withConfigParameters(gradleRunnerContext.buildRunnerContext.configParameters)
            .withLogger(logger)
            .withRunnerContext(gradleRunnerContext.buildRunnerContext)
            .withJavaHome(gradleRunnerContext.javaHome)
            .withCheckoutDir(build.checkoutDirectory.toPath())
            .withWorkingDir(workingDirectory.toPath())
            .withInitialGradleParams(commandLineParameters)
            .withExePath(executablePath)
            .withLaunchModeSelectionResult(launchModeSelectionResult)
            .build()
    }
}
