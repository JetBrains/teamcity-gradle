package my;

import org.junit.Assert;
import org.junit.Test;

public class PrintTest {
  @Test
  public void print() throws Exception {
    System.out.println("\u00A4\u00A4");
    System.err.println("'\n\r\u0085\u2028\u2029|]");
    System.err.println("##teamcity[ignore value='\u2029']");
  }
}
