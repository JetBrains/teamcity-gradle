package jetbrains.buildServer.gradle.test.integration;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import jetbrains.buildServer.util.ArchiveUtil;
import jetbrains.buildServer.util.StringUtil;
import org.testng.annotations.Test;

public class FilterTests extends GradleRunnerServiceMessageTest {
  // Plain
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE", Arrays.asList("test.My2Test", "test.My3Test")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeTests.txt", gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE", Arrays.asList("test.My2Test", "test.My3Test")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeTests.txt", gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE ALL", Collections.emptyList()).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAllTests.txt", gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE ALL", Collections.emptyList()).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeAllTests.txt", gradleVersion,
             "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])|(##teamcity\\[block(.*?)(?<!\\|)name=':test'\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE", Arrays.asList("NotExists1", "NotExists2")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeNotExistTests.txt", gradleVersion,
             "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])|(##teamcity\\[block(.*?)(?<!\\|)name=':test'\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE", Arrays.asList("NotExists1", "NotExists2")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeNotExistTests.txt", gradleVersion, "##teamcity\\[(test|message)(.*?)(?<!\\|)\\]");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainIncludeAndNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE", Arrays.asList("test.My2Test", "NotExists")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainIncludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void plainExcludeAndNotExistTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE", Arrays.asList("test.My2Test", "NotExists")).getCanonicalPath());
    testTest(PROJECT_S_NAME, "test", "filterTests/plainExcludeAndNotExistTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  // Multimodule
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE", Arrays.asList("module1.My1Test", "module2.My2Test")).getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleIncludeTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void multimoduleExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE", Arrays.asList("module1.My1Test", "module2.My2Test")).getCanonicalPath());
    testTest(PROJECT_SM_NAME, "test", "filterTests/multimoduleExcludeTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  // Custom filtering
  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringIncludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE", Arrays.asList("test.MyUI", "test.MyUnit", "test.MySmoke", "test.excludeDir.Test1")).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringIncludeTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringExcludeTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE", Arrays.asList("test.MyUI", "test.MyUnit", "test.MySmoke", "test.includeDir.Test1")).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringExcludeTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringIncludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("INCLUDE ALL", Collections.emptyList()).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringIncludeAllTests.txt", gradleVersion, "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])");
  }

  @Test(dataProvider = "gradle-version-provider>=5.0")
  public void withCustomFilteringExcludeAllTests(final String gradleVersion) throws Exception {
    myTeamCitySystemProps.put("teamcity.build.parallelTests.testsBatch.artifactPath",
                              createFilterFile("EXCLUDE ALL", Collections.emptyList()).getCanonicalPath());
    testTest(PROJECT_SF_NAME, "test", "filterTests/withCustomFilteringExcludeAllTests.txt", gradleVersion,
             "(##teamcity\\[(test|message)(.*?)(?<!\\|)\\])|(##teamcity\\[block(.*?)(?<!\\|)name=':test'\\])");
  }

  // --tests org.gradle.SomeTestClass

  private File createFilterFile(final String type, final List<String> tests) throws IOException {
    final String content = type + "\n" + StringUtil.join("\n", tests);
    final File path = File.createTempFile("testsBatch", ".gz");
    try (final OutputStream os = new FileOutputStream(path)) {
      ArchiveUtil.packStream(os, new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
    }
    return path;
  }
}
