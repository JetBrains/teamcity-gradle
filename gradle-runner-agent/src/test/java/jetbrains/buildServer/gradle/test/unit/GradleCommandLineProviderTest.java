package jetbrains.buildServer.gradle.test.unit;

import com.intellij.openapi.util.TCSystemInfo;
import jetbrains.TCJMockUtils;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.JavaRunnerUtil;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleLaunchModeSelector;
import jetbrains.buildServer.gradle.agent.GradleRunnerContext;
import jetbrains.buildServer.gradle.agent.GradleToolProvider;
import jetbrains.buildServer.gradle.agent.GradleUserHomeManager;
import jetbrains.buildServer.gradle.agent.commandLine.CommandLineParametersProcessor;
import jetbrains.buildServer.gradle.agent.commandLineComposers.*;
import jetbrains.buildServer.gradle.agent.gradleExecution.GradleCommandLineProvider;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleConfigurationCacheDetector;
import jetbrains.buildServer.gradle.agent.gradleOptions.GradleOptionValueFetcher;
import jetbrains.buildServer.gradle.agent.obsolete.GradleConnectorProvider;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.Option;
import jetbrains.buildServer.util.TestFor;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Invocation;
import org.jmock.lib.action.CustomAction;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/1/10
 */
public class GradleCommandLineProviderTest {
  private Mockery myContext;

  protected TempFiles myTempFiles = new TempFiles();
  protected final Map<String, String> myRunnerParams = new HashMap<>();
  protected final Map<String, String> myBuildParams = new HashMap<>();
  protected final Map<String, String> myEnvVars = new HashMap<>();
  protected final Map<String, String> mySystemProps = new HashMap<>();
  protected final Map<String, String> myConfigParameters = new HashMap<>();
  protected BuildRunnerContext myRunnerContext;
  protected AgentRunningBuild myBuild;
  protected File myGradleExe;
  protected File myWorkingDirectory;
  protected File myInitScript;
  private File myTempDir;
  private File myCoDir;
  private String javaHome;
  private GradleCommandLineComposerHolder myComposerHolder;
  private GradleTasksComposer myTasksComposer;
  private GradleLaunchModeSelector myLaunchModeSelector;
  private GradleConfigurationCacheDetector myConfigurationCacheDetector;
  private CommandLineParametersProcessor myCommandLineParametersProcessor;
  private GradleUserHomeManager myGradleUserHomeManager;
  private boolean myIsVirtualContext;

  @BeforeMethod
  public void setUp() throws Exception {
    myTempDir = myTempFiles.createTempDir();
    myContext = TCJMockUtils.createInstance();
    myIsVirtualContext = false;

    myRunnerContext = myContext.mock(BuildRunnerContext.class);
    myBuild = myContext.mock(AgentRunningBuild.class);
    final BuildParametersMap myBuildPrarams = myContext.mock(BuildParametersMap.class);

    myContext.checking(new Expectations() {{
      allowing(myBuild).getBuildLogger();

      allowing(myBuild).getBuildTempDirectory();
      will(returnValue(myTempDir));

      allowing(myRunnerContext).getRunnerParameters();
      will(returnValue(myRunnerParams));

      allowing(myRunnerContext).getBuildParameters();
      will(returnValue(myBuildPrarams));

      allowing(myRunnerContext).getBuild();
      will(returnValue(myBuild));

      allowing(myRunnerContext).getId();
      will(returnValue("myBuildPrarams"));

      allowing(myRunnerContext).isVirtualContext();
      will(new CustomAction("virtual context value") {
        @Override
        public Object invoke(Invocation invocation) {
          return myIsVirtualContext;
        }
      });

      allowing(myBuildPrarams).getAllParameters();
      will(returnValue(myBuildParams));

      allowing(myBuildPrarams).getEnvironmentVariables();
      will(returnValue(myEnvVars));

      allowing(myBuildPrarams).getSystemProperties();
      will(returnValue(mySystemProps));

      allowing(myRunnerContext).getConfigParameters();
      will(returnValue(myConfigParameters));

      allowing(myBuild).getBuildTypeOptionValue(with(any(Option.class)));

      will(new CustomAction("proxy Option") {
        @Override
        public Object invoke(Invocation invocation) {
          return ((Option)invocation.getParameter(0)).getDefaultValue();
        }
      });
    }});

    myTasksComposer = new GradleTasksComposer(Collections.emptyList());
    List<GradleCommandLineComposer> composers = Arrays.asList(
      new GradleCliCommandLineComposer(myTasksComposer),
      new GradleCliV2CommandLineComposer(myTasksComposer),
      new GradleToolingApiCommandLineComposer(Collections.emptyList(), myTasksComposer)
    );
    myComposerHolder = new GradleCommandLineComposerHolder(composers);
    myLaunchModeSelector = new GradleLaunchModeSelector();
    myConfigurationCacheDetector = new GradleConfigurationCacheDetector(new GradleOptionValueFetcher());
    myCommandLineParametersProcessor = new CommandLineParametersProcessor();
    myGradleUserHomeManager = new GradleUserHomeManager();

    myCoDir = myTempFiles.createTempDir();

    final HashMap<String, String> propsAndVars = new HashMap<String, String>();
    final String jdk = myRunnerParams.getOrDefault(JavaRunnerConstants.TARGET_JDK_HOME, System.getProperty("java.home"));
    propsAndVars.put("system.java.home", jdk);
    javaHome = JavaRunnerUtil.findJavaHome(jdk, propsAndVars, null);
    myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, javaHome);

  }

  @AfterMethod
  public void tearDown() {
    myTempFiles.cleanup();
    myRunnerParams.clear();
    myBuildParams.clear();
    myEnvVars.clear();
    mySystemProps.clear();
  }

  private ProgramCommandLine getGradleCommandLine() {
    GradleRunnerContext gradleRunnerContext = new GradleRunnerContext(myRunnerContext);
    GradleCommandLineProvider provider = createGradleCommandLineProvider(gradleRunnerContext);
    return provider.getGradleCommandLine(TCSystemInfo.isUnix, null, new GradleConnectorProvider(gradleRunnerContext));
  }

  @DataProvider(name = "gradle-version-provider")
  public static String[][] getGradlePaths() {
    return new String[][]{
      {"old"},
      {"8.0"},
      {"8.1"},
      {"9.4.1"}
    };
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateSimpleCommandLineTest(String gradleVersion) throws Exception {
    prepareGradleRequiredFiles(gradleVersion);

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);

    reportCmdLine(cmdLine);
  }

  @DataProvider(name = "enable daemon")
  public static Object[][] enableDaemonParam() {
    return new Object[][]{
      {"old", "--daemon"},
      {"8.0", "--daemon"},
      {"8.1", "--daemon"},
      {"9.4.1", "--daemon"},
      {"old", "-Dorg.gradle.daemon=true"},
      {"8.0", "-Dorg.gradle.daemon=true"},
      {"8.1", "-Dorg.gradle.daemon=true"},
      {"9.4.1", "-Dorg.gradle.daemon=true"}
    };
  }

  @Test(dataProvider = "enable daemon")
  public void generateWithDaemonCommandLineTest(String gradleVersion, String param) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, param);
    prepareGradleRequiredFiles(gradleVersion);

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    List<String> gradleArguments = cmdLine.getArguments();

    assertTrue(gradleArguments.contains(param), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain " + param);
    assertFalse(gradleArguments.contains("-Dorg.gradle.daemon=false"), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain '-Dorg.gradle.daemon=false'");

    reportCmdLine(cmdLine);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateWithoutDaemonCommandLineTest(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "--no-daemon");
    prepareGradleRequiredFiles(gradleVersion);

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    List<String> gradleArguments = cmdLine.getArguments();

    assertTrue(gradleArguments.contains("--no-daemon"), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain '--no-daemon'");
    assertFalse(gradleArguments.contains("-Dorg.gradle.daemon=false"),
                Arrays.toString(cmdLine.getArguments().toArray()) + " should not contain '-Dorg.gradle.daemon=false'");
    reportCmdLine(cmdLine);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateWithoutDaemonDuplicateParamCommandLineTest(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "-Dorg.gradle.daemon=false");
    prepareGradleRequiredFiles(gradleVersion);

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    List<String> gradleArguments = cmdLine.getArguments();

    assertEquals(Collections.frequency(gradleArguments, "-Dorg.gradle.daemon=false"), 1);

    reportCmdLine(cmdLine);
  }

  @Test
  public void generateCLwithJavaHome() throws Exception {
    String gradleVersion = "old";
    final String expectedJavaHome = myTempFiles.createTempDir().getAbsolutePath();
    myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, expectedJavaHome);

    prepareGradleRequiredFiles(gradleVersion);
    ProgramCommandLine cmdLine = getGradleCommandLine();
    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);

    String actualJavaHome = cmdLine.getEnvironment().get(JavaRunnerConstants.JAVA_HOME);
    assertEquals(actualJavaHome, expectedJavaHome, "Wrong Java Home environment variable.");
  }

  @Test(dataProvider = "gradle-version-provider")
  public void testCLGradleOpts(String gradleVersion) throws Exception {
    final String expectedRunnerGradleOpts = "-DrunnerGradleOpt";
    final String expectedRunnerJavaArgs = "-DrunnerJavaArg";

    myRunnerParams.put(GradleRunnerConstants.ENV_GRADLE_OPTS, expectedRunnerGradleOpts);

    prepareGradleRequiredFiles(gradleVersion);
    ProgramCommandLine cmdLine = getGradleCommandLine();
    String gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
    assertTrue(gradleOptsValue.contains(expectedRunnerGradleOpts), "Wrong Java arguments.");

    myRunnerParams.put(JavaRunnerConstants.JVM_ARGS_KEY, expectedRunnerJavaArgs);
    cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
    gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);

    then(gradleOptsValue.split(" ")).as("Should contain new temp dir").contains("\"-Djava.io.tmpdir=" + myTempDir.getCanonicalPath() + "\"")
                                    .as("Should contain java args").contains(expectedRunnerJavaArgs);
  }

  @Test(dataProvider = "gradle-version-provider")
  @TestFor(issues = "TW-57278")
  public void test_spaces_in_tasks_args(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_TASKS, "run --args=\"foo -bar this\"");

    prepareGradleRequiredFiles(gradleVersion);

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    List<String> gradleTasks = cmdLine.getArguments();

    then(gradleTasks).doesNotContain("-bar", "this\"", "this").contains("--args=\"foo -bar this\"");

    reportCmdLine(cmdLine);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateCLwithGradleParameters(String gradleVersion) throws Exception {
    final String gradleCmds = "clean build test";
    String[] cmdsArray = gradleCmds.split(" ");
    final String gradleArgs = "-arg1 -arg2 -arg3";
    String[] argsArray = gradleArgs.split(" ");
    String stackTraceFlag = "-s";
    String debugFlag = "-d";

    myRunnerParams.put(GradleRunnerConstants.GRADLE_TASKS, gradleCmds);
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, gradleArgs);
    myRunnerParams.put(GradleRunnerConstants.STACKTRACE, Boolean.TRUE.toString());
    myRunnerParams.put(GradleRunnerConstants.DEBUG, Boolean.TRUE.toString());

    prepareGradleRequiredFiles(gradleVersion);
    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
    List<String> args = cmdLine.getArguments();

    int paramsIndex = args.indexOf(argsArray[0]);
    int cmdIndex = args.indexOf(cmdsArray[0]);
    int stackTraceFlagIndex = args.indexOf(stackTraceFlag);
    int debugFlagIndex = args.indexOf(debugFlag);

    assertTrue(paramsIndex > -1, "Gradle parameters missing on the command line");
    assertTrue(cmdIndex > -1, "Gradle tasks missing on the command line");
    assertTrue(stackTraceFlagIndex > -1, "Stack trace flag is missing on the command line");
    assertTrue(debugFlagIndex > -1, "Debug flag is missing on the command line");

    assertEquals(args.size() - cmdsArray.length, cmdIndex, "Wrong Gradle tasks position. Tasks must last on cmd line.");

    for (String task : cmdsArray) {
      assertTrue(args.contains(task), "Gradle task [" + task + "] missing on the command line");
    }

    for (String param : argsArray) {
      assertTrue(args.contains(param), "Gradle parameter [" + param + "] missing on the command line");
    }
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateWrapperCL(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    prepareGradleRequiredFiles(gradleVersion);

    File gradlew = null;
    if (TCSystemInfo.isWindows) {
      gradlew = new File(myWorkingDirectory, GradleRunnerContext.WIN_GRADLEW);
    } else if (TCSystemInfo.isUnix) {
      gradlew = new File(myWorkingDirectory, GradleRunnerContext.UNIX_GRADLEW);
    }

    assert null != gradlew;
    gradlew.createNewFile();
    assertTrue(gradlew.exists(), "Could not create gradleW mock file.");

    File gradleWrapperPropertiesDir = new File(myWorkingDirectory, "gradle" + File.separator + "wrapper");
    gradleWrapperPropertiesDir.mkdirs();
    File gradleWrapperProperties = new File(gradleWrapperPropertiesDir, "gradle-wrapper.properties");
    gradleWrapperProperties.createNewFile();
    assertTrue(gradleWrapperProperties.exists(), "Could not create gradleWrapperProperties mock file.");

    ProgramCommandLine cmdLine = getGradleCommandLine();

    validateCmdLine(cmdLine, gradlew.getAbsolutePath(), true);
  }

  private GradleCommandLineProvider createGradleCommandLineProvider(GradleRunnerContext gradleRunnerContext) {
    return new GradleCommandLineProvider(
      gradleRunnerContext,
      myComposerHolder,
      myTasksComposer,
      myLaunchModeSelector,
      myConfigurationCacheDetector,
      myCommandLineParametersProcessor,
      myGradleUserHomeManager
    );
  }


  private void prepareGradleRequiredFiles(String gradleVersion) throws IOException {
    final File gradleToolDir = myTempFiles.createTempDir();
    final File agentPluginDir = myTempFiles.createTempDir();
    myWorkingDirectory = myTempFiles.createTempDir();
    myInitScript = new File(agentPluginDir, GradleRunnerConstants.RUNNER_TYPE
                                            + "/" + GradleRunnerConstants.INIT_SCRIPT_DIR
                                            + "/" + getDefaultInitScriptName(gradleVersion));

    myConfigParameters.put(GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM,
                           VersionComparatorUtil.compare(gradleVersion, "8.1") >= 0 ? GRADLE_RUNNER_COMMAND_LINE_V2_LAUNCH_MODE : GRADLE_RUNNER_COMMAND_LINE_LAUNCH_MODE);

    myGradleExe = new File(gradleToolDir, GradleRunnerContext.WIN_GRADLE_EXE);
    if (TCSystemInfo.isUnix) {
      myGradleExe = new File(gradleToolDir, GradleRunnerContext.UNIX_GRADLE_EXE);
    }
    myGradleExe.mkdirs();

    final BuildAgentConfiguration agentConfiguration = myContext.mock(BuildAgentConfiguration.class);

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath(GradleToolProvider.GRADLE_TOOL);
      will(returnValue(gradleToolDir.getAbsolutePath()));

      allowing(myRunnerContext).getWorkingDirectory();
      will(returnValue(myWorkingDirectory));

      allowing(myBuild).getAgentConfiguration();
      will(returnValue(agentConfiguration));

      allowing(agentConfiguration).getAgentPluginsDirectory();
      will(returnValue(agentPluginDir));

      allowing(myBuild).getCheckoutDirectory();
      will(returnValue(myCoDir));
    }});
  }

  private static String getDefaultInitScriptName(String gradleVersion) {
    return VersionComparatorUtil.compare(gradleVersion, "8.1") >= 0 ? INIT_SCRIPT_V2_NAME : INIT_SCRIPT_NAME;
  }

  private void reportCmdLine(final ProgramCommandLine cmdLine) throws RunBuildException {
    Reporter.log("Exe path : " + cmdLine.getExecutablePath(), true);
    Reporter.log("Working Directory : " + cmdLine.getWorkingDirectory(), true);
    Reporter.log("Args : " + cmdLine.getArguments(), true);
    Reporter.log("Env : " + cmdLine.getEnvironment(), true);
  }

  private void validateCmdLine(final ProgramCommandLine cmdLine, final String exePath, boolean isTestDaemon) throws Exception {
    final String workDir = myWorkingDirectory.getAbsolutePath();
    final String initScriptPath = myInitScript.getAbsolutePath();
    final List<String> args = cmdLine.getArguments();

    if (TCSystemInfo.isWindows) {
      assertEquals(cmdLine.getExecutablePath(), exePath, "Wrong Gradle executable path.");
    } else if (TCSystemInfo.isUnix) {
      assertEquals(cmdLine.getExecutablePath(), "bash", "Gradle startup script must be executed by explicit bash call.");
      assertEquals(args.get(0), exePath, "Wrong Gradle startup script path.");
    } else {
      fail("OS is not supported. This test must be started under Windows, Linux or *nix OS.");
    }

    assertEquals(cmdLine.getWorkingDirectory(), workDir, "Wrong working directory.");
    int initScriptIndex = args.indexOf("--init-script");
    assertTrue(initScriptIndex > -1, "--init-script argument not found!");
    assertEquals(args.get(initScriptIndex + 1), initScriptPath, "Wrong init script path");
    if (isTestDaemon) {
      assertTrue(args.contains("-Dorg.gradle.daemon=false"), "Gradle daemon should be disabled");
    }
  }

}
