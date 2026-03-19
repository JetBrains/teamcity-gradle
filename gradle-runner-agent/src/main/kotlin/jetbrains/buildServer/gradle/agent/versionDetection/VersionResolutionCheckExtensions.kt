package jetbrains.buildServer.gradle.agent.versionDetection

import jetbrains.buildServer.gradle.agent.GradleRunnerContext

object VersionResolutionCheckExtensions {
    /*
    This check is used to mimic the behavior that existed in the previous version detection implementation that used the Tooling API connector.
    This behavior is likely suboptimal and can be improved.
     */
    fun GradleRunnerContext.isGradleVersionNotNeeded() =
        // Currently, if the wrapper properties file could not be found, command line execution is used.
        // In this case, we do not need the version information.
        isWrapperPropertiesFileMissing ||
        // In the previous implementation, the Gradle user home could not be resolved for non-wrapper usages in a virtual context.
        // Because of this, the version lookup would fail.
        // We can skip the version resolution to mimic the previous behavior.
        noWrapperInVirtualContext
}
