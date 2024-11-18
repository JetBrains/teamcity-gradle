package jetbrains.buildServer.gradle.depcache

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootUsage
import org.gradle.tooling.GradleConnector
import java.io.File
import java.nio.file.Path

class GradleDependencyCacheManagerImpl(
    private val gradleDependencyCacheSettingsProvider: GradleDependencyCacheSettingsProvider,
    private val gradleProjectDependenciesCollector: GradleProjectDependenciesCollector,
) : GradleDependencyCacheManager {

    override val cache: DependencyCache?
        get() = gradleDependencyCacheSettingsProvider.cache

    override val cacheEnabled: Boolean
        get() = gradleDependencyCacheSettingsProvider.cache != null

    override fun prepareAndRestoreCache(
        projectConnector: GradleConnector?,
        stepId: String,
        gradleUserHome: File?,
        buildTempDirectory: File
    ) {
        projectConnector ?: run {
            logWarning("Gradle Connector is not set. The cache will not be created")
            return
        }

        if (!cacheEnabled) return

        val cache = gradleDependencyCacheSettingsProvider.cache
        val invalidator = gradleDependencyCacheSettingsProvider.postBuildInvalidator
        if (cache == null || invalidator == null) {
            // this is not an expected case, something is wrong
            logWarning("Gradle dependency cache is enabled but failed to initialize, it will not be used at the current execution")
            return
        }

        if (gradleUserHome == null) {
            logWarning("Failed to detect Gradle User Home location for the current Gradle execution, it will not be cached", cache)
            return
        }

        val gradleCachesLocation = File(gradleUserHome, "caches")
        if (!gradleCachesLocation.exists()) {
            LOG.debug("Gradle caches location doesn't exist, creating it: ${gradleCachesLocation.absolutePath}")
            gradleCachesLocation.mkdirs()
        }

        val gradleCachesPath = gradleCachesLocation.toPath()
        val deps: Set<String> = gradleProjectDependenciesCollector.collectProjectDependencies(buildTempDirectory, projectConnector).fold(
            onSuccess = { it },
            onFailure = { exception ->
                logWarning("Failed to collect Gradle project's dependencies, this execution will not be cached", cache, exception)
                return
            }
        )

        LOG.info("Creating a new cache root usage for Gradle caches location: $gradleCachesPath")

        invalidator.addDependenciesToGradleCachesLocation(gradleCachesPath, deps)
        val cacheRootUsage = newCacheRootUsage(gradleCachesPath, stepId)
        cache.registerAndRestore(cacheRootUsage)
    }

    private fun logWarning(message: String,
                           cache: DependencyCache? = null,
                           exception: Throwable? = null
    ) {
        exception?.let {
            LOG.warn(message, exception)
        } ?: run {
            LOG.warn(message)
        }
        cache?.logWarning(message)
    }

    private fun newCacheRootUsage(gradleCachesPath: Path,
                                  stepId: String): CacheRootUsage {
        return CacheRootUsage(
            GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
            gradleCachesPath.toAbsolutePath(),
            stepId
        )
    }

    companion object {
        private val LOG = Logger.getInstance(GradleDependencyCacheManagerImpl::class.java)
    }
}