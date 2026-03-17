package jetbrains.buildServer.gradle.runtime.output;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.gradle.runtime.listening.event.FailureKind;

/**
 * Information about tasks failed during the build
 */
public class BuildFailedTaskData {

  private final String myTaskPath;
  private final FailureKind myFailureKind;
  private Collection<String> myFailureMessages;

  public BuildFailedTaskData(String taskPath, FailureKind failureKind) {
    myTaskPath = taskPath;
    myFailureKind = failureKind;
    myFailureMessages = Collections.emptyList();
  }

  public String getTaskPath() {
    return myTaskPath;
  }

  public FailureKind getFailureKind() {
    return myFailureKind;
  }

  public Collection<String> getFailureMessages() {
    return myFailureMessages;
  }

  public void setFailureMessages(List<String> failureMessages) {
    myFailureMessages = Collections.unmodifiableList(failureMessages);
  }
}
