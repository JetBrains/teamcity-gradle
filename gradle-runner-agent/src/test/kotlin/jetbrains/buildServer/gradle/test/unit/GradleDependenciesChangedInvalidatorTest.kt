package jetbrains.buildServer.gradle.test.unit

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRoot
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootDescriptor
import jetbrains.buildServer.agent.cache.depcache.invalidation.Deserializer
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationMetadata
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationResult
import jetbrains.buildServer.agent.cache.depcache.invalidation.Serializable
import jetbrains.buildServer.gradle.depcache.GradleDependencies
import jetbrains.buildServer.gradle.depcache.GradleDependenciesChangedInvalidator
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

@Test
class GradleDependenciesChangedInvalidatorTest {

    private lateinit var instance: GradleDependenciesChangedInvalidator
    @MockK private lateinit var invalidationMetadataMock: InvalidationMetadata

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        instance = GradleDependenciesChangedInvalidator()
    }

    @DataProvider
    fun getDependenciesNotChangedTestData(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            arrayOf<Any>(
                """
                    {
                      "cacheRootDependencies": {
                        "cache-root-1": [
                          "com.google.code.gson:gson:2.9.0",
                          "commons-cli:commons-cli:1.5.0",
                          "commons-io:commons-io:2.11.0"
                        ],
                        "cache-root-2": [
                          "org.junit.jupiter:junit-jupiter-api:5.9.1",
                          "org.slf4j:slf4j-api:1.7.36"
                        ]
                      }
                    }
                """.trimIndent(),
                Arrays.asList<CacheRoot>(
                    CacheRoot(
                        "cache-root-1",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-1/caches"),
                        emptySet<String>()
                    ),
                    CacheRoot(
                        "cache-root-2",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-2/caches"),
                        emptySet<String>()
                    )
                ),
                mapOf(
                    Paths.get("/gradle-user-home-location-1/caches") to setOf(
                        "com.google.code.gson:gson:2.9.0",
                        "commons-cli:commons-cli:1.5.0",
                        "commons-io:commons-io:2.11.0"),
                    Paths.get("/gradle-user-home-location-2/caches") to setOf(
                        "org.junit.jupiter:junit-jupiter-api:5.9.1",
                        "org.slf4j:slf4j-api:1.7.36")
                )
            )
        )
    }

    @Test(dataProvider = "getDependenciesNotChangedTestData")
    fun `should not invalidate cache when dependency sets not changed`(
        cachedDependenciesJson: String,
        newCacheRoots: List<CacheRoot>,
        repoPathToDependencies: Map<Path, Set<String>>
    ) {
        // arrange
        val parameterName = "gradleDependencies"
        val cachedGradleDependencies: GradleDependencies = prepareGradleDependenciesMetadata(cachedDependenciesJson)
        every { invalidationMetadataMock.getObjectParameter(any(), any<Deserializer<Serializable>>()) } returns cachedGradleDependencies
        val serializableArgumentSlot = slot<Serializable>()

        // act
        repoPathToDependencies.forEach {
            instance.addDependenciesToGradleCachesLocation(it.key, it.value)
        }
        val invalidationResult: InvalidationResult = instance.run(invalidationMetadataMock, emptyList<CacheRootDescriptor>(), newCacheRoots)

        // assert
        Assert.assertFalse(invalidationResult.isInvalidated)
        Assert.assertNull(invalidationResult.invalidationReason)
        verify { invalidationMetadataMock.getObjectParameter(parameterName, any<Deserializer<Serializable>>()) }
        verify { invalidationMetadataMock.publishObjectParameter(parameterName, capture(serializableArgumentSlot)) }
        val capturedGradleDependenciesToPublish: GradleDependencies = serializableArgumentSlot.captured as GradleDependencies
        Assert.assertEquals(capturedGradleDependenciesToPublish, cachedGradleDependencies) // we publish the same dependency sets
    }

    @DataProvider
    fun getDependenciesChangedTestData(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            // set 1: dependency version changed
            arrayOf<Any>(
                """
                    {
                      "cacheRootDependencies": {
                        "cache-root-1": [
                          "com.google.code.gson:gson:2.9.0",
                          "commons-cli:commons-cli:1.5.0",
                          "commons-io:commons-io:2.11.0"
                        ],
                        "cache-root-2": [
                          "org.junit.jupiter:junit-jupiter-api:5.9.1",
                          "org.slf4j:slf4j-api:1.1.111" // <-- old version
                        ]
                      }
                    }
                """.trimIndent(),
                Arrays.asList<CacheRoot>(
                    CacheRoot(
                        "cache-root-1",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-1/caches"),
                        emptySet<String>()
                    ),
                    CacheRoot(
                        "cache-root-2",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-2/caches"),
                        emptySet<String>()
                    )
                ),
                mapOf(
                    Paths.get("/gradle-user-home-location-1/caches") to setOf(
                        "com.google.code.gson:gson:2.9.0",
                        "commons-cli:commons-cli:1.5.0",
                        "commons-io:commons-io:2.11.0"),
                    Paths.get("/gradle-user-home-location-2/caches") to setOf(
                        "org.junit.jupiter:junit-jupiter-api:5.9.1",
                        "org.slf4j:slf4j-api:1.7.36"// <-- new version
                    )
                )
            ),

            // set 2: a new dependency added
            arrayOf<Any>(
                """
                    {
                      "cacheRootDependencies": {
                        "cache-root-1": [
                          "com.google.code.gson:gson:2.9.0",
                          "commons-cli:commons-cli:1.5.0",
                          "commons-io:commons-io:2.11.0"
                        ],
                        "cache-root-2": [
                          "org.junit.jupiter:junit-jupiter-api:5.9.1",
                          "org.slf4j:slf4j-api:1.7.36"
                        ]
                      }
                    }
                """.trimIndent(),
                Arrays.asList<CacheRoot>(
                    CacheRoot(
                        "cache-root-1",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-1/caches"),
                        emptySet<String>()
                    ),
                    CacheRoot(
                        "cache-root-2",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-2/caches"),
                        emptySet<String>()
                    )
                ),
                mapOf(
                    Paths.get("/gradle-user-home-location-1/caches") to setOf(
                        "com.google.code.gson:gson:2.9.0",
                        "commons-cli:commons-cli:1.5.0",
                        "commons-io:commons-io:2.11.0"),
                    Paths.get("/gradle-user-home-location-2/caches") to setOf(
                        "org.junit.jupiter:junit-jupiter-api:5.9.1",
                        "org.slf4j:slf4j-api:1.7.36",
                        "org.apache.hadoop.thirdparty:hadoop-shaded-guava:1.2.0"// <-- the new dependency
                    )
                )
            ),

            // set 3: a dependency deleted
            arrayOf<Any>(
                """
                    {
                      "cacheRootDependencies": {
                        "cache-root-1": [
                          "com.google.code.gson:gson:2.9.0",
                          "commons-cli:commons-cli:1.5.0",
                          "commons-io:commons-io:2.11.0"
                        ],
                        "cache-root-2": [
                          "org.junit.jupiter:junit-jupiter-api:5.9.1",
                          "org.slf4j:slf4j-api:1.7.36",
                          "org.apache.hadoop.thirdparty:hadoop-shaded-guava:1.2.0"// <-- the deleted dependency
                        ]
                      }
                    }
                """.trimIndent(),
                Arrays.asList<CacheRoot>(
                    CacheRoot(
                        "cache-root-1",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-1/caches"),
                        emptySet<String>()
                    ),
                    CacheRoot(
                        "cache-root-2",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-2/caches"),
                        emptySet<String>()
                    )
                ),
                mapOf(
                    Paths.get("/gradle-user-home-location-1/caches") to setOf(
                        "com.google.code.gson:gson:2.9.0",
                        "commons-cli:commons-cli:1.5.0",
                        "commons-io:commons-io:2.11.0"),
                    Paths.get("/gradle-user-home-location-2/caches") to setOf(
                        "org.junit.jupiter:junit-jupiter-api:5.9.1",
                        "org.slf4j:slf4j-api:1.7.36"
                    )
                )
            ),

            // set 4: a dependency moved from one cache root to another
            arrayOf<Any>(
                """
                    {
                      "cacheRootDependencies": {
                        "cache-root-1": [
                          "com.google.code.gson:gson:2.9.0",
                          "commons-cli:commons-cli:1.5.0",
                          "commons-io:commons-io:2.11.0"
                        ],
                        "cache-root-2": [
                          "org.junit.jupiter:junit-jupiter-api:5.9.1",
                          "org.slf4j:slf4j-api:1.7.36"// <-- it was here, in the second cache root
                        ]
                      }
                    }
                """.trimIndent(),
                Arrays.asList<CacheRoot>(
                    CacheRoot(
                        "cache-root-1",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-1/caches"),
                        emptySet<String>()
                    ),
                    CacheRoot(
                        "cache-root-2",
                        GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
                        Paths.get("/gradle-user-home-location-2/caches"),
                        emptySet<String>()
                    )
                ),
                mapOf(
                    Paths.get("/gradle-user-home-location-1/caches") to setOf(
                        "com.google.code.gson:gson:2.9.0",
                        "commons-cli:commons-cli:1.5.0",
                        "commons-io:commons-io:2.11.0",
                        "org.slf4j:slf4j-api:1.7.36"// <-- now it's here, in the first cache root
                    ),
                    Paths.get("/gradle-user-home-location-2/caches") to setOf(
                        "org.junit.jupiter:junit-jupiter-api:5.9.1"
                    )
                )
            ),
        )
    }

    @Test(dataProvider = "getDependenciesChangedTestData")
    fun `should invalidate cache when local repositories dependency sets changed`(
        cachedDependenciesJson: String,
        newCacheRoots: List<CacheRoot>,
        repoPathToDependencies: Map<Path, Set<String>>
    ) {
        // arrange
        val parameterName = "gradleDependencies"
        var cachedGradleDependencies: GradleDependencies = prepareGradleDependenciesMetadata(cachedDependenciesJson)
        every { invalidationMetadataMock.getObjectParameter(any(), any<Deserializer<Serializable>>()) } returns cachedGradleDependencies
        val serializableArgumentSlot = slot<Serializable>()

        // act
        repoPathToDependencies.forEach {
            instance.addDependenciesToGradleCachesLocation(it.key, it.value)
        }
        var invalidationResult: InvalidationResult = instance.run(invalidationMetadataMock, emptyList<CacheRootDescriptor>(), newCacheRoots)

        // assert
        Assert.assertTrue(invalidationResult.isInvalidated)
        Assert.assertNotNull(invalidationResult.invalidationReason)
        verify { invalidationMetadataMock.getObjectParameter(parameterName, any<Deserializer<Serializable>>()) }
        verify { invalidationMetadataMock.publishObjectParameter(parameterName, capture(serializableArgumentSlot)) }
        var capturedGradleDependenciesToPublish: GradleDependencies? = serializableArgumentSlot.captured as GradleDependencies?
        Assert.assertNotEquals(capturedGradleDependenciesToPublish, cachedGradleDependencies) // we publish changed dependency sets
    }

    private fun prepareGradleDependenciesMetadata(json: String): GradleDependencies {
        return GradleDependencies.deserialize(json.toByteArray(StandardCharsets.UTF_8))
    }
}