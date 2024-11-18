package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import org.gradle.tooling.GradleConnector
import java.io.File

interface GradleDependencyCacheManager {

    val cache: DependencyCache?
    val cacheEnabled: Boolean

    fun prepareAndRestoreCache(
        projectConnector: GradleConnector?,
        stepId: String,
        gradleUserHome: File?,
        buildTempDirectory: File
    )
}