package jetbrains.buildServer.gradle.test.integration

import jetbrains.buildServer.util.StringUtil
import org.testng.Assert.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.Test

class BuildProblemServiceMessagesTest : GradleRunnerServiceMessageTest() {

    @AfterMethod
    @Throws(Exception::class)
    override fun tearDown() {
        myBuildEnvVars.remove(FAIL_CACHED_TASK_ENV_VAR)
        super.tearDown()
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should report build problem with configuration cache enabled`(gradleVersion: String) {
        // arrange
        val config =
            buildProblemConfig(PROJECT_WITH_BROKEN_TASKS_NAME, "brokenTask --configuration-cache", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertSingleBuildProblem(messages.messages, "java.lang.IllegalStateException: custom failure marker")
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should report build problem when configuration cache is reused`(gradleVersion: String) {
        // arrange
        val config =
            buildProblemConfig(PROJECT_WITH_BROKEN_TASKS_NAME, "maybeBroken --configuration-cache", gradleVersion)

        // act: store the configuration cache with a successful task execution
        val firstRunMessages = run(config)

        // assert: first run is successful and no build problem is reported
        assertBuildSuccessful(firstRunMessages.allMessages)
        assertContainsLine(firstRunMessages.allMessages, "Configuration cache entry stored.")
        assertNoBuildProblems(firstRunMessages.messages)

        // act: reuse the cached configuration and fail during task execution
        myBuildEnvVars[FAIL_CACHED_TASK_ENV_VAR] = true.toString()
        val secondRunMessages = run(config)

        // assert
        assertBuildFailed(secondRunMessages.allMessages)
        assertContainsLine(secondRunMessages.allMessages, "Reusing configuration cache.")
        assertSingleBuildProblem(secondRunMessages.messages, "java.lang.IllegalStateException: cached failure marker")
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should not report build problem for failing tests`(gradleVersion: String) {
        // arrange
        val config = buildProblemConfig(PROJECT_C_NAME, "junit --configuration-cache", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertContainsLine(messages.allMessages, "##teamcity[testFailed")
        assertNoBuildProblems(messages.messages)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should not report build problem for compilation errors`(gradleVersion: String) {
        // arrange
        val config = buildProblemConfig(PROJECT_B_NAME, "compileJava --configuration-cache", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertContainsLine(messages.allMessages, "##teamcity[compilationStarted")
        assertNoBuildProblems(messages.messages)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should include task execution exception cause in build problem description`(gradleVersion: String) {
        // arrange
        val config = buildProblemConfig(PROJECT_WITH_BROKEN_TASKS_NAME, "brokenTask", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertSingleBuildProblem(messages.messages, "Execution failed for task")
        assertSingleBuildProblem(messages.messages, "java.lang.IllegalStateException: custom failure marker")
    }

    private fun buildProblemConfig(project: String, command: String, gradleVersion: String): GradleRunConfiguration {
        return GradleRunConfiguration(project, command, null).also {
            it.gradleVersion = gradleVersion
            it.patternStr = BUILD_PROBLEM_PATTERN
        }
    }

    private fun assertSingleBuildProblem(serviceMessages: List<String>, expectedText: String) {
        val buildProblems = serviceMessages.filter { it.startsWith("##teamcity[buildProblem ") }
        assertEquals(buildProblems.size, 1, "Expected one build problem.\n${fullServiceMessages(serviceMessages)}")
        assertTrue(
            buildProblems.single().contains("type='gradleBuildProblem'"),
            "Expected a Gradle build problem.\n${buildProblems.single()}"
        )
        assertTrue(
            buildProblems.single().contains(expectedText),
            "Expected build problem to contain '$expectedText'.\n${buildProblems.single()}"
        )
    }

    private fun assertNoBuildProblems(serviceMessages: List<String>) {
        assertFalse(
            serviceMessages.any { it.startsWith("##teamcity[buildProblem ") },
            "Expected no build problem service messages.\n${fullServiceMessages(serviceMessages)}"
        )
    }

    private fun assertBuildSuccessful(allMessages: List<String>) {
        assertTrue(
            allMessages.any { it.startsWith("BUILD SUCCESSFUL") },
            "Expected: BUILD SUCCESSFUL\n${fullLog(allMessages)}"
        )
    }

    private fun assertBuildFailed(allMessages: List<String>) {
        assertTrue(allMessages.any { it.startsWith("BUILD FAILED") }, "Expected: BUILD FAILED\n${fullLog(allMessages)}")
    }

    private fun assertContainsLine(allMessages: List<String>, expectedText: String) {
        assertTrue(
            allMessages.any { it.contains(expectedText) },
            "Expected log to contain '$expectedText'.\n${fullLog(allMessages)}"
        )
    }

    private fun fullLog(allMessages: List<String>): String = "Full log:\n${StringUtil.join("\n", allMessages)}"

    private fun fullServiceMessages(serviceMessages: List<String>): String =
        "Full service messages:\n${StringUtil.join("\n", serviceMessages)}"

    private companion object {
        const val FAIL_CACHED_TASK_ENV_VAR = "TEAMCITY_FAIL_CACHED_BUILD_PROBLEM_TASK"
        const val BUILD_PROBLEM_PATTERN = "##teamcity\\[buildProblem (.*?)(?<!\\|)\\]"
    }
}
