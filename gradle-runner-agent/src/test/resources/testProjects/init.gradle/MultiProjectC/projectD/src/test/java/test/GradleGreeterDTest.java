package test;

import my.module.GreeterD;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class GradleGreeterDTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Project_D guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals("Wrong greeting", HELLO_WORLD, GreeterD.greet());
    }

    @Ignore
    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, GreeterD.intro());
    }
}
