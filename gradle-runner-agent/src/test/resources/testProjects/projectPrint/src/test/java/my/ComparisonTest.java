package my;

import org.junit.Test;

public class ComparisonTest {
  @Test
  public void junit4Comparison() throws Exception {
    org.junit.Assert.assertEquals("message", "1\nexpected\nvalue", "2\nactual\nvalue");
  }

  @Test
  public void junitFlatFormat() throws Exception {
    org.junit.Assert.fail(".... expected:<value1\n" +
                          "value2> but was:<value3\n" +
                          "value4> ....");
  }

  @Test
  public void testNgComparison() throws Exception {
    org.testng.Assert.assertEquals("2\nactual\nvalue", "1\nexpected\nvalue", "message");
  }

  @Test
  public void testNgFlatFormat() throws Exception {
    org.junit.Assert.fail(".... expected [value1\n" +
                          "value2] but found [value3\n" +
                          "value4] ....");
  }
}
