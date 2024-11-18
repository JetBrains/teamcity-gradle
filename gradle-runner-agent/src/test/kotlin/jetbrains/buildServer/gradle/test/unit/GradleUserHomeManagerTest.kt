package jetbrains.buildServer.gradle.test.unit

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.gradle.GradleRunnerConstants
import jetbrains.buildServer.gradle.agent.GradleUserHomeManager
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.tooling.model.build.GradleEnvironment
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

@Test
class GradleUserHomeManagerTest {

    @MockK private lateinit var projectConnector: GradleConnector
    @MockK private lateinit var projectConnection: ProjectConnection
    @MockK private lateinit var buildEnvironment: BuildEnvironment
    @MockK private lateinit var gradleEnvironment: GradleEnvironment
    private lateinit var gradleUserHomeManager: GradleUserHomeManager
    private val gradleUserHomeEnv = File("/GradleUserHome/FromEnvironment")
    private val environmentVariables = mapOf("GRADLE_USER_HOME" to gradleUserHomeEnv.path)
    private val gradleUserHomeDefault = File("/UserHome/.gradle")

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        gradleUserHomeManager = GradleUserHomeManager()

        every { projectConnector.connect() } returns projectConnection
        every { projectConnection.getModel(BuildEnvironment::class.java) } returns buildEnvironment
        every { buildEnvironment.gradle } returns gradleEnvironment
        every { gradleEnvironment.gradleUserHome } returns gradleUserHomeDefault
    }

    @DataProvider
    fun tasksAndArgumentsProvider(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            // an argument
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--gradle-user-home=/GradleUserHome", "--info", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "--gradle-user-home=/GradleUserHome", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "--offline", "--gradle-user-home=/GradleUserHome"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--gradle-user-home=/GradleUserHome"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "--gradle-user-home", "/GradleUserHome", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "-g", "/GradleUserHome", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "-g=/GradleUserHome", "--offline"),
                File("/GradleUserHome")
            ),

            // an argument in tasks
            arrayOf<Any>(
                listOf("clean", "-g=/GradleUserHome", "build"),
                listOf("--info", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("-g=/GradleUserHome", "clean", "build"),
                listOf("--info", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build", "-g=/GradleUserHome"),
                listOf("--info", "--offline"),
                File("/GradleUserHome")
            ),

            // system property
            arrayOf<Any>(
                listOf("clean", "build", "-Dgradle.user.home=/GradleUserHome"),
                listOf("--info", "--offline"),
                File("/GradleUserHome")
            ),
            arrayOf<Any>(
                listOf("clean", "build"),
                listOf("--info", "-Dgradle.user.home=/GradleUserHome", "--offline"),
                File("/GradleUserHome")
            ),
        )
    }

    @Test(dataProvider = "tasksAndArgumentsProvider")
    fun `should return Gradle User Home when defined in command line`(gradleTasks: List<String>,
                                                                      gradleArguments: List<String>,
                                                                      expectedValue: File) {
        // act
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, environmentVariables, projectConnector)

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), expectedValue)
    }

    @Test
    fun `should return Gradle User Home when not defined in command line and defined environment variables`() {
        // arrange
        val gradleTasks = listOf("clean build")
        val gradleArguments = listOf("--info")

        // act
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, environmentVariables, projectConnector)

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), gradleUserHomeEnv)
    }

    @Test
    fun `should return default Gradle User Home when not defined in command line and environment variables`() {
        // arrange
        val gradleTasks = listOf("clean build")
        val gradleArguments = listOf("--info")

        // act
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, mapOf(), projectConnector)

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), gradleUserHomeDefault)
    }

    @Test
    fun `should override GradleUserHome when config param is enabled`() {
        // arrange
        val buildRunnerContext = mockk<BuildRunnerContext>()
        every { buildRunnerContext.configParameters } returns mapOf(
            GradleRunnerConstants.GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED to "true"
        )
        every { buildRunnerContext.isVirtualContext } returns true
        val depCacheEnabled = true

        // act
        val result = gradleUserHomeManager.isGradleUserHomeOverrideNeeded(buildRunnerContext, depCacheEnabled)

        // assert
        assertTrue(result)
    }

    @Test
    fun `should override GradleUserHome when config param is missing`() {
        // arrange
        val buildRunnerContext = mockk<BuildRunnerContext>()
        every { buildRunnerContext.configParameters } returns mapOf()
        every { buildRunnerContext.isVirtualContext } returns true
        val depCacheEnabled = true

        // act
        val result = gradleUserHomeManager.isGradleUserHomeOverrideNeeded(buildRunnerContext, depCacheEnabled)

        // assert
        assertTrue(result)
    }

    @Test
    fun `should not override GradleUserHome when config param is false`() {
        // arrange
        val buildRunnerContext = mockk<BuildRunnerContext>()
        every { buildRunnerContext.configParameters } returns mapOf(
            GradleRunnerConstants.GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED to "false"
        )
        every { buildRunnerContext.isVirtualContext } returns true
        val depCacheEnabled = true

        // act
        val result = gradleUserHomeManager.isGradleUserHomeOverrideNeeded(buildRunnerContext, depCacheEnabled)

        // assert
        assertFalse(result)
    }

    @Test
    fun `should not override GradleUserHome in non-virtual context`() {
        // arrange
        val buildRunnerContext = mockk<BuildRunnerContext>()
        every { buildRunnerContext.configParameters } returns mapOf(
            GradleRunnerConstants.GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED to "true"
        )
        every { buildRunnerContext.isVirtualContext } returns false
        val depCacheEnabled = true

        // act
        val result = gradleUserHomeManager.isGradleUserHomeOverrideNeeded(buildRunnerContext, depCacheEnabled)

        // assert
        assertFalse(result)
    }

    @Test
    fun `should not override GradleUserHome when dep cache is disabled`() {
        // arrange
        val buildRunnerContext = mockk<BuildRunnerContext>()
        every { buildRunnerContext.configParameters } returns mapOf(
            GradleRunnerConstants.GRADLE_RUNNER_GRADLE_USER_HOME_OVERRIDE_ENABLED to "true"
        )
        every { buildRunnerContext.isVirtualContext } returns true
        val depCacheEnabled = false

        // act
        val result = gradleUserHomeManager.isGradleUserHomeOverrideNeeded(buildRunnerContext, depCacheEnabled)

        // assert
        assertFalse(result)
    }

    @DataProvider
    fun gradleUserHomeArgumentsProvider(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            // common cases
            arrayOf<Any>(
                mutableListOf<String>(),
                listOf<String>()
            ),
            arrayOf<Any>(
                mutableListOf<String>("--gradle-user-home=/GradleUserHome", "--gradle-user-home", "/GradleUserHome2", "-g=/GradleUserHome3", "-g", "/GradleUserHome4"),
                listOf<String>()
            ),
            // --gradle-user-home=val
            arrayOf<Any>(
                mutableListOf("--gradle-user-home=/GradleUserHome", "--info", "--offline"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf("--info", "--gradle-user-home=/GradleUserHome", "--offline"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf("--info", "--offline", "--gradle-user-home=/GradleUserHome"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf<String>("--gradle-user-home=/GradleUserHome"),
                listOf<String>()
            ),
            // --gradle-user-home val
            arrayOf<Any>(
                mutableListOf("--gradle-user-home", "/GradleUserHome", "--info", "--offline"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf("--info", "--gradle-user-home", "/GradleUserHome", "--offline"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf("--info", "--offline", "--gradle-user-home", "/GradleUserHome"),
                listOf("--info", "--offline")
            ),
            arrayOf<Any>(
                mutableListOf<String>("--gradle-user-home", "/GradleUserHome"),
                listOf<String>()
            ),
            arrayOf<Any>(
                mutableListOf<String>("--gradle-user-home"),
                listOf<String>()
            ),
            // -g=val
            arrayOf<Any>(
                mutableListOf("-g=/GradleUserHome", "clean", "build"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf("clean", "-g=/GradleUserHome", "build"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf("clean", "build", "-g=/GradleUserHome"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf<String>("-g=/GradleUserHome"),
                listOf<String>()
            ),
            // -g val
            arrayOf<Any>(
                mutableListOf("-g", "/GradleUserHome", "clean", "build"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf("clean", "-g", "/GradleUserHome", "build"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf("clean", "build", "-g", "/GradleUserHome"),
                listOf("clean", "build")
            ),
            arrayOf<Any>(
                mutableListOf<String>("-g", "/GradleUserHome"),
                listOf<String>()
            ),
            arrayOf<Any>(
                mutableListOf<String>("-g"),
                listOf<String>()
            ),
            // with a system property
            arrayOf<Any>(
                mutableListOf("-g=/GradleUserHome", "-Dgradle.user.home=/AnotherValue"),
                listOf("-Dgradle.user.home=/AnotherValue")
            ),
            arrayOf<Any>(
                mutableListOf("-Dgradle.user.home=/AnotherValue", "-g=/GradleUserHome"),
                listOf("-Dgradle.user.home=/AnotherValue")
            ),
        )
    }

    @Test(dataProvider = "gradleUserHomeArgumentsProvider")
    fun `should remove GradleUserHome from user-defined tasks or params`(
        items: List<String>,
        expected: List<String>
    ) {
        // arrange, act
        val result = gradleUserHomeManager.removeGradleUserHomeArgument(items)

        // assert
        assertEquals(result, expected)
    }

    @Test
    fun `should build GradleUserHome agent local`() {
        // arrange
        val cachesDir = File("/path/to/cache")
        val agentConfiguration = mockk<BuildAgentConfiguration>()
        every { agentConfiguration.getCacheDirectory(any()) } returns cachesDir
        val agentRunningBuild = mockk<AgentRunningBuild>()
        every { agentRunningBuild.agentConfiguration } returns agentConfiguration
        val localGradleUserHomeDir = "gradle.user.home"

        // act
        val result = gradleUserHomeManager.getGradleUserHomeAgentLocal(agentConfiguration)

        // assert
        assertEquals(result.absolutePath, File(cachesDir, localGradleUserHomeDir).absolutePath)
    }
}