package jetbrains.buildServer.gradle.agent.obsolete

import jetbrains.buildServer.gradle.agent.GradleRunnerContext

@Deprecated("Remove in a few releases after 2026.1 if the new version / user home detection implementations work without problems")
object GradleConnectorFeatureFlags {
    @Deprecated("Remove in a few releases after 2026.1 if the new version detection implementation works without problems")
    fun GradleRunnerContext.shouldUseObsoleteVersionDetection(): Boolean =
        buildRunnerContext.configParameters["teamcity.internal.gradle.runner.useProjectConnectorVersionDetection"]?.toBoolean() ?: false
}
