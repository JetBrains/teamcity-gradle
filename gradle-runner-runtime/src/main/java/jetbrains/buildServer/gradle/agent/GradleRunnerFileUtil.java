package jetbrains.buildServer.gradle.agent;

import java.io.*;
import java.nio.file.Files;
import java.util.Properties;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildProperties;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerFileUtil {

  public static void storeProperties(@NotNull File buildTempDirectory,
                                     @NotNull GradleBuildProperties data,
                                     @NotNull File destination) throws IOException {
    createFileInBuildTempDirectory(buildTempDirectory, destination);

    try (OutputStream output = new FileOutputStream(destination)) {
      data.store(output, null);
    }
  }

  public static File createFileInBuildTempDirectory(@NotNull File buildTempDirectory,
                                                    @NotNull File destination) throws IOException {
    buildTempDirectory.mkdirs();

    if (destination.isFile()) {
      FileUtil.delete(destination);
    }
    if (!destination.createNewFile()) {
      throw new IOException("Can't create file: " + destination.getAbsolutePath());
    }

    return FileUtil.getCanonicalFile(destination);
  }

  public static Properties readProperties(@NotNull File propsFile) throws IOException {
    Properties props = new Properties();

    try (InputStream input = Files.newInputStream(propsFile.toPath())) {
      props.load(input);
    }

    return props;
  }
}
