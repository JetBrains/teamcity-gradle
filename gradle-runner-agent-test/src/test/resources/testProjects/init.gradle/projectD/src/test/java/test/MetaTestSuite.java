package test;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Author: Nikita.Skvortsov
 * Date: Oct 7, 2010
 */

@RunWith(Suite.class)
@Suite.SuiteClasses({TestSuiteAlpha.class,
            TestSuiteBravo.class})
public class MetaTestSuite {
}
