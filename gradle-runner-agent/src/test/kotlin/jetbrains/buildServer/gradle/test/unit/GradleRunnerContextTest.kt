package jetbrains.buildServer.gradle.test.unit

import com.intellij.openapi.util.TCSystemInfo
import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildParametersMap
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.runner.JavaRunnerUtil
import jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_WRAPPER_FLAG
import jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_WRAPPER_PATH
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.runner.JavaRunnerConstants
import jetbrains.buildServer.util.Option
import org.testng.Assert
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

@Test
class GradleRunnerContextTest {

    private val tempFiles = TempFiles()
    private val runnerParams = mutableMapOf<String, String>()
    private val buildParams = mutableMapOf<String, String>()
    private val envVars = mutableMapOf<String, String>()
    private val systemProps = mutableMapOf<String, String>()
    private val configParameters = mutableMapOf<String, String>()

    private lateinit var runnerContext: BuildRunnerContext
    private lateinit var build: AgentRunningBuild
    private lateinit var tempDir: File

    @BeforeMethod
    fun setUp() {
        tempDir = tempFiles.createTempDir()

        val buildParametersMap = mockk<BuildParametersMap> {
            every { allParameters } returns buildParams
            every { environmentVariables } returns envVars
            every { systemProperties } returns systemProps
        }

        build = mockk<AgentRunningBuild>(relaxed = true) {
            every { buildTempDirectory } returns tempDir
            every { getBuildTypeOptionValue(any<Option<Any>>()) } answers {
                (firstArg<Option<Any>>()).defaultValue
            }
        }

        runnerContext = mockk<BuildRunnerContext> {
            every { runnerParameters } returns runnerParams
            every { this@mockk.buildParameters } returns buildParametersMap
            every { this@mockk.build } returns this@GradleRunnerContextTest.build
            every { id } returns "testRunner"
            every { isVirtualContext } returns false
            every { this@mockk.configParameters } returns this@GradleRunnerContextTest.configParameters
        }

        val jdk = System.getProperty("java.home")
        val propsAndVars = mapOf("system.java.home" to jdk)
        val javaHome = JavaRunnerUtil.findJavaHome(jdk, propsAndVars, null)!!
        runnerParams[JavaRunnerConstants.TARGET_JDK_HOME] = javaHome
    }

    @AfterMethod
    fun tearDown() {
        tempFiles.cleanup()
        runnerParams.clear()
        buildParams.clear()
        envVars.clear()
        systemProps.clear()
        configParameters.clear()
    }

    @Test(expectedExceptions = [RunBuildException::class])
    fun `should throw when gradle home does not exist`() {
        every { runnerContext.getToolPath("gradle") } returns ""
        every { runnerContext.workingDirectory } returns tempDir

        GradleRunnerContext(runnerContext)
    }

    @Test(expectedExceptions = [RunBuildException::class])
    fun `should throw when gradle executable does not exist in gradle home`() {
        every { runnerContext.getToolPath("gradle") } returns tempFiles.createTempDir().absolutePath
        every { runnerContext.workingDirectory } returns tempDir

        GradleRunnerContext(runnerContext)
    }

    @Test
    fun `should set wrapper properties missing flag when wrapper is enabled and properties file is absent`() {
        // arrange
        runnerParams[GRADLE_WRAPPER_FLAG] = true.toString()
        runnerParams[GRADLE_WRAPPER_PATH] = "."
        every { runnerContext.workingDirectory } returns tempDir
        File(tempDir, wrapperScriptName()).createNewFile()

        // act
        val gradleRunnerContext = GradleRunnerContext(runnerContext)

        // assert
        Assert.assertTrue(gradleRunnerContext.isWrapperPropertiesFileMissing)
        Assert.assertFalse(gradleRunnerContext.noWrapperInVirtualContext)
    }

    @Test
    fun `should set no wrapper in virtual context flag when wrapper is disabled in virtual context`() {
        // arrange
        every { runnerContext.isVirtualContext } returns true
        every { runnerContext.workingDirectory } returns tempDir

        // act
        val gradleRunnerContext = GradleRunnerContext(runnerContext)

        // assert
        Assert.assertTrue(gradleRunnerContext.noWrapperInVirtualContext)
        Assert.assertFalse(gradleRunnerContext.isWrapperPropertiesFileMissing)
    }

    private fun wrapperScriptName(): String {
        return if (TCSystemInfo.isWindows) {
            GradleRunnerContext.WIN_GRADLEW
        } else {
            GradleRunnerContext.UNIX_GRADLEW
        }
    }
}
