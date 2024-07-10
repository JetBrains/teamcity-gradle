package jetbrains.buildServer.gradle.test.unit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.gradle.agent.GradleDaemonEnhancementClassesProvider;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class GradleDaemonEnhancementClassesProviderTest {

  @DataProvider
  public Object[][] classesProvider() {
    return new Object[][]{
      { Arrays.asList("org.opentest4j.AssertionFailedError"), "['org.opentest4j.AssertionFailedError']" },
      { Arrays.asList("com.example.ClassA", "org.exam.ClassB", "com.MyClass"), "['com.example.ClassA','org.exam.ClassB','com.MyClass']" },
      { Collections.emptyList(), "[]" },
    };
  }
  @Test(dataProvider = "classesProvider")
  public void should_MapListToGroovyLiteral(List<String> classes, String expected) {
    // arrange, act
    String result = GradleDaemonEnhancementClassesProvider.mapToGroovyLiteral(classes);

    // assert
    assertEquals(result, expected);
  }
}
