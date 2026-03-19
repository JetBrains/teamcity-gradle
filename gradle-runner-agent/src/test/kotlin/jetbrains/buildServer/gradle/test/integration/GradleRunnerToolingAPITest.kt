package jetbrains.buildServer.gradle.test.integration

import jetbrains.buildServer.gradle.GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES
import jetbrains.buildServer.util.StringUtil
import org.testng.Assert.assertTrue
import org.testng.annotations.Test

class GradleRunnerToolingAPITest : GradleRunnerServiceMessageTest() {

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should execute happy path when doNotPopulateGradleProperties flag is set`(gradleVersion: String) {
        // arrange
        myTeamCityConfigParameters[GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES] = "true"
        val config = GradleRunConfiguration(PROJECT_E_NAME, "clean build", null)
        config.setGradleVersion(gradleVersion)

        // act
        val messages = run(config).allMessages

        // assert
        assertTrue(messages.stream().anyMatch { it.startsWith("BUILD SUCCESSFUL") },
            "Expected: BUILD SUCCESSFUL\nFull log:\n${StringUtil.join("\n", messages)}")
    }
}
