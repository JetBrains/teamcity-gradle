package jetbrains.buildServer.gradle.agent.gradleExecution

import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.agent.runner.ProcessListener
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.TerminationAction
import java.io.File

class GradleCommandExecution(
    private val gradleCommandLine: ProgramCommandLine,
    private val loggingListener: ProcessListener,
    private val onFinished: (Int) -> Unit
) : CommandExecution {

    override fun makeProgramCommandLine(): ProgramCommandLine = gradleCommandLine

    override fun beforeProcessStarted() {}

    override fun processStarted(programCommandLine: String, workingDirectory: File) {
        loggingListener.processStarted(programCommandLine, workingDirectory)
    }

    override fun onStandardOutput(text: String) {
        loggingListener.onStandardOutput(text)
    }

    override fun onErrorOutput(text: String) {
        loggingListener.onErrorOutput(text)
    }

    override fun processFinished(exitCode: Int) {
        loggingListener.processFinished(exitCode)
        onFinished(exitCode)
    }

    override fun interruptRequested(): TerminationAction = TerminationAction.KILL_PROCESS_TREE

    override fun isCommandLineLoggingEnabled(): Boolean = true
}
