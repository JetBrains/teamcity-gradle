package jetbrains.buildServer.gradle.test;

import java.io.File;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/8/10
 */
public class GradleTestUtil {
  public static final String ABS_MOD_DIR = "svnrepo/gradle-runner/gradle-runner-agent/";
  public static final String REL_SCRIPT_DIR = "src/main/scripts/";
  public static final String INIT_SCRIPT_NAME = "init.gradle";

  public static File setProjectRoot(File curDir) {
    if (!new File(curDir, REL_SCRIPT_DIR + INIT_SCRIPT_NAME).canRead()) {
      curDir = new File(ABS_MOD_DIR);
    }
    return curDir;
  }
}
