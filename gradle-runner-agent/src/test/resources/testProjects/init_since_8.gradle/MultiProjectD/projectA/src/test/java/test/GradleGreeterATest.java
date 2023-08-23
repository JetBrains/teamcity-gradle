package test;

import my.module.GreeterA;
import org.junit.Assert;
import org.junit.Test;

public class GradleGreeterATest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Project_A guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals("Wrong greeting", HELLO_WORLD, GreeterA.greet());
    }

    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, GreeterA.intro());
    }
}
