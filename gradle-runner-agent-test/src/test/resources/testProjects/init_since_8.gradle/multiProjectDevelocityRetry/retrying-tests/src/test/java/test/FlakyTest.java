package test;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.fail;

public class FlakyTest {
  @Test
  public void test() {
    File flag = new File("flaky.file");
    if (flag.exists()) {
      flag.delete();
      fail();
    }
  }
}
