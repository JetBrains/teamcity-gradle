package jetbrains.buildServer.gradle.test.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksPostProcessor;
import jetbrains.buildServer.gradle.agent.tasks.GradleTestFilterPostProcessor;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class GradleTestFilterPostProcessorTest {

  private final GradleTasksPostProcessor tasksPostProcessor = new GradleTestFilterPostProcessor();

  @DataProvider
  public Object[][] tasksProvider() {
    return new Object[][]{
      {
        Collections.emptyList(),
        Collections.emptyList()
      },
      {
        Arrays.asList(""),
        Arrays.asList("")
      },
      {
        Arrays.asList("--tests"),
        Arrays.asList("--tests")
      },
      {
        Arrays.asList("--tests", "com.example.TestClass.testMethod"),
        Arrays.asList("--tests", "com.example.TestClass.testMethod")
      },
      {
        Arrays.asList("--tests", "\"com.example.TestClass.testMethod\""),
        Arrays.asList("--tests", "com.example.TestClass.testMethod")
      },
      {
        Arrays.asList("test", "--tests", "\"com.example.TestClass.test method with spaces\""),
        Arrays.asList("test", "--tests", "com.example.TestClass.test method with spaces")
      },
      {
        Arrays.asList("build", "test", "--tests", "\"com.example.TestClass.test method with spaces\""),
        Arrays.asList("build", "test", "--tests", "com.example.TestClass.test method with spaces")
      },
      {
        Arrays.asList("build", "test", "--tests", "\"com.example.TestClass.test method with spaces\"", ":run"),
        Arrays.asList("build", "test", "--tests", "com.example.TestClass.test method with spaces", ":run")
      },
      {
        Arrays.asList("--tests", "\"com.example.TestClass.test method with spaces\"", "test", ":run"),
        Arrays.asList("--tests", "com.example.TestClass.test method with spaces", "test", ":run")
      },
      {
        Arrays.asList("clean", "build", "--continue"),
        Arrays.asList("clean", "build", "--continue")
      }
    };
  }
  @Test(dataProvider = "tasksProvider")
  public void should_ProcessTestFilters(List<String> tasks,
                                        List<String> expected) {
    // act
    tasksPostProcessor.process(tasks);

    // assert
    assertEquals(tasks, expected);
  }
}
