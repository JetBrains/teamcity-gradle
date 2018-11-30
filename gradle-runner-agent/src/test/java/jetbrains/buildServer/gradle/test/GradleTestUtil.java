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

import java.io.File;
import jetbrains.buildServer.gradle.GradleRunnerConstants;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/8/10
 */
public class GradleTestUtil {
  public static final String ABS_MOD_DIR = "bundled-plugins/gradle-runner/gradle-runner-agent/";
  public static final String REL_SCRIPT_DIR = "src/main/scripts/";

  public static File setProjectRoot(File curDir) {
    if (!new File(curDir, REL_SCRIPT_DIR + GradleRunnerConstants.INIT_SCRIPT_NAME).canRead()) {
      curDir = new File(ABS_MOD_DIR);
    }
    return curDir;
  }
}
