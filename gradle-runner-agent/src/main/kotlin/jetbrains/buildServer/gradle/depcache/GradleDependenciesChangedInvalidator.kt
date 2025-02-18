package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRoot
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootDescriptor
import jetbrains.buildServer.agent.cache.depcache.invalidation.DependencyCacheInvalidator
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationMetadata
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationResult
import java.nio.file.Path

class GradleDependenciesChangedInvalidator(
    private val checksumBuilder: GradleDependencyCacheChecksumBuilder
) : DependencyCacheInvalidator {

    private val absoluteCachesPathToChecksum: MutableMap<String, String> = HashMap()

    override fun run(
        invalidationMetadata: InvalidationMetadata,
        cacheRoots: List<CacheRootDescriptor>,
        newCacheRoots: List<CacheRoot>
    ): InvalidationResult {
        val absoluteCachesPathToNewCacheRootId: Map<String, String> = newCacheRoots.associate {
            it.location.toAbsolutePath().toString() to it.id
        }

        val newCacheRootIdToFilesChecksum: Map<String, String> = absoluteCachesPathToChecksum.entries.mapNotNull { entry ->
            absoluteCachesPathToNewCacheRootId[entry.key]?.let { cacheRootId ->
                cacheRootId to entry.value
            }
        }.toMap()

        var newChecksum = GradleDependencyCacheProjectFilesChecksum(newCacheRootIdToFilesChecksum)
        var cachedChecksum = invalidationMetadata.getObjectParameter("gradleProjectFilesChecksum") {
            GradleDependencyCacheProjectFilesChecksum.deserialize(it)
        }

        invalidationMetadata.publishObjectParameter<GradleDependencyCacheProjectFilesChecksum>("gradleProjectFilesChecksum", newChecksum)

        return if (newChecksum == cachedChecksum) InvalidationResult.validated()
        else InvalidationResult.invalidated("Gradle projects' dependencies have changed")
    }

    override fun shouldRunIfCacheInvalidated(): Boolean = true

    fun addChecksumToGradleCachesLocation(gradleCachesPath: Path, checksum: String) {
        val key = gradleCachesPath.toAbsolutePath().toString()
        val existingChecksum = absoluteCachesPathToChecksum[key]

        if (existingChecksum == null) {
            absoluteCachesPathToChecksum[key] = checksum
            return
        }

        absoluteCachesPathToChecksum[key] = checksumBuilder.merge(existingChecksum, checksum)
    }
}