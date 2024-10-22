package jetbrains.buildServer.gradle.server.depcache

import jetbrains.buildServer.gradle.GradleRunnerConstants.RUNNER_TYPE
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT
import jetbrains.buildServer.server.cache.depcache.buildFeature.RunnerDependencyCacheBuildFeature
import jetbrains.buildServer.web.openapi.PluginDescriptor

class GradleDependencyCacheBuildFeature(
    private val pluginDescriptor: PluginDescriptor,
) : RunnerDependencyCacheBuildFeature(RUNNER_TYPE, FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE, FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT) {

    public override fun getDisplayName(): String {
        return "Gradle Cache"
    }

    public override fun describeParameters(params: Map<String?, String?>): String {
        return "Caches Gradle dependencies to speed up the builds"
    }

    public override fun getEditParametersUrl(): String {
        return pluginDescriptor.getPluginResourcesPath("editGradleCacheBuildFeature.jsp")
    }
}