package jetbrains.buildServer.gradle.test.unit;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import jetbrains.buildServer.gradle.runtime.service.jvmargs.GradleJvmArgsMerger;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static com.intellij.util.containers.ContainerUtil.emptyList;
import static org.testng.Assert.*;

public class GradleJvmArgsMergerTest {

  private GradleJvmArgsMerger merger;

  @BeforeClass
  public void setUp() {
    Mockery context = new Mockery();
    GradleToolingLogger logger = context.mock(GradleToolingLogger.class);

    merger = new GradleJvmArgsMerger(logger);

    context.checking(new Expectations() {{
        allowing(logger).debug(with(any(String.class)));
    }});
  }

  @Test
  public void should_MergeJvmArgs_When_TcJvmArgsAreNotSet() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-X:foo");
    List<String> tcJvmArgs = emptyList();
    List<String> expectedResult = Arrays.asList("-X:foo");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @Test
  public void should_MergeJvmArgs_When_GradleProjectJvmArgsAreNotSet() {
    // arrange
    List<String> gradleProjectJvmArgs = emptyList();
    List<String> tcJvmArgs = Arrays.asList("-X:foo");
    List<String> expectedResult = Arrays.asList("-X:foo");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @Test
  public void should_OverrideGradleProjectJvmArgsWithTcJvmArgs_When_HaveTheSameSystemProperty() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-Dp=val");
    List<String> tcJvmArgs = Arrays.asList("-Dp=newVal");
    List<String> expectedResult = Arrays.asList("-Dp=newVal");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @Test
  public void should_MergeGradleProjectJvmArgsWithTcJvmArgs_When_HaveDifferentProperties() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-X:foo");
    List<String> tcJvmArgs = Arrays.asList("-Dp=v");
    List<String> expectedResult = Arrays.asList("-X:foo", "-Dp=v");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @Test
  public void should_MergeGradleProjectJvmArgsWithTcJvmArgs_When_HaveCompositeProperties() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-X:foo", "-Foo", "bar=001", "-Foo", "baz=002");
    List<String> tcJvmArgs = Arrays.asList("-Dp=v", "-Foo", "bar=003", "-Foo", "baz=002");
    List<String> expectedResult = Arrays.asList("-X:foo", "-Foo", "bar=003", "-Foo", "baz=002", "-Dp=v");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @Test
  public void should_MergeGradleProjectJvmArgsWithTcJvmArgs_When_HaveAddOpensProperties() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-Xmx256", "--add-opens", "java.base/java.util=ALL-UNNAMED");
    List<String> tcJvmArgs = Arrays.asList("-Xmx512", "--add-opens", "java.base/java.lang=ALL-UNNAMED");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertFalse(mergingResult.contains("-Xmx256"));
    assertTrue(mergingResult.contains("-Xmx512"));
    assertFalse(mergingResult.contains("--add-opens"));
    assertFalse(mergingResult.contains("java.base/java.util=ALL-UNNAMED"));
    assertFalse(mergingResult.contains("java.base/java.lang=ALL-UNNAMED"));
  }

  @Test
  public void should_FilterNullableAndEmptyValues_When_MergingArgs() {
    // arrange
    List<String> gradleProjectJvmArgs = Arrays.asList("-X:foo", "", null, "-Dparam=value", "-XX:ParallelGCThreads=1", "-XX:+UseZGC", null);
    List<String> tcJvmArgs = Arrays.asList(null, "-Dparam=value", "", "", null, "-XX:ParallelGCThreads=300");
    List<String> expectedResult = Arrays.asList("-X:foo", "-Dparam=value", "-XX:ParallelGCThreads=300", "-XX:+UseZGC");

    // act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @DataProvider
  public Object[][] memoryJvmArgsProvider() {
    return new Object[][]{
      { Arrays.asList("-Xmx512"), Arrays.asList("-Xmx512"), Arrays.asList("-Xmx512") },
      { Arrays.asList("-Xmx256"), Arrays.asList("-Xmx512"), Arrays.asList("-Xmx512") },
      { Arrays.asList("-Xmx512"), Arrays.asList("-Xmx256"), Arrays.asList("-Xmx256") },
      { Arrays.asList("-Xmx256"), emptyList(), Arrays.asList("-Xmx256") },
      { emptyList(), Arrays.asList("-Xmx256"), Arrays.asList("-Xmx256") },

      {
        Arrays.asList("-Xmx128", "-Xms128", "-Xss128", "-Xmn128"),
        Arrays.asList("-Xmx1024", "-Xms1024", "-Xss1024", "-Xmn1024"),
        Arrays.asList("-Xmx1024", "-Xms1024", "-Xss1024", "-Xmn1024")
      },
      {
        Arrays.asList("-Xmx128", "-Xms128", "-Xss128", "-Xmn128"),
        Arrays.asList("-Xms1024"),
        Arrays.asList("-Xmx128", "-Xms1024", "-Xss128", "-Xmn128")
      },
      {
        Arrays.asList("-Xmn64", "-Xms32"),
        Arrays.asList("-Xmx2048", "-Xms32", "-Xss2048", "-Xmn2048"),
        Arrays.asList("-Xmn2048", "-Xms32", "-Xmx2048", "-Xss2048")
      },

      {
        Arrays.asList("-XX:MaxPermSize=256m", "-XX:+HeapDumpOnOutOfMemoryError"),
        Arrays.asList("-XX:MaxPermSize=4096m"),
        Arrays.asList("-XX:MaxPermSize=4096m", "-XX:+HeapDumpOnOutOfMemoryError")
      },
      {
        Arrays.asList("-XX:MaxMetaspaceSize=256m"),
        Arrays.asList("-XX:MaxMetaspaceSize=4096m", "-XX:+UseG1GC"),
        Arrays.asList("-XX:MaxMetaspaceSize=4096m", "-XX:+UseG1GC")
      },
      {
        Arrays.asList("-XX:+UseParallelGC", "-Xms32"),
        Arrays.asList("-XX:+UseParallelGC", "-Xmx2048"),
        Arrays.asList("-XX:+UseParallelGC", "-Xms32", "-Xmx2048")
      },
      {
        Arrays.asList("-XX:+UseParallelGC", "-Xms32"),
        Arrays.asList("-Xmx2048"),
        Arrays.asList("-XX:+UseParallelGC", "-Xms32", "-Xmx2048")
      }
    };
  }
  @Test(dataProvider = "memoryJvmArgsProvider")
  public void should_MergeGradleProjectJvmArgsWithTcJvmArgs_When_HaveMemoryProperties(List<String> gradleProjectJvmArgs,
                                                                                      List<String> tcJvmArgs,
                                                                                      List<String> expectedResult) {
    // arrange, act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }

  @DataProvider
  public Object[][] colonSeparaterJvmArgsProvider() {
    return new Object[][]{
      {
        Arrays.asList("-agentlib:jdwp=transport=dt_socket,server=y,address=8000"),
        Arrays.asList("-agentlib:jdwp=transport=dt_socket"),
        Arrays.asList("-agentlib:jdwp=transport=dt_socket")
      },
      {
        Arrays.asList("-Xshare:auto"),
        Arrays.asList("-Xshare:on"),
        Arrays.asList("-Xshare:on")
      },
      {
        Arrays.asList("-XshowSettings:all"),
        Arrays.asList("-XshowSettings:locale"),
        Arrays.asList("-XshowSettings:locale")
      },
      {
        Arrays.asList("-Xdock:name=application_name"),
        Arrays.asList("-Xdock:name=application_name_overrided"),
        Arrays.asList("-Xdock:name=application_name_overrided"),
      },
      {
        Arrays.asList("-Xdock:icon=path_to_icon_file"),
        Arrays.asList("-Xdock:icon=path_to_icon_file_overrided"),
        Arrays.asList("-Xdock:icon=path_to_icon_file_overrided"),
      },
      {
        Arrays.asList("-XX:+UnlockDiagnosticVMOptions"),
        Arrays.asList("-XX:+UnlockDiagnosticVMOptions"),
        Arrays.asList("-XX:+UnlockDiagnosticVMOptions"),
      },
      {
        Arrays.asList("-XX:+UseG1GC"),
        Arrays.asList("-XX:+UnlockDiagnosticVMOptions"),
        Arrays.asList("-XX:+UseG1GC", "-XX:+UnlockDiagnosticVMOptions"),
      },
      {
        Arrays.asList("-verbose:class"),
        Arrays.asList("-verbose:gc"),
        Arrays.asList("-verbose:gc"),
      },
      {
        Arrays.asList("-agentlib:foo"),
        Arrays.asList("-agentlib:bar"),
        Arrays.asList("-agentlib:bar"),
      },
      {
        Arrays.asList("-agentpath:foo"),
        Arrays.asList("-agentpath:bar"),
        Arrays.asList("-agentpath:bar"),
      },
      {
        Arrays.asList("--class-path:/Applications/app.jar:/Applications/app-2.jar:/Users/app-3.jar"),
        Arrays.asList("--class-path:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
        Arrays.asList("--class-path:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
      },
      {
        Arrays.asList("-classpath:/Applications/app.jar:/Applications/app-2.jar:/Users/app-3.jar"),
        Arrays.asList("-classpath:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
        Arrays.asList("-classpath:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
      },
      {
        Arrays.asList("-cp:/Applications/app.jar:/Applications/app-2.jar:/Users/app-3.jar"),
        Arrays.asList("-cp:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
        Arrays.asList("-cp:/Applications/app.jar:/Applications/app-2.jar:/Users/app-300.jar"),
      },
      {
        Arrays.asList("-Xbootclasspath:lib/abc.jar;lib/def.jar;lib/ghi.jar"),
        Arrays.asList("-Xbootclasspath:lib/jkl.jar;lib/mno.jar;lib/pqr.jar"),
        Arrays.asList("-Xbootclasspath:lib/jkl.jar;lib/mno.jar;lib/pqr.jar"),
      },
      {
        Arrays.asList("-ea:com.assertion... com.assertion.Assertion"),
        Arrays.asList("-ea:ru.assertion... ru.assertion.Overrided"),
        Arrays.asList("-ea:ru.assertion... ru.assertion.Overrided"),
      },
      {
        Arrays.asList("-javaagent:/path/to/agent.jar=argumentstring"),
        Arrays.asList("-javaagent:/path/to/agent.jar=overrided"),
        Arrays.asList("-javaagent:/path/to/agent.jar=overrided"),
      },
      {
        Arrays.asList("-splash:images/splash.gif"),
        Arrays.asList("-splash:images/splash-overrided.gif"),
        Arrays.asList("-splash:images/splash-overrided.gif"),
      },
      {
        Arrays.asList("-Xlog"),
        Arrays.asList("-Xlog:disable"),
        Arrays.asList("-Xlog:disable"),
      },
    };
  }
  @Test(dataProvider = "colonSeparaterJvmArgsProvider")
  public void should_MergeGradleProjectJvmArgsWithTcJvmArgs_When_HaveColonSeparaterProperties(List<String> gradleProjectJvmArgs,
                                                                                              List<String> tcJvmArgs,
                                                                                              List<String> expectedResult) {
    // arrange, act
    Collection<String> mergingResult = merger.mergeJvmArguments(gradleProjectJvmArgs, tcJvmArgs);

    // assert
    assertEquals(mergingResult, expectedResult);
  }
}
