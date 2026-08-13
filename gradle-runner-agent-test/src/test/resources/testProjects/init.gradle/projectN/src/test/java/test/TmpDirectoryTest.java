package test;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class TmpDirectoryTest {
  @Test
  public void getSystemProperty() {
    System.out.println(System.getProperty("java.io.tmpdir"));
  }
}
