package jetbrains.buildServer.gradle.agent.versionDetection

import jetbrains.buildServer.agent.runner.CommandExecution
import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import jetbrains.buildServer.gradle.agent.obsolete.GradleConnectorProvider
import jetbrains.buildServer.gradle.agent.obsolete.ObsoleteGradleVersionDetector

class GradleVersionDetector(private val gradleRunnerContext: GradleRunnerContext) {
    @Deprecated("Remove in a few releases after 2026.1 if the new version detection implementation works without problems")
    fun detectGradleVersion(connectorProvider: GradleConnectorProvider): GradleVersion? {
        val version = ObsoleteGradleVersionDetector().detect(connectorProvider.getConnector(), gradleRunnerContext.flowLogger)
        return if (version.isPresent) GradleVersion(version.get().version) else null
    }

    fun detectGradleVersion(
        isUnix: Boolean,
        setDetectedGradleVersionCallback: (GradleVersion?) -> Unit
    ): CommandExecution {
        return VersionDetectionCommandExecution(gradleRunnerContext, isUnix) { exitCode, stdOut, stdErr ->
            if (exitCode != 0) {
                gradleRunnerContext.flowLogger.warning("Could not detect the Gradle version. The Gradle --version call finished with a non-zero exit code.")
                return@VersionDetectionCommandExecution
            }

            val detectedGradleVersion = parseGradleVersion(stdOut.joinToString("\n"))
            if (detectedGradleVersion == null) {
                gradleRunnerContext.flowLogger.warning("Couldn't parse the Gradle version from Gradle's '--version' output")
            } else {
                gradleRunnerContext.flowLogger.debug("Detected Gradle version: '$detectedGradleVersion'")
                setDetectedGradleVersionCallback(detectedGradleVersion)
            }
        }
    }

    private fun parseGradleVersion(output: String): GradleVersion? {
        val matchResult = VERSION_PATTERN.find(output)
        if (matchResult == null) return null

        val versionString = matchResult.groupValues[1]
        return GradleVersion(versionString)
    }

    private companion object {
        val VERSION_PATTERN = """Gradle\s+(\d+\.\d+(?:\.\d+)?(?:-[\w.-]+)?)""".toRegex()
    }
}
