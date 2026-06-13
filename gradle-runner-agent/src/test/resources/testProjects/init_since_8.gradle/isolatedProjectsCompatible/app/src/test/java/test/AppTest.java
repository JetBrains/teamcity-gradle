package test;

import isolated.app.AppGreeter;
import org.junit.Assert;
import org.junit.Test;

public class AppTest {
  @Test
  public void appUsesLib() {
    Assert.assertEquals("app uses hello from lib", new AppGreeter().greet());
  }
}
