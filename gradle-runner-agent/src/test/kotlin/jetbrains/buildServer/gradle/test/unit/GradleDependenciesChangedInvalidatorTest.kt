package jetbrains.buildServer.gradle.test.unit

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRoot
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootDescriptor
import jetbrains.buildServer.agent.cache.depcache.invalidation.Deserializer
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationMetadata
import jetbrains.buildServer.agent.cache.depcache.invalidation.InvalidationResult
import jetbrains.buildServer.agent.cache.depcache.invalidation.Serializable
import jetbrains.buildServer.gradle.depcache.GradleDependenciesChangedInvalidator
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheInvalidationData
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
    fun getInvalidationDataNotChangedTestData(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            arrayOf<Any>(
                """
                    {
                      "absoluteCachesPathToFilePathToChecksum": {
                        "cache-root-1": {
                          "/build.gradle": "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
                          "/settings.gradle": "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                        },
                        "cache-root-2": {
                          "/gradle/libs.versions.toml": "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                        }
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
                    Paths.get("/gradle-user-home-location-1/caches") to mapOf(
                        "/build.gradle" to "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
                        "/settings.gradle" to "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                    ),
                    Paths.get("/gradle-user-home-location-2/caches") to mapOf(
                        "/gradle/libs.versions.toml" to "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                    )
                )
            )
        )
    }

    @Test(dataProvider = "getInvalidationDataNotChangedTestData")
    fun `should not invalidate cache when invalidation data not changed`(
        cachedDependenciesJson: String,
        newCacheRoots: List<CacheRoot>,
        repoPathToDependencies: Map<Path, Map<String, String>>
    ) {
        // arrange
        val parameterName = "gradleInvalidationData"
        val cachedGradleDependencies: GradleDependencyCacheInvalidationData = prepareGradleDependenciesMetadata(cachedDependenciesJson)
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
        val capturedGradleDependenciesToPublish: GradleDependencyCacheInvalidationData = serializableArgumentSlot.captured as GradleDependencyCacheInvalidationData
        Assert.assertEquals(capturedGradleDependenciesToPublish, cachedGradleDependencies) // we publish the same dependency sets
    }

    @DataProvider
    fun getInvalidationDataChangedTestData(): Array<Array<Any>> {
        return arrayOf<Array<Any>>(
            // set 1: checksum changed
            arrayOf<Any>(
                """
                    {
                      "absoluteCachesPathToFilePathToChecksum": {
                        "cache-root-1": {
                          "/build.gradle": "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
                          "/settings.gradle": "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                        },
                        "cache-root-2": {
                          "/gradle/libs.versions.toml": "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                        }
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
                    Paths.get("/gradle-user-home-location-1/caches") to mapOf(
                        "/build.gradle" to "a2c9f2dafa9e40885d7109e3e5547fa602306d71f870e0d3e6245b99cccb432f", // <-- new checksum
                        "/settings.gradle" to "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                    ),
                    Paths.get("/gradle-user-home-location-2/caches") to mapOf(
                        "/gradle/libs.versions.toml" to "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                    )
                )
            ),

            // set 2: a new file added
            arrayOf<Any>(
                """
                    {
                      "absoluteCachesPathToFilePathToChecksum": {
                        "cache-root-1": {
                          "/build.gradle": "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
                          "/settings.gradle": "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                        },
                        "cache-root-2": {
                          "/gradle/libs.versions.toml": "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                        }
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
                    Paths.get("/gradle-user-home-location-1/caches") to mapOf(
                        "/build.gradle" to "a2c9f2dafa9e40885d7109e3e5547fa602306d71f870e0d3e6245b99cccb432f",
                        "/settings.gradle" to "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                    ),
                    Paths.get("/gradle-user-home-location-2/caches") to mapOf(
                        "/gradle/libs.versions.toml" to "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a",
                        "/subproject1/build.gradle" to "898c9a78530f745e304de1ae51809d5a5cf1771a976bbc4d5c217eb48dea1ba7" // <-- the new file
                    )
                )
            ),

            // set 3: a file deleted
            arrayOf<Any>(
                """
                    {
                      "absoluteCachesPathToFilePathToChecksum": {
                        "cache-root-1": {
                          "/build.gradle": "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
                          "/settings.gradle": "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                        },
                        "cache-root-2": {
                          "/gradle/libs.versions.toml": "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a",
                          "/subproject1/build.gradle": "898c9a78530f745e304de1ae51809d5a5cf1771a976bbc4d5c217eb48dea1ba7"// <-- the deleted file
                        }
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
                    Paths.get("/gradle-user-home-location-1/caches") to mapOf(
                        "/build.gradle" to "a2c9f2dafa9e40885d7109e3e5547fa602306d71f870e0d3e6245b99cccb432f",
                        "/settings.gradle" to "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
                    ),
                    Paths.get("/gradle-user-home-location-2/caches") to mapOf(
                        "/gradle/libs.versions.toml" to "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
                    )
                )
            )
        )
    }

    @Test(dataProvider = "getInvalidationDataChangedTestData")
    fun `should invalidate cache when local repositories dependency sets changed`(
        cachedDependenciesJson: String,
        newCacheRoots: List<CacheRoot>,
        repoPathToDependencies: Map<Path, Map<String, String>>
    ) {
        // arrange
        val parameterName = "gradleInvalidationData"
        var cachedGradleDependencies: GradleDependencyCacheInvalidationData = prepareGradleDependenciesMetadata(cachedDependenciesJson)
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
        var capturedGradleDependenciesToPublish: GradleDependencyCacheInvalidationData? = serializableArgumentSlot.captured as GradleDependencyCacheInvalidationData?
        Assert.assertNotEquals(capturedGradleDependenciesToPublish, cachedGradleDependencies) // we publish changed dependency sets
    }

    private fun prepareGradleDependenciesMetadata(json: String): GradleDependencyCacheInvalidationData {
        return GradleDependencyCacheInvalidationData.deserialize(json.toByteArray(StandardCharsets.UTF_8))
    }
}