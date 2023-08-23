package jetbrains.buildServer.gradle.runtime.logging;

import org.jetbrains.annotations.NotNull;

/**
 * Logger of the Gradle Tooling API build
 */
public interface GradleToolingLogger {

  void debug(@NotNull String message);

  void lifecycle(@NotNull String message);

  void warn(@NotNull String message);
}
