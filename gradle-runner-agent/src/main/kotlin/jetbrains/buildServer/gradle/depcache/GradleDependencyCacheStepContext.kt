package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootUsage
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.INVALIDATION_DATA_AWAITING_TIMEOUT_DEFAULT_MS
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.INVALIDATION_DATA_AWAITING_TIMEOUT_MS
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.INVALIDATION_DATA_SEARCH_DEPTH_LIMIT
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.INVALIDATION_DATA_SEARCH_DEPTH_LIMIT_DEFAULT
import kotlinx.coroutines.Deferred
import java.nio.file.Path

class GradleDependencyCacheStepContext(private val configParameters: Map<String, String> = emptyMap()) {

    var gradleCachesLocation: Path? = null

    var invalidationData: Deferred<Map<String, String>>? = null

    val invalidationDataAwaitTimeout: Long
        get() = configParameters[INVALIDATION_DATA_AWAITING_TIMEOUT_MS]?.toLongOrNull() ?: INVALIDATION_DATA_AWAITING_TIMEOUT_DEFAULT_MS

    val depthLimit: Int
        get() = configParameters[INVALIDATION_DATA_SEARCH_DEPTH_LIMIT]?.toIntOrNull() ?: INVALIDATION_DATA_SEARCH_DEPTH_LIMIT_DEFAULT

    fun newCacheRootUsage(gradleCachesPath: Path, stepId: String): CacheRootUsage {
        return CacheRootUsage(
            GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
            gradleCachesPath.toAbsolutePath(),
            stepId
        )
    }
}