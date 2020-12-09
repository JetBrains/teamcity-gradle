package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import jetbrains.buildServer.BuildProblemData;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

/**
* Created by Nikita.Skvortsov
* Date: 4/27/12, 2:19 PM
*/
class GradleLoggingListener extends ProcessListenerAdapter {

  private enum LastLine {NONE, EMPTY_ERROR, UNZIP_MESSAGE}

  private volatile LastLine myLastLineState = LastLine.NONE;
  private static final Pattern UNZIP_PATTERN = Pattern.compile("Unzipping (\\S*wrapper\\S*.zip) to.*");

  private final BuildProgressLogger myBuildLogger;
  private final List<String> myErrorMessages = new ArrayList<>();
  volatile private boolean myCollectErrors = false;
  private String myWrapperDistPath;

  public GradleLoggingListener(final BuildProgressLogger buildLogger) {
    myBuildLogger = buildLogger;
  }

  @Override
  public void onStandardOutput(@NotNull final String text) {
    final Matcher matcher = UNZIP_PATTERN.matcher(text);
    if (matcher.matches()) {
      myLastLineState = LastLine.UNZIP_MESSAGE;
      myWrapperDistPath = matcher.group(1);
    } else {
      myLastLineState = LastLine.NONE;
    }
    myBuildLogger.message(text);
  }

  @Override
  public void onErrorOutput(@NotNull final String text) {
    if (myCollectErrors) {
      myErrorMessages.add(text);
    } else {
      if ((text.trim().startsWith("FAILURE:") && LastLine.EMPTY_ERROR.equals(myLastLineState))
          || text.contains(".BuildExceptionReporter]")) {
        myCollectErrors = true;
        myErrorMessages.add(text);
        return;
      }

      if (StringUtil.isEmptyOrSpaces(text)) {
        myLastLineState = LastLine.EMPTY_ERROR;
      }
      myBuildLogger.warning(text);
    }
  }

  @Override
  public void processFinished(final int exitCode) {
    if (exitCode != 0 ) {
      if (!myErrorMessages.isEmpty()) {
        myBuildLogger.activityStarted("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
        flushErrorMessages();
        myBuildLogger.activityFinished("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
      } else if (LastLine.UNZIP_MESSAGE.equals(myLastLineState)) {
        myBuildLogger.warning("Gradle wrapper failed to unpack downloaded archive: [" + myWrapperDistPath + "]. It will be deleted to force re-download next time.");
        myBuildLogger.logBuildProblem(createBuildProblem("Gradle wrapper failed to unpack downloaded distribution."));
        FileUtil.delete(new File(myWrapperDistPath));
      }
    } else {
      flushErrorMessages();
    }
    myCollectErrors = false;
  }

  @NotNull
  private BuildProblemData createBuildProblem(@NotNull final String descr) {
    return BuildProblemData.createBuildProblem(String.valueOf(descr.hashCode()), GradleRunnerConstants.GRADLE_BUILD_PROBLEM_TYPE, descr);
  }

  private void flushErrorMessages() {
    myErrorMessages.forEach(msg -> myBuildLogger.warning(msg));
    myErrorMessages.clear();
  }

}
