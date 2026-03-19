package jetbrains.buildServer.gradle.agent

import com.intellij.openapi.util.TCSystemInfo
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.problems.ExitCodeProblemBuilder
import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandExecution
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider

class GradleRunnerBuildSession(
    private val gradleRunnerContext: GradleRunnerContext,
    private val gradleCommandLineProvider: GradleCommandLineProvider,
) : MultiCommandBuildSession {

    private enum class Phase {
        GRADLE_EXECUTION,
        DONE
    }

    private var currentPhase = Phase.GRADLE_EXECUTION

    private val isUnix = TCSystemInfo.isUnix
    private var gradleExecutionExitCode: Int = 0

    override fun sessionStarted() {}

    override fun getNextCommand(): CommandExecution? {
        when (currentPhase) {
            Phase.GRADLE_EXECUTION -> {
                currentPhase = Phase.DONE
                return executeGradle()
            }
            Phase.DONE -> return null
        }
    }

    private fun executeGradle(): CommandExecution {
        return GradleCommandExecution(
            gradleCommandLineProvider.getGradleCommandLine(isUnix),
            GradleLoggingListener(gradleRunnerContext.buildLogger),
        ) { exitCode ->
            gradleExecutionExitCode = exitCode
        }
    }

    override fun sessionFinished(): BuildFinishedStatus {
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
