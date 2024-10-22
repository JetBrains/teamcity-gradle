package jetbrains.buildServer.gradle.server.depcache

import io.mockk.*
import jetbrains.buildServer.gradle.depcache.GradleDependencyCacheConstants.FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE
import jetbrains.buildServer.serverSide.BuildStartContext
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SRunningBuild
import jetbrains.buildServer.serverSide.TeamCityProperties
import org.testng.Assert.assertEquals
import org.testng.annotations.AfterMethod
import org.testng.annotations.BeforeMethod
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

@Test
class GradleBuildStartContextProcessorTest {

    private lateinit var instance: GradleBuildStartContextProcessor

    @BeforeMethod
    fun setUp() {
        mockkStatic(TeamCityProperties::class)
        MockKAnnotations.init(this, relaxed = true)
        clearAllMocks()
        instance = GradleBuildStartContextProcessor()
    }

    @AfterMethod
    fun tearDown() {
        unmockkStatic(TeamCityProperties::class)
    }

    @DataProvider
    fun getInternalPropertyValues(): Array<Array<Any>> {
        return arrayOf(
            arrayOf("true"),
            arrayOf("false"),
            arrayOf("abcd")
        )
    }

    @Test(dataProvider = "getInternalPropertyValues")
    fun `should add feature flag config parameter when internal property exists and no config parameter present`(propertyValue: String) {
        // arrange
        every { TeamCityProperties.getPropertyOrNull(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE) } returns propertyValue

        val buildTypeMock: SBuildType = mockk<SBuildType>()
        every { buildTypeMock.configParameters } returns emptyMap()
        val buildMock: SRunningBuild = mockk<SRunningBuild>()
        every { buildMock.buildType } returns buildTypeMock
        val contextMock: BuildStartContext = mockk<BuildStartContext>()
        every { contextMock.build } returns buildMock
        val addedParamSlot = slot<String>()
        every { contextMock.addSharedParameter(any(), capture(addedParamSlot)) } just Runs

        // act
        instance.updateParameters(contextMock)

        // assert
        verify { contextMock.addSharedParameter(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE, addedParamSlot.captured) }
        val actualPropertyValue = addedParamSlot.captured
        assertEquals(actualPropertyValue, propertyValue)
    }

    @Test
    fun `should not add feature flag config parameter when no internal property exists`() {
        // arrange
        every { TeamCityProperties.getPropertyOrNull(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE) } returns null
        val contextMock = mockk<BuildStartContext>()

        // act
        instance.updateParameters(contextMock)

        // assert
        verify(exactly = 0) { contextMock.addSharedParameter(any(), any<String>()) }
    }

    @Test
    fun `should not add feature flag config parameter when internal property exists and config parameter already present`() {
        // arrange
        every { TeamCityProperties.getPropertyOrNull(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE) } returns "abc"

        val buildTypeMock: SBuildType = mockk<SBuildType>()
        every { buildTypeMock.configParameters } returns mapOf(FEATURE_TOGGLE_GRADLE_DEPENDENCY_CACHE to "def")
        val buildMock: SRunningBuild = mockk<SRunningBuild>()
        every { buildMock.buildType } returns buildTypeMock
        val contextMock = mockk<BuildStartContext>()
        every { contextMock.build } returns buildMock

        // act
        instance.updateParameters(contextMock)

        // assert
        verify(exactly = 0) { contextMock.addSharedParameter(any<String>(), any<String>()) }
    }
}