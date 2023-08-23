package test;

import org.junit.Test;

public class TestClass {

    @Test
    public void testA() throws Exception {
      System.out.println("StdOut message Could not compile initialization script /som/path/init.gradle");
    }

    @Test
    public void testB() throws Exception {
      System.err.println("StdErr message");
    }

}
