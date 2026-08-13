

package jetbrains.buildServer.gradle.test;

import java.io.File;
import jetbrains.buildServer.gradle.GradleRunnerConstants;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/8/10
 */
public class GradleTestUtil {
  public static final String ABS_MOD_DIR = "external-repos/gradle-runner/gradle-runner-agent/";
  public static final String REL_SCRIPT_DIR = "src/main/scripts/";

  public static File setProjectRoot(File curDir) {
    if (!new File(curDir, REL_SCRIPT_DIR + GradleRunnerConstants.INIT_SCRIPT_NAME).canRead()) {
      curDir = new File(ABS_MOD_DIR);
    }
    return curDir;
  }
}