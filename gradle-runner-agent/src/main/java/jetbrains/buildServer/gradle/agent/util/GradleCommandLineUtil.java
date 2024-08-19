package jetbrains.buildServer.gradle.agent.util;

import org.jetbrains.annotations.NotNull;

public class GradleCommandLineUtil {

  @NotNull
  public static String extractEqualSignSeparatedParamValue(@NotNull String commandLineArg) {
    String[] divided = commandLineArg.split("=");
    return divided.length > 1 ? divided[1] : "";
  }
}
