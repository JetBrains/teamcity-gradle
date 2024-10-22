package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.cache.depcache.DependencyCacheProvider
import jetbrains.buildServer.agent.cache.depcache.DependencyCacheSettingsProviderRegistry
import jetbrains.buildServer.agent.cache.depcache.buildFeature.RunnerDependencyCacheSettingsProvider
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootPublishPaths
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootPublisher
import jetbrains.buildServer.gradle.GradleRunnerConstants
import jetbrains.buildServer.util.EventDispatcher

class GradleDependencyCacheSettingsProvider(
    private val eventDispatcher: EventDispatcher<AgentLifeCycleListener>,
    private val cacheSettingsProviderRegistry: DependencyCacheSettingsProviderRegistry,
    private val cacheProvider: DependencyCacheProvider
) : RunnerDependencyCacheSettingsProvider<GradleDependenciesChangedInvalidator>(
    eventDispatcher, cacheSettingsProviderRegistry, cacheProvider,
    GradleRunnerConstants.RUNNER_TYPE,
    GradleRunnerConstants.DISPLAY_NAME,
    GradleDependencyCacheConstants.CACHE_DISPLAY_NAME,
    GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE,
    GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT
) {

    protected override fun createPostBuildInvalidator(): GradleDependenciesChangedInvalidator {
        return GradleDependenciesChangedInvalidator()
    }

    protected override fun createCacheRootPublisher(): CacheRootPublisher {
        // https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:cache_copy
        return CacheRootPublisher({
            CacheRootPublishPaths.includeAndExclude(
                listOf("modules-*/**"),
                listOf("**/*.lock", "**/gc.properties")
            )
        })
    }
}