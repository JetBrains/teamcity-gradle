package jetbrains.buildServer.gradle.test.init;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import jetbrains.buildServer.gradle.agent.init.TeamcityTaskListener;
import org.gradle.api.Task;
import org.gradle.api.execution.TaskExecutionListener;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ServiceRegistryFactory;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.logging.LogLevel;
import org.gradle.api.logging.StandardOutputListener;
import org.gradle.api.tasks.TaskState;
import org.gradle.api.tasks.compile.Compile;
import org.gradle.logging.LoggingManagerInternal;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/26/10
 */
public class TeamcityTaskListenerTest {

  protected static final String COMPILE_TASK_NAME = "compile-'task'-[name]-\nmultiline";
  protected static final String ESCAPED_COMPILE_TASK_NAME = "compile-|'task|'-|[name|]-|nmultiline";
  TaskExecutionListener myExecutionListener;
  Mockery myContext;
  PrintStream myConsoleStream;
  ByteArrayOutputStream myOutputStream;
  private List<StandardOutputListener> myStandardOutputListeners = new LinkedList<StandardOutputListener>();

  @BeforeMethod
  public void setUp() throws Exception {
    myExecutionListener = new TeamcityTaskListener();
    myContext = new Mockery();
    myOutputStream = new ByteArrayOutputStream();
    PrintStream ps = new PrintStream(myOutputStream);
    myConsoleStream = System.out;
    System.setOut(ps);
  }

  @AfterMethod
  public void tearDown() throws Exception {
    System.setOut(myConsoleStream);
    myStandardOutputListeners.clear();
  }

  @Test
  public void testBlockOpenedMessage() throws Exception {
    final Task task = myContext.mock(Task.class);
    myContext.checking(new Expectations() {{
        allowing(task).getPath(); will(returnValue(COMPILE_TASK_NAME));
      }}
    );
    myExecutionListener.beforeExecute(task);
    final String result = myOutputStream.toString("UTF-8").trim();
    assertEquals(result,"##teamcity[blockOpened name='" + ESCAPED_COMPILE_TASK_NAME + "']","Wrong message received.");
  }

  @Test
  public void testBlockClosedMessage() throws Exception {
    final Task task = myContext.mock(Task.class);
    final TaskState taskState = myContext.mock(TaskState.class);
    myContext.checking(new Expectations() {{
        allowing(task).getPath(); will(returnValue(COMPILE_TASK_NAME));
      }}
    );
    myExecutionListener.afterExecute(task, taskState);
    final String result = myOutputStream.toString("UTF-8").trim();
    assertEquals(result,"##teamcity[blockClosed name='" + ESCAPED_COMPILE_TASK_NAME + "']","Wrong message received.");
  }

  @Test
  public void testCompilationStartedMessage() throws Exception {

    final ProjectInternal project = prepareProjectForCompileTask();

    final Compile task = AbstractTask.injectIntoNewInstance(project, COMPILE_TASK_NAME, new Callable<Compile>() {
      public Compile call() throws Exception {
        return new Compile();
      }
    });

    myExecutionListener.beforeExecute(task);
    final String result = myOutputStream.toString("UTF-8").trim();
    assertEquals(result,"##teamcity[compilationStarted compiler=':test-project:" + ESCAPED_COMPILE_TASK_NAME + "']","Wrong message received.");
  }

  @Test
  public void testCompilationFinishedMessage() throws Exception {
    ProjectInternal project = prepareProjectForCompileTask();
    final TaskState taskState = myContext.mock(TaskState.class);
    myContext.checking(new Expectations(){{
      allowing(taskState).getFailure(); will(returnValue(null));
    }});

    final Compile task = AbstractTask.injectIntoNewInstance(project, COMPILE_TASK_NAME, new Callable<Compile>() {
      public Compile call() throws Exception {
        return new Compile();
      }
    });

    myExecutionListener.afterExecute(task, taskState);
    final String result = myOutputStream.toString("UTF-8").trim();
    assertEquals(result,"##teamcity[compilationFinished compiler=':test-project:" + ESCAPED_COMPILE_TASK_NAME + "']","Wrong message received.");
  }

  @Test
  public void testCompilationFailedMessage() throws Exception {
    ProjectInternal project = prepareProjectForCompileTask();
    final TaskState taskState = myContext.mock(TaskState.class);

    myContext.checking(new Expectations(){{
      allowing(taskState).getFailure(); will(returnValue(new Exception("TestExceptionMessage")));
    }});

    final Compile task = AbstractTask.injectIntoNewInstance(project, COMPILE_TASK_NAME, new Callable<Compile>() {
      public Compile call() throws Exception {
        return new Compile();
      }
    });

    myExecutionListener.beforeExecute(task);
    for(StandardOutputListener listener : myStandardOutputListeners) {
      listener.onOutput("TestErrorMessageAlpha");
    }
    myExecutionListener.afterExecute(task, taskState);

    final String result = myOutputStream.toString("UTF-8").trim();
    String exptected = "##teamcity[message text='TestErrorMessageAlpha' status='ERROR']";
    assertTrue(result.contains(exptected), "Result did not contain expected string.\n" +
                                           "Exptected to contain: " + exptected + "\n" +
                                           "Actual result: " + result);
  }

  private ProjectInternal prepareProjectForCompileTask() {
    final ProjectInternal project = myContext.mock(ProjectInternal.class);
    final TaskContainerInternal taskContainer = myContext.mock(TaskContainerInternal.class);
    final ServiceRegistryFactory factory = myContext.mock(ServiceRegistryFactory.class);
    final LoggingManagerInternal loggingManager = myContext.mock(LoggingManagerInternal.class);

    final Action addListener = new Action() {
      public Object invoke(final Invocation invocation) throws Throwable {
        myStandardOutputListeners.add((StandardOutputListener)invocation.getParameter(0));
        return null;
      }

      public void describeTo(final Description description) {
        description.appendText("Adds listener to a listener list");
      }
    };

    myContext.checking(new Expectations() {{
      allowing(project).absolutePath(COMPILE_TASK_NAME); will(returnValue(":test-project:" + COMPILE_TASK_NAME));
      allowing(project).getTasks(); will(returnValue(taskContainer));
      allowing(project).getServiceRegistryFactory(); will(returnValue(factory));
      allowing(factory).createFor(with(any(Object.class))); will(returnValue(factory));
      allowing(factory).get(LoggingManagerInternal.class); will(returnValue(loggingManager));
      allowing(factory).get(with(any(Class.class))); will(returnValue(null));
      allowing(loggingManager).captureStandardError(with(any(LogLevel.class)));
      allowing(loggingManager).addStandardOutputListener(with(any(StandardOutputListener.class))); will(addListener);
      allowing(project).getConvention(); will(returnValue(null));
    }}
    );
    return project;
  }
}
