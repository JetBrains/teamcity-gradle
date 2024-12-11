package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import java.io.File

interface GradleDependencyCacheManager {

    val cache: DependencyCache?
    val cacheEnabled: Boolean

    fun prepareInvalidationDataAsync(workingDirectory: File, depCacheContext: GradleDependencyCacheStepContext)

    fun registerAndRestoreCache(stepId: String, gradleUserHome: File?, depCacheContext: GradleDependencyCacheStepContext?)

    fun updateInvalidationData(depCacheContext: GradleDependencyCacheStepContext?)
}