package jetbrains.buildServer.gradle.agent;

import java.io.File;
import java.util.regex.Pattern;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.agent.runner.ProcessListener;
import jetbrains.buildServer.messages.ErrorData;
import org.jetbrains.annotations.NotNull;

/**
* Created by Nikita.Skvortsov
* Date: 4/27/12, 2:19 PM
*/
public class GradleVersionErrorsListener implements ProcessListener {

  public static final String WRONG_GRADLE_VERSION = "Incompatible Gradle initialization script API.\n" +
                                                    "Please, make sure you are running correct Gradle versoin. TeamCity requires Gradle ver. 0.9-rc-1 at least.";
  private final Pattern initScriptFailure = Pattern.compile("Could not load compiled classes for initialization script(.*?)init\\.gradle" +
                                                            "|Could not compile initialization script(.*?)init\\.gradle" +
                                                            "|'init-script' is not a recognized option" +
                                                            "|A problem occurred evaluating initialization script");

  private final BuildProgressLogger myLogger;

  GradleVersionErrorsListener(@NotNull final BuildProgressLogger logger) {
    myLogger = logger;
  }

  public void onStandardOutput(@NotNull final String text) {
    parseAndLog(text);
  }

  public void onErrorOutput(@NotNull final String text) {
    parseAndLog(text);
  }

  private void parseAndLog(@NotNull final String text) {
    if (!text.startsWith("##teamcity[") &&
      initScriptFailure.matcher(text).find()) {
      myLogger.internalError(ErrorData.BUILD_RUNNER_ERROR_TYPE,WRONG_GRADLE_VERSION,null);
    }
  }

  public void processStarted(@NotNull final String programCommandLine, @NotNull final File workingDirectory) {
    // do nothing
  }

  public void processFinished(final int exitCode) {
  }
}
