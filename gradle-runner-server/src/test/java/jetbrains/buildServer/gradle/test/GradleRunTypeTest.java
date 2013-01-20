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

package jetbrains.buildServer.gradle.test;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.server.GradleRunType;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/9/10
 */
public class GradleRunTypeTest {

  private GradleRunType myRunType;

  private static final String DEFAULT_DESCRIPTION = "Gradle tasks: Default\n" +
                                                    "Use wrapper script: no";

  private static final String NON_DEFAULT_DESCRIPTION = "Gradle tasks: clean build\n" +
                                                        "Use wrapper script: yes";

  private static final String INCREMENTAL_DESCRIPTION = "Run incremental builds using :buildDependents\n" +
                                                        "Use wrapper script: yes";

  @BeforeMethod
  public void setUp() {
    final Mockery context = new Mockery();
    final RunTypeRegistry runTypeRegistry = context.mock(RunTypeRegistry.class);
    context.checking(new Expectations() {{
      allowing(runTypeRegistry).registerRunType(with(any(GradleRunType.class)));
    }});
    myRunType = new GradleRunType(runTypeRegistry);
  }

  @Test
  public void testDefaultDescribeParameters() throws Exception {
    String description = myRunType.describeParameters(new HashMap<String, String>());
    assertEquals(description, DEFAULT_DESCRIPTION, "Wrong description received.");
  }

  @Test
  public void testNonDefaultDescribeParameters() throws Exception {
    Map<String,  String> params = new HashMap<String, String>();
    params.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");
    params.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());

    String description = myRunType.describeParameters(params);
    assertEquals(description, NON_DEFAULT_DESCRIPTION, "Wrong description received");
  }

  @Test
  public void testIncrementalSetting() throws Exception {
    Map<String,  String> params = new HashMap<String, String>();
    params.put(GradleRunnerConstants.IS_INCREMENTAL, Boolean.TRUE.toString());
    params.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");
    params.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());

    String description = myRunType.describeParameters(params);
    assertEquals(description, INCREMENTAL_DESCRIPTION, "Wrong description received");
  }
}
