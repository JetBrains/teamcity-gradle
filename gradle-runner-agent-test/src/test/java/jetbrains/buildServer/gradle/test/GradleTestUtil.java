

package jetbrains.buildServer.gradle.test;

import java.io.File;
import jetbrains.buildServer.gradle.GradleRunnerConstants;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/8/10
 */
public class GradleTestUtil {
  private static final String EXTERNAL_REPO_DIR = "external-repos/gradle-runner/";
  private static final String AGENT_MODULE_DIR = "gradle-runner-agent";
  private static final String AGENT_TEST_MODULE_DIR = "gradle-runner-agent-test";
  public static final String REL_SCRIPT_DIR = "src/main/scripts/";
  private static final String TEST_PROJECTS_DIR = "src/test/resources/testProjects";

  public static File setProjectRoot(File curDir) {
    return findModuleRoot(curDir, AGENT_TEST_MODULE_DIR, TEST_PROJECTS_DIR);
  }

  public static File setAgentProjectRoot(File curDir) {
    return findModuleRoot(curDir, AGENT_MODULE_DIR, REL_SCRIPT_DIR + GradleRunnerConstants.INIT_SCRIPT_NAME);
  }

  private static File findModuleRoot(File curDir, String moduleDir, String markerPath) {
    final File[] candidates = {
      curDir,
      new File(curDir, moduleDir),
      new File(curDir, "../" + moduleDir),
      new File(curDir, EXTERNAL_REPO_DIR + moduleDir)
    };
    for (File candidate : candidates) {
      if (new File(candidate, markerPath).exists()) {
        return candidate;
      }
    }
    return candidates[candidates.length - 1];
  }
}
