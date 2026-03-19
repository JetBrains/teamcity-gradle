package jetbrains.buildServer.gradle.test.unit.gradleExecution

import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.agent.runner.ProcessListener
import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.TerminationAction
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandExecution
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

@Test
class GradleCommandExecutionTest {

    @Test
    fun `makeProgramCommandLine should return provided command line`() {
        // arrange
        val commandLine = mockk<ProgramCommandLine>()
        val execution = GradleCommandExecution(commandLine, mockk(relaxed = true)) { }

        // act
        val actual = execution.makeProgramCommandLine()

        // assert
        Assert.assertSame(actual, commandLine)
    }

    @Test
    fun `process callbacks should be delegated to logging listener`() {
        // arrange
        val listener = mockk<ProcessListener>(relaxed = true)
        var callbackExitCode: Int? = null
        val execution = GradleCommandExecution(mockk(), listener) { exitCode ->
            callbackExitCode = exitCode
        }

        val commandLine = "gradle build"
        val workingDirectory = File("/tmp/project")

        // act
        execution.beforeProcessStarted()
        execution.processStarted(commandLine, workingDirectory)
        execution.onStandardOutput("stdout")
        execution.onErrorOutput("stderr")
        execution.processFinished(7)

        // assert
        verify { listener.processStarted(commandLine, workingDirectory) }
        verify { listener.onStandardOutput("stdout") }
        verify { listener.onErrorOutput("stderr") }
        verify { listener.processFinished(7) }
        Assert.assertEquals(callbackExitCode, 7)
    }

    @Test
    fun `interruptRequested should kill process tree`() {
        // arrange
        val execution = GradleCommandExecution(mockk(), mockk(relaxed = true)) { }

        // act + assert
        Assert.assertEquals(execution.interruptRequested(), TerminationAction.KILL_PROCESS_TREE)
    }

    @Test
    fun `command line logging should be enabled`() {
        // arrange
        val execution = GradleCommandExecution(mockk(), mockk(relaxed = true)) { }

        // act + assert
        Assert.assertTrue(execution.isCommandLineLoggingEnabled)
    }
}
