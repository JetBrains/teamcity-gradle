package jetbrains.buildServer.gradle.test.unit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.gradle.agent.GradleRunnerBuildSession
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandExecution
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider
import org.testng.Assert
import org.testng.annotations.Test

@Test
class GradleRunnerBuildSessionTest {

    @Test
    fun `should return gradle execution command then null`() {
        // arrange
        val session = createSession()
        session.sessionStarted()

        // act & assert
        Assert.assertTrue(session.nextCommand is GradleCommandExecution)
        Assert.assertNull(session.nextCommand)
    }

    @Test
    fun `should return FINISHED_SUCCESS when gradle execution exits with code 0`() {
        // arrange
        val session = createSession()
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(0)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_SUCCESS)
    }

    @Test
    fun `should return FINISHED_WITH_PROBLEMS when gradle execution exits with non-zero code`() {
        // arrange
        val session = createSession(failBuildOnExitCode = true)
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(1)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_WITH_PROBLEMS)
    }

    @Test
    fun `should return FINISHED_SUCCESS when gradle execution exits with non-zero code but failBuildOnExitCode is disabled`() {
        // arrange
        val session = createSession(failBuildOnExitCode = false)
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(1)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_SUCCESS)
    }

    @Test
    fun `should log build problem when gradle execution exits with non-zero code`() {
        // arrange
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        every { gradleRunnerContext.build.failBuildOnExitCode } returns true

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any()) } returns mockk(relaxed = true)

        val session = GradleRunnerBuildSession(
            gradleRunnerContext, gradleCommandLineProvider
        )
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(42)

        // act
        session.sessionFinished()

        // assert
        verify { gradleRunnerContext.buildLogger.logBuildProblem(any()) }
    }

    private fun createSession(
        failBuildOnExitCode: Boolean = true
    ): GradleRunnerBuildSession {
        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any()) } returns mockk(relaxed = true)

        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        every { gradleRunnerContext.build.failBuildOnExitCode } returns failBuildOnExitCode

        return GradleRunnerBuildSession(
            gradleRunnerContext, gradleCommandLineProvider
        )
    }
}
