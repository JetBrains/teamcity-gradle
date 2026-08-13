package test;

import my.module.Greeter;
import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;

public class GreeterJUnitTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Gradle guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals("Wrong greeting", HELLO_WORLD, Greeter.greet());
    }

    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, Greeter.intro());
    }

    @Test
    public void failedTest() throws Exception {
        Assert.fail("Explicit 'test' [failure] |");
    }

    @Ignore
    @Test
    public void skippedTest() throws Exception {
        Assert.fail("This test must be skipped");
    }
}
