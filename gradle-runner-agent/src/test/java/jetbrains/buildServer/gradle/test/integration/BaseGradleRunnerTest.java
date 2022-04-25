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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.*;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.JavaRunnerUtil;
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession;
import jetbrains.buildServer.agent.runner2.GenericCommandLineBuildProcess;
import jetbrains.buildServer.agent.runner2.SingleCommandLineBuildSessionFactoryAdapter;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerServiceFactory;
import jetbrains.buildServer.gradle.test.GradleTestUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.Option;
import jetbrains.buildServer.util.PasswordReplacer;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

import static org.testng.Assert.assertNotNull;
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
  public static final String PROJECT_F_NAME = "projectF";
  public static final String PROJECT_I_NAME = "projectI";
  public static final String PROJECT_J_NAME = "projectJ";
  public static final String PROJECT_L_NAME = "projectL";
  public static final String PROJECT_M_NAME = "projectM";
  public static final String PROJECT_N_NAME = "projectN";
  public static final String PROJECT_O_NAME = "projectO";
  public static final String PROJECT_P_NAME = "projectP";
  public static final String PROJECT_PRINT_NAME = "projectPrint";
  public static final String PROJECT_Q_NAME = "projectQ";
  public static final String PROJECT_S_NAME = "projectS";
  public static final String PROJECT_SM_NAME = "projectSm";
  public static final String PROJECT_SF_NAME = "projectSf";
  protected static final String MULTI_PROJECT_A_NAME = "MultiProjectA";
  protected static final String MULTI_PROJECT_B_NAME = "MultiProjectB";
  protected static final String MULTI_PROJECT_C_NAME = "MultiProjectC";
  protected static final String MULTI_PROJECT_E_NAME = "MultiProjectE";
  protected static final String DEMAND_MULTI_PROJECT_A_NAME = "demandMultiProjectA";
  private static final String TOOLS_GRADLE_PATH = "../../../tools/gradle";
  private static final String TOOLS_GRADLE_PATH_LOCAL = "../.tools/gradle";

  static {
    TestInternalProperties.init();
  }

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
  protected static File ourProjectRoot;
  protected Map<String, String> myRunnerParams = new ConcurrentHashMap<String,String>();
  protected Map<String, String> myBuildEnvVars = new ConcurrentHashMap<String,String>(System.getenv());
  protected Map<String, String> myTeamCitySystemProps = new ConcurrentHashMap<String,String>();
  protected Map<String, String> myTeamCityConfigParameters = new ConcurrentHashMap<String,String>();
  protected Map<Option<?>, Object> myBuildTypeOptionValue = new HashMap<Option<?>, Object>();
  protected boolean myVirtualContext = false;
  private final TestLogger myTestLogger = new TestLogger();

  private static final boolean IS_JRE_8 = System.getProperty("java.specification.version").contains("1.8");


  @DataProvider(name = "gradle-version-provider")
  public static Iterator<String[]> getGradlePaths() {
    return generateGradlePaths().iterator();
  }

  @DataProvider(name = "gradle-version-provider>2.0")
  public static Iterator<String[]> getGradlePaths20() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "2.0") > 0)
                    .iterator();
  }

  @DataProvider(name = "gradle-version-provider>=3.0")
  public static Iterator<String[]> getGradlePaths30() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "3.0") >= 0)
                    .iterator();
  }

  @DataProvider(name = "gradle-version-provider>=4.4")
  public static Iterator<String[]> getGradlePaths44() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "4.4") >= 0)
                    .iterator();
  }

  @DataProvider(name = "gradle-version-provider>=4.7")
  public static Iterator<String[]> getGradlePaths47() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "4.7") >= 0)
                    .iterator();
  }

  @DataProvider(name = "gradle-version-provider>=5.0")
  public static Iterator<String[]> getGradlePaths50() {
    return generateGradlePaths().stream()
                           .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "5.0") >= 0)
                           .iterator();
  }

  @DataProvider(name = "gradle-version-provider<4.4")
  public static Iterator<String[]> getGradlePathsLess44() {
    return generateGradlePaths().stream()
                           .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "4.4") < 0)
                           .iterator();
  }

  @DataProvider(name = "gradle-last-version-provider")
  public static Iterator<String[]> getGradlePathsLast() {
    return generateGradlePaths().stream()
      .max((a,b) -> VersionComparatorUtil.compare(getGradleVersionFromPath(a[0]), getGradleVersionFromPath(b[0])))
      .map(path -> Collections.singletonList(path).iterator())
      .orElseGet(Collections::emptyIterator);
  }

  public static List<String[]> generateGradlePaths() {
    if (ourProjectRoot == null) {
      ourProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
    }
    File gradleDir = new File(ourProjectRoot, TOOLS_GRADLE_PATH);
    if(!gradleDir.exists()) gradleDir = new File(ourProjectRoot, TOOLS_GRADLE_PATH_LOCAL);
    Reporter.log(gradleDir.getAbsolutePath());
    if (gradleDir.exists() && gradleDir.isDirectory()) {
      return listAvailableVersions(gradleDir);
    } else {
      return Collections.singletonList(new String[]{System.getProperty(PROPERTY_GRADLE_RUNTIME)});
    }
  }

  private static List<String[]> listAvailableVersions(final @NotNull File gradleDir) {
    final File[] versions = gradleDir.listFiles();
    assertNotNull(versions);

    final List<String[]> versionNames = new ArrayList<String[]>(versions.length);
    for (File version : versions) {
      if (looksLikeGradleDir(version) && versionFitsCurrentJdk(version)) {
        versionNames.add(new String[]{version.getName()});
      }
    }
    Collections.sort(versionNames, new Comparator<String[]>() {
      @Override
      public int compare(final String[] o1, final String[] o2) {
        final String v1 = getGradleVersionFromPath(o1[0]);
        final String v2 = getGradleVersionFromPath(o2[0]);
        return VersionComparatorUtil.compare(v1, v2);
      }
    });
    return versionNames;
  }

  private static boolean looksLikeGradleDir(final File version) {
    return new File(version, "bin/gradle" + (SystemInfo.isWindows ? ".bat" : "")).exists();
  }

  private static boolean versionFitsCurrentJdk(final File gradleDir) {
    if (IS_JRE_8) {
      try {
        final String versionString = getGradleVersionFromPath(gradleDir.getName());
        return VersionComparatorUtil.compare(versionString, "1.11") >= 0;
      } catch (NumberFormatException e) {
        return false;
      }
    }
    return true;
  }

  public static String getGradlePath(String gradleVersion) throws IOException {
    File gradleHome = new File(gradleVersion);
    if (gradleHome.isAbsolute()) {
      return gradleHome.getCanonicalPath();
    }
    gradleHome = new File(new File(ourProjectRoot, TOOLS_GRADLE_PATH), gradleVersion);
    if (gradleHome.exists()) {
      return gradleHome.getCanonicalPath();
    }

    return new File(new File(ourProjectRoot, TOOLS_GRADLE_PATH_LOCAL), gradleVersion).getCanonicalPath();
  }

  @BeforeMethod
  public void checkEnvironment() throws IOException {
    if (ourProjectRoot == null) {
      ourProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
    }
    findInitScript(ourProjectRoot);
    createProjectsWorkingCopy(ourProjectRoot);
    myTestLogger.onSuiteStart();
  }


  private void createProjectsWorkingCopy(final File curDir) throws IOException {
    myTempDir = new File(myTempFiles.createTempDir(), "Name With Spaces");
    myTempDir.mkdir();
    myCoDir = myTempFiles.createTempDir();
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

    final SingleCommandLineBuildSessionFactoryAdapter adapter = new SingleCommandLineBuildSessionFactoryAdapter(new GradleRunnerServiceFactory());
    final MultiCommandBuildSession session = adapter.createSession(myMockRunner);
    final GenericCommandLineBuildProcess proc = new GenericCommandLineBuildProcess(myMockRunner, session, myMockExtensionHolder);
    proc.start();
    proc.waitFor();

    context.assertIsSatisfied();
  }

  @BeforeMethod
  public void setUp() throws Exception {
    myTeamCityConfigParameters.clear();
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
    final HashMap<String, String> propsAndVars = new HashMap<String, String>();
    final String jdk = myRunnerParams.getOrDefault(JavaRunnerConstants.TARGET_JDK_HOME, System.getProperty("java.home"));
    propsAndVars.put("system.java.home", jdk);
    final String javaHome = JavaRunnerUtil.findJavaHome(null, propsAndVars, null);
    if (javaHome != null) {
      System.out.println("Found java at [" + javaHome + "] adding as " + JavaRunnerConstants.TARGET_JDK_HOME);
      myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, javaHome);
    } else {
      System.out.println("Failed to find java home!");
    }

    final Properties configProperties = new Properties();
    configProperties.putAll(myTeamCityConfigParameters);
    final File configFile = myTempFiles.createTempFile();
    configProperties.store(new FileOutputStream(configFile), null);

    final Properties systemProperties = new Properties();
    systemProperties.putAll(myTeamCitySystemProps);
    systemProperties.put(AgentRuntimeProperties.AGENT_CONFIGURATION_PARAMS_FILE_PROP, configFile.getCanonicalPath());
    final File propertiesFile = myTempFiles.createTempFile();
    systemProperties.store(new FileOutputStream(propertiesFile), null);
    myBuildEnvVars.put("TEAMCITY_BUILD_PROPERTIES_FILE", propertiesFile.getAbsolutePath());

    final File workingDir = new File(myCoDir, projectName);

    final Expectations initMockingCtx = new Expectations() {{
      //myBuildTypeOptionValue.entrySet().forEach(entry -> {
      //  allowing(myMockBuild).getBuildTypeOptionValue(entry.getKey()); will(returnValue(entry.getValue()));
      //});
      allowing(myMockBuild).getBuildTypeOptionValue(with(any(Option.class))); will(new CustomAction("proxy Option") {
        @Override
        public Object invoke(Invocation invocation) throws Throwable {
          final Option key = (Option)invocation.getParameter(0);
          if (myBuildTypeOptionValue.containsKey(key)) {
            return myBuildTypeOptionValue.get(key);
          } else {
            return key.getDefaultValue();
          }
        }
      });
      allowing(myMockBuild).getBuildLogger(); will(returnValue(myMockLogger));
      allowing(myMockBuild).getCheckoutDirectory(); will(returnValue(myCoDir));
      allowing(myMockBuild).getBuildTempDirectory(); will(returnValue(myTempDir));
      allowing(myMockBuild).getFailBuildOnExitCode(); will(returnValue(true));
      allowing(myMockBuild).getSharedConfigParameters(); will(returnValue(Collections.<String, String>emptyMap()));
      allowing(myMockBuild).getBuildNumber(); will(returnValue("12345"));
      allowing(myMockBuild).getPasswordReplacer(); will(returnValue(new NoOpPasswordReplacer()));

      allowing(myMockRunner).getId(); will(returnValue("myRunnerId"));
      allowing(myMockRunner).getName(); will(returnValue("myRunnerName"));
      allowing(myMockRunner).getRunnerParameters(); will(returnValue(myRunnerParams));
      allowing(myMockRunner).getBuildParameters(); will(returnValue(buildParams));
      allowing(myMockRunner).getConfigParameters(); will(returnValue(Collections.<String, String>emptyMap()));
      allowing(myMockRunner).getWorkingDirectory(); will(returnValue(workingDir));
      allowing(myMockRunner).getToolPath("gradle"); will(returnValue(gradlePath));
      allowing(myMockRunner).getRunType(); will(returnValue(GradleRunnerConstants.RUNNER_TYPE));
      allowing(myMockRunner).getBuild(); will(returnValue(myMockBuild));
      allowing(myMockRunner).getRunType(); will(returnValue(GradleRunnerConstants.RUNNER_TYPE));
      allowing(myMockRunner).isVirtualContext(); will(returnValue(myVirtualContext));

      allowing(buildParams).getAllParameters(); will(returnValue(myBuildEnvVars));
      allowing(buildParams).getEnvironmentVariables(); will(returnValue(myBuildEnvVars));
      allowing(buildParams).getSystemProperties(); will(returnValue(myTeamCitySystemProps));

      allowing(myMockLogger).getFlowId();will(returnValue(flowId));
      allowing(myMockLogger).getFlowLogger(with(any(String.class)));will(returnValue(myMockLogger));
      allowing(myMockLogger).startFlow();
      allowing(myMockLogger).disposeFlow();
      allowing(myMockLogger).activityStarted(with(any(String.class)), with(any(String.class)));
      allowing(myMockLogger).logBuildProblem(with(any(BuildProblemData.class)));
      allowing(myMockLogger).activityFinished(with(any(String.class)), with(any(String.class)));

      allowing(myMockExtensionHolder).getExtensions(with(Expectations.<Class<AgentExtension>>anything())); will(returnValue(Collections.emptyList()));
    }};

    context.checking(initMockingCtx);
    return context;
  }

  protected static String getGradleVersionFromPath(@NotNull final String path) {
    return path.substring(path.lastIndexOf("gradle-") + "gradle-".length());
  }

  private class NoOpPasswordReplacer implements PasswordReplacer {
    @NotNull
    @Override
    public String replacePasswords(@NotNull final String text) {
      return text;
    }

    @Override
    public boolean hasPasswords() {
      return false;
    }

    @NotNull
    @Override
    public Set<String> getPasswords() {
      return Collections.emptySet();
    }

    @Override
    public boolean addPassword(@NotNull final String password) {
      return false;
    }
  }
}
