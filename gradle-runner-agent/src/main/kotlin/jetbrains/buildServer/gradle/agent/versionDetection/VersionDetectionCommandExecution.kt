package jetbrains.buildServer.gradle.agent.versionDetection

import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine
import jetbrains.buildServer.agent.runner.TerminationAction
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue

class VersionDetectionCommandExecution(
    private val gradleRunnerContext: GradleRunnerContext,
    private val isUnix: Boolean,
    private val onProcessFinish: (exitCode: Int, stdOut: List<String>, stdErr: List<String>) -> Unit
) : CommandExecution {

    private val stdOutLines = mutableListOf<String>()
    private val stdErrLines = mutableListOf<String>()

    override fun makeProgramCommandLine(): ProgramCommandLine {
        return if (isUnix) {
            SimpleProgramCommandLine(
                gradleRunnerContext.environmentVariables,
                gradleRunnerContext.workingDirectory.absolutePath,
                "bash",
                listOf(gradleRunnerContext.gradleExecutablePath, "--version")
            )
        } else {
            SimpleProgramCommandLine(
                gradleRunnerContext.environmentVariables,
                gradleRunnerContext.workingDirectory.absolutePath,
                gradleRunnerContext.gradleExecutablePath,
                listOf("--version")
            )
        }
    }

    override fun beforeProcessStarted() {}

    override fun processStarted(programCommandLine: String, workingDirectory: File) {}

    override fun onStandardOutput(text: String) {
        stdOutLines.add(text)
    }

    override fun onErrorOutput(text: String) {
        stdErrLines.add(text)
    }

    override fun processFinished(exitCode: Int) {
        gradleRunnerContext.buildLogger.debug("Output of the Gradle '--version' process:\n${joinOutputToString(stdOutLines)}")
        if (stdErrLines.isNotEmpty()) {
            gradleRunnerContext.buildLogger.debug("Error output of the Gradle '--version' process:\n${joinOutputToString(stdErrLines)}")
        }
        onProcessFinish(exitCode, stdOutLines.toList(), stdErrLines.toList())
    }

    override fun interruptRequested(): TerminationAction = TerminationAction.KILL_PROCESS_TREE

    override fun isCommandLineLoggingEnabled(): Boolean = true

    private fun joinOutputToString(output: List<String>): String = output.filter { it.isNotEmpty() }.joinToString("\n")
}
