package my;

import org.junit.Assert;
import org.junit.Test;

public class SimpleTest {
  @Test
  public void print() throws Exception {
    System.out.println("##teamcity[testStarted name='my.PrintTest.print' flowId='|n']");
  }
}
