package jetbrains.buildServer.gradle.server.depcache

import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE
import jetbrains.buildServer.serverSide.BuildStartContext
import jetbrains.buildServer.serverSide.BuildStartContextProcessor
import jetbrains.buildServer.serverSide.TeamCityProperties

class GradleBuildStartContextProcessor : BuildStartContextProcessor {

    override fun updateParameters(context: BuildStartContext) {
        mapGradleDependencyCacheEnabledInternalProperty(context)
    }

    /**
     * Allows enabling/disabling agent-side of the Gradle dependency cache via an internal server property.
     */
    private fun mapGradleDependencyCacheEnabledInternalProperty(context: BuildStartContext) {
        val propertyValue = TeamCityProperties.getPropertyOrNull(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE) ?: return
        val buildType = context.build.buildType ?: return

        if (!buildType.configParameters.containsKey(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE)) {
            context.addSharedParameter(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE, propertyValue)
        }
    }
}