package jetbrains.buildServer.gradle.test.unit.versionDetection

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersion
import jetbrains.buildServer.gradle.agent.versionDetection.GradleVersionDetector
import org.testng.Assert
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
class GradleVersionDetectorTest {

    @DataProvider(name = "gradleVersionOutputs")
    fun gradleVersionOutputs(): Array<Array<Any>> = arrayOf(
        // Standard release versions
        arrayOf(GRADLE_2_0_OUTPUT, "2.0"),
        arrayOf(GRADLE_4_0_2_OUTPUT, "4.0.2"),
        arrayOf(GRADLE_8_0_OUTPUT, "8.0"),

        // Pre-release versions
        arrayOf(GRADLE_8_6_RC1_OUTPUT, "8.6-rc-1"),
        arrayOf(GRADLE_8_5_MILESTONE_OUTPUT, "8.5-milestone-1"),
        arrayOf(GRADLE_9_3_0_RC3_OUTPUT, "9.3.0-rc-3"),

        // Snapshot versions
        arrayOf(GRADLE_8_6_SNAPSHOT_OUTPUT, "8.6-20231201000000")
    )

    @Test(dataProvider = "gradleVersionOutputs")
    fun `should parse and return gradle version from command execution output`(output: String, expectedVersion: String) {
        // arrange
        var detectedVersion: GradleVersion? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val detector = GradleVersionDetector(gradleRunnerContext)
        val execution = detector.detectGradleVersion(false) { version ->
            detectedVersion = version
        }

        // act
        output.lines().forEach { execution.onStandardOutput(it) }
        execution.processFinished(0)

        // assert
        Assert.assertEquals(detectedVersion, GradleVersion(expectedVersion))
    }

    @DataProvider(name = "invalidGradleOutputs")
    fun invalidGradleOutputs(): Array<Array<Any>> = arrayOf(
        arrayOf(""),
        arrayOf("Some random output"),
        arrayOf("gradle 8.5"),  // lowercase 'gradle'
        arrayOf("Gradle"),  // no version
        arrayOf("Gradle "),  // no version after space
        arrayOf("Version 8.5"),  // wrong prefix
    )

    @Test(dataProvider = "invalidGradleOutputs")
    fun `should return null for the gradle version when the output does not contain a valid gradle version`(output: String) {
        // arrange
        var detectedVersion: GradleVersion? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val detector = GradleVersionDetector(gradleRunnerContext)
        val execution = detector.detectGradleVersion(false) { version ->
            detectedVersion = version
        }

        // act
        output.lines().forEach { execution.onStandardOutput(it) }
        execution.processFinished(0)

        // assert
        Assert.assertNull(detectedVersion)
    }

    @Test
    fun `should return null for the gradle version when the command execution fails`() {
        // arrange
        var detectedVersion: GradleVersion? = null
        val gradleRunnerContext = mockk<GradleRunnerContext>(relaxed = true)
        val detector = GradleVersionDetector(gradleRunnerContext)
        val execution = detector.detectGradleVersion(false) { version ->
            detectedVersion = version
        }

        // act
        execution.onStandardOutput("Error output")
        execution.processFinished(127)

        // assert
        Assert.assertNull(detectedVersion)
    }

    @Test
    fun `should log a warning when version detection command fails`() {
        // arrange
        val logger = mockk<BuildProgressLogger>(relaxed = true)
        val gradleRunnerContext = mockk<GradleRunnerContext>()
        every { gradleRunnerContext.buildLogger } returns logger
        val detector = GradleVersionDetector(gradleRunnerContext)
        val execution = detector.detectGradleVersion(false) { }

        // act
        execution.onStandardOutput("std out")
        execution.onErrorOutput("std err")
        execution.processFinished(1)

        // assert
        verify { logger.warning("Could not detect the Gradle version. The Gradle --version call finished with a non-zero exit code.") }
    }

    companion object {
        private val GRADLE_2_0_OUTPUT = """

            ------------------------------------------------------------
            Gradle 2.0
            ------------------------------------------------------------

            Build time:   2014-07-01 07:45:34 UTC
            Build number: none
            Revision:     b6ead6fa452dfdadec484059191eb641d817226c

            Groovy:       2.3.3
            Ant:          Apache Ant(TM) version 1.9.3 compiled on December 23 2013
            JVM:          1.8.0_432 (Amazon.com Inc. 25.432-b06)
            OS:           Mac OS X 15.6.1 aarch64

        """.trimIndent()

        private val GRADLE_4_0_2_OUTPUT = """

            ------------------------------------------------------------
            Gradle 4.0.2
            ------------------------------------------------------------

            Build time:   2017-07-26 15:04:56 UTC
            Revision:     108c593aa7b43852f39045337ee84ee1d87c87fd

            Groovy:       2.4.11
            Ant:          Apache Ant(TM) version 1.9.6 compiled on June 29 2015
            JVM:          1.8.0_432 (Amazon.com Inc. 25.432-b06)
            OS:           Mac OS X 15.6.1 aarch64

        """.trimIndent()

        private val GRADLE_8_0_OUTPUT = """

            ------------------------------------------------------------
            Gradle 8.0
            ------------------------------------------------------------

            Build time:   2023-02-13 14:19:58 UTC
            Revision:     62ab9b7c7f884426cf79fbedcf07658b2cbe0966

            Kotlin:       1.8.10
            Groovy:       3.0.13
            Ant:          Apache Ant(TM) version 1.10.11 compiled on July 10 2021
            JVM:          17.0.6 (Eclipse Adoptium 17.0.6+10)
            OS:           Linux 5.15.0-58-generic amd64

        """.trimIndent()

        private val GRADLE_8_6_RC1_OUTPUT = """

            ------------------------------------------------------------
            Gradle 8.6-rc-1
            ------------------------------------------------------------

            Build time:   2024-01-15 14:08:57 UTC
            Revision:     28aca86a7180baa17117e0e5ba01d8ea9feca598

            Kotlin:       1.9.21
            Groovy:       3.0.17
            Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
            JVM:          21.0.1 (Eclipse Adoptium 21.0.1+12)
            OS:           Mac OS X 14.2 aarch64

        """.trimIndent()

        private val GRADLE_8_5_MILESTONE_OUTPUT = """

            ------------------------------------------------------------
            Gradle 8.5-milestone-1
            ------------------------------------------------------------

            Build time:   2023-10-15 10:00:00 UTC
            Revision:     1234567890abcdef1234567890abcdef12345678

            Kotlin:       1.9.10
            Groovy:       3.0.17
            Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
            JVM:          17.0.8 (Eclipse Adoptium 17.0.8+7)
            OS:           Mac OS X 13.5 aarch64

        """.trimIndent()

        private val GRADLE_8_6_SNAPSHOT_OUTPUT = """

            ------------------------------------------------------------
            Gradle 8.6-20231201000000
            ------------------------------------------------------------

            Build time:   2023-12-01 00:00:00 UTC
            Revision:     abcdef1234567890abcdef1234567890abcdef12

            Kotlin:       1.9.21
            Groovy:       3.0.17
            Ant:          Apache Ant(TM) version 1.10.13 compiled on January 4 2023
            JVM:          21.0.1 (Eclipse Adoptium 21.0.1+12)
            OS:           Linux 6.5.0-14-generic amd64

        """.trimIndent()

        private val GRADLE_9_3_0_RC3_OUTPUT = """

            ------------------------------------------------------------
            Gradle 9.3.0-rc-3
            ------------------------------------------------------------

            Build time:    2026-01-12 11:00:23 UTC
            Revision:      8dd290bb13b269e80427d1c5ac5ba071dd77c211

            Kotlin:        2.2.21
            Groovy:        4.0.29
            Ant:           Apache Ant(TM) version 1.10.15 compiled on August 25 2024
            Launcher JVM:  11.0.20.1 (Amazon.com Inc. 11.0.20.1+9-LTS)
            Daemon JVM:    /Library/Java/JavaVirtualMachines/amazon-corretto-11.jdk/Contents/Home (no Daemon JVM specified, using current Java home)
            OS:            Mac OS X 15.6.1 aarch64

        """.trimIndent()
    }
}
