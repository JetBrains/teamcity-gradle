package jetbrains.buildServer.gradle.runtime.output;

import java.io.*;
import java.util.stream.Collectors;
import jetbrains.buildServer.gradle.runtime.logging.GradleToolingLogger;
import org.jetbrains.annotations.NotNull;

public class GradleBuildOutputWrapper implements AutoCloseable {

  private static final String TASK_OUTPUT_FILENAME_POSTFIX = "_out";
  private static final String EMPTY = "";

  private final GradleToolingLogger myLogger;
  private final File myTaskOutputFile;
  private FileWriter myTaskOutputFileWriter;

  public GradleBuildOutputWrapper(String taskOutputDir,
                                  String taskPath,
                                  GradleToolingLogger logger) {
    myTaskOutputFile = new File(taskOutputDir, composeFilename(taskPath));
    myLogger = logger;
  }

  public void append(@NotNull String line) {
    try {
      if (myTaskOutputFileWriter == null) {
        initOutput(line);
      }

      myTaskOutputFileWriter.write(line);
    } catch (IOException e) {
      myLogger.warn(String.format("Couldn't write output to a file: outputFile=%s, output=%s",
                                  myTaskOutputFile.getAbsolutePath(), line));
    }
  }

  @Override
  public void close() {
    if (myTaskOutputFileWriter != null) {
      try {
        myTaskOutputFileWriter.close();
        myTaskOutputFileWriter = null;
      } catch (IOException e) {
        myLogger.warn(String.format("Error while trying to close a file: file=%s", myTaskOutputFile.getAbsolutePath()));
      }
    }
  }

  @NotNull
  public String getOutput() {
    try {
      if (myTaskOutputFileWriter != null) {
        myTaskOutputFileWriter.flush();
      }
      return collectOutput();
    } catch (IOException e) {
      myLogger.warn(String.format("Couldn't read output from a file: outputFile=%s", myTaskOutputFile.getAbsolutePath()));
      return EMPTY;
    }
  }

  private String collectOutput() throws IOException {
    if (!myTaskOutputFile.exists()) {
      myLogger.warn(String.format("Couldn't collect task output, target file doesn't exist: file=%s", myTaskOutputFile.getAbsolutePath()));
      return EMPTY;
    }
    try (FileReader fileReader = new FileReader(myTaskOutputFile);
         BufferedReader bufferedReader = new BufferedReader(fileReader)) {
      return bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()));
    }
  }

  @NotNull
  private String composeFilename(@NotNull String taskPath) {
    String cleanTaskPath = taskPath.replace(":", "");
    return cleanTaskPath + TASK_OUTPUT_FILENAME_POSTFIX;
  }

  private void initOutput(@NotNull String line) throws IOException {
    if (!myTaskOutputFile.createNewFile()) {
      myLogger.warn(String.format("Couldn't create task output file. Output couldn't be added: outputFile=%s, output=%s", myTaskOutputFile.getAbsolutePath(), line));
      return;
    }

    myTaskOutputFileWriter = new FileWriter(myTaskOutputFile, true);
  }
}
