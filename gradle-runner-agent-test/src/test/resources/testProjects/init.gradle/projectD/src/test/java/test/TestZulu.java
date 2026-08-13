package test;

import org.junit.Test;
import org.junit.Assert;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 7, 2010
 */
public class TestZulu {

    @Test
    public void testMethodOne() {
    }

    @Test
    public void testMethodTwo() {
    }

    @Test
    public void testMethodThree() {
    }

    @Test
    public void testSystemProperty() throws Exception {
      String propertyValueAlpha = System.getProperty("test.property.alpha");
      String propertyValueBravo = System.getProperty("test.property.bravo");
      Assert.assertEquals("valueAlpha", propertyValueAlpha);
      Assert.assertEquals("valueBravo", propertyValueBravo);
    }
}
