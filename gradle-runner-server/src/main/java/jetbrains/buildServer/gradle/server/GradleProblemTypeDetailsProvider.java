package jetbrains.buildServer.gradle.server;

import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.problems.BaseBuildProblemTypeDetailsProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Nikita.Skvortsov
 * Date: 1/21/13, 1:49 PM
 */
public class GradleProblemTypeDetailsProvider extends BaseBuildProblemTypeDetailsProvider {

  @Nullable
  @Override
  public String getStatusText(@NotNull final BuildProblemData buildProblem, @NotNull final SBuild build) {
    return "Gradle exception";
  }

  @Nullable
  @Override
  public String getTypeDescription() {
    return "Gradle failure";
  }

  @NotNull
  public String getType() {
    return GradleRunnerConstants.GRADLE_BUILD_PROBLEM_TYPE;
  }
}
