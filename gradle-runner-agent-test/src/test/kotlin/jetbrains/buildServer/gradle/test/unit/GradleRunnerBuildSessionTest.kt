package jetbrains.buildServer.gradle.test.unit

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jetbrains.buildServer.BuildProblemData
import jetbrains.buildServer.agent.AgentRuntimeProperties
import jetbrains.buildServer.agent.FlowLogger
import jetbrains.buildServer.agent.BuildFinishedStatus
import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.gradle.agent.GradleRunnerBuildSession
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandExecution
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersion
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersionDetector
import org.testng.Assert
import org.testng.annotations.Test

@Test
class GradleRunnerBuildSessionTest {

    @Test
    fun `should return version detection and gradle execution commands in sequence`() {
        // arrange
        val versionDetectionCommand = mockk<CommandExecution>()
        val session = createSession(
            versionDetectionResult = versionDetectionCommand,
            isGradleVersionNotNeeded = false
        )
        session.sessionStarted()

        // act & assert
        Assert.assertSame(session.getNextCommand(), versionDetectionCommand)
        Assert.assertTrue(session.getNextCommand() is GradleCommandExecution)
        Assert.assertNull(session.getNextCommand())
    }

    @Test
    fun `should skip version detection when the gradle version is not needed`() {
        // arrange
        val session = createSession(isGradleVersionNotNeeded = true)
        session.sessionStarted()

        // act & assert
        Assert.assertTrue(session.nextCommand is GradleCommandExecution)
        Assert.assertNull(session.nextCommand)
    }

    @Test
    fun `should pass detected gradle version to gradle command line`() {
        // arrange
        val expectedVersion = GradleVersion("8.0")
        val versionDetectionCommand = mockk<CommandExecution>()

        val gradleRunnerContext = getGradleRunnerContext()

        val gradleVersionDetector = mockk<GradleVersionDetector>()
        val versionCallbackSlot = slot<(GradleVersion?) -> Unit>()
        every { gradleVersionDetector.detectGradleVersion(any(), capture(versionCallbackSlot)) } returns versionDetectionCommand

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any(), any(), any()) } returns mockk(relaxed = true)

        val session = GradleRunnerBuildSession(
            gradleRunnerContext, gradleVersionDetector, gradleCommandLineProvider
        )
        session.sessionStarted()

        // act - get version detection command and simulate the callback
        session.getNextCommand()
        versionCallbackSlot.captured(expectedVersion)

        session.getNextCommand()

        // assert
        verify { gradleCommandLineProvider.getGradleCommandLine(any(), expectedVersion, any()) }
    }

    @Test
    fun `should pass null gradle version when version detection is skipped`() {
        // arrange
        val gradleRunnerContext = getGradleRunnerContext(isGradleVersionNotNeeded = true)

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any(), any(), any()) } returns mockk(relaxed = true)

        val session = GradleRunnerBuildSession(
            gradleRunnerContext, mockk(), gradleCommandLineProvider
        )
        session.sessionStarted()

        // act
        session.getNextCommand()

        // assert
        verify { gradleCommandLineProvider.getGradleCommandLine(any(), null, any()) }
    }

    @Test
    fun `should return FINISHED_SUCCESS when gradle execution exits with code 0`() {
        // arrange
        val session = createSession(isGradleVersionNotNeeded = true)
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(0)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_SUCCESS)
    }

    @Test
    fun `should return FINISHED_WITH_PROBLEMS when gradle execution exits with non-zero code`() {
        // arrange
        val session = createSession(isGradleVersionNotNeeded = true, failBuildOnExitCode = true)
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(1)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_WITH_PROBLEMS)
    }

    @Test
    fun `should return FINISHED_SUCCESS when gradle execution exits with non-zero code but failBuildOnExitCode is disabled`() {
        // arrange
        val session = createSession(isGradleVersionNotNeeded = true, failBuildOnExitCode = false)
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(1)

        // act & assert
        Assert.assertEquals(session.sessionFinished(), BuildFinishedStatus.FINISHED_SUCCESS)
    }

    @Test
    fun `should log a build problem with the process flow id when gradle execution exits with a non-zero code`() {
        // arrange
        val flowId = "gradle-flow-id"
        val flowLogger = mockk<FlowLogger>(relaxed = true)
        every { flowLogger.flowId } returns flowId
        val gradleRunnerContext = getGradleRunnerContext(
            isGradleVersionNotNeeded = true,
            failBuildOnExitCode = true,
            flowLoggerMock = flowLogger
        )

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any(), any(), any()) } returns mockk(relaxed = true)

        val session = GradleRunnerBuildSession(
            gradleRunnerContext, mockk(), gradleCommandLineProvider
        )
        session.sessionStarted()

        val gradleCommand = session.nextCommand!!
        gradleCommand.processFinished(42)

        // act
        session.sessionFinished()

        // assert
        val buildProblemSlot = slot<BuildProblemData>()
        verify(exactly = 1) { flowLogger.logBuildProblem(capture(buildProblemSlot)) }
        Assert.assertEquals(buildProblemSlot.captured.additionalData, "${AgentRuntimeProperties.FLOW_ID_PROP}=$flowId")
    }

    @Test
    fun `should use the flow logger from GradleRunnerContext in the CommandExecution returned for the main command`() {
        // arrange
        val flowLogger = mockk<FlowLogger>(relaxed = true)
        val versionDetectionCommand = mockk<CommandExecution>()

        val gradleRunnerContext = getGradleRunnerContext(
            isGradleVersionNotNeeded = false,
            flowLoggerMock = flowLogger
        )

        val gradleVersionDetector = mockk<GradleVersionDetector>()
        every { gradleVersionDetector.detectGradleVersion(any(), any()) } returns versionDetectionCommand

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any(), any(), any()) } returns mockk(relaxed = true)

        val session = GradleRunnerBuildSession(
            gradleRunnerContext, gradleVersionDetector, gradleCommandLineProvider
        )
        session.sessionStarted()

        // act
        Assert.assertSame(session.nextCommand, versionDetectionCommand)
        val gradleCommand = session.nextCommand!!
        gradleCommand.onStandardOutput("gradle output")

        // assert
        verify(exactly = 1) { flowLogger.message("gradle output") }
    }

    private fun createSession(
        versionDetectionResult: CommandExecution? = mockk(),
        isGradleVersionNotNeeded: Boolean,
        failBuildOnExitCode: Boolean = true
    ): GradleRunnerBuildSession {
        val gradleVersionDetector = mockk<GradleVersionDetector>()
        if (versionDetectionResult != null) {
            every { gradleVersionDetector.detectGradleVersion(any(), any()) } returns versionDetectionResult
        }

        val gradleCommandLineProvider = mockk<GradleCommandLineProvider>()
        every { gradleCommandLineProvider.getGradleCommandLine(any(), any(), any()) } returns mockk(relaxed = true)

        val gradleRunnerContext = getGradleRunnerContext(isGradleVersionNotNeeded, failBuildOnExitCode)

        return GradleRunnerBuildSession(
            gradleRunnerContext, gradleVersionDetector, gradleCommandLineProvider
        )
    }

    private fun getGradleRunnerContext(
        isGradleVersionNotNeeded: Boolean = false,
        failBuildOnExitCode: Boolean = true,
        flowLoggerMock: FlowLogger = mockk(relaxed = true)
    ): GradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true).apply {
        if (isGradleVersionNotNeeded) {
            every { isWrapperPropertiesFileMissing } returns true
            every { noWrapperInVirtualContext } returns true
        }
        every { build.failBuildOnExitCode } returns failBuildOnExitCode
        every { flowLogger } returns flowLoggerMock
    }
}
