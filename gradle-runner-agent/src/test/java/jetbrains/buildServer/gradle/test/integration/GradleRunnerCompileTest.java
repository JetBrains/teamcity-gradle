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

import jetbrains.buildServer.gradle.GradleRunnerConstants;
import org.testng.annotations.Test;

import static org.testng.Assert.fail;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 20, 2010
 */
public class GradleRunnerCompileTest extends GradleRunnerServiceMessageTest {

  public static final String BUILD_CMD = "clean compileJava compileTest";
  private static final boolean isJre8 = System.getProperty("java.specification.version").contains("1.8");
  private static final boolean isJre7 = System.getProperty("java.specification.version").contains("1.7");
  private static final boolean isJre6 = System.getProperty("java.specification.version").contains("1.6");
  private static final boolean isJre5 = System.getProperty("java.specification.version").contains("1.5");
  private static final String COMPILATION_BLOCK_PROPS_MSGS_PATTERN = "##teamcity\\[(message|compilation|block)(.*?)(?<!\\|)\\]|##tc-property.*";
  private static final String COMPILATION_MSGS_PATTERN = "##teamcity\\[(message|compilation)(.*?)(?<!\\|)\\]";

  @Test(dataProvider = "gradle-version-provider")
  public void successfulCompileTest(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("property_alpha", "value_alpha");
    myTeamCitySystemProps.put("property.bravo", "value bravo");
    myTeamCitySystemProps.put("property charlie", "value charlie");

    GradleRunConfiguration config = new GradleRunConfiguration(MULTI_PROJECT_A_NAME, BUILD_CMD + " printProperties", "mProjectABlockSequence.txt");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void failedCompileTest(final String gradleVersion) throws Exception {
    GradleRunConfiguration config = null;
    // Compilation errors output differs on different javac versions
    if (isJre5) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_5.txt");
    } else if (isJre6) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_6.txt");
    } else if (isJre7 || isJre8) {
      config = new GradleRunConfiguration(PROJECT_B_NAME, BUILD_CMD, "failedCompilationSequence1_7.txt");
    } else {
      fail("Compiler test requires JRE version 5,6,7 or 8 to run; Current version: " + System.getProperty("java.specification.version"));
    }
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
  }

  @Test(dataProvider = "gradle-version-provider")
  public void pathToBuildGradleTest(final String gradleVersion)  throws Exception {
    GradleRunConfiguration config = new GradleRunConfiguration("subdir", "clean build", "projectABlockSequence.txt");
    myRunnerParams.put(GradleRunnerConstants.PATH_TO_BUILD_FILE, "projectA/run.gradle");
    config.setGradleVersion(gradleVersion);
    config.setPatternStr(COMPILATION_BLOCK_PROPS_MSGS_PATTERN);
    runAndCheckServiceMessages(config);
    myRunnerParams.remove(GradleRunnerConstants.PATH_TO_BUILD_FILE); // todo fix test infrastructure
  }
}

