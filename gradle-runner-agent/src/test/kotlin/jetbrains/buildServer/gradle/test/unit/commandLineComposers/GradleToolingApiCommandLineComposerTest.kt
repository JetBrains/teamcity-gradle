package jetbrains.buildServer.gradle.test.unit.commandLineComposers

import io.mockk.every
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildParametersMap
import jetbrains.buildServer.agent.BuildProgressLogger
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.gradle.GradleRunnerConstants
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerParameters
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleToolingApiCommandLineComposer
import jetbrains.buildServer.gradle.agent.propertySplit.InitScriptParametersConstants
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer
import org.assertj.core.api.BDDAssertions
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.nio.file.Files
import java.nio.file.Path

@Test
class GradleToolingApiCommandLineComposerTest {

    private lateinit var tempDir: Path
    private lateinit var composer: GradleToolingApiCommandLineComposer

    @BeforeMethod
    fun setUp() {
        tempDir = Files.createTempDirectory("gradleComposerTest")
        val tasksComposer = GradleTasksComposer(emptyList())
        composer = GradleToolingApiCommandLineComposer(emptyList(), tasksComposer)
    }

    @AfterMethod
    fun tearDown() {
        tempDir.toFile().deleteRecursively()
    }

    @Test
    fun `should populate system properties with explicit values when doNotPopulateGradleProperties flag is set`() {
        // arrange
        val systemProperties = mutableMapOf(
            InitScriptParametersConstants.TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY to "-Xmx512m",
            InitScriptParametersConstants.TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY to "/tmp/stacktrace",
            InitScriptParametersConstants.TEAMCITY_BUILD_CHANGED_FILES_KEY to "/tmp/changedFiles.txt",
            InitScriptParametersConstants.TEAMCITY_BUILD_TEMP_DIR_KEY to "/tmp/buildTemp"
        )
        val configParameters = mutableMapOf(
            GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES to "true",
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY to "false",
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY to "customFormat",
            InitScriptParametersConstants.TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY to "customIgnore"
        )
        val parameters = buildParameters(systemProperties, configParameters)

        // act
        val cmdLine = composer.compose(parameters)

        // assert
        BDDAssertions.then(cmdLine.arguments).contains(
            "-D${GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES}=true",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY}=-Xmx512m",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY}=/tmp/stacktrace",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_CHANGED_FILES_KEY}=/tmp/changedFiles.txt",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_TEMP_DIR_KEY}=/tmp/buildTemp",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY}=false",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY}=customFormat",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY}=customIgnore"
        )
    }

    @Test
    fun `should populate system properties with default values when doNotPopulateGradleProperties flag is set and no explicit values are configured`() {
        // arrange: only the flag is set, no source parameters
        val configParameters = mutableMapOf(
            GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES to "true"
        )
        val parameters = buildParameters(configParameters = configParameters)

        // act
        val cmdLine = composer.compose(parameters)

        // assert: defaults are used — empty strings for 6 properties, "true" for useTestRetryPlugin
        BDDAssertions.then(cmdLine.arguments).contains(
            "-D${GradleRunnerConstants.GRADLE_RUNNER_DO_NOT_POPULATE_GRADLE_PROPERTIES}=true",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY}=",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY}=",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_CHANGED_FILES_KEY}=",
            "-D${InitScriptParametersConstants.TEAMCITY_BUILD_TEMP_DIR_KEY}=",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY}=true",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY}=",
            "-D${InitScriptParametersConstants.TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY}="
        )
    }

    private fun buildParameters(
        systemProperties: Map<String, String> = emptyMap(),
        configParameters: MutableMap<String, String> = mutableMapOf()
    ): GradleCommandLineComposerParameters {
        val buildParametersMap = mockk<BuildParametersMap> {
            every { getSystemProperties() } returns systemProperties
        }
        val runnerContext = mockk<BuildRunnerContext> {
            every { isVirtualContext } returns false
            every { getBuildParameters() } returns buildParametersMap
        }
        val logger = mockk<BuildProgressLogger>(relaxed = true)

        val javaHome = System.getProperty("java.home")

        return GradleCommandLineComposerParameters.builder()
            .withEnv(mutableMapOf())
            .withBuildTempDir(tempDir)
            .withRunnerParameters(emptyMap())
            .withPluginsDir(tempDir)
            .withGradleOpts("")
            .withGradleTasks(emptyList())
            .withGradleUserDefinedParams(emptyList())
            .withConfigurationCacheEnabled(false)
            .withConfigParameters(configParameters)
            .withLogger(logger)
            .withRunnerContext(runnerContext)
            .withJavaHome(javaHome)
            .withCheckoutDir(tempDir)
            .withWorkingDir(tempDir)
            .withInitialGradleParams(emptyList())
            .withExePath("")
            .build()
    }
}
