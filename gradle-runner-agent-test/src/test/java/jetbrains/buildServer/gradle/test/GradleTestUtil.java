package jetbrains.buildServer.gradle.test;

import java.io.File;
import jetbrains.buildServer.gradle.GradleRunnerConstants;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/8/10
 */
public class GradleTestUtil {
  private static final String GRADLE_RUNNER_PATH = "external-repos/gradle-runner";
  private static final String AGENT_MODULE_NAME = "gradle-runner-agent";
  private static final String AGENT_TEST_MODULE_NAME = "gradle-runner-agent-test";

  public static final String REL_SCRIPT_DIR = "src/main/scripts/";
  public static final String REL_TEST_PROJECTS_DIR = "src/test/resources/testProjects";

  public static File setProjectRoot(File curDir) {
    return findModuleRoot(curDir, AGENT_TEST_MODULE_NAME, REL_TEST_PROJECTS_DIR);
  }

  public static File setAgentProjectRoot(File curDir) {
    return findModuleRoot(curDir, AGENT_MODULE_NAME, REL_SCRIPT_DIR + GradleRunnerConstants.INIT_SCRIPT_NAME);
  }

  private static File findModuleRoot(File curDir, String moduleName, String markerPath) {
    File dir = curDir.getAbsoluteFile();
    while (dir != null) {
      File result = findModuleRootNear(dir, moduleName, markerPath);
      if (result != null) {
        return result;
      }
      dir = dir.getParentFile();
    }
    return new File(moduleName);
  }

  private static File findModuleRootNear(File dir, String moduleName, String markerPath) {
    File result = checkModuleRoot(dir, markerPath);
    if (result != null) {
      return result;
    }

    result = checkModuleRoot(new File(dir, moduleName), markerPath);
    if (result != null) {
      return result;
    }

    return checkModuleRoot(new File(new File(dir, GRADLE_RUNNER_PATH), moduleName), markerPath);
  }

  private static File checkModuleRoot(File root, String markerPath) {
    return new File(root, markerPath).canRead() ? root : null;
  }
}
