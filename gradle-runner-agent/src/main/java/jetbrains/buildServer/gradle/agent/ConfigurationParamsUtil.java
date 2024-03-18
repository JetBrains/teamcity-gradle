

package jetbrains.buildServer.gradle.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.runner.CommandLineArgumentsUtil;
import jetbrains.buildServer.runner.JavaRunnerConstants;
import jetbrains.buildServer.util.StringUtil;
import jetbrains.buildServer.util.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.util.StringUtil.emptyIfNull;

public class ConfigurationParamsUtil
{
  public static @NotNull String getGradleHome(Map<String, String> runParameters)
  {
    return emptyIfNull(runParameters.get(GradleRunnerConstants.GRADLE_HOME));
  }

  public static @NotNull String getJavaArgs(Map<String, String> runParameters)
  {
    return StringUtil.newLineToSpaceDelimited(emptyIfNull(runParameters.get(JavaRunnerConstants.JVM_ARGS_KEY)));
  }

  public static @NotNull String getGradleTasks(Map<String, String> runParameters)
  {
    return emptyIfNull(runParameters.get(GradleRunnerConstants.GRADLE_TASKS));
  }

  public static @NotNull List<String> getGradleParams(Map<String, String> runParameters)
  {
    return CommandLineArgumentsUtil.extractArguments(emptyIfNull(runParameters.get(GradleRunnerConstants.GRADLE_PARAMS)));
  }

  public static boolean isParameterEnabled(final Map<String, String> runParameters, final String key)
  {
    return runParameters.containsKey(key) && runParameters.get(key).equals(Boolean.TRUE.toString());
  }

  public static @NotNull String getGradleInitScript(Map<String, String> runParameters)
  {
    return emptyIfNull(runParameters.get(GradleRunnerConstants.GRADLE_INIT_SCRIPT));
  }

  public static String getGradleWPath(final Map<String, String> runnerParameters) {
    return emptyIfNull(runnerParameters.get(GradleRunnerConstants.GRADLE_WRAPPER_PATH));
  }

  @NotNull
  public static String getGradleInitScript(@NotNull final String gradleVersion) {
    return VersionComparatorUtil.compare(gradleVersion, "8") >= 0
           ? GradleRunnerConstants.INIT_SCRIPT_SINCE_8_NAME
           : GradleRunnerConstants.INIT_SCRIPT_NAME;
  }

  @NotNull
  public static String getGradleLaunchMode(@NotNull final Map<String, String> configurationParameters) {
    return emptyIfNull(configurationParameters.get(GradleRunnerConstants.GRADLE_RUNNER_LAUNCH_MODE_CONFIG_PARAM));
  }

  @NotNull
  public static String getGradleWrapperPropertiesPath(@NotNull final Map<String, String> configurationParameters) {
    return emptyIfNull(configurationParameters.get(GradleRunnerConstants.GRADLE_RUNNER_WRAPPER_PROPERTIES_PATH_CONFIG_PARAM));
  }

  @NotNull
  public static Boolean getBooleanOrDefault(@NotNull final Map<String, String> configurationParameters,
                                            @NotNull final String key,
                                            final boolean defaultValue) {
    return Optional.ofNullable(configurationParameters.get(key))
      .map(Boolean::valueOf)
      .orElse(defaultValue);
  }
}