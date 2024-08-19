package jetbrains.buildServer.gradle.depcache

import org.gradle.tooling.GradleConnector
import java.io.File

interface GradleDependencyCacheManager {

    fun prepareAndRestoreCache(
        projectConnector: GradleConnector?,
        stepId: String,
        gradleUserHome: File?,
        buildTempDirectory: File
    )
}