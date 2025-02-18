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
    private lateinit var checksumBuilder: GradleDependencyCacheChecksumBuilder
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
    private val checksum = "932710cf8b4e31b5dd242a72540fe51c2fb9510fedbeaf7866780843d39af699"

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

        every { checksumBuilder.build(workDir, cache, any()) } returns Result.success(checksum)
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
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, checksumBuilder, coroutineScope)

        // act
        dependencyCacheManager.registerAndRestoreCache(stepId, null, cacheContext)

        // assert
        verify { cache.logWarning("Failed to detect Gradle User Home location for the current Gradle execution, it will not be cached") }
        assertFalse(gradleCachesDir.exists())
        verify(exactly = 0) { invalidator.addChecksumToGradleCachesLocation(any(), any()) }
        verify(exactly = 0) { cache.registerAndRestore(any()) }
    }

    @Test
    fun `should register and restore dependency cache`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, checksumBuilder, coroutineScope)

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
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, checksumBuilder, coroutineScope)
        coEvery { checksumBuilder.build(workDir, cache, any()) } returns Result.success(checksum)

        // act
        dependencyCacheManager.prepareChecksumAsync(workDir, cacheContext)

        // assert
        verify(exactly = 1) { cacheContext.projectFilesChecksum = any() }
    }

    @Test
    fun `should update invalidation data`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, checksumBuilder, coroutineScope)
        val deferredData = mockk<Deferred<String>>()
        coEvery { deferredData.await() } returns checksum
        every { cacheContext.gradleCachesLocation } returns gradleCachesDir.toPath()
        every { cacheContext.projectFilesChecksum } returns deferredData
        every { cacheContext.projectFilesChecksumAwaitTimeout } returns 10000
        val invalidationDataSlot = slot<String>()

        // act
        dependencyCacheManager.updateInvalidatorWithChecksum(cacheContext)

        // assert
        verify { invalidator.addChecksumToGradleCachesLocation(gradleCachesDir.toPath(), capture(invalidationDataSlot)) }
        assertEquals(invalidationDataSlot.captured, checksum)
    }
}