package jetbrains.buildServer.gradle.server;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jetbrains.MockBuildType;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.serverSide.discovery.BuildRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.serverSide.impl.BaseServerTestCase;
import jetbrains.buildServer.util.browser.FileSystemBrowser;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Nikita.Skvortsov
 * date: 19.11.13.
 */
public class GradleRunnerDiscoveryExtensionTest extends BaseServerTestCase {

  private BuildRunnerDiscoveryExtension myExtension;
  private File myRoot;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myExtension = new GradleRunnerDiscoveryExtension();
    final String gradleLocalPathToData = "gradle-runner-agent/src/test/resources/testProjects";
    myRoot = new File(gradleLocalPathToData);
    if (!myRoot.exists()) {
      myRoot = new File("svnrepo/gradle-runner/" + gradleLocalPathToData);
    }

    assertTrue("Can not find test data. Please check if test projects exist", myRoot.exists());
  }

  @Test
  public void testSimpleBuildDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "projectA"));
    final List<DiscoveredObject> discovered = myExtension.discover(new MockBuildType(), browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals("clean build", parameters.get(GradleRunnerConstants.GRADLE_TASKS));
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
  }

  @Test
  public void testWrapperDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "wrappedProjectA"));
    final List<DiscoveredObject> discovered = myExtension.discover(new MockBuildType(), browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals("clean build", parameters.get(GradleRunnerConstants.GRADLE_TASKS));
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals("true", parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_FLAG));
    assertEquals("gradle-runtime", parameters.get(GradleRunnerConstants.GRADLE_WRAPPER_PATH));
  }

  @Test
  public void testWrapperMultiProjectDiscovery() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "MultiProjectB"));
    final List<DiscoveredObject> discovered = myExtension.discover(new MockBuildType(), browser);
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

    myBuildType.addBuildRunner("", GradleRunnerConstants.RUNNER_TYPE, Collections.<String, String>emptyMap());

    final List<DiscoveredObject> discovered = myExtension.discover(myBuildType, browser);

    assertNotNull(discovered);
    assertEquals(0, discovered.size());
  }

  @Test
  public void testPathToBuildFileDetected() throws Exception {
    final FileSystemBrowser browser = new FileSystemBrowser(new File(myRoot, "subdir"));
    final List<DiscoveredObject> discovered = myExtension.discover(new MockBuildType(), browser);
    assertNotNull(discovered);
    assertEquals(1, discovered.size());
    final Map<String,String> parameters = discovered.get(0).getParameters();
    assertEquals(GradleRunnerConstants.RUNNER_TYPE, discovered.get(0).getType());
    assertEquals("projectB/build.gradle", parameters.get(GradleRunnerConstants.PATH_TO_BUILD_FILE));
  }
}
