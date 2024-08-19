package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRoot
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootDescriptor
import jetbrains.buildServer.agent.cache.depcache.invalidation.DependencyCacheInvalidator
import jetbrains.buildServer.agent.cache.depcache.invalidation.Deserializer
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationMetadata
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationResult
import java.nio.file.Path

class GradleDependenciesChangedInvalidator : DependencyCacheInvalidator {

    private val absoluteCachesPathToDependencies: MutableMap<String, Set<String>> = HashMap()

    override fun run(
        invalidationMetadata: InvalidationMetadata,
        cacheRoots: List<CacheRootDescriptor>,
        newCacheRoots: List<CacheRoot>
    ): InvalidationResult {
        val absoluteCachesPathToNewCacheRootId: Map<String, String> = newCacheRoots.associate {
            it.location.toAbsolutePath().toString() to it.id
        }

        val newCacheRootIdToDependencySet: Map<String, Set<String>> = absoluteCachesPathToDependencies.entries.mapNotNull { entry ->
            absoluteCachesPathToNewCacheRootId[entry.key]?.let { cacheRootId ->
                cacheRootId to entry.value
            }
        }.toMap()

        var newDependencies = GradleDependencies(newCacheRootIdToDependencySet)
        var cachedDependencies = invalidationMetadata.getObjectParameter("gradleDependencies") {
            GradleDependencies.deserialize(it)
        }

        invalidationMetadata.publishObjectParameter<GradleDependencies>("gradleDependencies", newDependencies)

        return if (newDependencies == cachedDependencies) InvalidationResult.validated()
        else InvalidationResult.invalidated("Gradle projects' dependencies have changed")
    }

    override fun shouldRunIfCacheInvalidated(): Boolean = true

    fun addDependenciesToGradleCachesLocation(gradleCachesPath: Path, dependencies: Set<String>) {
        absoluteCachesPathToDependencies.put(gradleCachesPath.toAbsolutePath().toString(), dependencies)
    }
}