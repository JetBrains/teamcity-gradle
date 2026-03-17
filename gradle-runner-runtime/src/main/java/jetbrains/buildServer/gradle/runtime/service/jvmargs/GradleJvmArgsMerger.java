package jetbrains.buildServer.gradle.runtime.service.jvmargs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GradleJvmArgsMerger {

  private final GradleToolingLogger logger;

  public GradleJvmArgsMerger(@NotNull GradleToolingLogger logger) {
    this.logger = logger;
  }

  /**
   * Merges JVM arguments defined in Gradle (e.g. in gradle.properties) with arguments defined in the TC's build configuraiton.
   * If some argument is present in both collections, its value from TC will override the value from Gradle.
   *
   * @param gradleProjectJvmArgs JVM arguments from Gradle project
   * @param tcJvmArgs JVM arguments from TC
   * @return merged JVM arguments
   */
  @NotNull
  public Collection<String> mergeJvmArguments(@NotNull Collection<String> gradleProjectJvmArgs, @NotNull Collection<String> tcJvmArgs) {
    logger.debug("Merging JVM arguments");
    logger.debug("Gradle JVM arguments: " + String.join(" ", gradleProjectJvmArgs));
    logger.debug("TC JVM arguments: " + String.join(" ", tcJvmArgs));

    Map<String, LinkedHashSet<String>> arguments = groupArgumentValuesByKey(gradleProjectJvmArgs, tcJvmArgs);

    for (String argKey : arguments.keySet()) {
      Collection<String> argValues = arguments.get(argKey);
      if (argValues.size() > 1) {
        Collection<String> mergedValues = mergeArgumentValues(argKey, argValues);
        argValues.clear();
        argValues.addAll(mergedValues);
      }
    }

    List<String> result = new ArrayList<>();
    arguments.keySet().forEach(key -> arguments.get(key).forEach(val -> {
      if (isCompositeValue(val)) {
          result.add(key);
          result.add(val);
      } else {
        result.add(key + val);
      }
    }));

    logger.debug("Merging result: " + String.join(" ", result));
    return result;
  }

  /**
   * Groups all the values with the same key from both collections into a single map
   */
  private Map<String, LinkedHashSet<String>> groupArgumentValuesByKey(@NotNull Collection<String> gradleProjectJvmArgs,
                                                                      @NotNull Collection<String> tcJvmArgs) {
    Map<String, LinkedHashSet<String>> result = new LinkedHashMap<>();
    Collection<String> concatenated = Stream.concat(gradleProjectJvmArgs.stream(), tcJvmArgs.stream())
            .filter(this::isNotEmpty)
            .map(StringUtil::unquoteString)
            .collect(Collectors.toList());

    String lastKey = null;
    for (String unparsedArg : concatenated) {
      JvmArg arg = JvmArg.ofString(unparsedArg);
      if (arg.getKey().startsWith(JvmArg.PREFIX) || lastKey == null) {
        result.computeIfAbsent(arg.getKey(), k -> new LinkedHashSet<>()).add(arg.getValue());
        lastKey = arg.getKey();
      } else {
        result.get(lastKey).add(unparsedArg);
        lastKey = null;
      }
    }

    return result;
  }

  /**
   * Merges JVM argument's values.
   * Values could be either composite or simple.
   *
   * For example, there are JVM args: ["-Dparam=val", "-Dparam=newVal", "-Foo", "bar=001", "-Foo", "bar=003", "-Foo", "baz=001"]
   * Composite arg values are here: "bar=001", "bar=003" and "baz=001" associated with the "-Foo" key.
   * Simple arg value here are "val" and "newVal" associated with the "-Dparam" key.
   * In both cases we should only consider the newest value.
   * The result of merging the values of the "-Foo" key will be: ["bar=003", "baz=001"].
   * And the result of merging the values of the "-Dparam" key will be: ["newVal"]
   *
   * @param argKey JVM argument key
   * @param argValues JVM argument values to merge
   * @return merged values
   */
  private Collection<String> mergeArgumentValues(@NotNull String argKey,
                                                 @NotNull Collection<String> argValues) {
    Collection<String> merged = new ArrayList<>();

    Map<String, String> compositeValues = new LinkedHashMap<>();
    String lastSingleValue = null;
    for (String argValue : argValues) {
      if (argValue.isEmpty()) {
        continue;
      }
      if (isCompositeValue(argValue) || JvmArg.isPackageAccessibilityJvmArg(argKey)) {
        JvmArg composite = JvmArg.ofString(argValue);
        compositeValues.put(composite.getKey(), composite.getValue());
      } else {
        lastSingleValue = argValue;
      }
    }

    compositeValues.forEach((key, value) -> merged.add(key + value));
    if (lastSingleValue != null) {
      merged.add(lastSingleValue);
    }

    return merged;
  }

  private boolean isCompositeValue(@NotNull String argValue) {
    return argValue.indexOf(JvmArg.ARGS_DELIMITER) > 0;
  }

  private boolean isNotEmpty(@Nullable String val) {
    return val != null && !val.isEmpty();
  }
}
