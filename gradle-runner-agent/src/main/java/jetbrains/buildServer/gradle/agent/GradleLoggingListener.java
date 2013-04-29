package jetbrains.buildServer.gradle.agent;

import java.util.LinkedList;
import java.util.List;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
* Created by Nikita.Skvortsov
* Date: 4/27/12, 2:19 PM
*/
class GradleLoggingListener extends ProcessListenerAdapter {

  private final BuildProgressLogger myBuildLogger;
  final private List<String> myErrorMessages = new LinkedList<String>();
  volatile private boolean myCollectErrors = false;
  volatile private boolean myPreviousLineWasEmptyError = false;

  public GradleLoggingListener(final BuildProgressLogger buildLogger) {
    myBuildLogger = buildLogger;
  }

  @Override
  public void onStandardOutput(@NotNull final String text) {
    myBuildLogger.message(text);
    myPreviousLineWasEmptyError = false;
  }

  @Override
  public void onErrorOutput(@NotNull final String text) {
    if (myCollectErrors) {
      myErrorMessages.add(text);
    } else {
      if ((text.trim().startsWith("FAILURE:") && myPreviousLineWasEmptyError)
          || text.contains("[org.gradle.BuildExceptionReporter]")) {
        myCollectErrors = true;
        myErrorMessages.add(text);
        return;
      }

      myPreviousLineWasEmptyError = StringUtil.isEmptyOrSpaces(text);
      myBuildLogger.warning(text);
    }
  }

  @Override
  public void processFinished(final int exitCode) {
    if (exitCode != 0 && shouldReportBuildProblem()) {
      myBuildLogger.activityStarted("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
      myBuildLogger.logBuildProblem(createBuildProblem());
      flushErrorMessages();
      myBuildLogger.activityFinished("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
    } else {
      flushErrorMessages();
    }
    myCollectErrors = false;
  }

  @NotNull
  private BuildProblemData createBuildProblem() {
    final String descr = StringUtil.join("\n", myErrorMessages);
    return BuildProblemData.createBuildProblem(String.valueOf(descr.hashCode()), GradleRunnerConstants.GRADLE_BUILD_PROBLEM_TYPE, descr);
  }

  private boolean shouldReportBuildProblem() {
    final String testErrorPrefix = "> There were failing tests";
    final String compileErrorPrefix = "> Compilation failed";

    for (String errorMessage : myErrorMessages) {
      if (errorMessage.startsWith(testErrorPrefix)
          || errorMessage.startsWith(compileErrorPrefix)) {
        return false;
      }
    }
    return true;
  }

  private void flushErrorMessages() {
    for (String errorMessage : myErrorMessages) {
      myBuildLogger.warning(errorMessage);
    }
    myErrorMessages.clear();
  }

}
