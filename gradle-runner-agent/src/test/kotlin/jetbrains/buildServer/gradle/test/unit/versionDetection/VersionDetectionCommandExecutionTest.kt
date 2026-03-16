package jetbrains.buildServer.gradle.test.unit.versionDetection

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.agent.FlowLogger
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.versionDetection.VersionDetectionCommandExecution
import org.testng.Assert
import org.testng.annotations.Test
import java.io.File

@Test
class VersionDetectionCommandExecutionTest {

    @Test
    fun `should build command line with gradle executable directly when not on unix`() {
        // arrange
        val workingDirectory = File("/project/dir")
        val environmentVariables = mapOf("JAVA_HOME" to "/path/to/java")
        val gradleExecutablePath = "/path/to/gradle"
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        every { gradleRunnerContext.workingDirectory } returns workingDirectory
        every { gradleRunnerContext.environmentVariables } returns environmentVariables
        every { gradleRunnerContext.gradleExecutablePath } returns gradleExecutablePath
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, _ -> }

        // act
        val commandLine = execution.makeProgramCommandLine()

        // assert
        Assert.assertEquals(commandLine.workingDirectory, workingDirectory.absolutePath)
        Assert.assertEquals(commandLine.environment, environmentVariables)
        Assert.assertEquals(commandLine.executablePath, gradleExecutablePath)
        Assert.assertEquals(commandLine.arguments, listOf("--version"))
    }

    @Test
    fun `should build command line with bash as executable when on unix`() {
        // arrange
        val workingDirectory = File("/project/dir")
        val environmentVariables = mapOf("JAVA_HOME" to "/path/to/java")
        val gradleExecutablePath = "/path/to/gradle"
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        every { gradleRunnerContext.workingDirectory } returns workingDirectory
        every { gradleRunnerContext.environmentVariables } returns environmentVariables
        every { gradleRunnerContext.gradleExecutablePath } returns gradleExecutablePath
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, true) { _, _, _ -> }

        // act
        val cmdLine = execution.makeProgramCommandLine()

        // assert
        Assert.assertEquals(cmdLine.workingDirectory, workingDirectory.absolutePath)
        Assert.assertEquals(cmdLine.environment, environmentVariables)
        Assert.assertEquals(cmdLine.executablePath, "bash")
        Assert.assertEquals(cmdLine.arguments, listOf(gradleExecutablePath, "--version"))
    }

    @Test
    fun `should collect standard output lines and pass them to callback on process finish`() {
        // arrange
        var callbackExitCode: Int? = null
        var callbackStdOut: List<String>? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { exitCode, stdOut, _ ->
            callbackExitCode = exitCode
            callbackStdOut = stdOut
        }

        // act
        execution.onStandardOutput("line 1")
        execution.onStandardOutput("line 2")
        execution.onStandardOutput("line 3")
        execution.processFinished(0)

        // assert
        Assert.assertEquals(callbackExitCode, 0)
        Assert.assertEquals(callbackStdOut, listOf("line 1", "line 2", "line 3"))
    }

    @Test
    fun `should pass non-zero exit code to callback`() {
        // arrange
        var callbackExitCode: Int? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { exitCode, _, _ ->
            callbackExitCode = exitCode
        }

        // act
        execution.processFinished(127)

        // assert
        Assert.assertEquals(callbackExitCode, 127)
    }

    @Test
    fun `should pass empty output list when no standard output was received`() {
        // arrange
        var callbackStdOut: List<String>? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, stdOut, _ ->
            callbackStdOut = stdOut
        }

        // act
        execution.processFinished(0)

        // assert
        Assert.assertEquals(callbackStdOut, emptyList<String>())
    }

    @Test
    fun `should collect error output lines and pass them to callback on process finish`() {
        // arrange
        var callbackStdErr: List<String>? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, stdErr ->
            callbackStdErr = stdErr
        }

        // act
        execution.onErrorOutput("error 1")
        execution.onErrorOutput("error 2")
        execution.processFinished(1)

        // assert
        Assert.assertEquals(callbackStdErr, listOf("error 1", "error 2"))
    }

    @Test
    fun `should pass empty error output list when no error output was received`() {
        // arrange
        var callbackStdErr: List<String>? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, stdErr ->
            callbackStdErr = stdErr
        }

        // act
        execution.processFinished(0)

        // assert
        Assert.assertEquals(callbackStdErr, emptyList<String>())
    }

    @Test
    fun `should log command line`() {
        // arrange
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, _ -> }

        // assert
        Assert.assertTrue(execution.isCommandLineLoggingEnabled)
    }

    @Test
    fun `should log standard output on process finish`() {
        // arrange
        val logger = mockk<FlowLogger>(relaxed = true)
        val gradleRunnerContext = mockk<GradleRunnerContext>()
        every { gradleRunnerContext.flowLogger } returns logger
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, _ -> }

        // act
        execution.onStandardOutput("line 1")
        execution.onStandardOutput("line 2")
        execution.processFinished(0)

        // assert
        verify { logger.debug("Output of the Gradle '--version' process:\nline 1\nline 2") }
    }

    @Test
    fun `should log error output on process finish when present`() {
        // arrange
        val logger = mockk<FlowLogger>(relaxed = true)
        val gradleRunnerContext = mockk<GradleRunnerContext>()
        every { gradleRunnerContext.flowLogger } returns logger
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, _ -> }

        // act
        execution.onStandardOutput("std out")
        execution.onErrorOutput("error 1")
        execution.onErrorOutput("error 2")
        execution.processFinished(1)

        // assert
        verify { logger.debug("Output of the Gradle '--version' process:\nstd out") }
        verify { logger.debug("Error output of the Gradle '--version' process:\nerror 1\nerror 2") }
    }

    @Test
    fun `should not log error output on process finish when absent`() {
        // arrange
        val logger = mockk<FlowLogger>(relaxed = true)
        val gradleRunnerContext = mockk<GradleRunnerContext>()
        every { gradleRunnerContext.flowLogger } returns logger
        val execution = VersionDetectionCommandExecution(gradleRunnerContext, false) { _, _, _ -> }

        // act
        execution.onStandardOutput("std out")
        execution.processFinished(0)

        // assert
        verify { logger.debug("Output of the Gradle '--version' process:\nstd out") }
        verify(exactly = 0) { logger.debug(match { it.startsWith("Error output") }) }
    }
}
