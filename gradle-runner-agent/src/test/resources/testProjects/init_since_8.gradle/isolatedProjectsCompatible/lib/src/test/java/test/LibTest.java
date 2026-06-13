package test;

import isolated.lib.LibGreeter;
import org.junit.Assert;
import org.junit.Test;

public class LibTest {
  @Test
  public void libRuns() {
    Assert.assertEquals("hello from lib", new LibGreeter().greet());
  }
}
