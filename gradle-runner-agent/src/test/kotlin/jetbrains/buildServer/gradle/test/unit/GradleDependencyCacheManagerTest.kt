package jetbrains.buildServer.gradle.test.unit

import io.mockk.*
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.gradle.depcache.GradleDependenciesChangedInvalidator
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheManagerImpl
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheSettingsProvider
import jetbrains.buildServer.gradle.depcache.GradleProjectDependenciesCollector
import org.gradle.tooling.GradleConnector
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File
import java.nio.file.Path

@Test
class GradleDependencyCacheManagerTest {

    @MockK private lateinit var settingsProvider: GradleDependencyCacheSettingsProvider
    @MockK private lateinit var dependenciesCollector: GradleProjectDependenciesCollector
    @MockK private lateinit var invalidator: GradleDependenciesChangedInvalidator
    @MockK private lateinit var cache: DependencyCache
    @MockK private lateinit var projectConnector: GradleConnector

    private lateinit var tempFiles: TempFiles
    private lateinit var buildTempDir: File
    private lateinit var gradleUserHomeDir: File
    private lateinit var gradleCachesDir: File
    private val stepId = "gradle_step"
    private val dependencies: MutableSet<String> = mutableSetOf(
        "com.google.code.gson:gson:2.9.0",
        "org.junit.jupiter:junit-jupiter-api:5.9.1",
        "org.slf4j:slf4j-api:1.7.36"
    )

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        tempFiles = TempFiles()
        buildTempDir = tempFiles.createTempDir()
        gradleUserHomeDir = tempFiles.createTempDir()
        gradleCachesDir = File(gradleUserHomeDir, "caches")

        every { dependenciesCollector.collectProjectDependencies(buildTempDir, projectConnector) } returns Result.success(dependencies)
        every { settingsProvider.cache } returns cache
        every { settingsProvider.postBuildInvalidator } returns invalidator
    }

    @Test
    fun `should not prepare dependency cache when Gradle User Home is not passed`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, dependenciesCollector)

        // act
        dependencyCacheManager.prepareAndRestoreCache(projectConnector, stepId, null, buildTempDir)

        // assert
        verify { cache.logWarning("Failed to detect Gradle User Home location for the current Gradle execution, it will not be cached") }
        assertFalse(gradleCachesDir.exists())
        verify(exactly = 0) { invalidator.addDependenciesToGradleCachesLocation(any(), any()) }
        verify(exactly = 0) { cache.registerAndRestore(any()) }
    }

    @Test
    fun `should prepare and restore dependency cache`() {
        // arrange
        val dependencyCacheManager = GradleDependencyCacheManagerImpl(settingsProvider, dependenciesCollector)
        val pathSlot = slot<Path>()
        val dependenciesSlot = slot<MutableSet<String>>()

        // act
        dependencyCacheManager.prepareAndRestoreCache(projectConnector, stepId, gradleUserHomeDir, buildTempDir)

        // assert
        assertTrue(gradleCachesDir.exists())
        verify { invalidator.addDependenciesToGradleCachesLocation(capture(pathSlot), capture(dependenciesSlot)) }
        assertEquals(pathSlot.captured, gradleCachesDir.toPath())
        assertEquals(dependenciesSlot.captured, dependencies)
        verify { cache.registerAndRestore(any()) }
    }
}