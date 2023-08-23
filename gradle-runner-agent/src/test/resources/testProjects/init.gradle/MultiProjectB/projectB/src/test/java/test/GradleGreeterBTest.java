package test;

import my.module.GreeterB;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GradleGreeterBTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Project_B guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals("Wrong greeting", HELLO_WORLD, GreeterB.greet());
    }

    @Ignore
    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, GreeterB.intro());
    }
}
