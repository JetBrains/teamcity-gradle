package jetbrains.buildServer.gradle.agent

import com.intellij.openapi.util.TCSystemInfo
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.problems.ExitCodeProblemBuilder
import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandExecution
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider
import jetbrains.buildServer.gradle.agent.obsolete.GradleConnectorFeatureFlags.shouldUseObsoleteVersionDetection
import jetbrains.buildServer.gradle.agent.obsolete.GradleConnectorProvider
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersion
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersionDetector
import jetbrains.buildServer.gradle.agent.versionDetection.VersionResolutionCheckExtensions.isGradleVersionNotNeeded

class GradleRunnerBuildSession(
    private val gradleRunnerContext: GradleRunnerContext,
    private val gradleVersionDetector: GradleVersionDetector,
    private val gradleCommandLineProvider: GradleCommandLineProvider
) : MultiCommandBuildSession {
    private val connectorProvider: GradleConnectorProvider = GradleConnectorProvider(gradleRunnerContext)

    private enum class Phase {
        VERSION_DETECTION,
        GRADLE_EXECUTION,
        DONE
    }

    private var currentPhase = Phase.VERSION_DETECTION

    private val isUnix = TCSystemInfo.isUnix
    private var detectedGradleVersion: GradleVersion? = null
    private var gradleExecutionExitCode: Int = 0

    override fun sessionStarted() {}

    override fun getNextCommand(): CommandExecution? {
        while (currentPhase != Phase.DONE) {
            when (currentPhase) {
                Phase.VERSION_DETECTION -> {
                    currentPhase = Phase.GRADLE_EXECUTION
                    if (gradleRunnerContext.isGradleVersionNotNeeded()) continue

                    // Remove in a few releases after 2026.1 if the new version detection implementation works without problems
                    if (gradleRunnerContext.shouldUseObsoleteVersionDetection()) {
                        detectedGradleVersion = gradleVersionDetector.detectGradleVersion(connectorProvider)
                        continue
                    }

                    return gradleVersionDetector.detectGradleVersion(isUnix) { detectedGradleVersion = it }
                }

                Phase.GRADLE_EXECUTION -> {
                    currentPhase = Phase.DONE
                    return executeGradle()
                }

                Phase.DONE -> return null
            }
        }
        return null
    }

    private fun executeGradle(): CommandExecution {
        return GradleCommandExecution(
            gradleCommandLineProvider.getGradleCommandLine(isUnix, detectedGradleVersion, connectorProvider),
            GradleLoggingListener(gradleRunnerContext.buildLogger)
        ) { exitCode ->
            gradleExecutionExitCode = exitCode
        }
    }

    override fun sessionFinished(): BuildFinishedStatus {
        connectorProvider.close()

        if (gradleExecutionExitCode == 0 || !gradleRunnerContext.build.failBuildOnExitCode) {
            return BuildFinishedStatus.FINISHED_SUCCESS
        }

        gradleRunnerContext.buildLogger.logBuildProblem(
            ExitCodeProblemBuilder().setExitCode(gradleExecutionExitCode)
                .setBuildRunnerContext(gradleRunnerContext.buildRunnerContext)
                .build()
        )

        return BuildFinishedStatus.FINISHED_WITH_PROBLEMS
    }
}
