package jetbrains.buildServer.gradle.test.integration;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import jetbrains.TCJMockUtils;
import jetbrains.buildServer.*;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.runner.JavaRunnerUtil;
import jetbrains.buildServer.agent.runner.MultiCommandBuildSession;
import jetbrains.buildServer.agent.runner2.GenericCommandLineBuildProcess;
import jetbrains.buildServer.agent.runner2.SingleCommandLineBuildSessionFactoryAdapter;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.*;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposer;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleCommandLineComposerHolder;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleSimpleCommandLineComposer;
import jetbrains.buildServer.gradle.agent.commandLineComposers.GradleToolingApiCommandLineComposer;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleOptionValueFetcher;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildPropertiesSplitter;
import jetbrains.buildServer.gradle.agent.propertySplit.TeamCityBuildPropertiesGradleSplitter;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
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

import static jetbrains.buildServer.gradle.GradleRunnerConstants.INIT_SCRIPT_NAME;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.INIT_SCRIPT_SINCE_8_NAME;
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
  public static final String PROJECT_BROKEN_NAME = "projectBroken";
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
  public static final String PROJECT_T_NAME = "projectT";
  public static final String PROJECT_WITH_READING_PROPERTIES_NAME = "projectWithReadingProperties";
  public static final String PROJECT_WITH_READING_DYNAMIC_PROPERTIES_NAME = "projectWithReadingDynamicProperties";
  public static final String PROJECT_WITH_STATIC_PROPERTY_NAME = "projectWithStaticProperty";
  public static final String PROJECT_WITH_GENERATED_TASKS_A_NAME = "projectWithGeneratedTasksA";
  public static final String PROJECT_WITH_GENERATED_TASKS_B_NAME = "projectWithGeneratedTasksB";
  public static final String PROJECT_WITH_JDK_INTERNAL_MODULE_NAME = "projectWithJdkInternalModule";
  public static final String PROJECT_WITH_WRAPPER_PROPERTIES_NON_STANDARD_LOCATION = "projectWithWrapperPropertiesNonStardardLocation";
  protected static final String MULTI_PROJECT_A_NAME = "MultiProjectA";
  protected static final String MULTI_PROJECT_B_NAME = "MultiProjectB";
  protected static final String MULTI_PROJECT_C_NAME = "MultiProjectC";
  protected static final String MULTI_PROJECT_E_NAME = "MultiProjectE";
  protected static final String DEMAND_MULTI_PROJECT_A_NAME = "demandMultiProjectA";
  protected static final String DEMAND_MULTI_PROJECT_B_NAME = "demandMultiProjectB";
  protected static final String WRAPPED_PROJECT_A_NAME = "wrappedProjectA";
  protected static final String OPENTEST4J_PROJECT = "opentest4jProject";
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

  @DataProvider(name = "gradle-version-provider>=8")
  public static Iterator<String[]> getGradlePaths8() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "8") >= 0)
                    .iterator();
  }

  @DataProvider(name = "gradle-version-provider>=3.0")
  public static Iterator<String[]> getGradlePaths30() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "3.0") >= 0)
                    .iterator();
  }

  @DataProvider(name = "8 > gradle-version-provider >= 3.0")
  public static Iterator<String[]> getGradlePaths3080() {
    return generateGradlePaths().stream()
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "3.0") >= 0)
                    .filter(version -> VersionComparatorUtil.compare(getGradleVersionFromPath(version[0]), "8.0") < 0)
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

  protected String getGradleVersion(String gradleVersion) {
    return gradleVersion.startsWith("gradle-") ? getGradleVersionFromPath(gradleVersion) : gradleVersion;
  }

  protected File getWorkingDir(String gradleVersionNum,
                               String projectName) {
    return new File(new File(myCoDir, ConfigurationParamsUtil.getGradleInitScript(gradleVersionNum)), projectName);
  }

  @BeforeMethod
  public void checkEnvironment() throws IOException {
    if (ourProjectRoot == null) {
      ourProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
    }
    createProjectsWorkingCopy(ourProjectRoot);
    myTestLogger.onSuiteStart();
  }


  private void createProjectsWorkingCopy(final File curDir) throws IOException {
    myTempDir = new File(myTempFiles.createTempDir(), "Name With Spaces");
    myTempDir.mkdir();
    myCoDir = myTempFiles.createTempDir();
    FileUtil.copyDir(new File(curDir, "src/test/resources/testProjects"), myCoDir, true);
    assertTrue(new File(myCoDir, INIT_SCRIPT_NAME + "/" + PROJECT_A_NAME + "/build.gradle").canRead(), "Failed to copy test projects.");
    assertTrue(new File(myCoDir, INIT_SCRIPT_SINCE_8_NAME + "/" + PROJECT_A_NAME + "/build.gradle").canRead(), "Failed to copy test projects.");
  }

  private void setupInitScripts(File projectRoot) throws IOException {
    File sourceScriptsDir = new File(projectRoot, GradleTestUtil.REL_SCRIPT_DIR);
    File pluginsDir = myMockBuild.getAgentConfiguration().getAgentPluginsDirectory().getAbsoluteFile();
    File gradleRunnerPluginDir = new File(pluginsDir, GradleRunnerConstants.RUNNER_TYPE);
    File targetScriptsDir = new File(gradleRunnerPluginDir, GradleRunnerConstants.INIT_SCRIPT_DIR);
    //noinspection ResultOfMethodCallIgnored
    targetScriptsDir.mkdirs();

    FileUtil.copyDir(sourceScriptsDir, targetScriptsDir);

    File consoleInitScript = new File(targetScriptsDir, INIT_SCRIPT_NAME);
    File tapiInitScript = new File(targetScriptsDir, INIT_SCRIPT_SINCE_8_NAME);
    assertTrue(consoleInitScript.canRead(), "The command-line-gradle init script must be an existing readable file.");
    assertTrue(tapiInitScript.canRead(), "The tooling-api-gradle init script must be an existing readable file.");
  }

  @AfterMethod
  public void tearDownClass() {
    myTempFiles.cleanup();
  }

  protected void runTest(final Expectations expectations, final Mockery context) throws RunBuildException {
    context.checking(expectations);

    List<GradleBuildPropertiesSplitter> splitters = Arrays.asList(new TeamCityBuildPropertiesGradleSplitter());
    GradleTasksComposer tasksComposer = new GradleTasksComposer(Collections.emptyList());
    List<GradleCommandLineComposer> composers = Arrays.asList(
      new GradleSimpleCommandLineComposer(tasksComposer), new GradleToolingApiCommandLineComposer(splitters, tasksComposer)
    );
    GradleCommandLineComposerHolder composerHolder = new GradleCommandLineComposerHolder(composers);

    final SingleCommandLineBuildSessionFactoryAdapter adapter = new SingleCommandLineBuildSessionFactoryAdapter(new GradleRunnerServiceFactory(
        composerHolder,
        tasksComposer,
        new GradleLaunchModeSelector(),
        new GradleConfigurationCacheDetector(new GradleOptionValueFetcher()),
        new CommandLineParametersProcessor(),
        new GradleVersionDetector(),
        new GradleUserHomeManager()
    ));

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
    myTeamCitySystemProps.put("teamcity.build.tempDir", myTempDir.getAbsolutePath());
    Mockery context = TCJMockUtils.createInstance();

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

    String gradleVersionNum = getGradleVersion(gradleVersion);
    if (VersionComparatorUtil.compare(gradleVersionNum, "8.0") >= 0 && !myTeamCityConfigParameters.containsKey(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM)) {
      myTeamCityConfigParameters.put(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM, GradleRunnerConstants.GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE);
    }

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
    final File configFile = new File(myTempDir, "teamcity.config.parameters");
    try(final OutputStream out = Files.newOutputStream(configFile.toPath())) {
      configProperties.store(out, null);
    }

    final Properties systemProperties = new Properties();
    systemProperties.putAll(myTeamCitySystemProps);
    systemProperties.put(AgentRuntimeProperties.AGENT_CONFIGURATION_PARAMS_FILE_PROP, configFile.getCanonicalPath());
    final File propertiesFile = new File(myTempDir, "teamcity.build.parameters");
    try(final OutputStream out = Files.newOutputStream(propertiesFile.toPath())) {
      systemProperties.store(out, null);
    }
    myBuildEnvVars.put(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV, propertiesFile.getAbsolutePath());
    System.setProperty(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_PROP, propertiesFile.getAbsolutePath());

    final File workingDir = getWorkingDir(gradleVersionNum, projectName);

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
      allowing(myMockRunner).getConfigParameters(); will(returnValue(myTeamCityConfigParameters));
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
      allowing(myMockLogger).ignoreServiceMessages(with(any(Runnable.class)));

      allowing(myMockExtensionHolder).getExtensions(with(Expectations.<Class<AgentExtension>>anything())); will(returnValue(Collections.emptyList()));

      final BuildAgentConfiguration agentConfiguration = context.mock(BuildAgentConfiguration.class);
      final File agentPluginDir = myTempFiles.createTempDir();
      allowing(myMockBuild).getAgentConfiguration(); will(returnValue(agentConfiguration));
      allowing(agentConfiguration).getAgentPluginsDirectory(); will(returnValue(agentPluginDir));
    }};

    context.checking(initMockingCtx);

    setupInitScripts(ourProjectRoot);

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

    @Override
    public void addPasswordsFilter(@NotNull String filterName, @NotNull Function<String, String> filter) {
    }

    @Override
    public boolean removePasswordsFilter(@NotNull String filterName) {
      return false;
    }
  }
}