package jetbrains.buildServer.gradle.server.depcache

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.impl.annotations.MockK
import io.mockk.verify
import jetbrains.buildServer.server.cache.depcache.buildFeature.DependencyCacheBuildFeatureRunnersRegistry
import jetbrains.buildServer.server.cache.depcache.buildFeature.DependencyCacheBuildFeatureSupportedRunner
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

@Test
class GradleDependencyCacheRunnerRegistratorTest {
    @MockK private lateinit var runnerRegistryMock: DependencyCacheBuildFeatureRunnersRegistry
    private lateinit var instance: GradleDependencyCacheRunnerRegistrator

    @BeforeMethod
    fun setUp() {
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        instance = GradleDependencyCacheRunnerRegistrator(runnerRegistryMock)
    }

    fun `should register Gradle Runner when plugin spring context is created`() {
        // act
        instance.register()

        // assert
        verify { runnerRegistryMock.register(any<DependencyCacheBuildFeatureSupportedRunner>()) }
    }

    fun `should unregister Gradle Runner when plugin spring context is destroyed`() {
        // act
        instance.unregister()

        // assert
        verify { runnerRegistryMock.unregister(any<DependencyCacheBuildFeatureSupportedRunner>()) }
    }
}