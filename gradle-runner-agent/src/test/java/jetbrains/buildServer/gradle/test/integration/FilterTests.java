package jetbrains.buildServer.gradle.test.integration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import jetbrains.buildServer.util.ArchiveUtil;
import org.testng.annotations.Test;

public class FilterTests extends GradleRunnerServiceMessageTest {
  // Plain
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=1\n" +
                                "#total=2\n" +
                                "#filtering_mode=includes\n" +
                                "test.My2Test\n" +
                                "test.My3Test\n" +
                                "#filtering_mode=excludes\n" +
                                "test.My1Test\n" +
                                "test.My4Test\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.My1Test\n" +
                                "test.My4Test\n" +
                                "#filtering_mode=excludes\n" +
                                "test.My2Test\n" +
                                "test.My3Test\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=include all\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAllTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=exclude all\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAllTests.txt", gradleVersion,
             "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.My1Test\n" +
                                "test.My2Test\n" +
                                "test.My3Test\n" +
                                "test.My4Test\n" +
                                "#filtering_mode=excludes\n" +
                                "NotExists1\n" +
                                "NotExists2\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeNotExistTests.txt", gradleVersion, "##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeAndNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=2\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.My2Test\n" +
                                "NotExists1\n" +
                                "#filtering_mode=excludes\n" +
                                "NotExists2\n" +
                                "test.My1Test\n" +
                                "test.My3Test\n" +
                                "test.My4Test\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeAndNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.My1Test\n" +
                                "test.My3Test\n" +
                                "test.My4Test\n" +
                                "#filtering_mode=excludes\n" +
                                "test.My2Test\n" +
                                "NotExists\n"
                              ).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // Multimodule
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=2\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "module1.My1Test\n" +
                                "module2.My2Test\n" +
                                "#filtering_mode=excludes\n" +
                                "module1.My2Test\n" +
                                "module2.My1Test\n"
                              ).getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleIncludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "module2.My1Test\n" +
                                "module1.My2Test\n" +
                                "#filtering_mode=excludes\n" +
                                "module1.My1Test\n" +
                                "module2.My2Test\n"
                              ).getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleExcludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // Custom filtering
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=2\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.MyUI\n" +
                                "test.MyUnit\n" +
                                "test.MySmoke\n" +
                                "test.excludeDir.Test1\n" +
                                "#filtering_mode=excludes\n" +
                                "test.MyIntegration\n" +
                                "test.includeDir.Test2\n" +
                                "test.includeDir.Test1\n"
                              ).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringIncludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=3\n" +
                                "#total=3\n" +
                                "#filtering_mode=includes\n" +
                                "test.MyIntegration\n" +
                                "test.includeDir.Test2\n" +
                                "#filtering_mode=excludes\n" +
                                "test.MyUI\n" +
                                "test.MyUnit\n" +
                                "test.MySmoke\n" +
                                "test.includeDir.Test1"
                              ).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringExcludeTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringIncludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.file",
                              createFilterFile(
                                "#version=1.0\n" +
                                "#algorithm=test\n" +
                                "#batch_num=2\n" +
                                "#total=3\n" +
                                "#filtering_mode=include all\n"
                              ).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringIncludeAllTests.txt", gradleVersion, "(##teamcity\\[(testStarted|testFinished)(.*?)(?<!\\|)\\])");
  }

  // --tests org.gradle.SomeTestClass
  private File createFilterFile(final String content) throws IOException {
    final File path = File.createTempFile("testsBatch", ".gz");
    try (final OutputStream os = Files.newOutputStream(path.toPath())) {
      ArchiveUtil.packStream(os, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
    return path;
  }
}
