package jetbrains.buildServer.gradle.test.unit

import io.mockk.MockKAnnotations
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.impl.annotations.MockK
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.BuildAgentConfiguration
import jetbrains.buildServer.gradle.agent.GradleRunnerCacheDirectoryProvider
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

@Test
class GradleRunnerCacheDirectoryProviderTest {

    @MockK
    private lateinit var agentConfiguration: BuildAgentConfiguration
    private lateinit var tempFiles: TempFiles
    private lateinit var cachesDir: File
    private lateinit var instance: GradleRunnerCacheDirectoryProvider

    @BeforeMethod
    fun setup(){
        clearAllMocks()
        MockKAnnotations.init(this)
        tempFiles = TempFiles()
        cachesDir = File(tempFiles.createTempDir(), GradleRunnerCacheDirectoryProvider.GRADLE_CACHE_DIR)
        instance = GradleRunnerCacheDirectoryProvider()

        every { agentConfiguration.getCacheDirectory(any()) } returns cachesDir
    }

    @Test
    fun `should return Gradle runner cache location`() {
        // arrange, act
        val result = GradleRunnerCacheDirectoryProvider.getGradleRunnerCacheDirectory(agentConfiguration)

        // assert
        assertEquals(result, cachesDir)
    }
}