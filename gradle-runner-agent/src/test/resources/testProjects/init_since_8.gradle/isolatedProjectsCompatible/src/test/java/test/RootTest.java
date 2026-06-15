package test;

import isolated.root.RootGreeter;
import org.junit.Assert;
import org.junit.Test;

public class RootTest {
  @Test
  public void rootRuns() {
    Assert.assertEquals("hello from root", new RootGreeter().greet());
  }
}
