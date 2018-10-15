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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.TempFiles;
import jetbrains.buildServer.agent.AgentRunningBuild;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.BuildParametersMap;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.agent.runner.ProgramCommandLine;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerService;
import jetbrains.buildServer.gradle.agent.GradleRunnerServiceFactory;
import jetbrains.buildServer.gradle.agent.GradleToolProvider;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.TestFor;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.Reporter;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.BDDAssertions.then;
import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/1/10
 */
public class GradleRunnerServiceTest {
  private Mockery myContext;

  protected TempFiles myTempFiles = new TempFiles();
  protected final Map<String, String> myRunnerParams = new HashMap<String, String>();
  protected final Map<String, String> myBuildParams = new HashMap<String, String>();
  protected final Map<String, String> myEnvVars = new HashMap<String, String>();
  protected final Map<String, String> mySystemProps = new HashMap<String, String>();
  protected BuildRunnerContext myRunnerContext;
  protected AgentRunningBuild myBuild;
  protected GradleRunnerService myService;
  protected File myGradleExe;
  protected File myWorkingDirectory;
  protected File myInitScript;
  private File myTempDir;


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
    }});
    myService = (GradleRunnerService) new GradleRunnerServiceFactory().createService();
  }

  @AfterMethod
  public void tearDown() {
    myTempFiles.cleanup();
    myRunnerParams.clear();
    myBuildParams.clear();
    myEnvVars.clear();
    mySystemProps.clear();
  }

  @Test
  public void generateSimpleCommandLineTest() throws Exception {
    prepareGradleRequiredFiles();

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);
    reportCmdLine(cmdLine);
  }

  @DataProvider(name = "enable daemon")
  public static Object[][] enableDaemonParam() {
    return new Object[][] {{"--daemon"}, {"-Dorg.gradle.daemon=true"}};
  }

  @Test(dataProvider = "enable daemon")
  public void generateWithDaemonCommandLineTest(String param) throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, param);
    prepareGradleRequiredFiles();

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    assertTrue(cmdLine.getArguments().indexOf(param) > -1);
    assertTrue(cmdLine.getArguments().indexOf("-Dorg.gradle.daemon=false") == -1);

    reportCmdLine(cmdLine);
  }

  @Test
  public void generateWithoutDaemonCommandLineTest() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "--no-daemon");
    prepareGradleRequiredFiles();

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    assertTrue(cmdLine.getArguments().indexOf("--no-daemon") > -1);
    assertEquals(cmdLine.getArguments().indexOf("-Dorg.gradle.daemon=false"), -1);

    reportCmdLine(cmdLine);
  }

  @Test
  public void generateWithoutDaemonDuplicateParamCommandLineTest() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_PARAMS, "-Dorg.gradle.daemon=false");
    prepareGradleRequiredFiles();

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    assertEquals(Collections.frequency(cmdLine.getArguments(), "-Dorg.gradle.daemon=false"), 1);

    reportCmdLine(cmdLine);
  }

  @Test
  public void generateCLwithJavaHome() throws Exception {
    final String expectedJavaHome = myTempFiles.createTempDir().getAbsolutePath();
    myRunnerParams.put(JavaRunnerConstants.TARGET_JDK_HOME, expectedJavaHome);

    prepareGradleRequiredFiles();
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);

    String actualJavaHome = cmdLine.getEnvironment().get(JavaRunnerConstants.JAVA_HOME);
    assertEquals(actualJavaHome, expectedJavaHome, "Wrong Java Home environment variable.");
  }

  @Test
  public void testCLGradleOpts() throws Exception {
    final String expectedRunnerGradleOpts = "-DrunnerGradleOpt";
    final String expectedRunnerJavaArgs = "-DrunnerJavaArg";

    myRunnerParams.put(GradleRunnerConstants.ENV_GRADLE_OPTS, expectedRunnerGradleOpts);

    prepareGradleRequiredFiles();
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);

    String gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);
    assertTrue(gradleOptsValue.contains(expectedRunnerGradleOpts), "Wrong Java arguments." );

    myRunnerParams.put(JavaRunnerConstants.JVM_ARGS_KEY, expectedRunnerJavaArgs);

    myService.initialize(myBuild,myRunnerContext);
    cmdLine = myService.makeProgramCommandLine();
    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), true);

    gradleOptsValue = cmdLine.getEnvironment().get(GradleRunnerConstants.ENV_GRADLE_OPTS);


    then(gradleOptsValue.split(" ")).as("Should contain new temp dir").contains("\"-Djava.io.tmpdir=" + myTempDir.getCanonicalPath() + "\"")
                         .as("Should contain java args").contains(expectedRunnerJavaArgs);

  }

  @Test
  @TestFor(issues = "TW-57278")
  public void test_spaces_in_tasks_args() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_TASKS, "run --args=\"foo -bar this\"");

    prepareGradleRequiredFiles();

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, myGradleExe.getAbsolutePath(), false);
    then(cmdLine.getArguments()).doesNotContain("-bar", "this\"", "this").contains("--args=\"foo -bar this\"");

    reportCmdLine(cmdLine);

  }

  @Test
  public void generateCLwithGradleParameters() throws Exception {

    final File tempDir = myTempFiles.createTempDir();

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

    prepareGradleRequiredFiles();
    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
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

    for(String task : cmdsArray) {
      assertTrue(args.indexOf(task) > -1, "Gradle task [" + task + "] missing on the command line");
    }

    for(String param : argsArray) {
      assertTrue(args.indexOf(param) > -1, "Gradle parameter [" + param + "] missing on the command line");
    }
  }

  @Test
  public void generateWrapperCL() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    prepareGradleRequiredFiles();

    File gradlew = null;
    if (SystemInfo.isWindows) {
      gradlew = new File(myWorkingDirectory,GradleRunnerServiceFactory.WIN_GRADLEW);
    } else if (SystemInfo.isUnix) {
      gradlew = new File(myWorkingDirectory,GradleRunnerServiceFactory.UNIX_GRADLEW);
    }

    assert null != gradlew;
    gradlew.createNewFile();
    assertTrue(gradlew.exists(), "Could not create gradleW mock file.");

    myService.initialize(myBuild,myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();

    validateCmdLine(cmdLine, gradlew.getAbsolutePath(), true);
  }


  @Test(expectedExceptions = RunBuildException.class)
  public void gradleHomeDoesNotExistTest() throws RunBuildException {

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath("gradle"); will(returnValue(""));
    }});

    myService.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = myService.makeProgramCommandLine();
  }


  @Test(expectedExceptions = RunBuildException.class)
  public void gradleExeDoesNotExistTest() throws RunBuildException, IOException {
    GradleRunnerService service = (GradleRunnerService) new GradleRunnerServiceFactory().createService();

    myContext.checking(new Expectations() {{
      allowing(myRunnerContext).getToolPath("gradle"); will(returnValue(myTempFiles.createTempDir().getAbsolutePath()));
    }});

    service.initialize(myBuild, myRunnerContext);
    ProgramCommandLine cmdLine = service.makeProgramCommandLine();
  }


  private void prepareGradleRequiredFiles() throws IOException {
    final File gradleToolDir = myTempFiles.createTempDir();
    final File agentPluginDir = myTempFiles.createTempDir();
    myWorkingDirectory = myTempFiles.createTempDir();
    myInitScript = new File(agentPluginDir, GradleRunnerConstants.RUNNER_TYPE
                                            + "/" + GradleRunnerConstants.INIT_SCRIPT_SUFFIX);

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
      assertTrue(args.indexOf("-Dorg.gradle.daemon=false") > -1, "Gradle daemon should be disabled");
    }
  }
}
