package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootUsage
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_DEFAULT_MS
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_MS
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT_DEFAULT
import kotlinx.coroutines.Deferred
import java.nio.file.Path

class GradleDependencyCacheStepContext(private val configParameters: Map<String, String> = emptyMap()) {

    var gradleCachesLocation: Path? = null

    var projectFilesChecksum: Deferred<String>? = null

    val projectFilesChecksumAwaitTimeout: Long
        get() = configParameters[PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_MS]?.toLongOrNull() ?: PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_DEFAULT_MS

    val depthLimit: Int
        get() = configParameters[PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT]?.toIntOrNull() ?: PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT_DEFAULT

    fun newCacheRootUsage(gradleCachesPath: Path, stepId: String): CacheRootUsage {
        return CacheRootUsage(
            GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
            gradleCachesPath.toAbsolutePath(),
            stepId
        )
    }
}