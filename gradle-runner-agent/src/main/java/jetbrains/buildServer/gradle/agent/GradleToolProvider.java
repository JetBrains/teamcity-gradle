

package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.Map;
import jetbrains.buildServer.agent.*;
import jetbrains.buildServer.agent.util.ParameterValueFinder;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Author: Nikita.Skvortsov
 * Date: Sep 24, 2010
 */
public class GradleToolProvider {

  public static final String GRADLE_TOOL = "gradle";
  private static final String GRADLE_HOME = "GRADLE_HOME";

  public GradleToolProvider(@NotNull ToolProvidersRegistry toolProvidersRegistry,
                            @NotNull final BundledToolsRegistry bundled) {
    toolProvidersRegistry.registerToolProvider(new ToolProvider() {

      public boolean supports(@NotNull final String toolName) {
        return GRADLE_TOOL.equals(toolName);
      }

      @NotNull
      public String getPath(@NotNull final String toolName) throws ToolCannotBeFoundException {
        // try to get locally installed gradle
        String gradleHomePath = System.getenv(GRADLE_HOME);
        if (null != gradleHomePath && gradleHomePath.length() > 0) {
          return gradleHomePath;
        } else {
          final BundledTool tool = bundled.findTool(GRADLE_TOOL);
          if (tool == null) {
            throw new ToolCannotBeFoundException("Couldn't locate Gradle installation. Please use wrapper script or install Gradle and set environment variable GRADLE_HOME");
          }
          return tool.getRootPath().getPath();
        }
      }

      @NotNull
      public String getPath(@NotNull final String toolName,
                            @NotNull final AgentRunningBuild build,
                            @NotNull final BuildRunnerContext runner)
        throws ToolCannotBeFoundException {
        final Map<String, String> runParameters = runner.getRunnerParameters();
        final Map<String, String> buildParameters = runner.getBuildParameters().getAllParameters();
        final ParameterValueFinder parameterValueFinder = new ParameterValueFinder("Gradle runner home", Constants.ENV_PREFIX + GRADLE_HOME);
        final String gradleHome = parameterValueFinder.getPropertyValue(runParameters.get(GradleRunnerConstants.GRADLE_HOME), buildParameters);

        if (gradleHome != null) {
                  return FileUtil.resolvePath(new File(AgentRuntimeProperties.getCheckoutDir(runParameters)), gradleHome).getAbsolutePath();
        } else {
          throw new ToolCannotBeFoundException("Couldn't locate Gradle installation. Please use wrapper script or install Gradle and set environment variable GRADLE_HOME");
        }
      }
    });
  }

}