package jetbrains.buildServer.gradle.depcache

object GradleDependencyCacheConstants {
    const val CACHE_ROOT_TYPE: String = "gradle-caches"
    const val GRADLE_DEP_CACHE_ENABLED: String = "teamcity.internal.depcache.buildFeature.gradle.enabled"
    const val GRADLE_DEP_CACHE_ENABLED_DEFAULT: Boolean = false
}