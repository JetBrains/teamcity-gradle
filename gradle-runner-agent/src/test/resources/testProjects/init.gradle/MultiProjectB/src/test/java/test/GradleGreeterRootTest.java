package test;

import my.module.GreeterRoot;
import org.junit.Assert;
import org.junit.Test;

public class GradleGreeterRootTest {
    private static final String HELLO_WORLD = "Hello, World!";
    private static final String GRADLE_GUINEA_PIG = "I'm Root guinea pig";

    @Test
    public void testGreet() throws Exception {
         Assert.assertEquals("Wrong greeting", HELLO_WORLD, GreeterRoot.greet());
    }

    @Test
    public void testIntro() throws Exception {
          Assert.assertEquals("Wrong greeter", GRADLE_GUINEA_PIG, GreeterRoot.intro());
    }
}
