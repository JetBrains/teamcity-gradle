 package jetbrains.buildServer.gradle.server;

 import java.io.File;
 import java.util.Collections;
 import java.util.List;
 import java.util.Map;
 import jetbrains.MockBuildType;
 import jetbrains.buildServer.BaseTestCase;
 import jetbrains.buildServer.gradle.GradleRunnerConstants;
 import jetbrains.buildServer.serverSide.BuildTypeSettings;
 import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
 import jetbrains.buildServer.serverSide.discovery.BuildRunnerDiscoveryExtension;
 import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
 import jetbrains.buildServer.util.browser.FileSystemBrowser;
 import org.jmock.Expectations;
 import org.jmock.Mockery;
 import org.testng.annotations.BeforeMethod;
 import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * date: 19.11.13.
 */
public class GradleRunnerDiscoveryExtensionTest extends BaseTestCase {

  private BuildRunnerDiscoveryExtension myExtension;
  private File myRoot;
  private BuildTypeSettings mySettings;
  private Mockery myContext;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myExtension = new GradleRunnerDiscoveryExtension();
    myRoot = new File("../gradle-runner-agent/src/test/resources/testProjects");
    if (!myRoot.exists()) {
      myRoot = new File("svnrepo/gradle-runner/gradle-runner-agent/src/test/resources/testProjects");
    }

    assertTrue("Can not find test data. Please check if test projects exist", myRoot.exists());
    myContext = new Mockery();
    mySettings = myContext.mock(BuildTypeSettings.class, "empty");
    myContext.checking(new Expectations() {{
      allowing(mySettings).getBuildRunners();  will(returnValue(Collections.emptyList()));
    }});
  }

  @Test
  public void testSimpleBuildDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "projectA"));
    final List<DiscoveredObject> discovered = myExtension.discover(mySettings, browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals("clean build", parameters.get(GradleRunnerConstants.GRADLE_TASKS));
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals(null, parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));
  }

  @Test
  public void testWrapperDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "wrappedProjectA"));
    final List<DiscoveredObject> discovered = myExtension.discover(mySettings, browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();

    assertEquals("clean build", parameters.get(GradleRunnerConstants.GRADLE_TASKS));
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals("true", parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));
  }

  @Test
  public void testWrapperMultiProjectDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "MultiProjectB"));
    final List<DiscoveredObject> discovered = myExtension.discover(mySettings, browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals("clean build", parameters.get(GradleRunnerConstants.GRADLE_TASKS));
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals("true", parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));
    final String wrapperPath = parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_PATH);
    assertTrue(wrapperPath == null || wrapperPath.length() == 0);
  }

  @Test
  public void testExistingRunnersNotReported() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "MultiProjectB"));
    final BuildTypeSettings buildTypeSettings = myContext.mock(BuildTypeSettings.class, "btWithGradle");
    final SBuildRunnerDescriptor runnerDescriptor = myContext.mock(SBuildRunnerDescriptor.class);
    myContext.checking(new Expectations() {{
      allowing(buildTypeSettings).getBuildRunners();  will(returnValue(Collections.singletonList(runnerDescriptor)));
      allowing(runnerDescriptor).getType();             will(returnValue(GradleRunnerConstants.RUNNER_TYPE));
      allowing(runnerDescriptor).getParameters();       will(returnValue(Collections.emptyMap()));
    }});

    final List<DiscoveredObject> discovered = myExtension.discover(buildTypeSettings, browser);

    assertNotNull(discovered);
    assertEquals(0, discovered.size());
  }

  @Test
  public void testPathToBuildFileDetected() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "subdir"));
    final List<DiscoveredObject> discovered = myExtension.discover(mySettings, browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals("projectB/build.gradle", parameters.get(GradleRunnerConstants.PATH_TO_BUILD_FILE));
  }

  @Test // TW-47404
  public void testDetectedWrapperIsResetBetweenInvocations() {
    final List<DiscoveredObject> discoveredWrapper = myExtension.discover(new MockBuildType(), new FileSystemBrowser(new File(myRoot, "wrappedProjectA")));
    assertEquals("true", discoveredWrapper.get(0).getParameters().get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));

    final List<DiscoveredObject> discoveredNoWrapper = myExtension.discover(new MockBuildType(), new FileSystemBrowser(new File(myRoot, "projectA")));
    assertEquals(null, discoveredNoWrapper.get(0).getParameters().get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));
  }
}
