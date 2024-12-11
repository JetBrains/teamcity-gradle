package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.AgentRunningBuild
import jetbrains.buildServer.agent.BuildFinishedStatus
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
) : BuildRunnerDependencyCacheSettingsProvider(
    eventDispatcher, cacheSettingsProviderRegistry, cacheProvider,
    GradleRunnerConstants.RUNNER_TYPE,
    GradleRunnerConstants.DISPLAY_NAME,
    GradleDependencyCacheConstants.CACHE_DISPLAY_NAME,
    GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE,
    GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT
) {
    var postBuildInvalidator: GradleDependenciesChangedInvalidator? = null
        private set

    protected override fun createPostBuildInvalidators(): List<GradleDependenciesChangedInvalidator> {
        postBuildInvalidator = GradleDependenciesChangedInvalidator()
        return listOf(postBuildInvalidator!!)
    }

    protected override fun createCacheRootPublisher(): CacheRootPublisher {
        // https://docs.gradle.org/current/userguide/dependency_caching.html#sec:cache-copy
        return CacheRootPublisher({
            CacheRootPublishPaths.includeAndExclude(
                listOf("modules-*/**"),
                listOf("**/*.lock", "**/gc.properties")
            )
        })
    }

    public override fun buildFinished(build: AgentRunningBuild, buildStatus: BuildFinishedStatus) {
        postBuildInvalidator = null
    }
}