package test;

import my.module.Greeter;
import org.testng.Assert;
import org.testng.annotations.Test;

public class GreeterTestNGTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Gradle guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals(Greeter.greet(), HELLO_WORLD, "Wrong greeting");
    }

    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals(Greeter.intro(), GRADLE_GUINEA_PIG, "Wrong greeter");
    }

    @Test
    public void failedTest() throws Exception {
        Assert.fail("Explicit 'test' [failure] |");
    }

    @Test(enabled=false)
    public void skippedTest() throws Exception {
        Assert.fail("This test must be skipped");
    }
}