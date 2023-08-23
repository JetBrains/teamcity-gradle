package big.message;

import java.lang.StringBuilder;
import org.junit.Assert;
import org.junit.Test;

public class MyTest {
  @Test
  public void test() throws InterruptedException {
    StringBuilder str = new StringBuilder();
    for (int i = 0; i < 5000; i++) {
      str.append(i);
    }
    Assert.assertEquals(str, new StringBuilder(str).reverse().toString());
  }
}