package jetbrains.buildServer.gradle.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Nikita.Skvortsov
 * date: 31.10.13.
 */
public class GradleRunnerDiscoveryExtension extends BreadthFirstRunnerDiscoveryExtension {
  @NotNull
  @Override
  protected List<DiscoveredObject> discoverRunnersInDirectory(@NotNull final Element dir, @NotNull final List<Element> filesAndDirs) {
    final List<DiscoveredObject> res = new ArrayList<DiscoveredObject>();
    boolean foundBuildGradle = false;
    boolean foundWrapperScript = false;
    for (Element child: filesAndDirs) {
      if (child.isLeaf() && "build.gradle".equals(child.getName())) {
        foundBuildGradle = true;
      }
      if (child.isLeaf() && "gradlew".equals(child.getName())) {
        foundWrapperScript = true;
      }
    }

    if (foundBuildGradle) {
      Map<String, String> props = new HashMap<String, String>();
      props.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");

      final boolean isSubdirectory = dir.getFullName().length() > dir.getName().length();
      if (isSubdirectory) {
        props.put("teamcity.build.workingDir", dir.getFullName());
      }

      if (foundWrapperScript) {
        props.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, "true");
      }
      res.add(new DiscoveredObject(GradleRunnerConstants.RUNNER_TYPE, props));
    }
    return res;
  }
}
