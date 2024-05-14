package jetbrains.buildServer.gradle.agent.tasks;

import java.util.List;
import jetbrains.buildServer.util.StringUtil;
import org.jetbrains.annotations.NotNull;

public class GradleTestFilterPostProcessor implements GradleTasksPostProcessor {

  /**
   * Some arguments must be passed to Gradle in quotes if this is configured in the user's build configuration.
   * For this purpose, the {@link StringUtil#splitHonorQuotes} method is used when splitting the tasks string into a list of tasks.
   * See https://youtrack.jetbrains.com/issue/TW-57278.
   *
   * However, in the case of the "--tests" argument for the Java test filtering plugin (https://docs.gradle.org/current/userguide/java_testing.html#test_filtering),
   * we must consider the quotes when splitting into a list of tasks, but the filter value should be passed to Gradle without quotes.
   * Otherwise, Gradle will include them in the filtering, and the tests will not be found:
   * > No tests found for given includes: ["com.example.TestClass.test method with spaces"](--tests filter).
   *
   * E.g.: there is a test in a project: com.example.TestClass.test method with spaces.
   * To correctly account for spaces in the test name, the test is specified in quotes in the user's build configuration:
   * test --tests "com.example.TestClass.test method with spaces".
   * When splitting the string representation of tasks into a list, the quotes will be taken into account:
   * "test --tests \"com.example.TestClass.test method with spaces\"" --> ["test", "--tests", "\"com.example.TestClass.test method with spaces\""].
   * However, for Gradle to correctly apply the filter, extra quotes must be removed:
   * "\"com.example.TestClass.test method with spaces\"" --> "com.example.TestClass.test method with spaces".
   *
   * Solves https://youtrack.jetbrains.com/issue/TW-80350.
   */
  @Override
  public void process(@NotNull List<String> gradleTasks) {
    for (int i = 0; i < gradleTasks.size() - 1; i++) {
      if ("--tests".equals(gradleTasks.get(i))) {
        String testFilter = gradleTasks.get(i + 1);
        if (testFilter != null && isDoubleQuoted(testFilter)) {
          gradleTasks.set(i + 1, StringUtil.unquoteString(testFilter));
        }
      }
    }
  }

  private boolean isDoubleQuoted(@NotNull String aString) {
    return aString.startsWith("\"") && aString.endsWith("\"");
  }
}
