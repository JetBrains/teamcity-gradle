package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.gradle.GradleRunnerConstants

object GradleDependencyCacheConstants {
    const val CACHE_ROOT_TYPE: String = "gradle-caches"
    const val CACHE_DISPLAY_NAME: String = GradleRunnerConstants.DISPLAY_NAME
    const val FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE: String = "teamcity.internal.depcache.buildFeature.gradle.enabled"
    const val FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT: Boolean = false

    const val PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT: String = "teamcity.internal.depcache.gradle.projectFilesChecksumSearchDepthLimit"
    const val PROJECT_FILES_CHECKSUM_SEARCH_DEPTH_LIMIT_DEFAULT: Int = Integer.MAX_VALUE
    const val PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_MS: String = "teamcity.internal.depcache.gradle.projectFilesChecksumAwaitingTimeoutMs"
    const val PROJECT_FILES_CHECKSUM_AWAITING_TIMEOUT_DEFAULT_MS: Long = 60000
}