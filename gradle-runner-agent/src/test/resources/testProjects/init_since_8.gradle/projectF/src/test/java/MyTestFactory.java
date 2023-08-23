import org.testng.ITest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MyTestFactory implements ITest {

    private int id;
    private String name;

    @DataProvider
    public static Iterator<Object[]> data() {
        List<Object[]> testData = new ArrayList<>();

        for (int i=0; i<10; i++) {
            testData.add(new Object[] { i });
        }

        return testData.iterator();
    }

    @Factory (dataProvider = "data")
    public MyTestFactory(int id) {
        this.id = id;
        this.name = "test_" + id;
    }

    @Test
    public void test() throws InterruptedException {
        for (int i=0; i<10; i++) {
            System.out.println("This is some test output from test #" + id);
            Thread.sleep((long)(Math.random() * 100));
        }
    }

    @Override
    public String getTestName() {
        return this.name;
    }
}
