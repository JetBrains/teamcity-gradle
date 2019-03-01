package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import test.suiteB.TestDelta;
import test.suiteB.TestEcho;
import test.suiteB.TestFoxtrot;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 7, 2010
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({TestDelta.class,
            TestEcho.class,
            TestZulu.class,
            TestFoxtrot.class})
public class TestSuiteBravo {
}
