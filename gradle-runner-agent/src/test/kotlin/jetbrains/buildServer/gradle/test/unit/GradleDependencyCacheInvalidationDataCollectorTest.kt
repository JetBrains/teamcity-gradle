package jetbrains.buildServer.gradle.test.unit

import io.mockk.mockk
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheInvalidationDataCollector
import jetbrains.buildServer.gradle.test.GradleTestUtil
import jetbrains.buildServer.util.FileUtil
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class GradleDependencyCacheInvalidationDataCollectorTest {

    private lateinit var collector: GradleDependencyCacheInvalidationDataCollector
    private lateinit var tempFiles: TempFiles
    private lateinit var testDir: File
    private lateinit var projectRoot: File

    @BeforeMethod
    fun beforeTest() {
        collector = GradleDependencyCacheInvalidationDataCollector()
        tempFiles = TempFiles()
        testDir = tempFiles.createTempDir()
        projectRoot = GradleTestUtil.setProjectRoot(File("."));
    }

    @Test
    fun `should collect invalidation data for a groovy project`() {
        // arrange
        val projectDir = "groovyMultimoduleProject"
        FileUtil.copyDir(File(projectRoot, "src/test/resources/testProjects/depcache"), testDir, true)
        val workingDir = File(testDir, projectDir)
        val depCache = mockk<DependencyCache>(relaxed = true)

        // act
        val result = collector.collect(workingDir, depCache, Integer.MAX_VALUE)

        // assert
        Assert.assertFalse(result.isFailure)
        val data = result.getOrThrow()
        Assert.assertEquals(data.size, 7)
        // gradle-wrapper.properties
        Assert.assertNotNull(data["/gradle/wrapper/gradle-wrapper.properties"])
        // libs.versions.toml
        Assert.assertNotNull(data["/gradle/libs.versions.toml"])
        // settings.gradle
        Assert.assertNotNull(data["/settings.gradle"])
        // from root project
        Assert.assertNotNull(data["/build.gradle"])
        // from subproject1
        Assert.assertNotNull(data["/subproject1/build.gradle"])
        // from subproject2
        Assert.assertNotNull(data["/subproject2/build.gradle"])
        // from sub-sub-project1
        Assert.assertNotNull(data["/subproject1/sub-sub-project1/build.gradle"])

        // act: checking for the second time
        val result2 = collector.collect(workingDir, depCache, Integer.MAX_VALUE)

        // assert: the result is consistent
        Assert.assertEquals(result2, result)
    }

    @Test
    fun `should collect invalidation data for a kotlin project`() {
        // arrange
        val projectDir = "kotlinMultimoduleProject"
        FileUtil.copyDir(File(projectRoot, "src/test/resources/testProjects/depcache"), testDir, true)
        val workingDir = File(testDir, projectDir)
        val depCache = mockk<DependencyCache>(relaxed = true)

        // act
        val result = collector.collect(workingDir, depCache, Integer.MAX_VALUE)

        // assert
        Assert.assertFalse(result.isFailure)
        val data = result.getOrThrow()
        Assert.assertEquals(data.size, 9)
        // Dependencies.kt
        Assert.assertNotNull(data["/buildSrc/src/main/java/Dependencies.kt"])
        // Versions.kt
        Assert.assertNotNull(data["/buildSrc/src/main/java/Versions.kt"])
        // gradle-wrapper.properties
        Assert.assertNotNull(data["/gradle/wrapper/gradle-wrapper.properties"])
        // from buildSrc
        Assert.assertNotNull(data["/buildSrc/build.gradle.kts"])
        // settings.gradle.kts
        Assert.assertNotNull(data["/settings.gradle.kts"])
        // from root project
        Assert.assertNotNull(data["/build.gradle.kts"])
        // from subproject1
        Assert.assertNotNull(data["/subproject1/build.gradle.kts"])
        // from subproject2
        Assert.assertNotNull(data["/subproject2/build.gradle.kts"])
        // from sub-sub-project1
        Assert.assertNotNull(data["/subproject1/sub-sub-project1/build.gradle.kts"])

        // act: checking for the second time
        val result2 = collector.collect(workingDir, depCache, Integer.MAX_VALUE)

        // assert: the result is consistent
        Assert.assertEquals(result2, result)
    }

    @Test
    fun `should respect depth limit when searching for invalidation data`() {
        // arrange
        val projectDir = "groovyMultimoduleProject"
        FileUtil.copyDir(File(projectRoot, "src/test/resources/testProjects/depcache"), testDir, true)
        val workingDir = File(testDir, projectDir)
        val depCache = mockk<DependencyCache>(relaxed = true)
        val depthLimit = 0

        // act
        val result = collector.collect(workingDir, depCache, depthLimit)

        // assert
        Assert.assertFalse(result.isFailure)
        val data = result.getOrThrow()
        Assert.assertEquals(data.size, 2)
        // settings.gradle
        Assert.assertNotNull(data["/settings.gradle"])
        // from root project
        Assert.assertNotNull(data["/build.gradle"])
    }
}