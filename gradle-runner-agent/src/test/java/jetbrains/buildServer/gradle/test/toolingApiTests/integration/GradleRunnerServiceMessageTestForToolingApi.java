package jetbrains.buildServer.gradle.test.toolingApiTests.integration;

import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.test.integration.GradleRunnerServiceMessageTest;
import org.testng.annotations.BeforeMethod;

public abstract class GradleRunnerServiceMessageTestForToolingApi extends GradleRunnerServiceMessageTest {
    @Override
    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    }
}
