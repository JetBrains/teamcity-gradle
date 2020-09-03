package jetbrains.buildServer.gradle;

import java.util.HashMap;
import java.util.Map;
import jetbrains.buildServer.messages.serviceMessages.MessageWithAttributes;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes;

/**
 * Created by Nikita.Skvortsov
 * date: 10.10.2014.
 */
public class GradleBuildProblem extends MessageWithAttributes {

  public GradleBuildProblem(final String message) {
    super(ServiceMessageTypes.BUILD_PORBLEM, getAttributes(message));
  }

  private static Map<String, String> getAttributes(final String message) {
    final Map<String, String> result = new HashMap<String, String>();
    result.put("type", GradleRunnerConstants.GRADLE_BUILD_PROBLEM_TYPE);
    result.put("identity", String.valueOf((GradleRunnerConstants.GRADLE_BUILD_PROBLEM_TYPE + message).hashCode()));
    result.put("description", message);
    return result;
  }
}
