package jetbrains.buildServer.gradle.test.init;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import jetbrains.buildServer.gradle.agent.init.PropertiesLoadListener;
import jetbrains.buildServer.gradle.test.GradleTestUtil;
import jetbrains.buildServer.gradle.test.integration.BaseGradleRunnerTest;
import org.gradle.api.Project;
import org.gradle.api.ProjectState;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.internal.project.ServiceRegistryFactory;
import org.gradle.api.internal.tasks.TaskContainerInternal;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.testing.Test;
import org.gradle.listener.ListenerManager;
import org.hamcrest.Description;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.api.Action;
import org.jmock.api.Invocation;
import org.testng.annotations.BeforeMethod;

import static org.testng.Assert.*;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/28/10
 */
public class PropertiesLoadListenerTest {
  Mockery myContext;
  File myProjectRoot;

  @BeforeMethod
  public void setUp() {
    myContext = new Mockery();
    myProjectRoot = GradleTestUtil.setProjectRoot(new File("."));
  }

  @org.testng.annotations.Test
  public void testBeforeEvaluate() throws Exception {

    PropertiesLoadListener loadListener = new PropertiesLoadListener();
    final Project project = myContext.mock(Project.class);
    final File propsFile = new File(myProjectRoot, "src/test/resources/testProjects/test.properties");
    final String propsFilePath = propsFile.getAbsolutePath();
    System.setProperty(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_PROP, propsFilePath);

    Properties actualProperties = loadProperties(propsFile);

    final Map<String,Object> projectProperties = new HashMap<String, Object>();
    final Action storeValue = new Action() {
      public Object invoke(final Invocation invocation) throws Throwable {
        String key = (String) invocation.getParameter(0);
        Object value = invocation.getParameter(1);
        projectProperties.put(key, value);
        return null;
      }

      public void describeTo(final Description description) {
        description.appendText("Store a value in the hash map");
      }
    };

    myContext.checking(new Expectations() {{
      allowing(project).file(propsFilePath); will(returnValue(propsFile));
      allowing(project).getProperties(); will(returnValue(projectProperties));
      allowing(project).setProperty(with(any(String.class)), with(any(Object.class))); will(storeValue);
    }});

    loadListener.beforeEvaluate(project);

    final Properties teamcity = (Properties) projectProperties.get("teamcity");
    assertNotNull(teamcity);

    for(Map.Entry<Object, Object> entry : actualProperties.entrySet()) {
      assertEquals(teamcity.get(entry.getKey()), entry.getValue());
    }
  }

  @org.testng.annotations.Test
  public void testAfterEvaluate() throws Exception {
    final ProjectInternal project = myContext.mock(ProjectInternal.class);
    final ProjectState projectState = myContext.mock(ProjectState.class);
    final File propsFile = new File(myProjectRoot, "src/test/resources/testProjects/testJvmArgs.properties");
    final Properties actualProperties = loadProperties(propsFile);

    final String[] argStrings = ((String) actualProperties.get("gradle.test.jvmargs")).split("\\n");

    final Map projectProperties = Collections.singletonMap("teamcity", actualProperties);
    final TaskContainerInternal taskContainer = myContext.mock(TaskContainerInternal.class);
    final TaskCollection taskCollection = myContext.mock(TaskCollection.class);

    // following is needed to instantiate Test ojbects
    final ServiceRegistryFactory factory = myContext.mock(ServiceRegistryFactory.class);
    final ListenerManager listenerManager = myContext.mock(ListenerManager.class);
    final FileResolver fileResolver = myContext.mock(FileResolver.class);

    final List<Test> taskList = new ArrayList<Test>();

    myContext.checking(new Expectations(){{
      allowing(project).absolutePath("testTask1");
      allowing(project).absolutePath("testTask2");
      allowing(project).getServiceRegistryFactory(); will(returnValue(factory));
      allowing(factory).createFor(with(any(Object.class))); will(returnValue(factory));
      allowing(factory).get(ListenerManager.class); will(returnValue(listenerManager));
      allowing(factory).get(FileResolver.class); will(returnValue(fileResolver));
      allowing(factory).get(with(any(Class.class))); will(returnValue(null));
      allowing(listenerManager).createAnonymousBroadcaster(with(any(Class.class)));
      allowing(fileResolver).resolveLater(with(any(Object.class)));
      allowing(project).getProperties(); will(returnValue(projectProperties));
      allowing(project).getTasks(); will(returnValue(taskContainer));
      allowing(project).getConvention(); will(returnValue(null));
      allowing(taskContainer).withType(org.gradle.api.tasks.testing.Test.class); will(returnValue(taskCollection));
    }});


    taskList.add(AbstractTask.injectIntoNewInstance(project, "testTask1", new Callable<Test>() {
      public Test call() throws Exception {
        return new Test();
      }
    }));

    taskList.add(AbstractTask.injectIntoNewInstance(project, "testTask2", new Callable<Test>() {
      public Test call() throws Exception {
        return new Test();
      }
    }));

    myContext.checking(new Expectations() {{
      allowing(taskCollection).iterator(); will(returnValue(taskList.iterator()));
    }});

    PropertiesLoadListener loadListener = new PropertiesLoadListener();


    loadListener.afterEvaluate(project, projectState);

    for(Test task : taskList) {
      List<String> args = task.getAllJvmArgs();
      for(String str : argStrings) {
        assertTrue(args.contains(str), "Argument missing: " + str);
      }
    }
  }


  private Properties loadProperties(final File propsFile) throws IOException {
    Properties actualProperties = new Properties();
    InputStream inStream = null;
    try {
        inStream = new FileInputStream(propsFile);
        actualProperties.load(inStream);
      } finally {
        if (null != inStream) {
          inStream.close();
        }
      }
    return actualProperties;
  }

}
