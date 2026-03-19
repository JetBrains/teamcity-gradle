package jetbrains.buildServer.gradle.test.unit

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.GradleUserHomeManager
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

@Test
class GradleUserHomeManagerTest {
    @MockK private lateinit var gradleRunnerContext: GradleRunnerContext
    private lateinit var gradleUserHomeManager: GradleUserHomeManager
    private val gradleUserHomeEnv = File("/GradleUserHome/FromEnvironment")
    private val environmentVariables = mapOf("GRADLE_USER_HOME" to gradleUserHomeEnv.path)

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        gradleUserHomeManager = GradleUserHomeManager()

        every { gradleRunnerContext.environmentVariables } returns environmentVariables
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
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, gradleRunnerContext, mockk(relaxed = true))

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
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, gradleRunnerContext, mockk(relaxed = true))

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), gradleUserHomeEnv)
    }

    @Test
    fun `should return default Gradle User Home when not defined in command line and environment variables`() {
        // arrange
        val gradleTasks = listOf("clean build")
        val gradleArguments = listOf("--info")
        every { gradleRunnerContext.environmentVariables } returns mapOf()
        System.setProperty("user.home", "/UserHome")

        // act
        val result = gradleUserHomeManager.detectGradleUserHome(gradleTasks, gradleArguments, gradleRunnerContext, mockk(relaxed = true))

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), File("/UserHome/.gradle"))
    }
}
