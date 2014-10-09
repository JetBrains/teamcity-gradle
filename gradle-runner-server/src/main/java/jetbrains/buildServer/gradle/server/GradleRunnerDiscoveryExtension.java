package jetbrains.buildServer.gradle.server;

import java.util.*;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.browser.Browser;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Created by Nikita.Skvortsov
 * date: 31.10.13.
 */
public class GradleRunnerDiscoveryExtension extends BreadthFirstRunnerDiscoveryExtension {

  private Element myWrapperScriptDir;
  private static final int WRAPPER_DEPTH_LIMIT = 1;

  @NotNull
  @Override
  protected List<DiscoveredObject> discoverRunnersInDirectory(@NotNull final Element dir, @NotNull final List<Element> filesAndDirs) {
    final List<DiscoveredObject> res = new ArrayList<DiscoveredObject>();
    boolean foundBuildGradle = false;

    for (Element child: filesAndDirs) {
      if (child.isLeaf() && "build.gradle".equals(child.getName())) {
        foundBuildGradle = true;
        if (myWrapperScriptDir == null) {
          lookForWrapperScript(dir, filesAndDirs, 0);
        }
      }
    }

    if (foundBuildGradle) {
      Map<String, String> props = new HashMap<String, String>();
      props.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");

      final boolean isSubdirectory = !dir.getBrowser().getRoot().equals(dir);
      if (isSubdirectory) {
        props.put(GradleRunnerConstants.PATH_TO_BUILD_FILE, dir.getFullName() + "/build.gradle");
      }

      res.add(new DiscoveredObject(GradleRunnerConstants.RUNNER_TYPE, props));
    }
    return res;
  }

  private void lookForWrapperScript(final Element dir, final Iterable<Element> children, int depth) {
    for (Element child : children) {
      if (child.isLeaf() && "gradlew".equals(child.getName())) {
        myWrapperScriptDir = dir;
      }
    }

    if (depth < WRAPPER_DEPTH_LIMIT) {
    for (Element child : children) {
      if (!child.isLeaf() && myWrapperScriptDir == null) {
        lookForWrapperScript(child, child.getChildren(), depth + 1);
      }
    }
    }
  }

  @NotNull
  @Override
  protected List<DiscoveredObject> postProcessDiscoveredObjects(@NotNull final BuildTypeSettings settings,
                                                                @NotNull final Browser browser,
                                                                @NotNull final List<DiscoveredObject> discovered) {
    final Set<String> foundGradles = new HashSet<String>();
    for (SBuildRunnerDescriptor descriptor : settings.getBuildRunners()) {
      if (GradleRunnerConstants.RUNNER_TYPE.equals(descriptor.getType())) {
        foundGradles.add(StringUtil.emptyIfNull(descriptor.getParameters().get(GradleRunnerConstants.GRADLE_WORKING_DIR)));
      }
    }
    final Iterator<DiscoveredObject> iterator = discovered.iterator();
    while (iterator.hasNext()) {
      final DiscoveredObject discoveredObject = iterator.next();
      final Map<String, String> parameters = discoveredObject.getParameters();
      final String workingDir = StringUtil.emptyIfNull(parameters.get(GradleRunnerConstants.GRADLE_WORKING_DIR));

      if (foundGradles.contains(workingDir)) {
        iterator.remove();
      } else {
      if (myWrapperScriptDir != null) {
        parameters.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, "true");
        final int workingDirLength = workingDir.length();
        final String wrapperDir = myWrapperScriptDir.getFullName();
        if (wrapperDir.length() > workingDirLength) {
          if (workingDirLength > 0) {
            parameters.put(GradleRunnerConstants.GRADLE_WRAPPER_PATH, wrapperDir.substring(workingDirLength));
          }
          parameters.put(GradleRunnerConstants.GRADLE_WRAPPER_PATH, wrapperDir);
        }
      }
      }
    }
    return super.postProcessDiscoveredObjects(settings, browser, discovered);
  }
}
