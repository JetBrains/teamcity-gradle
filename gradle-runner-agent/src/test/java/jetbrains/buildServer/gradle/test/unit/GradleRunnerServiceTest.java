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

package jetbrains.buildServer.gradle.test.unit;

import com.intellij.openapi.util.SystemInfo;
import java.io.File;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.JavaRunnerUtil;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.*;
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

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;
import static org.assertj.core.api.BDDAssertions.then;
import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/1/10
 */
public class GradleRunnerServiceTest {
  private Mockery myContext;

  protected TempFiles myTempFiles = new TempFiles();
  protected final Map<String, String> myRunnerParams = new HashMap<>();
  protected final Map<String, String> myBuildParams = new HashMap<>();
  protected final Map<String, String> myEnvVars = new HashMap<>();
  protected final Map<String, String> mySystemProps = new HashMap<>();
  protected final Map<String, String> myConfigParameters = new HashMap<>();
  private final Map<String, String> toolingApiLauncherFiles = new HashMap<>();
  protected BuildRunnerContext myRunnerContext;
  protected AgentRunningBuild myBuild;
  protected GradleRunnerService myService;
  protected File myGradleExe;
  protected File myWorkingDirectory;
  protected File myInitScript;
  private File myTempDir;
  private File myCoDir;
  private String javaHome;
  private List<String> toolingApiGradleArgs = Collections.emptyList();
  private List<String> toolingApiJvmGradleArgs = Collections.emptyList();
  private Map<String, String> toolingApiGradleEnvParameters = Collections.emptyMap();
  private List<String> toolingApiGradleTasks = Collections.emptyList();


  @BeforeMethod
  public void setUp() throws Exception {
    myTempDir = myTempFiles.createTempDir();
    myContext = new Mockery();

    myRunnerContext = myContext.mock(BuildRunnerContext.class);
    myBuild = myContext.mock(AgentRunningBuild.class);
    final BuildParametersMap myBuildPrarams = myContext.mock(BuildParametersMap.class);

    myContext.checking(new Expectations() {{
      allowing(myBuild).getBuildLogger();
      allowing(myBuild).getBuildTempDirectory();          will(returnValue(myTempDir));
      allowing(myRunnerContext).getRunnerParameters();    will(returnValue(myRunnerParams));
      allowing(myRunnerContext).getBuildParameters();     will(returnValue(myBuildPrarams));
      allowing(myRunnerContext).isVirtualContext();       will(returnValue(false));
      allowing(myBuildPrarams).getAllParameters();        will(returnValue(myBuildParams));
      allowing(myBuildPrarams).getEnvironmentVariables(); will(returnValue(myEnvVars));
      allowing(myBuildPrarams).getSystemProperties();     will(returnValue(mySystemProps));
      allowing(myRunnerContext).getConfigParameters();    will(returnValue(myConfigParameters));
      allowing(myBuild).getBuildTypeOptionValue(with(any(Option.class))); will(new CustomAction("proxy Option") {
        @Override
        public Object invoke(Invocation invocation) {
          return ((Option)invocation.getParameter(0)).getDefaultValue();
        }
      });
    }});
    myService = (GradleRunnerService) new GradleRunnerServiceFactory(Collections.emptyList()).createService();

    myCoDir = myTempFiles.createTempDir();

    final HashMap<String, String> propsAndVars = new HashMap<String, String>();
    final String jdk = myRunnerParams.getOrDefault(JavaRunnerConstants.TARGET_JDK_HOME, System.getProperty("java.home"));
    propsAndVars.put("system.java.home", jdk);
    javaHome = JavaRunnerUtil.findJavaHome(jdk, propsAndVars, null);
    myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, javaHome);

    toolingApiLauncherFiles.put(GRADLE_LAUNCHER_ENV_FILE_ENV_KEY, myTempDir.getAbsolutePath() + File.separator + GRADLE_LAUNCHER_ENV_FILE);
    toolingApiLauncherFiles.put(GRADLE_PARAMS_FILE_ENV_KEY, myTempDir.getAbsolutePath() + File.separator + GRADLE_PARAMS_FILE);
    toolingApiLauncherFiles.put(GRADLE_JVM_PARAMS_FILE_ENV_KEY, myTempDir.getAbsolutePath() + File.separator + GRADLE_JVM_PARAMS_FILE);
    toolingApiLauncherFiles.put(GRADLE_TASKS_FILE_ENV_KEY, myTempDir.getAbsolutePath() + File.separator + GRADLE_TASKS_FILE);
  }

  @AfterMethod
  public void tearDown() {
    myTempFiles.cleanup();
    myRunnerParams.clear();
    myBuildParams.clear();
    myEnvVars.clear();
    mySystemProps.clear();
    toolingApiGradleArgs.clear();
    toolingApiJvmGradleArgs.clear();
    toolingApiGradleEnvParameters.clear();
    toolingApiGradleTasks.clear();
  }

  @DataProvider(name = "gradle-version-provider")
  public static String[][] getGradlePaths() {
    return new String[][] {
      {"old"},
      {"8.2"}
    };
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateSimpleCommandLineTest(String gradleVersion) throws Exception {
    prepareGradleRequiredFiles(gradleVersion);

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, true);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
    }

    reportCmdLine(cmdLine);
  }

  @DataProvider(name = "enable daemon")
  public static Object[][] enableDaemonParam() {
    return new Object[][] {
      {"old", "--daemon"},
      {"8.2", "--daemon"},
      {"old", "-Dorg.gradle.daemon=true"},
      {"8.2", "-Dorg.gradle.daemon=true"}
    };
  }

  @Test(dataProvider = "enable daemon")
  public void generateWithDaemonCommandLineTest(String gradleVersion, String param) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, param);
    prepareGradleRequiredFiles(gradleVersion);

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    List<String> gradleArguments;

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, false);
      gradleArguments = new ArrayList<>(toolingApiGradleArgs);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
      gradleArguments = cmdLine.getArguments();
    }

    assertTrue(gradleArguments.contains(param), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain " + param);
    assertFalse(gradleArguments.contains("-Dorg.gradle.daemon=false"), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain '-Dorg.gradle.daemon=false'");

    reportCmdLine(cmdLine);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateWithoutDaemonCommandLineTest(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "--no-daemon");
    prepareGradleRequiredFiles(gradleVersion);

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    List<String> gradleArguments;

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, false);
      gradleArguments = new ArrayList<>(toolingApiGradleArgs);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
      gradleArguments = cmdLine.getArguments();
    }

    assertTrue(gradleArguments.contains("--no-daemon"), Arrays.toString(cmdLine.getArguments().toArray()) + " must contain '--no-daemon'");
    assertFalse(gradleArguments.contains("-Dorg.gradle.daemon=false"),
                Arrays.toString(cmdLine.getArguments().toArray()) + " should not contain '-Dorg.gradle.daemon=false'");
    reportCmdLine(cmdLine);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void generateWithoutDaemonDuplicateParamCommandLineTest(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "-Dorg.gradle.daemon=false");
    prepareGradleRequiredFiles(gradleVersion);

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    List<String> gradleArguments;

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, false);
      gradleArguments = new ArrayList<>(toolingApiGradleArgs);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
      gradleArguments = cmdLine.getArguments();
    }

    assertEquals(Collections.frequency(gradleArguments, "-Dorg.gradle.daemon=false"), 1);

    reportCmdLine(cmdLine);
  }

  @Test
  public void generateCLwithJavaHome() throws Exception {
    String gradleVersion = "old";
    final String expectedJavaHome = myTempFiles.createTempDir().getAbsolutePath();
    myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, expectedJavaHome);

    prepareGradleRequiredFiles(gradleVersion);
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
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
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
    String gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, true);
      assertTrue(toolingApiJvmGradleArgs.contains(expectedRunnerGradleOpts), "Wrong Java arguments.");
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
      assertTrue(gradleOptsValue.contains(expectedRunnerGradleOpts), "Wrong Java arguments.");
    }

    myRunnerParams.put(JavaRunnerConstants.JVM_ARGS_KEY, expectedRunnerJavaArgs);
    myService.initialize(myBuild,myRunnerContext);
    cmdLine = myService.makeProgramCommandLine();

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, true);
      gradleOptsValue = toolingApiGradleEnvParameters.get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
      gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    }

    then(gradleOptsValue.split(" ")).as("Should contain new temp dir").contains("\"-Djava.io.tmpdir=" + myTempDir.getCanonicalPath() + "\"")
                                    .as("Should contain java args").contains(expectedRunnerJavaArgs);
  }

  @Test(dataProvider = "gradle-version-provider")
  @TestFor(issues = "TW-57278")
  public void test_spaces_in_tasks_args(String gradleVersion) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_TASKS, "run --args=\"foo -bar this\"");

    prepareGradleRequiredFiles(gradleVersion);

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    List<String> gradleTasks;
    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, false);
      gradleTasks = new ArrayList<>(toolingApiGradleTasks);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
      gradleTasks = cmdLine.getArguments();
    }

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
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    List<String> args;

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, true);
      args = new ArrayList<>(toolingApiGradleArgs);
      args.addAll(toolingApiGradleTasks);
    } else {
      validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
      args = cmdLine.getArguments();
    }

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
    if (SystemInfo.isWindows) {
      gradlew = new File(myWorkingDirectory,GradleRunnerServiceFactory.WIN_GRADLEW);
    } else if (SystemInfo.isUnix) {
      gradlew = new File(myWorkingDirectory,GradleRunnerServiceFactory.UNIX_GRADLEW);
    }

    assert null != gradlew;
    gradlew.createNewFile();
    assertTrue(gradlew.exists(), "Could not create gradleW mock file.");

    File gradleWrapperProperties = new File(myWorkingDirectory, GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION);
    gradleWrapperProperties.mkdirs();
    gradleWrapperProperties.createNewFile();
    assertTrue(gradleWrapperProperties.exists(), "Could not create gradleWrapperProperties mock file.");

    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    if (VersionComparatorUtil.compare(gradleVersion, "8") >= 0) {
      validateCmdLineSince8(cmdLine, true);
    } else {
      validateCmdLine(cmdLine, gradlew.getAbsolutePath(), true);
    }
  }


  @Test(expectedExceptions = RunBuildException.class)
  public void gradleHomeDoesNotExistTest() throws RunBuildException {

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath("gradle"); will(returnValue(""));
      allowing(myRunnerContext).getWorkingDirectory(); will(returnValue(myTempDir));
    }});

    myService.initialize(myBuild, myRunnerContext);
    myService.makeProgramCommandLine();
  }


  @Test(expectedExceptions = RunBuildException.class)
  public void gradleExeDoesNotExistTest() throws RunBuildException, IOException {
    GradleRunnerService service = (GradleRunnerService) new GradleRunnerServiceFactory(Collections.emptyList()).createService();

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath("gradle"); will(returnValue(myTempFiles.createTempDir().getAbsolutePath()));
      allowing(myRunnerContext).getWorkingDirectory(); will(returnValue(myTempDir));
    }});

    service.initialize(myBuild, myRunnerContext);
    service.makeProgramCommandLine();
  }


  private void prepareGradleRequiredFiles(String gradleVersion) throws IOException {
    final File gradleToolDir = myTempFiles.createTempDir();
    final File agentPluginDir = myTempFiles.createTempDir();
    myWorkingDirectory = myTempFiles.createTempDir();
    myInitScript = new File(agentPluginDir, GradleRunnerConstants.RUNNER_TYPE
                                            + "/" + GradleRunnerConstants.INIT_SCRIPT_DIR
                                            + "/" + ConfigurationParamsUtil.getGradleInitScript(gradleVersion));

    myConfigParameters.put(GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM,
                           VersionComparatorUtil.compare(gradleVersion, "8") >= 0 ? GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE : GRADLE_RUNNER_GRADLE_LAUNCH_MODE);

    myGradleExe = new File(gradleToolDir, GradleRunnerServiceFactory.WIN_GRADLE_EXE);
    if (SystemInfo.isUnix) {
      myGradleExe = new File(gradleToolDir, GradleRunnerServiceFactory.UNIX_GRADLE_EXE);
    }
    myGradleExe.mkdirs();

    final BuildAgentConfiguration agentConfiguration = myContext.mock(BuildAgentConfiguration.class);

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath(GradleToolProvider.GRADLE_TOOL); will(returnValue(gradleToolDir.getAbsolutePath()));
      allowing(myRunnerContext).getWorkingDirectory(); will(returnValue(myWorkingDirectory));
      allowing(myBuild).getAgentConfiguration(); will(returnValue(agentConfiguration));
      allowing(agentConfiguration).getAgentPluginsDirectory(); will(returnValue(agentPluginDir));
      allowing(myBuild).getCheckoutDirectory(); will(returnValue(myCoDir));
    }});
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

    if (SystemInfo.isWindows) {
      assertEquals(cmdLine.getExecutablePath(), exePath, "Wrong Gradle executable path.");
    } else if (SystemInfo.isUnix) {
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

  private void validateCmdLineSince8(final ProgramCommandLine cmdLine, boolean isTestDaemon) throws Exception {
    final String workDir = myWorkingDirectory.getAbsolutePath();
    final String initScriptPath = myInitScript.getAbsolutePath();
    StringBuilder javaHomeBuilder = new StringBuilder();
    javaHomeBuilder.append(javaHome).append(File.separator)
                  .append("bin").append(File.separator)
                  .append("java");

    if (SystemInfo.isWindows) {
      javaHomeBuilder.append(".exe");
    }

    assertEquals(cmdLine.getExecutablePath(), javaHomeBuilder.toString(), "Gradle Tooling API startup script must be executed by separate Java process");
    assertEquals(cmdLine.getWorkingDirectory(), workDir, "Wrong working directory.");

    File gradleLauncherEnvFile = new File(myTempDir, GRADLE_LAUNCHER_ENV_FILE);
    File gradleParamsFile = new File(myTempDir, GRADLE_PARAMS_FILE);
    File gradleJvmParamsFile = new File(myTempDir, GRADLE_JVM_PARAMS_FILE);
    File gradleTasksFile = new File(myTempDir, GRADLE_TASKS_FILE);
    assertTrue(gradleLauncherEnvFile.exists(), "Gradle Tooling API launcher environment file must exist");
    assertTrue(gradleParamsFile.exists(), "Gradle Tooling API gradle params file must exist");
    assertTrue(gradleJvmParamsFile.exists(), "Gradle Tooling API JVM params file must exist");
    assertTrue(gradleTasksFile.exists(), "Gradle Tooling API gradle tasks file must exist");

    toolingApiGradleArgs = GradleRunnerFileUtil.readParams(toolingApiLauncherFiles.get(GradleRunnerConstants.GRADLE_PARAMS_FILE_ENV_KEY));
    int initScriptIndex = toolingApiGradleArgs.indexOf("--init-script");
    assertTrue(initScriptIndex > -1, "--init-script argument not found!");
    assertEquals(toolingApiGradleArgs.get(initScriptIndex + 1), initScriptPath, "Wrong init script path");
    if (isTestDaemon) {
      assertTrue(toolingApiGradleArgs.contains("-Dorg.gradle.daemon=false"), "Gradle daemon should be disabled");
    }

    toolingApiJvmGradleArgs = GradleRunnerFileUtil.readParams(toolingApiLauncherFiles.get(GradleRunnerConstants.GRADLE_JVM_PARAMS_FILE_ENV_KEY));
    toolingApiGradleEnvParameters = GradleRunnerFileUtil.readParamsMap(toolingApiLauncherFiles.get(GradleRunnerConstants.GRADLE_LAUNCHER_ENV_FILE_ENV_KEY));
    toolingApiGradleTasks = GradleRunnerFileUtil.readParams(toolingApiLauncherFiles.get(GradleRunnerConstants.GRADLE_TASKS_FILE_ENV_KEY));
  }
}
