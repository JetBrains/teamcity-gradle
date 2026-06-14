package test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

@RunWith(TestClass.NameSortedRunner.class)
public class TestClass {

    @Test
    public void testA() throws Exception {
      System.out.println("StdOut message Could not compile initialization script /som/path/init.gradle");
    }

    @Test
    public void testB() throws Exception {
      System.err.println("StdErr message");
    }

    public static class NameSortedRunner extends BlockJUnit4ClassRunner {
      public NameSortedRunner(Class<?> klass) throws InitializationError {
        super(klass);
      }

      @Override
      protected List<FrameworkMethod> computeTestMethods() {
        List<FrameworkMethod> methods = new ArrayList<FrameworkMethod>(super.computeTestMethods());
        Collections.sort(methods, new Comparator<FrameworkMethod>() {
          public int compare(FrameworkMethod first, FrameworkMethod second) {
            return first.getName().compareTo(second.getName());
          }
        });
        return methods;
      }
    }

}
