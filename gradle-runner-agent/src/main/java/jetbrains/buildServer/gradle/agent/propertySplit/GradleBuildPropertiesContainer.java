package jetbrains.buildServer.gradle.agent.propertySplit;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jetbrains.buildServer.gradle.agent.propertySplit.SplitPropertiesFilenameBuilder.buildStaticPropertiesFilename;

/**
 * A container for all the parameters used in the Gradle build.
 * It reads parameters from the appropriate files on demand, and stores into the cache.
 *
 * Static parameters is parameters that doesn't change from build to build.
 * Dynamic parameters is that change from build to build. E.g.: build.number.
 *
 * Static parameters are loaded into the cache when the Container is initialized.
 *
 * The algorithm for finding a parameter value by key looks like this:
 * 1. The first place the Container will look for the parameter is the cache.
 * 2. If the parameter is not found, the container will read parameters from the root file with dynamic parameters.
 *
 * Only the 2-nd step will cause inability to use configuration cache feature, so the goal is to avoid reading from the root file.
 * But if for some reason user needs to read some "dynamic" parameters like build.number,
 * he could set {@link GradleBuildPropertiesContainer#shouldReadAllParameters}, but configuration-cache will not be available.
 */
public class GradleBuildPropertiesContainer implements Map<Object, Object> {

  /**
   * see {@link GradleRunnerConstants.GRADLE_RUNNER_READ_ALL_CONFIG_PARAM}
   */
  private final boolean shouldReadAllParameters;
  private final String propsFilePath;
  private volatile boolean dynamicParametersRead = false;
  private final ConcurrentHashMap<Object, Object> cache = new ConcurrentHashMap<>();

  public GradleBuildPropertiesContainer(@NotNull String propsFilePath,
                                        boolean shouldReadAllParameters) {
    this.shouldReadAllParameters = shouldReadAllParameters;
    this.propsFilePath = propsFilePath;

    init(buildStaticPropertiesFilename(propsFilePath));
  }

  @Override
  public int size() {
    return cache.size();
  }

  @Override
  public boolean isEmpty() {
    return cache.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    return cache.containsKey(key);
  }

  @Override
  public boolean containsValue(Object value) {
    return cache.containsValue(value);
  }

  @Override
  public Object get(Object key) {
    if (cache.containsKey(key)) {
      return cache.get(key);
    }

    synchronized (this) {
      if (!dynamicParametersRead) {
        readDynamicParameters();
      }
    }

    return cache.get(key);
  }

  @Nullable
  @Override
  public Object put(Object key, Object value) {
    return cache.put(key, value);
  }

  @Override
  public Object remove(Object key) {
    return cache.remove(key);
  }

  @Override
  public void putAll(@NotNull Map<? extends Object, ? extends Object> m) {
    cache.putAll(m);
  }

  @Override
  public void clear() {
    cache.clear();
  }

  @NotNull
  @Override
  public Set<Object> keySet() {
    return cache.keySet();
  }

  @NotNull
  @Override
  public Collection<Object> values() {
    return cache.values();
  }

  @NotNull
  @Override
  public Set<Entry<Object, Object>> entrySet() {
    return cache.entrySet();
  }

  private void init(@NotNull String staticPropsFilePath) {
    Map<Object, Object> staticParamsMap = readParams(staticPropsFilePath);
    staticParamsMap.forEach(cache::put);

    if (shouldReadAllParameters) {
      readDynamicParameters();
    }
  }

  private void readDynamicParameters() {
    Map<Object, Object> dynamicParamsMap = readParams(propsFilePath);
    dynamicParamsMap.forEach(cache::put);
    dynamicParametersRead = true;
  }

  private Map<Object, Object> readParams(@NotNull String propertyFilePath) {
    try {
      return GradleRunnerFileUtil.readProperties(new File(propertyFilePath));
    } catch (IOException e) {
      throw new RuntimeException("Couldn't read properties from: " + propertyFilePath + " " + e.getMessage());
    }
  }
}
