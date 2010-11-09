/*
 * Copyright 2000-2010 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
import org.testng.annotations.*;

import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 29, 2010
 */
public class GradleToolProviderTest {

  private static final String GRADLE_HOME = "GRADLE_HOME";
  private final TempFiles myTempFiles = new TempFiles();

  private ToolProvider myToolProvider;
  private Mockery myContext;

  private File myPluginsDir;
  private File myGradlePluginDir;
  private BuildAgentConfiguration buildAgentConfig;
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


  private ToolProvider getToolProvider() {
    return registeredProvider;
  }

  private void setToolProvider(final ToolProvider provider) {
    registeredProvider = provider;
  }

  @BeforeClass
  public void initDirs() throws IOException {
    myPluginsDir = myTempFiles.createTempDir();
    myGradlePluginDir = new File(myPluginsDir, "gradle");
    myGradlePluginDir.mkdirs();
  }

  @BeforeMethod
  public void setUp() {
    myContext = new Mockery();
    buildAgentConfig = myContext.mock(BuildAgentConfiguration.class);
    tpRegistry = myContext.mock(ToolProvidersRegistry.class);
    build = myContext.mock(AgentRunningBuild.class);
    runner = myContext.mock(BuildRunnerContext.class);

    myContext.checking(new Expectations() {{
      allowing(tpRegistry).registerToolProvider(with(any(ToolProvider.class))); will(registerToolProvider);
      allowing(tpRegistry).findToolProvider(GradleToolProvider.GRADLE_TOOL); will(getRegisteredProvider);
      allowing(buildAgentConfig).getAgentPluginsDirectory(); will(returnValue(myPluginsDir));
    }});

    new GradleToolProvider(tpRegistry, buildAgentConfig);
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
    final Map<String,String> runnerParams = new HashMap<String, String>();
    runnerParams.put(GradleRunnerConstants.GRADLE_HOME, expectedPath);

    myContext.checking(new Expectations() {{
      allowing(runner).getRunnerParameters();will(returnValue(runnerParams));
    }});

    final String path = myToolProvider.getPath(GradleToolProvider.GRADLE_TOOL, build, runner);
    assertEquals(path, expectedPath, "Wrong server-provided gradle path");
  }


}
