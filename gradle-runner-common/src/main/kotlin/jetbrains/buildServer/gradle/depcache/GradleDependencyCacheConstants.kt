package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.gradle.GradleRunnerConstants

object GradleDependencyCacheConstants {
    const val CACHE_ROOT_TYPE: String = "gradle-caches"
    const val CACHE_DISPLAY_NAME: String = GradleRunnerConstants.DISPLAY_NAME
    const val FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE: String = "teamcity.internal.depcache.buildFeature.gradle.enabled"
    const val FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE_DEFAULT: Boolean = false
}