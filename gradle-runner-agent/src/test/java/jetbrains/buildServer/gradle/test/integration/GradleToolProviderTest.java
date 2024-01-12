

package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleToolProvider;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 29, 2010
 */
public class GradleToolProviderTest {

  private static final String GRADLE_HOME = "GRADLE_HOME";
  private final TempFiles myTempFiles = new TempFiles();

  private ToolProvider myToolProvider;
  private Mockery myContext;

  private File myGradlePluginDir;
  private File myWorkingDir;
  private ToolProvidersRegistry tpRegistry;
  private ToolProvider registeredProvider;
  private AgentRunningBuild build;
  private BuildRunnerContext runner;

  protected Action registerToolProvider = new CustomAction("Registers Tool Provider") {
    public Object invoke(final Invocation invocation) throws Throwable {
      if (invocation.getParameterCount() > 0) {
        setToolProvider((ToolProvider)invocation.getParameter(0));
      } else {
        throw new IllegalArgumentException("No Tool Providers specified");
      }
      return null;
    }
  };
  private final Action getRegisteredProvider = new CustomAction("Returns registered Tool Provider") {
    public Object invoke(final Invocation invocation) throws Throwable {
      return getToolProvider();
    }
  };
  private Map<String,String> myRunnerParams;

  private ToolProvider getToolProvider() {
    return registeredProvider;
  }

  private void setToolProvider(final ToolProvider provider) {
    registeredProvider = provider;
  }

  @BeforeClass
  public void initDirs() throws IOException {
    final File pluginsDir = myTempFiles.createTempDir();
    myWorkingDir = myTempFiles.createTempDir();
    myGradlePluginDir = new File(pluginsDir, "gradle");
    myGradlePluginDir.mkdirs();
  }

  @BeforeMethod
  public void setUp() {
    myContext = new Mockery();
    tpRegistry = myContext.mock(ToolProvidersRegistry.class);
    build = myContext.mock(AgentRunningBuild.class);
    runner = myContext.mock(BuildRunnerContext.class);
    myRunnerParams = new HashMap<String, String>();
    myRunnerParams.put("teamcity.build.checkoutDir", myWorkingDir.getAbsolutePath());
    final BundledToolsRegistry reg = myContext.mock(BundledToolsRegistry.class);
    final BundledTool tool = myContext.mock(BundledTool.class);
    final BuildParametersMap buildParams = myContext.mock(BuildParametersMap.class);

    myContext.checking(new Expectations() {{
      allowing(tpRegistry).registerToolProvider(with(any(ToolProvider.class))); will(registerToolProvider);
      allowing(tpRegistry).findToolProvider(GradleToolProvider.GRADLE_TOOL); will(getRegisteredProvider);

      allowing(reg).findTool("gradle"); will(returnValue(tool));
      allowing(tool).getRootPath(); will(returnValue(myGradlePluginDir));

      allowing(runner).getRunnerParameters(); will(returnValue(myRunnerParams));
      allowing(runner).getBuildParameters(); will(returnValue(buildParams));
      allowing(buildParams).getAllParameters(); will(returnValue(myRunnerParams));
    }});

    new GradleToolProvider(tpRegistry, reg);
    final ToolProvider toolProvider = tpRegistry.findToolProvider(GradleToolProvider.GRADLE_TOOL);
    assertNotNull(toolProvider);
    myToolProvider = toolProvider;
  }

  @AfterClass
  public void cleanupDirs() {
    myTempFiles.cleanup();
  }

  @Test
  public void testDefaultGradlePath() {
    String envGradleHome = System.getenv(GRADLE_HOME);
    final String path = myToolProvider.getPath(GradleToolProvider.GRADLE_TOOL);
    String expectedPath;

    if (null != envGradleHome && envGradleHome.length() > 0 ) {
      expectedPath = System.getenv(GRADLE_HOME);
    } else {
      expectedPath = myGradlePluginDir.getAbsolutePath();
    }
    assertEquals(path, expectedPath, "Wrong environment-provided gradle path");
  }

  @Test
  public void testProvidedGradlePath() {
    final String expectedPath = "testPathString";
    try {
      myRunnerParams.put(GradleRunnerConstants.GRADLE_HOME, expectedPath);

      final String path = myToolProvider.getPath(GradleToolProvider.GRADLE_TOOL, build, runner);
      assertEquals(path, new File(myWorkingDir, expectedPath).getAbsolutePath(), "Wrong server-provided gradle path");
    } finally {
      myRunnerParams.remove(GradleRunnerConstants.GRADLE_HOME);
    }
  }

  // TW-24588
  @Test
  public void testEnvSettingGradlePath() {
    final String expectedPath = "testPathString";
    try {
      myRunnerParams.put(Constants.ENV_PREFIX + GRADLE_HOME, expectedPath);
      final String path = myToolProvider.getPath(GradleToolProvider.GRADLE_TOOL, build, runner);
      assertEquals(path, new File(myWorkingDir, expectedPath).getAbsolutePath(), "Wrong env provided gradle path");
    } finally {
      myRunnerParams.remove(Constants.ENV_PREFIX + GRADLE_HOME);
    }
  }
}