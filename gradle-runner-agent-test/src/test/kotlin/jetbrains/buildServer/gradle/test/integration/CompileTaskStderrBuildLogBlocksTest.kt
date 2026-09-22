package jetbrains.buildServer.gradle.test.integration

import jetbrains.buildServer.util.StringUtil
import org.testng.Assert.*
import org.testng.annotations.Test

class CompileTaskStderrBuildLogBlocksTest : GradleRunnerServiceMessageTest() {

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should report failed compile task stderr inside a compilation block`(gradleVersion: String) {
        // arrange
        val config = compileStderrConfig("compileJava", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertCompileErrorBlock(messages.messages)
        assertErrorMessageReported(messages.messages, COMPILE_STDERR_MARKER_1)
        assertErrorMessageReported(messages.messages, COMPILE_STDERR_MARKER_2)
        assertCompileBlockUsesOneFlow(messages.messages)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should report failed compile task stderr inside a compilation block with configuration cache enabled`(gradleVersion: String) {
        // arrange
        val config = compileStderrConfig("compileJava --configuration-cache", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertCompileErrorBlock(messages.messages)
        assertErrorMessageReported(messages.messages, COMPILE_STDERR_MARKER_1)
        assertErrorMessageReported(messages.messages, COMPILE_STDERR_MARKER_2)
        assertCompileBlockUsesOneFlow(messages.messages)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should not report non-compile task stderr as compilation error messages`(gradleVersion: String) {
        // arrange
        val config = compileStderrConfig("writeToStdErr compileJava", gradleVersion)

        // act
        val messages = run(config)

        // assert
        assertBuildFailed(messages.allMessages)
        assertTrue(
            messages.allMessages.any { it.contains(NON_COMPILE_STDERR_MARKER) },
            "Expected raw non-compile stderr in the build log.\n${fullLog(messages.allMessages)}"
        )
        assertFalse(
            messages.messages.any { it.startsWith("##teamcity[message ") && it.contains(NON_COMPILE_STDERR_MARKER) },
            "Expected non-compile stderr not to be converted to compilation error messages.\n${fullServiceMessages(messages.messages)}"
        )
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should truncate compile stderr and emit truncation notice when it exceeds maximal error buffer size`(gradleVersion: String) {
        // lower the compile-error buffer cap (default is 5MB) to verify its truncation gets triggered
        val config = GradleRunConfiguration(PROJECT_WITH_LARGE_COMPILE_STDERR_NAME, "-Dteamcity.gradle.compileError.maxLength=1024 compileJava", null).also {
            it.gradleVersion = gradleVersion
            it.patternStr = COMPILE_BLOCK_PATTERN
        }

        val messages = run(config)

        assertBuildFailed(messages.allMessages)
        assertCompileErrorBlock(messages.messages)
        assertErrorMessageReported(messages.messages, VISIBLE_STDERR_MARKER)
        assertErrorMessageReported(messages.messages, TRUNCATION_NOTICE_FRAGMENT)
        assertFalse(
            messages.messages.any { it.startsWith("##teamcity[message ") && it.contains(NOT_VISIBLE_STDERR_MARKER) },
            "Stderr past the cap should have been truncated.\n${fullServiceMessages(messages.messages)}"
        )
    }

    private fun compileStderrConfig(command: String, gradleVersion: String): GradleRunConfiguration {
        return GradleRunConfiguration(PROJECT_WITH_COMPILE_STDERR_NAME, command, null).also {
            it.gradleVersion = gradleVersion
            it.patternStr = COMPILE_BLOCK_PATTERN
        }
    }

    private fun assertCompileErrorBlock(serviceMessages: List<String>) {
        assertEquals(
            serviceMessages.count { it.startsWith("##teamcity[compilationStarted ") && it.contains("compiler=':compileJava'") },
            1,
            "Expected one compileJava compilationStarted message.\n${fullServiceMessages(serviceMessages)}"
        )
        assertEquals(
            serviceMessages.count { it.startsWith("##teamcity[compilationFinished ") && it.contains("compiler=':compileJava'") },
            1,
            "Expected one compileJava compilationFinished message.\n${fullServiceMessages(serviceMessages)}"
        )
    }

    private fun assertErrorMessageReported(serviceMessages: List<String>, expectedText: String) {
        assertTrue(
            serviceMessages.any {
                it.startsWith("##teamcity[message ") &&
                    it.contains("status='ERROR'") &&
                    it.contains(expectedText)
            },
            "Expected compile stderr marker '$expectedText' to be reported as an error message.\n${fullServiceMessages(serviceMessages)}"
        )
    }

    private fun assertCompileBlockUsesOneFlow(serviceMessages: List<String>) {
        val compileBlockMessages = serviceMessages.filter {
            it.contains("compiler=':compileJava'") || it.contains(COMPILE_STDERR_MARKER_1) || it.contains(COMPILE_STDERR_MARKER_2)
        }
        val flowIds = compileBlockMessages.map { serviceMessage ->
            val flowId = FLOW_ID_REGEX.find(serviceMessage)?.groupValues?.get(1)
            assertNotNull(flowId, "Expected flowId in service message: $serviceMessage")
            flowId
        }.toSet()
        assertEquals(flowIds.size, 1, "Expected compile block messages to use one flowId.\n${fullServiceMessages(compileBlockMessages)}")
    }

    private fun assertBuildFailed(allMessages: List<String>) {
        assertTrue(allMessages.any { it.startsWith("BUILD FAILED") }, "Expected: BUILD FAILED\n${fullLog(allMessages)}")
    }

    private fun fullLog(allMessages: List<String>): String = "Full log:\n${StringUtil.join("\n", allMessages)}"

    private fun fullServiceMessages(serviceMessages: List<String>): String =
        "Full service messages:\n${StringUtil.join("\n", serviceMessages)}"

    private companion object {
        const val COMPILE_STDERR_MARKER_1 = "compile stderr marker one"
        const val COMPILE_STDERR_MARKER_2 = "compile stderr marker two"
        const val NON_COMPILE_STDERR_MARKER = "non-compile stderr marker"
        const val VISIBLE_STDERR_MARKER = "Stderr marker that should be visible"
        const val NOT_VISIBLE_STDERR_MARKER = "Stderr marker that should NOT be visible"
        const val TRUNCATION_NOTICE_FRAGMENT = "TeamCity truncated compile error output, because it is too large. See raw build log for full stderr."
        const val COMPILE_BLOCK_PATTERN = "##teamcity\\[(message|compilation)(.*?)(?<!\\|)\\]"
        val FLOW_ID_REGEX = Regex("flowId='([^']+)'")
    }
}
