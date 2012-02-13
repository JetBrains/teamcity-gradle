/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.util.SystemInfo;
import jetbrains.buildServer.agent.AgentBuildRunnerInfo;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.agent.runner.CommandLineBuildService;
import jetbrains.buildServer.agent.runner.CommandLineBuildServiceFactory;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.log.Loggers;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerServiceFactory implements CommandLineBuildServiceFactory
{
  public static final String WIN_GRADLE_EXE = "bin/gradle.bat";
  public static final String WIN_GRADLEW = "gradlew.bat";
  public static final String UNIX_GRADLE_EXE = "bin/gradle";
  public static final String UNIX_GRADLEW = "gradlew";

  private static final Info info = new Info();

  @NotNull public CommandLineBuildService createService()
   {
     final String exePath;
     final String wrapperName;

    if (SystemInfo.isWindows) {
      exePath = WIN_GRADLE_EXE;
      wrapperName = WIN_GRADLEW;
    } else if (SystemInfo.isUnix) {
      exePath = UNIX_GRADLE_EXE;
      wrapperName = UNIX_GRADLEW;
    } else {
      throw new RuntimeException("OS not supported");
    }

    return new GradleRunnerService(exePath, wrapperName);
   }

   @NotNull public AgentBuildRunnerInfo getBuildRunnerInfo()
   {
      return info;
   }

   private static class Info implements AgentBuildRunnerInfo
   {
      private final boolean isOSSupported;

      public Info() {
        isOSSupported = SystemInfo.isWindows || SystemInfo.isUnix;
        if (!isOSSupported) {
          Loggers.AGENT.warn("Gradle runner plugin does not support current OS. Gradle build runner will not be available.");
        }
      }

      @NotNull public String getType()
      {
         return GradleRunnerConstants.RUNNER_TYPE;
      }

      public boolean canRun(@NotNull BuildAgentConfiguration buildAgentConfiguration)
      {
         return isOSSupported;
      }
   }
}

