package jetbrains.buildServer.gradle.runtime.output;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import jetbrains.buildServer.gradle.runtime.listening.BuildLifecycleListener;
import jetbrains.buildServer.gradle.runtime.listening.event.TaskOutputEventImpl;

/**
 * Wraps Gradle Tooling API build output in order to delegate it and have an option to postprocess it.
 */
public class GradleOutputWrapper extends OutputStream {

  private StringBuilder myBuffer;

  private final BuildLifecycleListener myBuildLifecycleListener;
  private final OutputStream myDelegate;
  private final OutputType myOutputType;

  public GradleOutputWrapper(BuildLifecycleListener buildLifecycleListener,
                             OutputType outputType) {
    myBuildLifecycleListener = buildLifecycleListener;
    myOutputType = outputType;

    if (OutputType.STD_OUT == outputType) {
      myDelegate = System.out;
    } else {
      myDelegate = System.err;
    }
  }

  @Override
  public synchronized void write(int b) throws IOException {
    myDelegate.write(b);

    if (myBuffer == null) {
      myBuffer = new StringBuilder();
    }
    myBuffer.append((char)b);
  }

  @Override
  public synchronized void write(byte[] b, int off, int len) throws IOException {
    myDelegate.write(b, off, len);

    if (myBuffer == null) {
      myBuffer = new StringBuilder();
    }
    myBuffer.append(new String(b, off, len, StandardCharsets.UTF_8));
  }

  @Override
  public synchronized void flush() throws IOException {
    doFlush();
  }

  private void doFlush() throws IOException {
    myDelegate.flush();

    if (myBuffer == null) {
      return;
    }
    myBuildLifecycleListener.onTaskOutput(new TaskOutputEventImpl(System.currentTimeMillis(), myBuffer.toString(), myOutputType));
    myBuffer.setLength(0);
  }
}
