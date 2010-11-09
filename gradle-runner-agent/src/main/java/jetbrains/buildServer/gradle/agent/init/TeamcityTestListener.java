package jetbrains.buildServer.gradle.agent.init;

import java.util.concurrent.ConcurrentHashMap;
import jetbrains.buildServer.messages.serviceMessages.*;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestListener;
import org.gradle.api.tasks.testing.TestResult;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/26/10
 */
public class TeamcityTestListener implements TestListener {
  ConcurrentHashMap<TestDescriptor, String> workerCodes = new ConcurrentHashMap();
  TestDescriptor root;

  String findFlowId(TestDescriptor desc) {
    String code = "";
    while ((null != desc) && ((code = workerCodes.get(desc)) == null)) {
      desc = desc.getParent();
    }
    return code;
  }

  /**
   * Called before a test suite is started.
   * @param suite The suite whose tests are about to be executed.
   */
  public void beforeSuite(TestDescriptor suite) {
    if (null == suite.getParent()) {
      root = suite;
    }
    String code = "";
    if (suite.getParent() == root) {
      // assume, we have worker thread descriptor here
      code = workerCodes.get(suite);
      if (null == code) {
        code = "" + System.identityHashCode(suite);
        String oldCode = workerCodes.putIfAbsent(suite, code);
        if (null != oldCode) {
          code = oldCode;
        }
      }
    }

    if (null != suite.getParent()) { // do not report root empty suite
      ServiceMessage msg = new TestSuiteStarted(suite.getName());
      msg.setFlowId(findFlowId(suite));
      System.out.println(msg.asString());
    }
  }

  /**
   * Called after a test suite is finished.
   * @param suite The suite whose tests have finished being executed.
   * @param result The aggregate result for the suite.
   */
  public void afterSuite(TestDescriptor suite, TestResult result) {
    if (null != suite.getParent()) { // do not report root empty suite
      ServiceMessage msg = new TestSuiteFinished(suite.getName());
      msg.setFlowId(findFlowId(suite));
      System.out.println(msg.asString());
    }
  }

  /**
   * Called before a test is started.
   * @param testDescriptor The test which is about to be executed.
   */
  public void beforeTest(TestDescriptor testDescriptor) {
    String testName = testDescriptor.getClassName() + "." + testDescriptor.getName();
    ServiceMessage msg = new TestStarted(testName, true, null);
    msg.setFlowId(findFlowId(testDescriptor));
    System.out.println(msg.asString());
  }

  /**
   * Called after a test is finished.
   * @param testDescriptor The test which has finished executing.
   * @param result The test result.
   */
  public void afterTest(TestDescriptor testDescriptor, TestResult result) {
    String testName = testDescriptor.getClassName() + "." + testDescriptor.getName();
    ServiceMessage msg;
    switch (result.getResultType()) {
      case FAILURE:
        msg = new TestFailed(testName, result.getException());
        msg.setFlowId(findFlowId(testDescriptor));
        System.out.println(msg.asString());
        break;
      case SKIPPED:
        msg = new TestIgnored(testName, "");
        msg.setFlowId(findFlowId(testDescriptor));
        System.out.println(msg.asString());
        break;
    };

    final int duration = (int) (result.getEndTime() - result.getStartTime());
    msg = new TestFinished(testName, duration);
    msg.setFlowId(findFlowId(testDescriptor));
    System.out.println(msg.asString());
  }
}
