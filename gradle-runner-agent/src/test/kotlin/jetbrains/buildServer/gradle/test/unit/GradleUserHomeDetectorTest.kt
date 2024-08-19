package jetbrains.buildServer.gradle.test.unit

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.gradle.agent.GradleUserHomeDetector
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.build.BuildEnvironment
import org.gradle.tooling.model.build.GradleEnvironment
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.io.File

@Test
class GradleUserHomeDetectorTest {

    @MockK private lateinit var projectConnector: GradleConnector
    @MockK private lateinit var projectConnection: ProjectConnection
    @MockK private lateinit var buildEnvironment: BuildEnvironment
    @MockK private lateinit var gradleEnvironment: GradleEnvironment
    private lateinit var detector: GradleUserHomeDetector
    private val gradleUserHomeEnv = File("/GradleUserHome/FromEnvironment")
    private val environmentVariables = mapOf("GRADLE_USER_HOME" to gradleUserHomeEnv.path)
    private val gradleUserHomeDefault = File("/UserHome/.gradle")

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        detector = GradleUserHomeDetector()

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
        val result = detector.detect(gradleTasks, gradleArguments, environmentVariables, projectConnector)

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
        val result = detector.detect(gradleTasks, gradleArguments, environmentVariables, projectConnector)

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
        val result = detector.detect(gradleTasks, gradleArguments, mapOf(), projectConnector)

        // assert
        assertTrue(result.isPresent)
        assertEquals(result.get(), gradleUserHomeDefault)
    }
}