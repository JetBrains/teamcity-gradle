package jetbrains.buildServer.gradle.depcache

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import kotlinx.coroutines.*
import java.io.File

class GradleDependencyCacheManagerImpl(
    private val gradleDependencyCacheSettingsProvider: GradleDependencyCacheSettingsProvider,
    private val checksumBuilder: GradleDependencyCacheChecksumBuilder,
    private val coroutineScope: CoroutineScope
) : GradleDependencyCacheManager {

    override val cache: DependencyCache?
        get() = gradleDependencyCacheSettingsProvider.cache

    override val cacheEnabled: Boolean
        get() = gradleDependencyCacheSettingsProvider.cache != null

    override fun prepareChecksumAsync(workingDirectory: File, depCacheContext: GradleDependencyCacheStepContext) {
        if (!cacheEnabled) return
        val cache = gradleDependencyCacheSettingsProvider.cache
        if (cache == null) {
            // this is not an expected case, something is wrong
            logWarning("Gradle dependency cache is enabled but failed to initialize, couldn't prepare a checksum")
            return
        }

        val deferred: Deferred<String> = coroutineScope.async {
            withContext(Dispatchers.IO) {
                checksumBuilder.build(workingDirectory, cache, depCacheContext.depthLimit).fold(
                    onSuccess = { it },
                    onFailure = { exception ->
                        logWarning("Error while preparing a checksum, this execution will not be cached", cache, exception)
                        return@fold ""
                    }
                )
            }
        }

        depCacheContext.projectFilesChecksum = deferred
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

    override fun updateInvalidatorWithChecksum(depCacheContext: GradleDependencyCacheStepContext?) {
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
        if (depCacheContext.projectFilesChecksum == null) {
            logWarning("Checksum hasn't been built, this execution will not be cached", cache)
            return
        }

        val projectFilesChecksum: String = runCatching {
            runBlocking {
                withTimeout(depCacheContext.projectFilesChecksumAwaitTimeout) {
                    depCacheContext.projectFilesChecksum!!.await()
                }
            }
        }.getOrElse { e ->
            logWarning("An error occurred during getting the project files checksum", cache, e)
            ""
        }

        if (projectFilesChecksum.isEmpty()) {
            logWarning("Checksum wasn't built, something went wrong", cache)
            return
        }

        invalidator.addChecksumToGradleCachesLocation(depCacheContext.gradleCachesLocation!!, projectFilesChecksum)
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