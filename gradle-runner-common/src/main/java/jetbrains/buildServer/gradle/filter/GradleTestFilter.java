package jetbrains.buildServer.gradle.filter;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class GradleTestFilter {
  private final File myFile;

  public GradleTestFilter(final File fileName) {
    myFile = fileName;
  }

  public void process(final Processor processor) {
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(myFile))));
      String mode = null;
      while (reader.ready()) {
        final String line = reader.readLine();
        if (line.startsWith("#")) {
          if (line.startsWith("#filtering_mode=")) mode = line.substring("#filtering_mode=".length());
        } else if ("excludes".equals(mode)) {
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
