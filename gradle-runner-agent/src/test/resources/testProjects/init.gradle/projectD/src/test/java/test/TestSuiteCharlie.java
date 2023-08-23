package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import test.suiteC.TestGolf;
import test.suiteC.TestHotel;
import test.suiteC.TestIndia;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 7, 2010
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({TestGolf.class,
            TestHotel.class,
            TestIndia.class,
            TestZulu.class})
public class TestSuiteCharlie {
}
