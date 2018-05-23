package jetbrains.buildServer.gradle.server;

import com.intellij.openapi.util.Pair;
import java.util.*;
import java.util.function.BiPredicate;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.serverSide.BuildTypeSettings;
import jetbrains.buildServer.serverSide.SBuildRunnerDescriptor;
import jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.BuildRunnerDiscoveryExtension;
import jetbrains.buildServer.serverSide.discovery.DiscoveredObject;
import jetbrains.buildServer.util.CollectionsUtil;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.browser.Browser;
import jetbrains.buildServer.util.browser.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.serverSide.discovery.BreadthFirstRunnerDiscoveryExtension.DEFAULT_DEPTH_LIMIT;

/**
 *
 * We have to traverse tree manually to correctly detect wrappers out of dir with build.gradle
 * That's why we're not extending BreadthFirstRunnerDiscoveryExtension, though
 * {@linkplain BreadthFirstRunnerDiscoveryExtension#DEFAULT_DEPTH_LIMIT} is reused
 *
 * @author Nikita.Skvortsov
 * @author Vladislav.Rassokhin
 */
public class GradleRunnerDiscoveryExtension implements BuildRunnerDiscoveryExtension {

  private static final Set<String> ourBuildFileSupportedNames = CollectionsUtil.setOf("build.gradle", "build.gradle.kts");
  private static final Set<String> ourWrapperSupportedNames = CollectionsUtil.setOf("gradlew", "gradlew.bat");

  @Nullable
  @Override
  public List<DiscoveredObject> discover(@NotNull final BuildTypeSettings settings, @NotNull final Browser browser) {
    final List<DiscoveredObject> runners = scan(browser.getRoot());
    return postProcess(settings, runners);
  }

  @NotNull
  private List<DiscoveredObject> scan(@NotNull Element dir) {
    final List<Pair<Element, Element>> foundBuildFiles = new ArrayList<>(0);

    traverse(dir, (d, child) -> {
      if (child.isLeaf() && ourBuildFileSupportedNames.contains(child.getName())) {
        foundBuildFiles.add(Pair.create(d, child));
        return false;
      }
      return true;
    });
    if (foundBuildFiles.isEmpty()) return Collections.emptyList();

    final Set<Element> foundWrapperDirs = new LinkedHashSet<>(0);
    traverse(dir, (d, child) -> {
      if (child.isLeaf() && ourWrapperSupportedNames.contains(child.getName())) {
        foundWrapperDirs.add(d);
        return false;
      }
      return true;
    });

    final List<DiscoveredObject> res = new ArrayList<DiscoveredObject>(foundBuildFiles.size());

    for (Pair<Element, Element> pair : foundBuildFiles) {
      final Element directory = pair.first;
      final Map<String, String> props = new HashMap<String, String>();
      props.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");

      final boolean isSubdirectory = !directory.getBrowser().getRoot().equals(directory);
      if (isSubdirectory) {
        props.put(GradleRunnerConstants.PATH_TO_BUILD_FILE, directory.getFullName() + "/" + pair.second);
      }
      if (!foundWrapperDirs.isEmpty()) {
        // Seems it's safe to choose first one
        final Element wrapperDirElement = foundWrapperDirs.iterator().next();
        props.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, "true");
        final String wrapperDir = wrapperDirElement.getFullName();
        props.put(GradleRunnerConstants.GRADLE_WRAPPER_PATH, wrapperDir);
      }
      res.add(new DiscoveredObject(GradleRunnerConstants.RUNNER_TYPE, props));
    }
    return res;
  }

  private void traverse(@NotNull final Element dir, @NotNull final BiPredicate<Element, Element> consumer) {
    traverse(dir, 0, consumer);
  }

  private void traverse(@NotNull final Element dir,
                        int depth,
                        @NotNull final BiPredicate<Element, Element> mapper) {
    final Iterable<Element> children = dir.getChildren();
    if (children == null) return;
    boolean into = true;
    for (Element child : children) {
      into &= mapper.test(dir, child);
    }
    if (into && depth < DEFAULT_DEPTH_LIMIT) {
      for (Element child : children) {
        if (!child.isLeaf()) {
          traverse(child, depth + 1, mapper);
        }
      }
    }
  }

  @NotNull
  private List<DiscoveredObject> postProcess(@NotNull final BuildTypeSettings settings, @NotNull final List<DiscoveredObject> discovered) {
    final Set<String> existingGradleProjects = new HashSet<String>();

    for (SBuildRunnerDescriptor descriptor : settings.getBuildRunners()) {
      if (GradleRunnerConstants.RUNNER_TYPE.equals(descriptor.getType())) {
        existingGradleProjects.add(StringUtil.emptyIfNull(descriptor.getParameters().get(GradleRunnerConstants.PATH_TO_BUILD_FILE)));
      }
    }

    final Iterator<DiscoveredObject> iterator = discovered.iterator();
    while (iterator.hasNext()) {
      final DiscoveredObject discoveredObject = iterator.next();
      final Map<String, String> parameters = discoveredObject.getParameters();
      final String pathToBuildFile = StringUtil.emptyIfNull(parameters.get(GradleRunnerConstants.PATH_TO_BUILD_FILE));
      if (existingGradleProjects.contains(pathToBuildFile)) {
        iterator.remove();
      }
    }
    return discovered;
  }
}
