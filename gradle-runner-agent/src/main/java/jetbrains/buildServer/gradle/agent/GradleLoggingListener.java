package jetbrains.buildServer.gradle.agent;

import java.util.LinkedList;
import java.util.List;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.ProcessListenerAdapter;
import jetbrains.buildServer.messages.DefaultMessagesInfo;
import org.jetbrains.annotations.NotNull;

/**
* Created by Nikita.Skvortsov
* Date: 4/27/12, 2:19 PM
*/
class GradleLoggingListener extends ProcessListenerAdapter {

  private final BuildProgressLogger myBuildLogger;
  final private List<String> myErrorMessages = new LinkedList<String>();
  volatile private boolean myCollectErrors = false;

  public GradleLoggingListener(final BuildProgressLogger buildLogger) {
    myBuildLogger = buildLogger;
  }

  @Override
  public void onStandardOutput(@NotNull final String text) {
    myBuildLogger.message(text);
  }

  @Override
  public void onErrorOutput(@NotNull final String text) {
    if (myCollectErrors) {
      myErrorMessages.add(text);
    } else {
      if ("".equals(text.trim())) {
        assert myErrorMessages.size() == 0;
        myErrorMessages.add(text);
        return;
      }

      if ((text.trim().startsWith("FAILURE:")
          && myErrorMessages.size() == 1
          && "".equals(myErrorMessages.get(0)))
          || text.contains("[org.gradle.BuildExceptionReporter]")) {
        myCollectErrors = true;
        myErrorMessages.add(text);
        return;
      }

      myBuildLogger.warning(text);
    }
  }

  @Override
  public void processFinished(final int exitCode) {
    if (exitCode != 0) {
      myBuildLogger.activityStarted("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
      flushErrorMessages();
      myBuildLogger.activityFinished("Gradle failure report", DefaultMessagesInfo.BLOCK_TYPE_TARGET);
    } else {
      flushErrorMessages();
      myCollectErrors = false;
    }
  }

  private void flushErrorMessages() {
    for (String errorMessage : myErrorMessages) {
      myBuildLogger.warning(errorMessage);
    }
    myErrorMessages.clear();
  }

}
