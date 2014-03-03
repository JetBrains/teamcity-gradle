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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 11, 2010
 */
public class GradleRunnerWrapperTest extends GradleRunnerServiceMessageTest {


  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    myRunnerParams.clear();
  }

  @Test
  public void simpleWrapperTest() throws Exception {
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());
    myRunnerParams.put(GradleRunnerConstants.GRADLE_WRAPPER_PATH, "gradle-runtime");
    GradleRunConfiguration config = new GradleRunConfiguration("wrappedProjectA", "clean build", "wrappedProjASequence.txt");
    config.setPatternStr("^Downloading(.*)|^Unzipping(.*)|##teamcity\\[(.*?)(?<!\\|)\\]");
    runAndCheckServiceMessages(config);
  }
}
