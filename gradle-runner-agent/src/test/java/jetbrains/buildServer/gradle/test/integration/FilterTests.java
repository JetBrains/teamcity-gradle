package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.testng.annotations.Test;

public class FilterTests extends GradleRunnerServiceMessageTest {
  // Plain
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=1\n" +
      "#total_batches=2\n" +
      "test.My1Test\n" +
      "test.My4Test\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=3\n" +
      "#total_batches=3\n" +
      "test.My2Test\n" +
      "test.My3Test\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());

    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeNotExistTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=3\n" +
      "#total_batches=3\n" +
      "NotExists1\n" +
      "NotExists2\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeNotExistTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeAndNotExistTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=2\n" +
      "#total_batches=3\n" +
      "NotExists2\n" +
      "test.My1Test\n" +
      "test.My3Test\n" +
      "test.My4Test\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeAndNotExistTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=3\n" +
      "#total_batches=3\n" +
      "test.My2Test\n" +
      "NotExists\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // Multimodule
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleIncludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=2\n" +
      "#total_batches=3\n" +
      "module1.My2Test\n" +
      "module2.My1Test\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleIncludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleExcludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=3\n" +
      "#total_batches=3\n" +
      "module1.My1Test\n" +
      "module2.My2Test\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleExcludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // Custom filtering
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringIncludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=2\n" +
      "#total_batches=3\n" +
      "test.MyIntegration\n" +
      "test.includeDir.Test2\n" +
      "test.includeDir.Test1\n"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringIncludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringExcludeTests(final String gradleVersion) throws Exception {
    final File excludes = createFilterFile(
      "#version=1\n" +
      "#algorithm=test\n" +
      "#current_batch=3\n" +
      "#total_batches=3\n" +
      "test.MyUI\n" +
      "test.MyUnit\n" +
      "test.MySmoke\n" +
      "test.includeDir.Test1"
    );
    myTeamCitySystemProps.put("teamcity.build.parallelTests.excludesFile", excludes.getCanonicalPath());
    myTeamCitySystemProps.put("teamcity.build.parallelTests.includesFile", excludes.getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringExcludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // --tests org.gradle.SomeTestClass
  private File createFilterFile(final String content) throws IOException {
    final File path = File.createTempFile("excludedTests", ".txt");
    try (final OutputStream os = Files.newOutputStream(path.toPath())) {
      os.write(content.getBytes(StandardCharsets.UTF_8));
    }
    return path;
  }
}
