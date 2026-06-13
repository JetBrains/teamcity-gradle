package jetbrains.buildServer.gradle.test.integration

import jetbrains.buildServer.agent.IncrementalBuild
import jetbrains.buildServer.gradle.GradleRunnerConstants.CHANGED_FILES_FILE_SYSTEM_PROPERTY_KEY
import jetbrains.buildServer.gradle.GradleRunnerConstants.IS_INCREMENTAL
import jetbrains.buildServer.util.StringUtil
import org.testng.Assert.assertFalse
import org.testng.Assert.assertTrue
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

class IsolatedProjectsTest : GradleRunnerServiceMessageTest() {
    @AfterMethod
    @Throws(Exception::class)
    override fun tearDown() {
        System.clearProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM)
        super.tearDown()
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should successfully execute a multi-project build with isolated projects enabled`(gradleVersion: String) {
        // arrange
        val config = isolatedProjectsConfig("clean build", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildSuccessful(messages.allMessages)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should report tests from multi-project build with isolated projects enabled`(gradleVersion: String) {
        // arrange
        val config = isolatedProjectsConfig("clean test", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildSuccessful(messages.allMessages)
        assertTestReported(messages.messages, "test.RootTest.rootRuns")
        assertTestReported(messages.messages, "test.LibTest.libRuns")
        assertTestReported(messages.messages, "test.AppTest.appUsesLib")
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should fall back to full build when incremental mode is requested with isolated projects`(gradleVersion: String) {
        // arrange
        System.clearProperty(IncrementalBuild.TEAMCITY_INCREMENTAL_MODE_PARAM)
        myRunnerParams[IS_INCREMENTAL] = true.toString()
        val changedFile = myTempFiles.createTempFile("app/src/main/java/isolated/app/AppGreeter.java:EDIT:1").absolutePath.replace("\\", "/")
        myTeamCitySystemProps[CHANGED_FILES_FILE_SYSTEM_PROPERTY_KEY] = changedFile
        val config = isolatedProjectsConfig("clean test", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildSuccessful(messages.allMessages)
        assertContainsLine(
            messages.allMessages,
            "Incremental mode is not compatible with Gradle Isolated Projects. Falling back to a full build."
        )
        assertTestReported(messages.messages, "test.RootTest.rootRuns")
        assertTestReported(messages.messages, "test.LibTest.libRuns")
        assertTestReported(messages.messages, "test.AppTest.appUsesLib")
    }

    private fun isolatedProjectsConfig(tasks: String, gradleVersion: String): GradleRunConfiguration {
        return GradleRunConfiguration(
            ISOLATED_PROJECTS_COMPATIBLE_PROJECT,
            "-Dorg.gradle.unsafe.isolated-projects=true --configuration-cache $tasks",
            null
        ).also { it.gradleVersion = gradleVersion }
    }

    private fun assertBuildSuccessful(allMessages: List<String>) {
        val fullLog = fullLog(allMessages)
        assertTrue(allMessages.any { it.startsWith("BUILD SUCCESSFUL") }, "Expected: BUILD SUCCESSFUL\n$fullLog")
        assertFalse(allMessages.any { it.contains("init_v2.gradle:") }, "Unexpected init script error.\n$fullLog")
    }

    private fun assertTestReported(serviceMessages: List<String>, testName: String) {
        assertTrue(
            serviceMessages.any { it.startsWith("##teamcity[testStarted name='$testName'") },
            "Expected test '$testName' to be reported.\nFull service messages:\n${StringUtil.join("\n", serviceMessages)}"
        )
    }

    private fun assertContainsLine(allMessages: List<String>, expectedLine: String) {
        assertTrue(
            allMessages.any { it.contains(expectedLine) },
            "Expected log line: $expectedLine\n${fullLog(allMessages)}"
        )
    }

    private fun fullLog(allMessages: List<String>): String = "Full log:\n${StringUtil.join("\n", allMessages)}"
}
