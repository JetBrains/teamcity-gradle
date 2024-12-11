package jetbrains.buildServer.gradle.test.unit

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.agent.cache.depcache.cacheroot.CacheRootUsage
import jetbrains.buildServer.gradle.depcache.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.testng.Assert.*
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

@Test
class GradleDependencyCacheManagerTest {

    @MockK
    private lateinit var settingsProvider: GradleDependencyCacheSettingsProvider
    @MockK
    private lateinit var invalidationDataCollector: GradleDependencyCacheInvalidationDataCollector
    @MockK
    private lateinit var invalidator: GradleDependenciesChangedInvalidator
    @MockK
    private lateinit var cache: DependencyCache
    @MockK
    private lateinit var cacheContext: GradleDependencyCacheStepContext
    private lateinit var tempFiles: TempFiles
    private lateinit var workDir: File
    private lateinit var gradleUserHomeDir: File
    private lateinit var gradleCachesDir: File
    private lateinit var cacheRootUsage: CacheRootUsage
    private lateinit var coroutineScope: CoroutineScope
    private val testDispatcher = StandardTestDispatcher()
    private val stepId = "gradle_step"
    private val invalidationData: Map<String, String> = mapOf(
        "/build.gradle" to "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699",
        "/settings.gradle" to "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        tempFiles = TempFiles()
        workDir = tempFiles.createTempDir()
        gradleUserHomeDir = tempFiles.createTempDir()
        gradleCachesDir = File(gradleUserHomeDir, "caches")
        cacheRootUsage = CacheRootUsage(
            GradleDependencyCacheConstants.CACHE_ROOT_TYPE,
            gradleCachesDir.toPath().toAbsolutePath(),
            stepId
        )
        Dispatchers.setMain(testDispatcher)
        coroutineScope = CoroutineScope(testDispatcher)

        every { invalidationDataCollector.collect(workDir, cache, any()) } returns Result.success(invalidationData)
        every { settingsProvider.cache } returns cache
        every { settingsProvider.postBuildInvalidator } returns invalidator
        every { cacheContext.newCacheRootUsage(any(), any()) } returns cacheRootUsage
        every { cacheContext.gradleCachesLocation = any() } returns Unit
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterMethod
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `should not register dependency cache when Gradle User Home is not passed`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, invalidationDataCollector, coroutineScope)

        // act
        dependencyCacheManager.registerAndRestoreCache(stepId, null, cacheContext)

        // assert
        verify { cache.logWarning("Failed to detect Gradle User Home location for the current Gradle execution, it will not be cached") }
        assertFalse(gradleCachesDir.exists())
        verify(exactly = 0) { invalidator.addDependenciesToGradleCachesLocation(any(), any()) }
        verify(exactly = 0) { cache.registerAndRestore(any()) }
    }

    @Test
    fun `should register and restore dependency cache`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, invalidationDataCollector, coroutineScope)

        // act
        dependencyCacheManager.registerAndRestoreCache(stepId, gradleUserHomeDir, cacheContext)

        // assert
        assertTrue(gradleCachesDir.exists())
        verify(exactly = 1) { cache.registerAndRestore(any()) }
        verify(exactly = 1) { cacheContext.gradleCachesLocation = any() }
    }

    @Test
    fun `should prepare invalidation data`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, invalidationDataCollector, coroutineScope)
        coEvery { invalidationDataCollector.collect(workDir, cache, any()) } returns Result.success(invalidationData)

        // act
        dependencyCacheManager.prepareInvalidationDataAsync(workDir, cacheContext)

        // assert
        verify(exactly = 1) { cacheContext.invalidationData = any() }
    }

    @Test
    fun `should update invalidation data`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, invalidationDataCollector, coroutineScope)
        val deferredData = mockk<Deferred<Map<String, String>>>()
        coEvery { deferredData.await() } returns invalidationData
        every { cacheContext.gradleCachesLocation } returns gradleCachesDir.toPath()
        every { cacheContext.invalidationData } returns deferredData
        every { cacheContext.invalidationDataAwaitTimeout } returns 10000
        val invalidationDataSlot = slot<Map<String, String>>()

        // act
        dependencyCacheManager.updateInvalidationData(cacheContext)

        // assert
        verify { invalidator.addDependenciesToGradleCachesLocation(gradleCachesDir.toPath(), capture(invalidationDataSlot)) }
        assertEquals(invalidationDataSlot.captured, invalidationData)
    }
}