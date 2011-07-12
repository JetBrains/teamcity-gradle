/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.gradle.agent;

import java.util.List;
import java.util.Map;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class ConfigurationParamsUtil
{
  public static @NotNull String getGradleHome(Map<String, String> runParameters)
  {
    return preventNull(runParameters.get(GradleRunnerConstants.GRADLE_HOME), "");
  }

  public static @NotNull String getJavaArgs(Map<String, String> runParameters)
  {
    return StringUtil.newLineToSpaceDelimited(preventNull(runParameters.get(JavaRunnerConstants.JVM_ARGS_KEY), ""));
  }

  public static @NotNull String getGradleTasks(Map<String, String> runParameters)
  {
    return preventNull(runParameters.get(GradleRunnerConstants.GRADLE_TASKS), "");
  }

  public static @NotNull List<String> getGradleParams(Map<String, String> runParameters)
  {
    return CommandLineArgumentsUtil.extractArguments(runParameters.get(GradleRunnerConstants.GRADLE_PARAMS));
  }

  public static boolean isParameterEnabled(final Map<String, String> runParameters, final String key)
  {
    return runParameters.containsKey(key) && runParameters.get(key).equals(Boolean.TRUE.toString());
  }

  public static @NotNull String getGradleInitScript(Map<String, String> runParameters)
  {
    return preventNull(runParameters.get(GradleRunnerConstants.GRADLE_INIT_SCRIPT), "");
  }

  private static @NotNull <T> T preventNull(T t, T defaultValue)
  {
    return t == null ? defaultValue : t;
  }

  public static String getGradleWPath(final Map<String, String> runnerParameters) {
    return preventNull(runnerParameters.get(GradleRunnerConstants.GRADLE_WRAPPER_PATH), "");
  }
}
