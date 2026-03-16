package jetbrains.buildServer.gradle.agent

import com.intellij.openapi.util.TCSystemInfo
import jetbrains.buildServer.RunBuildException
import jetbrains.buildServer.agent.AgentRuntimeProperties
import jetbrains.buildServer.agent.BuildRunnerContext
import jetbrains.buildServer.agent.FlowGenerator
import jetbrains.buildServer.agent.FlowLogger
import jetbrains.buildServer.agent.IncrementalBuild
import jetbrains.buildServer.agent.ToolCannotBeFoundException
import jetbrains.buildServer.agent.runner.JavaRunnerUtil
import jetbrains.buildServer.gradle.GradleRunnerConstants.*
import jetbrains.buildServer.log.Loggers
import jetbrains.buildServer.messages.ErrorData
import jetbrains.buildServer.runner.JavaRunnerConstants
import jetbrains.buildServer.serverSide.BuildTypeOptions
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.StringUtil
import java.io.File
import java.io.IOException

class GradleRunnerContext(val buildRunnerContext: BuildRunnerContext) {
    val useWrapper: Boolean
    val gradleHome: File?
    val gradleExecutablePath: String
    val gradleWrapperProperties: File?
    val javaHome: String
    val gradleOptions: String
    val environmentVariables: Map<String, String>
    val isWrapperPropertiesFileMissing: Boolean
    val noWrapperInVirtualContext: Boolean

    val workingDirectory = buildRunnerContext.workingDirectory
    val build = buildRunnerContext.build
    val flowLogger: FlowLogger = build.buildLogger.getFlowLogger(FlowGenerator.generateNewFlow())

    init {
        val relativeGradleExecutablePath: String
        val wrapperName: String

        if (TCSystemInfo.isWindows) {
            relativeGradleExecutablePath = WIN_GRADLE_EXE
            wrapperName = WIN_GRADLEW
        } else if (TCSystemInfo.isUnix) {
            relativeGradleExecutablePath = UNIX_GRADLE_EXE
            wrapperName = UNIX_GRADLEW
        } else {
            throw RuntimeException("OS not supported")
        }

        useWrapper = ConfigurationParamsUtil.isParameterEnabled(buildRunnerContext.runnerParameters, GRADLE_WRAPPER_FLAG)
        gradleHome = resolveGradleHome(useWrapper, buildRunnerContext)
        gradleExecutablePath = resolveGradleExecutablePath(useWrapper, gradleHome, relativeGradleExecutablePath, wrapperName, buildRunnerContext)
        gradleWrapperProperties = resolveGradleWrapperProperties(useWrapper, buildRunnerContext)
        javaHome = resolveJavaHome(buildRunnerContext)
        gradleOptions = resolveGradleOptions(buildRunnerContext)
        environmentVariables = resolveEnvironmentVariables(useWrapper, gradleWrapperProperties, gradleHome, gradleOptions, javaHome, buildRunnerContext).toMap()
        isWrapperPropertiesFileMissing = useWrapper && gradleWrapperProperties?.exists() != true
        noWrapperInVirtualContext = !useWrapper && buildRunnerContext.isVirtualContext
    }

    private fun resolveGradleHome(useWrapper: Boolean, runnerContext: BuildRunnerContext): File? {
        if (useWrapper) return null

        if (runnerContext.isVirtualContext) {
            flowLogger.debug("Step is running in a virtual context, skip detecting GRADLE_HOME")
            return null
        }

        val gradleHome = try {
            val gradlePath = runnerContext.getToolPath(GradleToolProvider.GRADLE_TOOL)
            File(gradlePath)
        } catch (e: ToolCannotBeFoundException) {
            throw RunBuildException(e.message, e, ErrorData.BUILD_RUNNER_ERROR_TYPE).apply { isLogStacktrace = false }
        }

        if (!gradleHome.exists()) {
            throw RunBuildException("Gradle home path ($gradleHome) is invalid.")
        }

        return gradleHome
    }

    private fun resolveGradleExecutablePath(
        useWrapper: Boolean,
        gradleHome: File?,
        relativeGradleExecutablePath: String,
        wrapperName: String,
        runnerContext: BuildRunnerContext
    ): String {
        if (!useWrapper) {
            if (runnerContext.isVirtualContext) {
                flowLogger.debug("Step is running in a virtual context, skip detecting the Gradle executable path")
                return "gradle"
            } else {
                val gradleExecutable = File(gradleHome, relativeGradleExecutablePath)
                if (!gradleExecutable.exists()) {
                    throw RunBuildException("Gradle home path ($gradleHome) does not contain a Gradle installation. Cannot find $relativeGradleExecutablePath.")
                }

                return gradleExecutable.absolutePath
            }
        } else {
            val relativeGradleWPath = ConfigurationParamsUtil.getGradleWPath(runnerContext.runnerParameters)

            val gradleExecutable = File(runnerContext.workingDirectory, "$relativeGradleWPath${File.separator}$wrapperName")
            if (!gradleExecutable.exists()) {
                throw RunBuildException(
                    "Gradle wrapper script $wrapperName can not be found at ${gradleExecutable.absolutePath}\n" +
                            "Please, provide the path to the wrapper script in the build configuration settings."
                )
            }

            return gradleExecutable.absolutePath
        }
    }

    private fun resolveGradleWrapperProperties(useWrapper: Boolean, runnerContext: BuildRunnerContext): File? {
        if (!useWrapper) return null

        val wrapperPropertiesPath = ConfigurationParamsUtil.getGradleWrapperPropertiesPath(runnerContext.configParameters)
        val gradleWrapperProperties = if (wrapperPropertiesPath.isNotEmpty()) {
            File(runnerContext.workingDirectory, "$wrapperPropertiesPath${File.separator}$GRADLE_WRAPPER_PROPERTIES_FILENAME")
        } else {
            val relativeGradleWPath = ConfigurationParamsUtil.getGradleWPath(runnerContext.runnerParameters)
            File(runnerContext.workingDirectory, "$relativeGradleWPath${File.separator}$GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION")
        }

        if (!gradleWrapperProperties.exists()) {
            flowLogger.warning("gradle-wrapper.properties couldn't be found at ${gradleWrapperProperties.absolutePath}")
        }

        return gradleWrapperProperties
    }

    private fun resolveJavaHome(runnerContext: BuildRunnerContext): String {
        val javaHome = JavaRunnerUtil.findJavaHome(
            runnerContext.runnerParameters[JavaRunnerConstants.TARGET_JDK_HOME],
            runnerContext.buildParameters.allParameters,
            AgentRuntimeProperties.getCheckoutDir(runnerContext.runnerParameters)
        ) ?: throw RunBuildException("Unable to find Java home")
        return FileUtil.getCanonicalFile(File(javaHome)).path
    }

    private fun resolveGradleOptions(runnerContext: BuildRunnerContext): String {
        val runnerGradleOpts = runnerContext.runnerParameters[ENV_GRADLE_OPTS]
        val runnerJavaArguments = ConfigurationParamsUtil.getJavaArgs(runnerContext.runnerParameters)

        return when {
            StringUtil.isNotEmpty(runnerJavaArguments) -> runnerJavaArguments
            StringUtil.isNotEmpty(runnerGradleOpts) -> runnerGradleOpts!!
            else -> runnerContext.buildParameters.environmentVariables.getOrDefault(ENV_GRADLE_OPTS, StringUtil.EMPTY)
        }
    }

    private fun resolveEnvironmentVariables(
        useWrapper: Boolean,
        gradleWrapperProperties: File?,
        gradleHome: File?,
        gradleOptions: String,
        javaHome: String,
        runnerContext: BuildRunnerContext
    ): MutableMap<String, String> {
        val envVars = HashMap(runnerContext.buildParameters.environmentVariables)
        if (gradleHome != null) envVars[GRADLE_HOME_ENV_KEY] = gradleHome.absolutePath
        envVars["GRADLE_EXIT_CONSOLE"] = "true"

        if (!runnerContext.isVirtualContext) {
            envVars[JavaRunnerConstants.JAVA_HOME] = javaHome
        }

        envVars[ENV_GRADLE_OPTS] = appendTmpDirToGradleOptions(gradleOptions, runnerContext.build.buildTempDirectory)
        envVars[ENV_INCREMENTAL_PARAM] = getIncrementalMode(runnerContext)
        envVars[ENV_SUPPORT_TEST_RETRY] = runnerContext.build.getBuildTypeOptionValue(BuildTypeOptions.BT_SUPPORT_TEST_RETRY).toString()

        val systemProps = runnerContext.buildParameters.systemProperties
        val parallelTestsParam = systemProps.getOrDefault("teamcity.build.parallelTests.excludesFile", "")
        val riskTestsParam = systemProps.getOrDefault("teamcity.build.testPrioritization.riskTests.excludesFile", "")
        if (parallelTestsParam.isNotEmpty() && riskTestsParam.isNotEmpty()) {
            flowLogger.warning("Both filter parameters for parallel tests and risk tests are present")
        }
        envVars[TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH] = parallelTestsParam
        envVars[TEAMCITY_RISK_TESTS_ARTIFACT_PATH] = riskTestsParam

        if (gradleWrapperProperties != null) envVars[GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY] = gradleWrapperProperties.absolutePath

        envVars[WORKING_DIRECTORY_ENV_KEY] = runnerContext.workingDirectory.absolutePath
        envVars[USE_WRAPPER_ENV_KEY] = useWrapper.toString()

        return envVars
    }

    private fun appendTmpDirToGradleOptions(gradleOptions: String, tempDir: File): String {
        return try {
            "$gradleOptions \"-Djava.io.tmpdir=${tempDir.canonicalPath}\""
        } catch (e: IOException) {
            Loggers.AGENT.warnAndDebugDetails("Failed patch temp dir for Gradle runtime environment: $e", e)
            gradleOptions
        }
    }

    private fun getIncrementalMode(runnerContext: BuildRunnerContext): String {
        val incrementalOptionEnabled = runnerContext.runnerParameters[IS_INCREMENTAL].toBoolean()
        if (!incrementalOptionEnabled) return false.toString()

        val internalFullBuildOverride = !IncrementalBuild.isEnabled()
        if (internalFullBuildOverride) return ENV_INCREMENTAL_VALUE_SKIP

        return ENV_INCREMENTAL_VALUE_PROCEED
    }

    companion object {
        const val WIN_GRADLE_EXE: String = "bin/gradle.bat"
        const val WIN_GRADLEW: String = "gradlew.bat"
        const val UNIX_GRADLE_EXE: String = "bin/gradle"
        const val UNIX_GRADLEW: String = "gradlew"
    }
}
