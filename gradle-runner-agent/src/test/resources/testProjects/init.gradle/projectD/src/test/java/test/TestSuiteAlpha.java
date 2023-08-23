package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import test.suiteA.TestAlpha;
import test.suiteA.TestBravo;
import test.suiteA.TestCharlie;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 7, 2010
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({TestAlpha.class,
            TestZulu.class,
            TestBravo.class,
            TestCharlie.class})
public class TestSuiteAlpha {
}
