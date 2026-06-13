package jetbrains.buildServer.gradle.test.integration

import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

class TestTaskLogBlocksAndRetryMessagesTest : GradleRunnerServiceMessageTest() {

    @BeforeMethod
    fun resetBuildTypeOptions() {
        myBuildTypeOptionValue.clear()
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should wrap test task service messages in a test task log block`(gradleVersion: String) {
        // arrange
        val config = GradleRunConfiguration(PROJECT_E_NAME, "clean test", null).also {
            it.gradleVersion = gradleVersion
            it.patternStr = TEST_TASK_BLOCK_PATTERN
        }

        // act
        val messages = run(config)

        // assert
        runAndCheckServiceMessages(messages.messages, """
            ##teamcity[blockOpened name='Tests of task :test' flowId='flow_1']
            ##teamcity[flowStarted flowId='flow_2' parent='flow_1']
            ##teamcity[testSuiteStarted name='test.TestClass' flowId='flow_2']
            ##teamcity[flowStarted flowId='flow_3' parent='flow_2']
            ##teamcity[testStarted name='test.TestClass.testA' flowId='flow_3']
            ##teamcity[testStdOut tc:tags='tc:parseServiceMessagesInside' name='test.TestClass.testA' out='StdOut message Could not compile initialization script /som/path/init.gradle' flowId='flow_3']
            ##teamcity[testFinished name='test.TestClass.testA' duration='##Duration##' flowId='flow_3']
            ##teamcity[flowFinished flowId='flow_3']
            ##teamcity[flowStarted flowId='flow_4' parent='flow_2']
            ##teamcity[testStarted name='test.TestClass.testB' flowId='flow_4']
            ##teamcity[testStdErr tc:tags='tc:parseServiceMessagesInside' name='test.TestClass.testB' out='StdErr message' flowId='flow_4']
            ##teamcity[testFinished name='test.TestClass.testB' duration='##Duration##' flowId='flow_4']
            ##teamcity[flowFinished flowId='flow_4']
            ##teamcity[testSuiteFinished name='test.TestClass' flowId='flow_2']
            ##teamcity[flowFinished flowId='flow_2']
            ##teamcity[blockClosed name='Tests of task :test' flowId='flow_1']
        """)
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should emit test retry support messages in the test task log block when retries are enabled`(gradleVersion: String) {
        // arrange
        val config = GradleRunConfiguration(
            MULTI_PROJECT_E_NAME,
            ":projectA:clean :projectA:test :projectA:retryTest -PmaxRetriesProperty=2",
            null
        ).also {
            it.gradleVersion = gradleVersion
            it.patternStr = TEST_TASK_BLOCK_AND_RETRY_PATTERN
        }

        // act
        val messages = run(config)

        // assert
        runAndCheckServiceMessages(messages.messages, """
            ##teamcity[blockOpened name='Tests of task :projectA:test' flowId='flow_1']
            ##teamcity[testRetrySupport enabled='true' flowId='flow_1']
            ##teamcity[flowStarted flowId='flow_2' parent='flow_1']
            ##teamcity[testSuiteStarted name='test.AFlakyTest' flowId='flow_2']
            ##teamcity[flowStarted flowId='flow_3' parent='flow_2']
            ##teamcity[testStarted name='test.AFlakyTest.test' flowId='flow_3']
            ##teamcity[testFailed name='test.AFlakyTest.test' message='java.lang.AssertionError: ' details='##Assert_Stacktrace##' flowId='flow_3']
            ##teamcity[testFinished name='test.AFlakyTest.test' duration='##Duration##' flowId='flow_3']
            ##teamcity[flowFinished flowId='flow_3']
            ##teamcity[testSuiteFinished name='test.AFlakyTest' flowId='flow_2']
            ##teamcity[flowFinished flowId='flow_2']
            ##teamcity[flowStarted flowId='flow_4' parent='flow_1']
            ##teamcity[testSuiteStarted name='test.AFlakyTest' flowId='flow_4']
            ##teamcity[flowStarted flowId='flow_5' parent='flow_4']
            ##teamcity[testStarted name='test.AFlakyTest.test' flowId='flow_5']
            ##teamcity[testFinished name='test.AFlakyTest.test' duration='##Duration##' flowId='flow_5']
            ##teamcity[flowFinished flowId='flow_5']
            ##teamcity[testSuiteFinished name='test.AFlakyTest' flowId='flow_4']
            ##teamcity[flowFinished flowId='flow_4']
            ##teamcity[testRetrySupport enabled='false' flowId='flow_1']
            ##teamcity[blockClosed name='Tests of task :projectA:test' flowId='flow_1']
            ##teamcity[blockOpened name='Tests of task :projectA:retryTest' flowId='flow_6']
            ##teamcity[testRetrySupport enabled='true' flowId='flow_6']
            ##teamcity[flowStarted flowId='flow_7' parent='flow_6']
            ##teamcity[testSuiteStarted name='test.AFlakyTest' flowId='flow_7']
            ##teamcity[flowStarted flowId='flow_8' parent='flow_7']
            ##teamcity[testStarted name='test.AFlakyTest.test' flowId='flow_8']
            ##teamcity[testFailed name='test.AFlakyTest.test' message='java.lang.AssertionError: ' details='##Assert_Stacktrace##' flowId='flow_8']
            ##teamcity[testFinished name='test.AFlakyTest.test' duration='##Duration##' flowId='flow_8']
            ##teamcity[flowFinished flowId='flow_8']
            ##teamcity[testSuiteFinished name='test.AFlakyTest' flowId='flow_7']
            ##teamcity[flowFinished flowId='flow_7']
            ##teamcity[flowStarted flowId='flow_9' parent='flow_6']
            ##teamcity[testSuiteStarted name='test.AFlakyTest' flowId='flow_9']
            ##teamcity[flowStarted flowId='flow_10' parent='flow_9']
            ##teamcity[testStarted name='test.AFlakyTest.test' flowId='flow_10']
            ##teamcity[testFinished name='test.AFlakyTest.test' duration='##Duration##' flowId='flow_10']
            ##teamcity[flowFinished flowId='flow_10']
            ##teamcity[testSuiteFinished name='test.AFlakyTest' flowId='flow_9']
            ##teamcity[flowFinished flowId='flow_9']
            ##teamcity[testRetrySupport enabled='false' flowId='flow_6']
            ##teamcity[blockClosed name='Tests of task :projectA:retryTest' flowId='flow_6']
        """)
    }

    private fun runAndCheckServiceMessages(actualMessages: List<String>, expectedMessages: String) {
        val normalizedActualMessages = normalizeServiceMessages(actualMessages)
        val expectedMessageLines = expectedMessages.trimIndent().lines()
        assertEquals(
            normalizedActualMessages,
            expectedMessageLines,
            "Actual messages:\n${normalizedActualMessages.joinToString("\n")}\n\nExpected messages:\n${expectedMessageLines.joinToString("\n")}"
        )
    }

    private fun normalizeServiceMessages(serviceMessages: List<String>): List<String> {
        val messages = serviceMessages
            .filterNot { shouldIgnoreMessage(it) }
            .toMutableList()
        var flowIndex = 1
        for (messageIndex in messages.indices) {
            val flowId = FLOW_ID_REGEX.find(messages[messageIndex])?.groupValues?.get(1) ?: continue
            if (NORMALIZED_FLOW_ID_REGEX.matches(flowId)) continue

            val normalizedFlowId = "flow_${flowIndex++}"
            for (followingMessageIndex in messageIndex until messages.size) {
                messages[followingMessageIndex] = messages[followingMessageIndex]
                    .replace("flowId='$flowId'", "flowId='$normalizedFlowId'")
                    .replace("parent='$flowId'", "parent='$normalizedFlowId'")
            }
        }
        return messages.map(::normalizeVolatileAttributes)
    }

    private fun normalizeVolatileAttributes(message: String): String {
        return message
            .replace(Regex("duration='\\d+'"), "duration='##Duration##'")
            .replace(Regex("details='java.lang.AssertionError.*?[^|]'"), "details='##Assert_Stacktrace##'")
    }

    private companion object {
        const val TEST_TASK_BLOCK_PATTERN = "##teamcity\\[(block|flow|test)(.*?)(?<!\\|)\\]"
        const val TEST_TASK_BLOCK_AND_RETRY_PATTERN = "##teamcity\\[(block|flow|test|message|testRetrySupport)(.*?)(?<!\\|)\\]"
        val FLOW_ID_REGEX = Regex("flowId='([^']+)'")
        val NORMALIZED_FLOW_ID_REGEX = Regex("flow_\\d+")
    }
}
