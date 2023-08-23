package test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.fail;

public class AFlakyTest {
  @Test
  public void test() {
    File flag = new File("a-flaky.file");
    if (flag.exists()) {
      flag.delete();
      fail();
    }
  }
}
