package jetbrains.buildServer.gradle.agent;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import jetbrains.buildServer.gradle.agent.propertySplit.GradleBuildProperties;
import jetbrains.buildServer.util.FileUtil;
import org.jetbrains.annotations.NotNull;

public class GradleRunnerFileUtil {

  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  public static void storeParams(@NotNull File buildTempDirectory,
                                 @NotNull Map<String, String> data,
                                 @NotNull File destination) throws IOException {
    createFileInBuildTempDirectory(buildTempDirectory, destination);

    try (FileWriter writer = new FileWriter(destination)) {
      GSON.toJson(data, writer);
    }
  }

  public static void storeProperties(@NotNull File buildTempDirectory,
                                     @NotNull GradleBuildProperties data,
                                     @NotNull File destination) throws IOException {
    createFileInBuildTempDirectory(buildTempDirectory, destination);

    try (OutputStream output = new FileOutputStream(destination)) {
      data.store(output, null);
    }
  }

  public static Map<String, String> readParamsMap(@NotNull String sourceFilePath) throws IOException {
    File source = new File(sourceFilePath);
    if (!source.exists()) {
      throw new IOException("Source file doesn't exist in file system: " + sourceFilePath);
    }

    Map<String, String> result;
    try (Reader reader = new FileReader(source)) {
      result = GSON.fromJson(reader, new TypeToken<Map<String, String>>() {}.getType());
    }

    return result;
  }

  public static void storeParams(@NotNull File buildTempDirectory,
                                 @NotNull Collection<String> data,
                                 @NotNull File destination) throws IOException {
    createFileInBuildTempDirectory(buildTempDirectory, destination);

    try (FileWriter writer = new FileWriter(destination)) {
      GSON.toJson(data, writer);
    }
  }

  public static List<String> readParams(@NotNull String sourceFilePath) throws IOException {
    File source = new File(sourceFilePath);
    if (!source.exists()) {
      throw new IOException("Source file doesn't exist in file system: " + sourceFilePath);
    }

    List<String> result;
    try (Reader reader = new FileReader(source)) {
      result = GSON.fromJson(reader, new TypeToken<List<String>>() {}.getType());
    }

    return result;
  }

  private static File createFileInBuildTempDirectory(@NotNull File buildTempDirectory,
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
