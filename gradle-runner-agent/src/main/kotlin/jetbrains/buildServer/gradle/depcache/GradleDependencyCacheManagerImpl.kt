package jetbrains.buildServer.gradle.depcache

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import kotlinx.coroutines.*
import java.io.File

class GradleDependencyCacheManagerImpl(
    private val gradleDependencyCacheSettingsProvider: GradleDependencyCacheSettingsProvider,
    private val invalidationDataCollector: GradleDependencyCacheInvalidationDataCollector,
    private val coroutineScope: CoroutineScope
) : GradleDependencyCacheManager {

    override val cache: DependencyCache?
        get() = gradleDependencyCacheSettingsProvider.cache

    override val cacheEnabled: Boolean
        get() = gradleDependencyCacheSettingsProvider.cache != null

    override fun prepareInvalidationDataAsync(workingDirectory: File, depCacheContext: GradleDependencyCacheStepContext) {
        if (!cacheEnabled) return
        val cache = gradleDependencyCacheSettingsProvider.cache
        if (cache == null) {
            // this is not an expected case, something is wrong
            logWarning("Gradle dependency cache is enabled but failed to initialize, couldn't prepare invalidation data")
            return
        }

        val deferred: Deferred<Map<String, String>> = coroutineScope.async {
            withContext(Dispatchers.IO) {
                invalidationDataCollector.collect(workingDirectory, cache, depCacheContext.depthLimit).fold(
                    onSuccess = { it },
                    onFailure = { exception ->
                        logWarning("Error while preparing invalidation data, this execution will not be cached", cache, exception)
                        return@fold emptyMap()
                    }
                )
            }
        }

        depCacheContext.invalidationData = deferred
    }

    override fun registerAndRestoreCache(stepId: String, gradleUserHome: File?, depCacheContext: GradleDependencyCacheStepContext?) {
        if (!cacheEnabled) return

        val cache = gradleDependencyCacheSettingsProvider.cache
        if (cache == null) {
            // this is not an expected case, something is wrong
            logWarning("Gradle dependency cache is enabled but failed to initialize, it will not be used at the current execution")
            return
        }

        if (gradleUserHome == null) {
            logWarning("Failed to detect Gradle User Home location for the current Gradle execution, it will not be cached", cache)
            return
        }

        if (depCacheContext == null) {
            logWarning("Gradle caches context wasn't initialized, current execution will not be cached", cache)
            return
        }

        val gradleCachesLocation = File(gradleUserHome, "caches")
        if (!gradleCachesLocation.exists()) {
            LOG.debug("Gradle caches location doesn't exist, creating it: ${gradleCachesLocation.absolutePath}")
            gradleCachesLocation.mkdirs()
        }

        val gradleCachesPath = gradleCachesLocation.toPath()

        LOG.info("Creating a new cache root usage for Gradle caches location: $gradleCachesPath")

        val cacheRootUsage = depCacheContext.newCacheRootUsage(gradleCachesPath, stepId)
        cache.registerAndRestore(cacheRootUsage)
        depCacheContext.gradleCachesLocation = gradleCachesPath
    }

    override fun updateInvalidationData(depCacheContext: GradleDependencyCacheStepContext?) {
        if (!cacheEnabled) return
        val cache = gradleDependencyCacheSettingsProvider.cache
        val invalidator = gradleDependencyCacheSettingsProvider.postBuildInvalidator
        if (cache == null || invalidator == null) {
            // this is not an expected case, something is wrong
            logWarning("Gradle dependency cache is enabled but failed to initialize, couldn't update invalidation data")
            return
        }
        if (depCacheContext == null || depCacheContext.gradleCachesLocation == null) {
            logWarning("Gradle caches location hasn't been initialized, this execution will not be cached", cache)
            return
        }
        if (depCacheContext.invalidationData == null) {
            logWarning("Gradle caches invalidation data hasn't been prepared, this execution will not be cached", cache)
            return
        }

        val invalidationData: Map<String, String> = runCatching {
            runBlocking {
                withTimeout(depCacheContext.invalidationDataAwaitTimeout) {
                    depCacheContext.invalidationData!!.await()
                }
            }
        }.getOrElse { e ->
            logWarning("An error occurred during getting the invalidation data", cache, e)
            emptyMap()
        }

        if (invalidationData.isEmpty()) {
            logWarning("Invalidation data wasn't collected, something went wrong", cache)
            return
        }

        invalidator.addDependenciesToGradleCachesLocation(depCacheContext.gradleCachesLocation!!, invalidationData)
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

    companion object {
        private val LOG = Logger.getInstance(GradleDependencyCacheManagerImpl::class.java)
    }
}