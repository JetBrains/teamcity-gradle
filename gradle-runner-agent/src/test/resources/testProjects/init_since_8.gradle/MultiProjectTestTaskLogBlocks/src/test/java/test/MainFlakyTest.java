package test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.fail;

public class MainFlakyTest {
  @Test
  public void test() {
    File flag = new File(System.getProperty("java.io.tmpdir"), "main-flaky.file");
    if (flag.exists()) {
      flag.delete();
      fail();
    }
  }
}
