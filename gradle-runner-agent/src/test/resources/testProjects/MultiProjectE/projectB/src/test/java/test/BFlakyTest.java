package test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.fail;

public class BFlakyTest {
  @Test
  public void test() {
    File flag = new File(System.getProperty("java.io.tmpdir"), "b-flaky.file");
    if (flag.exists()) {
      flag.delete();
      fail();
    }
  }
}
