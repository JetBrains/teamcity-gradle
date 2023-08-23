package test;

import my.module.Greeter;
import org.junit.Assert;
import org.junit.Test;

public class GradleGreeterTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Gradle guinea pig";

    @Test
    public void testGreet() throws Exception {
      Assert.assertEquals("Wrong greeting", HELLO_WORLD, Greeter.greet());
    }

    @Test
    public void testIntro() throws Exception {
      System.out.println();
      Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, Greeter.intro());
    }

    @Test
    public void testSystemProperty() throws Exception {
      String propertyValueAlpha = System.getProperty("test.property.alpha");
      String propertyValueBravo = System.getProperty("test.property.bravo");
      Assert.assertEquals("valueAlpha", propertyValueAlpha);
      Assert.assertEquals("valueBravo", propertyValueBravo);
    }
}
