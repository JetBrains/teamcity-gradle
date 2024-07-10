package jetbrains.buildServer.gradle.agent;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;

/**
 * Provides classes for adding extra libraries to the Gradle daemon classpath.
 * This is necessary for supporting non-Gradle errors in test listeners.
 *
 * The issue is that if a test fails due to an org.opentest4j.AssertionFailedError from the opentest4j-1.3.0 library,
 * then in org.gradle.api.tasks.testing.TestResult#getException, we do not receive this error
 * but rather an org.gradle.internal.serialize.PlaceholderAssertionError.
 * As a result, the actual and expected values are lost, because Gradle does not support this exception out-of-the-box.
 */
public class GradleDaemonEnhancementClassesProvider {

  private static final List<String> ENHANCEMENT_CLASS_NAMES = Arrays.asList("org.opentest4j.AssertionFailedError");

  @NotNull
  public static String provide() {
    return mapToGroovyLiteral(ENHANCEMENT_CLASS_NAMES);
  }

  @NotNull
  public static String mapToGroovyLiteral(@NotNull Collection<String> collection) {
    return "["
           + collection.stream()
                       .map(it -> "'" + it + "'")
                       .collect(Collectors.joining(","))
           + "]";
  }
}
