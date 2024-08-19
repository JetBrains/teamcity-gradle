package jetbrains.buildServer.gradle.test.integration

import jetbrains.buildServer.TempFiles
import jetbrains.buildServer.gradle.depcache.GradleProjectDependenciesCollector
import org.testng.Assert.*
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test
import java.io.File

class GradleProjectDependenciesCollectorTest : GradleRunnerToolingApiTest() {

    private lateinit var collector: GradleProjectDependenciesCollector
    private lateinit var tempFiles: TempFiles
    private lateinit var buildTempDir: File

    @BeforeMethod
    fun beforeTest() {
        collector = GradleProjectDependenciesCollector()
        tempFiles = TempFiles()
        buildTempDir = tempFiles.createTempDir()
    }

    @Test(dataProvider = "8 > gradle-version-provider >= 3.0")
    fun `should collect gradle dependencies when gradle is less than 8`(gradleVersion: String) {
        // arrange
        val wd: File = getWorkingDir(getGradleVersion(gradleVersion), BaseGradleRunnerTest.MULTIMODULE_PROJECT_1)
        val gradlePath = File(getGradlePath(gradleVersion))
        val gradleConnector = configureGradleConnector(wd, gradlePath)

        // act
        val result = collector.collectProjectDependencies(buildTempDir, gradleConnector)

        // assert
        assertFalse(result.isFailure)
        val collectedDependencies = result.getOrThrow()
        assertEquals(collectedDependencies.size, 4)
        assertTrue(collectedDependencies.contains("junit:junit:4.13.2")) // from root project
        assertTrue(collectedDependencies.contains("org.testng:testng:7.8.0")) // from subproject1
        assertTrue(collectedDependencies.contains("com.google.code.gson:gson:2.8.9")) // from subproject2
        assertTrue(collectedDependencies.contains("org.apache.commons:commons-lang3:3.12.0")) // from sub-sub-project1
    }

    @Test(dataProvider = "gradle-version-provider>=8")
    fun `should collect gradle dependencies when gradle is 8 and higher`(gradleVersion: String) {
        // arrange
        val wd: File = getWorkingDir(getGradleVersion(gradleVersion), BaseGradleRunnerTest.MULTIMODULE_PROJECT_1)
        val gradlePath = File(getGradlePath(gradleVersion))
        val gradleConnector = configureGradleConnector(wd, gradlePath)

        // act
        val result = collector.collectProjectDependencies(buildTempDir, gradleConnector)

        // assert
        assertFalse(result.isFailure)
        val collectedDependencies = result.getOrThrow()
        assertEquals(collectedDependencies.size, 4)
        assertTrue(collectedDependencies.contains("junit:junit:4.13.2")) // from root project
        assertTrue(collectedDependencies.contains("org.testng:testng:7.8.0")) // from subproject1
        assertTrue(collectedDependencies.contains("com.google.code.gson:gson:2.8.9")) // from subproject2
        assertTrue(collectedDependencies.contains("org.apache.commons:commons-lang3:3.12.0")) // from sub-sub-project1
    }
}