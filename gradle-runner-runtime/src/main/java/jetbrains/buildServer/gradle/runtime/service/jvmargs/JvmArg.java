package jetbrains.buildServer.gradle.runtime.service.jvmargs;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

public class JvmArg {

  private static final String XMS_KEY = "-Xms";
  private static final String XMX_KEY = "-Xmx";
  private static final String XSS_KEY = "-Xss";
  private static final String XMN_KEY = "-Xmn";
  private static final Set<String> SUPPORTED_COLON_SEPARATED_ARG_PREFIXES = new HashSet<>(Arrays.asList(
    "-X", "-verbose", "-agentlib", "-agentpath", "-javaagent", "-splash",
    "--class-path", "-classpath", "-cp",
    "-enableassertions", "-ea", "-disableassertions", "-da"
    ));
  private static final Set<String> PACKAGE_ACCESSIBILITY_JVM_ARGS = new HashSet<>(Arrays.asList("--add-opens", "--add-exports"));

  public static final char ARGS_DELIMITER = '=';
  public static final String PREFIX = "-";
  public static final String EMPTY_VALUE = "";

  private final String key;
  private final String value;

  private JvmArg(String key, String value) {
    this.key = key;
    this.value = value;
  }

  @NotNull
  public static JvmArg ofString(@NotNull String arg) {
    if (arg.startsWith(XMS_KEY)) {
      return new JvmArg(XMS_KEY, arg.substring(XMS_KEY.length()));
    }
    if (arg.startsWith(XMX_KEY)) {
      return new JvmArg(XMX_KEY, arg.substring(XMX_KEY.length()));
    }
    if (arg.startsWith(XSS_KEY)) {
      return new JvmArg(XSS_KEY, arg.substring(XSS_KEY.length()));
    }
    if (arg.startsWith(XMN_KEY)) {
      return new JvmArg(XMN_KEY, arg.substring(XMN_KEY.length()));
    }

    if (isColonSeparatedArg(arg)) {
      return new JvmArg(arg.substring(0, arg.indexOf(':')), arg.substring(arg.indexOf(':')));
    }

    int i = arg.indexOf(ARGS_DELIMITER);
    return i <= 0
           ? new JvmArg(arg, EMPTY_VALUE)
           : new JvmArg(arg.substring(0, i), arg.substring(i));
  }

  private static boolean isColonSeparatedArg(@NotNull String arg) {
    int colonIndex = arg.indexOf(':');
    int delimiterIndex = arg.indexOf(ARGS_DELIMITER);

    return SUPPORTED_COLON_SEPARATED_ARG_PREFIXES.stream().anyMatch(prefix -> arg.startsWith(prefix))
           && !arg.startsWith("-XX:")
           && colonIndex > 0 && delimiterIndex <= 0;
  }

  public static boolean isPackageAccessibilityJvmArg(@NotNull String argKey) {
    return PACKAGE_ACCESSIBILITY_JVM_ARGS.contains(argKey);
  }

  @NotNull
  public String getKey() {
    return key;
  }

  @NotNull
  public String getValue() {
    return value;
  }
}
