package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRoot
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootDescriptor
import jetbrains.buildServer.agent.cache.depcache.invalidation.DependencyCacheInvalidator
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationMetadata
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationResult
import java.nio.file.Path

class GradleDependenciesChangedInvalidator : DependencyCacheInvalidator {

    private val absoluteCachesPathToFilePathToChecksum: MutableMap<String, Map<String, String>> = HashMap()

    override fun run(
        invalidationMetadata: InvalidationMetadata,
        cacheRoots: List<CacheRootDescriptor>,
        newCacheRoots: List<CacheRoot>
    ): InvalidationResult {
        val absoluteCachesPathToNewCacheRootId: Map<String, String> = newCacheRoots.associate {
            it.location.toAbsolutePath().toString() to it.id
        }

        val newCacheRootIdToFilesChecksum: Map<String, Map<String, String>> = absoluteCachesPathToFilePathToChecksum.entries.mapNotNull { entry ->
            absoluteCachesPathToNewCacheRootId[entry.key]?.let { cacheRootId ->
                cacheRootId to entry.value
            }
        }.toMap()

        var newData = GradleDependencyCacheInvalidationData(newCacheRootIdToFilesChecksum)
        var cachedData = invalidationMetadata.getObjectParameter("gradleInvalidationData") {
            GradleDependencyCacheInvalidationData.deserialize(it)
        }

        invalidationMetadata.publishObjectParameter<GradleDependencyCacheInvalidationData>("gradleInvalidationData", newData)

        return if (newData == cachedData) InvalidationResult.validated()
        else InvalidationResult.invalidated("Gradle projects' dependencies have changed")
    }

    override fun shouldRunIfCacheInvalidated(): Boolean = true

    fun addDependenciesToGradleCachesLocation(gradleCachesPath: Path, filePathToChecksum: Map<String, String>) {
        absoluteCachesPathToFilePathToChecksum.put(gradleCachesPath.toAbsolutePath().toString(), filePathToChecksum)
    }
}