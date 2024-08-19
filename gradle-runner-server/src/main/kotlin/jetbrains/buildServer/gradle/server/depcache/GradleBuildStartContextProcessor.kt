package jetbrains.buildServer.gradle.server.depcache

import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.GRADLE_DEP_CACHE_ENABLED
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
        val propertyValue = TeamCityProperties.getPropertyOrNull(GRADLE_DEP_CACHE_ENABLED) ?: return
        val buildType = context.build.buildType ?: return

        if (!buildType.configParameters.containsKey(GRADLE_DEP_CACHE_ENABLED)) {
            context.addSharedParameter(GRADLE_DEP_CACHE_ENABLED, propertyValue)
        }
    }
}