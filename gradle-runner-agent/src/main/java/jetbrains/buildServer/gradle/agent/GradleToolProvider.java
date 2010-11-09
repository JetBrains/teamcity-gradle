/*
 * Copyright 2000-2010 JetBrains s.r.o.
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

import java.io.File;
import jetbrains.buildServer.agent.*;
import org.jetbrains.annotations.NotNull;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 24, 2010
 */
public class GradleToolProvider {

  public static final String GRADLE_TOOL = "gradle";
  private static final String GRADLE_HOME = "GRADLE_HOME";

  public GradleToolProvider(@NotNull ToolProvidersRegistry toolProvidersRegistry, @NotNull final BuildAgentConfiguration agentConfiguration) {
    toolProvidersRegistry.registerToolProvider(new ToolProvider() {

      public boolean supports(@NotNull final String toolName) {
        return GRADLE_TOOL.equals(toolName);
      }

      @NotNull
      public String getPath(@NotNull final String toolName) throws ToolCannotBeFoundException {
        // try to get locally installed gradle
        String gradleHomePath = System.getenv(GRADLE_HOME);
        if (null != gradleHomePath && gradleHomePath.length() > 0) {
          return gradleHomePath;
        } else {
         // try to get bundled gradle
          File gradleDir = new File(agentConfiguration.getAgentPluginsDirectory(), "gradle");
          if (gradleDir.exists()) {
            return gradleDir.getAbsolutePath();
          } else {
            throw new ToolCannotBeFoundException("Unable to find Gradle's home");
          }
        }
      }

      @NotNull
      public String getPath(@NotNull final String toolName,
                            @NotNull final AgentRunningBuild build,
                            @NotNull final BuildRunnerContext runner)
        throws ToolCannotBeFoundException {
        String homePath = ConfigurationParamsUtil.getGradleHome(runner.getRunnerParameters());
        if (homePath.length() != 0) {
          return homePath;
        } else {
          return getPath(toolName);
        }
      }
    });
  }

}
