package jetbrains.buildServer.gradle.test.init;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.Random;
import jetbrains.buildServer.gradle.agent.init.TeamcityTestListener;
import jetbrains.buildServer.messages.serviceMessages.MapSerializerUtil;
import jetbrains.buildServer.util.StringUtil;
import org.gradle.api.tasks.testing.TestDescriptor;
import org.gradle.api.tasks.testing.TestResult;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/28/10
 */
public class TeamcityTestListenerTest {

  protected static final String TEST_SUITE = "'test|Suite]";
  protected static final String WORKER_SUITE = "'worker|Suite]";
  protected static final String ROOT_SUITE = "root|Suite";
  private static final String TEST_NAME = "'test|Name]";
  private static final String TEST_CLASS_NAME = "test'class]name|";


  protected static final String ESC_WORKER_SUITE = MapSerializerUtil.escapeStr(WORKER_SUITE, MapSerializerUtil.STD_ESCAPER);
  protected static final String ESC_TEST_NAME = MapSerializerUtil.escapeStr(TEST_NAME, MapSerializerUtil.STD_ESCAPER);
  protected static final String ESC_TEST_CLASS_NAME = MapSerializerUtil.escapeStr(TEST_CLASS_NAME, MapSerializerUtil.STD_ESCAPER);

  protected static final String START_MGS_PATTERN = "##teamcity[testSuiteStarted name=''{1}'' flowId=''{0}'']";
  protected static final String START_FINISH_MSG_PATTERN = "##teamcity[testSuiteStarted name=''{1}'' flowId=''{0}'']" +
                                                            "##teamcity[testSuiteFinished name=''{1}'' flowId=''{0}'']";
  protected static final String START_TEST_PATTERN = "##teamcity[testSuiteStarted name=''{1}'' flowId=''{0}'']" +
                                                     "##teamcity[testStarted name=''{2}'' captureStandardOutput=''true'' flowId=''{0}'']";
  private static final String FINISH_TEST_PATTERN = "##teamcity[testSuiteStarted name=''{1}'' flowId=''{0}'']" +
                                                     "##teamcity[testFinished name=''{2}'' duration=''{3}'' flowId=''{0}'']";
  private static final String FAILED_TEST_PATTERN = "##teamcity[testSuiteStarted name=''{1}'' flowId=''{0}'']" +
                                                      "##teamcity[testFailed name=''{2}'' message=''{3}'' details=''{4}'' flowId=''{0}'']" +
                                                      "##teamcity[testFinished name=''{2}'' duration=''{5}'' flowId=''{0}'']";


  private static final String EXCEPTION_MESSAGE = "Test | exception ] message |";

  TeamcityTestListener myTestListener;
  Mockery myContext;
  private ByteArrayOutputStream myOutputStream;
  private PrintStream myConsoleStream;


  @BeforeMethod
  public void setUp() throws Exception {
    myContext = new Mockery();
    myOutputStream = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(myOutputStream);
    myConsoleStream = System.out;
    System.setOut(ps);
    myTestListener = new TeamcityTestListener();
    myContext = new Mockery();
  }

    @AfterMethod
  public void tearDown() throws Exception {
    System.setOut(myConsoleStream);
  }

  @Test
  public void testBeforeSuite() throws Exception {
    final TestDescriptor rootSuite = createTestDescriptor(ROOT_SUITE, null);
    final TestDescriptor workerSuite = createTestDescriptor(WORKER_SUITE, rootSuite);

    myTestListener.beforeSuite(rootSuite);
    myTestListener.beforeSuite(workerSuite);

    assertOutputPattern(START_MGS_PATTERN, String.valueOf(System.identityHashCode(workerSuite)),
                        ESC_WORKER_SUITE);
  }

  @Test
  public void testAfterSuite() throws Exception {

    final TestDescriptor rootSuite = createTestDescriptor(ROOT_SUITE, null);
    final TestDescriptor workerSuite = createTestDescriptor(WORKER_SUITE, rootSuite);
    final TestResult testResult = myContext.mock(TestResult.class);

    myTestListener.beforeSuite(rootSuite);
    myTestListener.beforeSuite(workerSuite);
    myTestListener.afterSuite(workerSuite, testResult);

    assertOutputPattern(START_FINISH_MSG_PATTERN, String.valueOf(System.identityHashCode(workerSuite)),
                        ESC_WORKER_SUITE);
  }

  @Test
  public void testBeforeTest() throws Exception {

    final TestDescriptor rootSuite = createTestDescriptor(ROOT_SUITE, null);
    final TestDescriptor workerSuite = createTestDescriptor(WORKER_SUITE, rootSuite);
    final TestDescriptor testDescriptor = createTestDescriptor(TEST_NAME, workerSuite);

    myContext.checking(new Expectations(){{
      allowing(testDescriptor).getClass(); will(returnValue(TEST_CLASS_NAME));
    }});

    myTestListener.beforeSuite(rootSuite);
    myTestListener.beforeSuite(workerSuite);
    myTestListener.beforeTest(testDescriptor);

    assertOutputPattern(START_TEST_PATTERN, String.valueOf(System.identityHashCode(workerSuite)),
                        ESC_WORKER_SUITE, ESC_TEST_CLASS_NAME + "." + ESC_TEST_NAME);
  }

  @Test
  public void testAfterTest() throws Exception {
    final TestDescriptor rootSuite = createTestDescriptor(ROOT_SUITE, null);
    final TestDescriptor workerSuite = createTestDescriptor(WORKER_SUITE, rootSuite);
    final TestDescriptor testDescriptor = createTestDescriptor(TEST_NAME, workerSuite);
    final TestResult testResult = myContext.mock(TestResult.class);

    final Random random = new Random();
    final long startTime = random.nextInt();
    final long endTime = startTime + random.nextInt();

    myContext.checking(new Expectations(){{
      allowing(testDescriptor).getClass(); will(returnValue(TEST_CLASS_NAME));
      allowing(testResult).getResultType(); will(returnValue(TestResult.ResultType.SUCCESS));
      allowing(testResult).getEndTime(); will(returnValue(endTime));
      allowing(testResult).getStartTime(); will(returnValue(startTime));
    }});

    myTestListener.beforeSuite(rootSuite);
    myTestListener.beforeSuite(workerSuite);
    myTestListener.afterTest(testDescriptor, testResult);

    assertOutputPattern(FINISH_TEST_PATTERN, String.valueOf(System.identityHashCode(workerSuite)),
                        ESC_WORKER_SUITE, ESC_TEST_CLASS_NAME + "." + ESC_TEST_NAME, Long.toString(endTime - startTime));
  }

  @Test
  public void testAfterFailedTest() throws Exception {
    final TestDescriptor rootSuite = createTestDescriptor(ROOT_SUITE, null);
    final TestDescriptor workerSuite = createTestDescriptor(WORKER_SUITE, rootSuite);
    final TestDescriptor testDescriptor = createTestDescriptor(TEST_NAME, workerSuite);
    final TestResult testResult = myContext.mock(TestResult.class);

    final Exception exception = new Exception(EXCEPTION_MESSAGE);

    final Random random = new Random();
    final long startTime = random.nextInt();
    final long endTime = startTime + random.nextInt();

    myContext.checking(new Expectations(){{
      allowing(testDescriptor).getClass(); will(returnValue(TEST_CLASS_NAME));
      allowing(testResult).getResultType(); will(returnValue(TestResult.ResultType.FAILURE));
      allowing(testResult).getException(); will(returnValue(exception));
      allowing(testResult).getEndTime(); will(returnValue(endTime));
      allowing(testResult).getStartTime(); will(returnValue(startTime));
    }});

    myTestListener.beforeSuite(rootSuite);
    myTestListener.beforeSuite(workerSuite);
    myTestListener.afterTest(testDescriptor, testResult);

    String msg =  MapSerializerUtil.escapeStr(exception.getMessage(), MapSerializerUtil.STD_ESCAPER);

    ByteArrayOutputStream out = new ByteArrayOutputStream();
    exception.printStackTrace(new PrintStream(out));
    String stacktrace = out.toString();

    String escapedStackTrace = MapSerializerUtil.escapeStr(stacktrace.toString(), MapSerializerUtil.STD_ESCAPER);

    assertOutputPattern(FAILED_TEST_PATTERN, String.valueOf(System.identityHashCode(workerSuite)),
                        ESC_WORKER_SUITE, ESC_TEST_CLASS_NAME + "." + ESC_TEST_NAME,
                        msg, escapedStackTrace, Long.toString(endTime - startTime));
  }

  private void assertOutputPattern(final String pattern, Object... args) throws UnsupportedEncodingException {
    final String expectedMessage = MessageFormat.format(pattern, args);
    final String singleResultString = dropLineSeparators(getCapturedOutput());
    assertEquals(singleResultString, expectedMessage);
  }

  private TestDescriptor createTestDescriptor(final String name, final TestDescriptor parent) {
    final TestDescriptor descriptor = myContext.mock(TestDescriptor.class, name);
    myContext.checking(new Expectations(){{
      allowing(descriptor).getName(); will(returnValue(name));
      allowing(descriptor).getParent(); will(returnValue(parent));
    }});
    return descriptor;
  }

  private String dropLineSeparators(final String string) {
    return StringUtil.join(string.split("\\n|\\r"), "");
  }

  private String getCapturedOutput() throws UnsupportedEncodingException {
    return myOutputStream.toString("UTF-8").trim();
  }
}
