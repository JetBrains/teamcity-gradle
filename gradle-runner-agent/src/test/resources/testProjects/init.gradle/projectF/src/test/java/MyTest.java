import org.testng.annotations.Test;

/**
 * Created by Julia.Reshetnikova on 08-Aug-17.
 */
public class MyTest {

    @Test
    public void test1() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test1");
            Thread.sleep((long)(Math.random() * 100));
        }
    }

    @Test
    public void test2() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test2");
            Thread.sleep((long)(Math.random() * 100));
        }
    }

    @Test
    public void test3() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test3");
            Thread.sleep((long)(Math.random() * 100));
        }
    }

    @Test
    public void test4() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test4");
            Thread.sleep((long)(Math.random() * 100));
        }
    }

    @Test
    public void test5() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test5");
            Thread.sleep((long)(Math.random() * 100));
        }
    }

}
