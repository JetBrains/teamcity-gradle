package jetbrains.buildServer.gradle.server.depcache

import jetbrains.buildServer.gradle.GradleRunnerConstants
import jetbrains.buildServer.server.cache.depcache.buildFeature.DependencyCacheBuildFeatureRunnersRegistry
import jetbrains.buildServer.server.cache.depcache.buildFeature.DependencyCacheBuildFeatureSupportedRunner

/**
 * Registers Gradle runner in the registry of runners supporting Dependency Cache.
 */
class GradleDependencyCacheRunnerRegistrator(private val runnersRegistry: DependencyCacheBuildFeatureRunnersRegistry) {
    private val myRunner = DependencyCacheBuildFeatureSupportedRunner(GradleRunnerConstants.RUNNER_TYPE, GradleRunnerConstants.DISPLAY_NAME)

    fun register() {
        runnersRegistry.register(myRunner)
    }

    fun unregister() {
        runnersRegistry.unregister(myRunner)
    }
}
