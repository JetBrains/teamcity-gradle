package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.cache.depcache.DependencyCacheProvider
import jetbrains.buildServer.agent.cache.depcache.DependencyCacheSettingsProviderRegistry
import jetbrains.buildServer.agent.cache.depcache.buildFeature.BuildRunnerDependencyCacheSettingsProvider
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootPublishPaths
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootPublisher
import jetbrains.buildServer.gradle.GradleRunnerConstants
import jetbrains.buildServer.util.EventDispatcher

class GradleDependencyCacheSettingsProvider(
    private val eventDispatcher: EventDispatcher<AgentLifeCycleListener>,
    private val cacheSettingsProviderRegistry: DependencyCacheSettingsProviderRegistry,
    private val cacheProvider: DependencyCacheProvider
) : BuildRunnerDependencyCacheSettingsProvider<GradleDependenciesChangedInvalidator>(
    eventDispatcher, cacheSettingsProviderRegistry, cacheProvider,
    GradleRunnerConstants.RUNNER_TYPE,
    GradleDependencyCacheConstants.GRADLE_DEP_CACHE_ENABLED,
    GradleDependencyCacheConstants.GRADLE_DEP_CACHE_ENABLED_DEFAULT) {

    protected override fun createPostBuildInvalidator(): GradleDependenciesChangedInvalidator {
        return GradleDependenciesChangedInvalidator()
    }

    protected override fun createCacheRootPublisher(): CacheRootPublisher {
        // https://docs.gradle.org/current/userguide/dependency_resolution.html#sub:cache_copy
        return CacheRootPublisher({
            CacheRootPublishPaths.includeAndExclude(
                listOf("modules-*/**"),
                listOf("**/*.lock", "**/gc.properties"))
        })
    }
}