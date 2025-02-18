package jetbrains.buildServer.gradle.test.unit

import io.mockk.mockk
import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheChecksumBuilder
import jetbrains.buildServer.gradle.test.GradleTestUtil
import jetbrains.buildServer.util.FileUtil
import org.testng.Assert
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class GradleDependencyCacheChecksumBuilderTest {

    private lateinit var checksumBuilder: GradleDependencyCacheChecksumBuilder
    private lateinit var tempFiles: TempFiles
    private lateinit var testDir: File
    private lateinit var projectRoot: File

    @BeforeMethod
    fun beforeTest() {
        checksumBuilder = GradleDependencyCacheChecksumBuilder()
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
        // Consists of:
        // /gradle/wrapper/gradle-wrapper.properties
        // /gradle/libs.versions.toml
        // /settings.gradle
        // /build.gradle
        // /subproject1/build.gradle
        // /subproject2/build.gradle
        // /subproject1/sub-sub-project1/build.gradle
        val expected = "5930937cb2ffa94489cddccce5a34d95e0acaa0c984b828d45e42c7c5b1e519a"

        // act
        val result = checksumBuilder.build(workingDir, depCache, Integer.MAX_VALUE)

        // assert
        Assert.assertFalse(result.isFailure)
        val checksum = result.getOrThrow()
        Assert.assertEquals(checksum, expected)

        // act: checking for the second time
        val result2 = checksumBuilder.build(workingDir, depCache, Integer.MAX_VALUE)

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
        // Consists of:
        // /buildSrc/src/main/java/Dependencies.kt
        // /buildSrc/src/main/java/Versions.kt
        // /gradle/wrapper/gradle-wrapper.properties
        // /buildSrc/build.gradle.kts
        // /settings.gradle.kts
        // /build.gradle.kts
        // /subproject1/build.gradle.kts
        // /subproject2/build.gradle.kts
        // /subproject1/sub-sub-project1/build.gradle.kts
        val expected = "2a3f3ab6dc0c7607a3f06a02ab7a8757f7bab29ba9cf416c36b9d8bccfe0a6a8"

        // act
        val result = checksumBuilder.build(workingDir, depCache, Integer.MAX_VALUE)

        // assert
        Assert.assertFalse(result.isFailure)
        val checksum = result.getOrThrow()
        Assert.assertEquals(checksum, expected)

        // act: checking for the second time
        val result2 = checksumBuilder.build(workingDir, depCache, Integer.MAX_VALUE)

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
        // a checksum of settings.gradle and build.gradle from the root of the project
        val expected = "579fcb991387cd1e5eba828dd4ce93f14c22072e767248bd57ee8eb9c1a381f5"

        // act
        val result = checksumBuilder.build(workingDir, depCache, depthLimit)

        // assert
        Assert.assertFalse(result.isFailure)
        val checksum = result.getOrThrow()
        Assert.assertFalse(checksum.isNullOrEmpty())
        Assert.assertEquals(checksum, expected)
    }

    @Test
    fun `should merge checksums`() {
        // arrange
        val first = "ae990de7ec4fa1af7ce5fc014f55623c34e15857baddf63b2dabc43fc9c5dec3"
        val second = "994e24667e8b8412cb2b4ca645bd69c54ee2490dde5d727f2c835d809a7c386a"
        val expected = "28b30ba7f4cdf6720969754a936bc5e1e720704623da3b1caa5940950ac993ac"

        // act
        val result = checksumBuilder.merge(first, second)

        // assert
        Assert.assertEquals(result, expected)
    }
}