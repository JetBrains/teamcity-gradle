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

package jetbrains.buildServer.gradle;

import java.io.File;

public class GradleRunnerConstants
{
  public static final String RUNNER_TYPE = "gradle-runner";

  public static final String GRADLE_INIT_SCRIPT = "ui.gradleRunner.gradle.init.script";
  public static final String GRADLE_HOME = "ui.gradleRunner.gradle.home";
  public static final String GRADLE_TASKS = "ui.gradleRunner.gradle.tasks.names";
  public static final String GRADLE_PARAMS = "ui.gradleRunner.additional.gradle.cmd.params";
  public static final String STACKTRACE = "ui.gradleRunner.gradle.stacktrace.enabled";
  public static final String DEBUG = "ui.gradleRunner.gradle.debug.enabled";
  public static final String GRADLE_WRAPPER_FLAG = "ui.gradleRunner.gradle.wrapper.useWrapper";
  public static final String GRADLE_WRAPPER_PATH = "ui.gradleRunner.gradle.wrapper.path";
  public static final String IS_INCREMENTAL = "ui.gradleRunner.gradle.incremental";
  public static final String PATH_TO_BUILD_FILE = "ui.gradleRUnner.gradle.build.file";

  // incremental options run
  public static final String ENV_INCREMENTAL_PARAM = "TEAMCITY_GRADLE_INCREMENTAL_MODE";

  // internal configuration parameters
  public static final String GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM = "teamcity.internal.gradle.runner.launch.mode";
  public static final String GRADLE_RUNNER_TOOLING_API_LAUNCH_MODE = "gradle-tooling-api";
  public static final String GRADLE_RUNNER_GRADLE_LAUNCH_MODE = "gradle";
  /**
   * Normally, we don't read all the parameters from teamcity.build.parameters file,
   * because it always changes from build to build. Because of that, Gradle configuration-cache doesn't work.
   * But if for some reason user wants to use all the parameters, he could set this configuration parameter to true.
   */
  public static final String GRADLE_RUNNER_READ_ALL_CONFIG_PARAM = "teamcity.internal.gradle.runner.read.all.params";

  public static final String ENV_INCREMENTAL_VALUE_SKIP = "skip_incremental";
  public static final String ENV_INCREMENTAL_VALUE_PROCEED = "do_incremental";
  public static final String ENV_GRADLE_OPTS = "GRADLE_OPTS";
  public static final String ENV_SUPPORT_TEST_RETRY = "TEAMCITY_SUPPORT_TEST_RETRY";
  public static final String TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH = "TEAMCITY_PARALLEL_TESTS_ARTIFACT_PATH";

  public static final String INIT_SCRIPT_DIR = "scripts";
  public static final String INIT_SCRIPT_NAME = "init.gradle";
  public static final String INIT_SCRIPT_SINCE_8_NAME = "init_since_8.gradle";
  public static final String GRADLE_BUILD_PROBLEM_TYPE = "gradleBuildProblem";

  public static final String GRADLE_WRAPPER_PROPERTIES_DEFAULT_LOCATION = "gradle" + File.separator + "wrapper" + File.separator + "gradle-wrapper.properties";

  public static final String GRADLE_HOME_ENV_KEY = "GRADLE_HOME";
  public static final String GRADLE_WRAPPED_DISTRIBUTION_ENV_KEY = "GRADLE_WRAPPED_DISTRIBUTION_PATH";
  public static final String WORKING_DIRECTORY_ENV_KEY = "WORKING_DIRECTORY";
  public static final String USE_WRAPPER_ENV_KEY = "USE_WRAPPER";

  public static final char GRADLE_TASKS_DELIMITER = ' ';

  public static final String GRADLE_LAUNCHER_ENV_FILE = "teamcity.gradle.env.parameters";
  public static final String GRADLE_PARAMS_FILE = "teamcity.gradle.config.parameters";
  public static final String GRADLE_JVM_PARAMS_FILE = "teamcity.gradle.jvm.parameters";
  public static final String GRADLE_TASKS_FILE = "teamcity.gradle.tasks";

  public static final String TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY = "teamcity.build.properties.file";
  public static final String TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY = "TEAMCITY_BUILD_PROPERTIES_FILE";

  public static final String SPLIT_PROPERTY_STATIC_POSTFIX = ".static";
  public static final String BUILD_TEMP_DIR_TASK_OUTPUT_SUBDIR = "task-output";
}