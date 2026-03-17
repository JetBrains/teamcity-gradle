package jetbrains.buildServer.gradle.runtime;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.jetbrains.annotations.NotNull;

public class LauncherParameters {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

  @NotNull
  private final List<String> values;

  private LauncherParameters(@NotNull List<String> values) {
    this.values = values;
  }

  @NotNull
  public static LauncherParameters fromValues(@NotNull List<String> values) {
    return new LauncherParameters(values);
  }

  @NotNull
  public static LauncherParameters fromFile(@NotNull Path file) throws IOException {
    try (Reader reader = Files.newBufferedReader(file)) {
      List<String> values = GSON.fromJson(reader, new TypeToken<List<String>>() {
      }.getType());
      return new LauncherParameters(values);
    } catch (IOException e) {
      throw new IOException("Failed to read Gradle launcher parameters from " + file, e);
    }
  }

  @NotNull
  public List<String> get() {
    return values;
  }

  public void writeToFile(@NotNull Path file) throws IOException {
    try {
      Files.createDirectories(file.getParent());
      try (Writer writer = Files.newBufferedWriter(file)) {
        GSON.toJson(values, writer);
      }
    } catch (IOException e) {
      throw new IOException("Failed to write Gradle launcher parameters to " + file, e);
    }
  }
}
