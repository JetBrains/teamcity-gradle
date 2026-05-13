package jetbrains.buildServer.gradle.test.integration;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.testng.SkipException;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public class GradleRuntimeProviderTest {

  @Test
  public void shouldListAllGradleVersionsFromConfiguredRuntimeDirectory() throws Exception {
    final String gradleRuntime = System.getProperty(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME);
    if (gradleRuntime == null) {
      throw new SkipException(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME + " is not configured");
    }

    final File runtimeDir = new File(gradleRuntime);
    if (!runtimeDir.isDirectory() || new File(runtimeDir, "bin/gradle").isFile()) {
      throw new SkipException(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME + " does not point to a runtimes directory");
    }

    final File originalProjectRoot = BaseGradleRunnerTest.ourProjectRoot;
    try {
      BaseGradleRunnerTest.ourProjectRoot = Files.createTempDirectory("gradle-runtime-provider").toFile();

      final List<String> versions = BaseGradleRunnerTest.generateGradlePaths().stream()
        .map(version -> version[0])
        .collect(Collectors.toList());

      assertEquals(versions, Arrays.asList(
        "gradle-2.0",
        "gradle-2.5",
        "gradle-3.4.1",
        "gradle-4.0.2",
        "gradle-4.4",
        "gradle-4.10.3",
        "gradle-5.6.4",
        "gradle-6.5.1",
        "gradle-8.2",
        "gradle-8.9",
        "gradle-9.4.1"
      ));
      assertTrue(new File(runtimeDir, "gradle-8.2-bin.zip").isFile());
    } finally {
      BaseGradleRunnerTest.ourProjectRoot = originalProjectRoot;
    }
  }

  @Test
  public void shouldListGradleVersionsFromConfiguredRuntimeDirectory() throws Exception {
    final File originalProjectRoot = BaseGradleRunnerTest.ourProjectRoot;
    final String originalGradleRuntime = System.getProperty(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME);

    final File projectRoot = Files.createTempDirectory("gradle-runtime-provider").toFile();
    final File runtimeDir = new File(projectRoot, "runtimes");
    createGradleRuntime(runtimeDir, "gradle-8.2");
    createGradleRuntime(runtimeDir, "gradle-8.9");

    try {
      BaseGradleRunnerTest.ourProjectRoot = projectRoot;
      System.setProperty(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME, runtimeDir.getAbsolutePath());

      final List<String> versions = BaseGradleRunnerTest.generateGradlePaths().stream()
        .map(version -> version[0])
        .collect(Collectors.toList());

      assertEquals(versions, Arrays.asList("gradle-8.2", "gradle-8.9"));
      assertEquals(BaseGradleRunnerTest.getGradlePath("gradle-8.9"),
                   new File(runtimeDir, "gradle-8.9").getCanonicalPath());
    } finally {
      BaseGradleRunnerTest.ourProjectRoot = originalProjectRoot;
      if (originalGradleRuntime == null) {
        System.clearProperty(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME);
      } else {
        System.setProperty(BaseGradleRunnerTest.PROPERTY_GRADLE_RUNTIME, originalGradleRuntime);
      }
    }
  }

  private static void createGradleRuntime(final File runtimeDir, final String version) throws Exception {
    final File binDir = new File(new File(runtimeDir, version), "bin");
    if (!binDir.mkdirs() && !binDir.isDirectory()) {
      throw new IllegalStateException("Failed to create " + binDir);
    }
    Files.createFile(new File(binDir, "gradle").toPath());
    Files.createFile(new File(binDir, "gradle.bat").toPath());
  }
}
