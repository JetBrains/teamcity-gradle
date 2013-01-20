/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.ExtensionHolder;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.TestLogger;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner2.GenericCommandLineBuildProcess;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerServiceFactory;
import jetbrains.buildServer.gradle.test.GradleTestUtil;
import jetbrains.buildServer.util.FileUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import static org.testng.Assert.assertTrue;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public class BaseGradleRunnerTest {

  public static final String PROPERTY_GRADLE_RUNTIME = "gradle.runtime";
  public static final String REPORT_SEQ_DIR = "src/test/resources/reportSequences";

  public static final String PROJECT_A_NAME = "projectA";
  public static final String PROJECT_B_NAME = "projectB";
  public static final String PROJECT_C_NAME = "projectC";
  public static final String PROJECT_D_NAME = "projectD";
  public static final String PROJECT_E_NAME = "projectE";
  protected static final String MULTI_PROJECT_A_NAME = "MultiProjectA";
  protected static final String MULTI_PROJECT_B_NAME = "MultiProjectB";
  private static final String TOOLS_GRADLE_PATH = "../../../tools/gradle";

  protected final TempFiles myTempFiles = new TempFiles();

  final static Action reportMessage = new CustomAction("Echoes test output") {
    public Object invoke(final Invocation invocation) throws Throwable {
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String output = param == null ? "" : param.toString() ;
          Reporter.log("[MSG]" + output);
        }
      }
      return null;
    }
  };

  final static Action reportWarning = new CustomAction("Echoes test output") {
    public Object invoke(final Invocation invocation) throws Throwable {
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String output = param == null ? "" : param.toString() ;
          Reporter.log("[WRN]" + output);
        }
      }
      return null;
    }
  };

    final static Action reportError = new CustomAction("Echoes test output") {
    public Object invoke(final Invocation invocation) throws Throwable {
      if (invocation.getParameterCount() > 0) {
        for(Object param : invocation.getParametersAsArray()) {
          final String output = param == null ? "" : param.toString() ;
          Reporter.log("[ERR]" + output);
        }
      }
      return null;
    }
  };

  protected File myInitScript;
  protected File myTempDir;
  protected File myCoDir;
  protected AgentRunningBuild myMockBuild;
  protected ExtensionHolder myMockExtensionHolder;
  protected BuildRunnerContext myMockRunner;
  protected FlowLogger myMockLogger;
  protected File myProjectRoot;
  protected Map<String, String> myRunnerParams = new ConcurrentHashMap<String,String>();
  protected Map<String, String> myBuildEnvVars = new ConcurrentHashMap<String,String>(System.getenv());
  private TestLogger myTestLogger = new TestLogger();


  @DataProvider(name = "gradle-version-provider")
  public Object[][] getGradlePaths(Method m) {
    Object[][] result;
    if (myProjectRoot == null) {
      myProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
    }
    File gradleDir = new File(myProjectRoot, TOOLS_GRADLE_PATH);
    Reporter.log(gradleDir.getAbsolutePath());
    if (gradleDir.exists() && gradleDir.isDirectory()) {
      File[] versions = gradleDir.listFiles();
      List<Object[]> versionNames = new LinkedList<Object[]>();
      for (File version : versions) {
        if (new File(version, "bin/gradle" + (SystemInfo.isWindows ? ".bat" : "")).exists()) {
          versionNames.add(new Object[] { version.getName() });
        }
      }
      result = versionNames.toArray(new Object[versionNames.size()][]);
    } else {
        final String propsGradleHome = System.getProperty(PROPERTY_GRADLE_RUNTIME);
        result = new Object[][] { new Object [] { propsGradleHome }};
    }
    return result;
  }

  public String getGradlePath(String gradleVersion) throws IOException {
    final File gradleHome = new File(gradleVersion);
    if (gradleHome.isAbsolute()) {
      return gradleHome.getCanonicalPath();
    } else {
      return new File(new File(myProjectRoot, TOOLS_GRADLE_PATH), gradleVersion).getCanonicalPath();
    }
  }

  @BeforeMethod
  public void checkEnvironment() throws IOException {
    if (myProjectRoot == null) {
      myProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
    }
    findInitScript(myProjectRoot);
    createProjectsWorkingCopy(myProjectRoot);
    myTestLogger.onSuiteStart();
  }


  private void createProjectsWorkingCopy(final File curDir) throws IOException {
    myTempDir = myTempFiles.createTempDir();
    myCoDir = new File(myTempDir, "checkoutDir");
    FileUtil.copyDir(new File(curDir, "src/test/resources/testProjects"), myCoDir, true);
    assertTrue(new File(myCoDir, PROJECT_A_NAME + "/build.gradle").canRead(), "Failed to copy test projects.");
  }

  private void findInitScript(File curDir) {
    myInitScript = new File(curDir, GradleTestUtil.REL_SCRIPT_DIR + GradleRunnerConstants.INIT_SCRIPT_NAME);
    assertTrue(myInitScript.canRead(), "Path to init script must point to existing and readable file.");
  }

  @AfterMethod
  public void tearDownClass() {
    myTempFiles.cleanup();
  }

  protected void runTest(final Expectations expectations, final Mockery context) throws RunBuildException {

    context.checking(expectations);

    CommandLineBuildService service = new GradleRunnerServiceFactory().createService();
    service.initialize(myMockBuild, myMockRunner);
    GenericCommandLineBuildProcess proc = GenericCommandLineBuildProcess.createProcessWithOldStyleCLBuildService(myMockRunner, service, myMockExtensionHolder);
    proc.start();
    proc.waitFor();

    context.assertIsSatisfied();
  }

  @BeforeMethod
  public void setUp() throws Exception {
    myTestLogger.onTestStart();
  }

  @AfterMethod
  public void tearDown() throws Exception {
    myTestLogger.onTestFinish(false);
  }

  protected Mockery initContext(final String projectName, final String gradleParams, final String gradleVersion) throws IOException {
    Mockery context = new Mockery();

    final String flowId = FlowGenerator.generateNewFlow();

    myMockExtensionHolder = context.mock(ExtensionHolder.class);

    myMockBuild = context.mock(AgentRunningBuild.class);
    myMockRunner = context.mock(BuildRunnerContext.class);
    myMockLogger = context.mock(FlowLogger.class);
    final BuildParametersMap buildParams = context.mock(BuildParametersMap.class);

    final String gradlePath;
    if (gradleVersion != null) {
       gradlePath = getGradlePath(gradleVersion);
       myRunnerParams.put(GradleRunnerConstants.GRADLE_HOME, gradlePath);
    } else {
      gradlePath = null;
    }

    myRunnerParams.put(GradleRunnerConstants.GRADLE_INIT_SCRIPT, myInitScript.getAbsolutePath());
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, gradleParams);

    final File workingDir = new File(myCoDir, projectName);

    final Expectations initMockingCtx = new Expectations() {{
      allowing(myMockBuild).getBuildLogger(); will(returnValue(myMockLogger));
      allowing(myMockBuild).getCheckoutDirectory(); will(returnValue(myCoDir));
      allowing(myMockBuild).getBuildTempDirectory(); will(returnValue(myTempDir));
      allowing(myMockBuild).getFailBuildOnExitCode(); will(returnValue(true));
      allowing(myMockBuild).getSharedConfigParameters(); will(returnValue(Collections.<String, String>emptyMap()));
      allowing(myMockBuild).getBuildNumber(); will(returnValue("12345"));

      allowing(myMockRunner).getRunnerParameters(); will(returnValue(myRunnerParams));
      allowing(myMockRunner).getBuildParameters(); will(returnValue(buildParams));
      allowing(myMockRunner).getConfigParameters(); will(returnValue(Collections.<String, String>emptyMap()));
      allowing(myMockRunner).getWorkingDirectory(); will(returnValue(workingDir));
      allowing(myMockRunner).getToolPath("gradle"); will(returnValue(gradlePath));
      allowing(myMockRunner).getBuild(); will(returnValue(myMockBuild));

      allowing(buildParams).getAllParameters(); will(returnValue(myBuildEnvVars));
      allowing(buildParams).getEnvironmentVariables(); will(returnValue(myBuildEnvVars));

      allowing(myMockLogger).getFlowId();will(returnValue(flowId));
      allowing(myMockLogger).getFlowLogger(with(any(String.class)));will(returnValue(myMockLogger));
      allowing(myMockLogger).startFlow();
      allowing(myMockLogger).disposeFlow();

      allowing(myMockExtensionHolder).getExtensions(with(Expectations.<Class<AgentExtension>>anything())); will(returnValue(Collections.<Object>emptyList()));
    }};

    context.checking(initMockingCtx);
    return context;
  }


}
