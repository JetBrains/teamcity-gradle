package jetbrains.buildServer.gradle.filter;

import java.io.*;

public class GradleTestFilter {
  private final File myFile;

  public GradleTestFilter(final File fileName) {
    myFile = fileName;
  }

  public void process(final Processor processor) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(myFile)));
      while (reader.ready()) {
        final String line = reader.readLine();
        if (!line.startsWith("#")) {
          processor.process(line);
        }
      }
    } catch (IOException ignore) {
      //
    } finally {
      try {
        if (reader != null) reader.close();
      } catch (IOException ignore) {
        //
      }
    }
  }

  public interface Processor {
    void process(String line);
  }
}
