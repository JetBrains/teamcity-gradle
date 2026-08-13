package jetbrains.buildServer.gradle.test.unit.versionDetection

import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.versionDetection.VersionResolutionCheckExtensions.isGradleVersionNotNeeded
import org.testng.Assert
import org.testng.annotations.Test

@Test
class VersionResolutionCheckExtensionsTest {

    @Test
    fun `should not require gradle version when wrapper properties file is missing`() {
        // arrange
        val context = mockk<GradleRunnerContext>()
        every { context.isWrapperPropertiesFileMissing } returns true
        every { context.noWrapperInVirtualContext } returns false

        // act
        val actual = context.isGradleVersionNotNeeded()

        // assert
        Assert.assertTrue(actual)
    }

    @Test
    fun `should not require gradle version when wrapper is not used in a virtual context`() {
        // arrange
        val context = mockk<GradleRunnerContext>()
        every { context.isWrapperPropertiesFileMissing } returns false
        every { context.noWrapperInVirtualContext } returns true

        // act
        val actual = context.isGradleVersionNotNeeded()

        // assert
        Assert.assertTrue(actual)
    }

    @Test
    fun `should require gradle version when neither skip condition is met`() {
        // arrange
        val context = mockk<GradleRunnerContext>()
        every { context.isWrapperPropertiesFileMissing } returns false
        every { context.noWrapperInVirtualContext } returns false

        // act
        val actual = context.isGradleVersionNotNeeded()

        // assert
        Assert.assertFalse(actual)
    }
}
