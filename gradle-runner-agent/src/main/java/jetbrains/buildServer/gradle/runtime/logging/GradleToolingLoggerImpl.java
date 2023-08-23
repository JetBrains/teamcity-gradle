package jetbrains.buildServer.gradle.runtime.logging;

import org.jetbrains.annotations.NotNull;

public class GradleToolingLoggerImpl implements GradleToolingLogger {

  private final boolean isDebugEnabled;

  public GradleToolingLoggerImpl(boolean isDebugEnabled) {
    this.isDebugEnabled = isDebugEnabled;
  }

  @Override
  public void debug(@NotNull String message) {
    if (isDebugEnabled) {
      System.out.println(message);
    }
  }

  @Override
  public void lifecycle(@NotNull String message) {
    System.out.println(message);
  }

  @Override
  public void warn(@NotNull String message) {
    System.out.println(message);
  }
}
