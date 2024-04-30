package jetbrains.buildServer.gradle.agent.tasks;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.agent.ConfigurationParamsUtil;
import jetbrains.buildServer.gradle.agent.GradleLaunchMode;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.*;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.INIT_SCRIPT_DIR;

public class GradleTasksComposer {

  @NotNull
  public List<String> getGradleTasks(@NotNull Map<String, String> runnerParameters) {
    String gradleTasks = ConfigurationParamsUtil.getGradleTasks(runnerParameters);
    return gradleTasks.length() > 0
           ? StringUtil.splitHonorQuotes(gradleTasks, GRADLE_TASKS_DELIMITER).stream()
                       .map(String::trim)
                       .collect(Collectors.toList())
           : Collections.emptyList();
  }

  @NotNull
  public List<String> getGradleParameters(@NotNull GradleLaunchMode gradleLaunchMode,
                                          @NotNull Map<String, String> runnerParameters,
                                          @NotNull File pluginsDirectory) {
    final List<String> params = new ArrayList<>();

    params.addAll(getInitScriptParams(gradleLaunchMode, runnerParameters, pluginsDirectory));
    params.addAll(ConfigurationParamsUtil.getGradleParams(runnerParameters));

    if (!hasDaemonParam(runnerParameters))
      params.add("-Dorg.gradle.daemon=false");

    if (ConfigurationParamsUtil.isParameterEnabled(runnerParameters, GradleRunnerConstants.DEBUG))
      params.add("-d");

    if (ConfigurationParamsUtil.isParameterEnabled(runnerParameters, GradleRunnerConstants.STACKTRACE))
      params.add("-s");

    String path = runnerParameters.get(GradleRunnerConstants.PATH_TO_BUILD_FILE);
    if (StringUtil.isNotEmpty(path)) {
      params.add("-b");
      params.add(path);
    }

    return params;
  }

  private boolean hasDaemonParam(@NotNull Map<String, String> runnerParameters) {
    for (String param: ConfigurationParamsUtil.getGradleParams(runnerParameters)) {
      if (param.startsWith("-Dorg.gradle.daemon=")) {
        return true;
      }
      if ("--daemon".equals(param) || "--no-daemon".equals(param)) {
        return true;
      }
    }
    return false;
  }

  @NotNull
  private List<String> getInitScriptParams(@NotNull GradleLaunchMode gradleLaunchMode,
                                           @NotNull Map<String, String> runnerParameters,
                                           @NotNull File pluginsDirectory) {
    final String scriptPath = ConfigurationParamsUtil.getGradleInitScript(runnerParameters);
    File initScript;

    if (!scriptPath.isEmpty()) {
      initScript = new File(scriptPath);
    } else {
      File runnerPluginDir = new File(pluginsDirectory, GradleRunnerConstants.RUNNER_TYPE);
      String initScriptName = gradleLaunchMode == GradleLaunchMode.COMMAND_LINE
                              ? INIT_SCRIPT_NAME
                              : INIT_SCRIPT_SINCE_8_NAME;
      initScript = new File(new File(runnerPluginDir, INIT_SCRIPT_DIR), initScriptName);
    }

    return Arrays.asList("--init-script", initScript.getAbsolutePath());
  }
}
