

package jetbrains.buildServer.gradle.test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jetbrains.buildServer.gradle.GradleRunnerConstants;
import jetbrains.buildServer.gradle.server.GradleRunType;
import jetbrains.buildServer.requirements.Requirement;
import jetbrains.buildServer.requirements.RequirementType;
import jetbrains.buildServer.serverSide.MockServerPluginDescriptior;
import jetbrains.buildServer.serverSide.RunTypeRegistry;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * Author: Nikita.Skvortsov
 * Date: 11/9/10
 */
public class GradleRunTypeTest {

  private GradleRunType myRunType;

  private static final String DEFAULT_DESCRIPTION = "Gradle tasks: Default\n" +
                                                    "Use wrapper script: no";

  private static final String NON_DEFAULT_DESCRIPTION = "Gradle tasks: clean build\n" +
                                                        "Use wrapper script: yes";

  private static final String INCREMENTAL_DESCRIPTION = "Run incremental builds using :buildDependents\n" +
                                                        "Use wrapper script: yes";

  @BeforeMethod
  public void setUp() {
    final Mockery context = new Mockery();
    final RunTypeRegistry runTypeRegistry = context.mock(RunTypeRegistry.class);
    context.checking(new Expectations() {{
      allowing(runTypeRegistry).registerRunType(with(any(GradleRunType.class)));
    }});
    myRunType = new GradleRunType(runTypeRegistry, new MockServerPluginDescriptior());
  }

  @Test
  public void testDefaultDescribeParameters() throws Exception {
    String description = myRunType.describeParameters(new HashMap<String, String>());
    assertEquals(description, DEFAULT_DESCRIPTION, "Wrong description received.");
  }

  @Test
  public void testNonDefaultDescribeParameters() throws Exception {
    Map<String,  String> params = new HashMap<String, String>();
    params.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");
    params.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());

    String description = myRunType.describeParameters(params);
    assertEquals(description, NON_DEFAULT_DESCRIPTION, "Wrong description received");
  }

  @Test
  public void testIncrementalSetting() throws Exception {
    Map<String,  String> params = new HashMap<String, String>();
    params.put(GradleRunnerConstants.IS_INCREMENTAL, Boolean.TRUE.toString());
    params.put(GradleRunnerConstants.GRADLE_TASKS, "clean build");
    params.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, Boolean.TRUE.toString());

    String description = myRunType.describeParameters(params);
    assertEquals(description, INCREMENTAL_DESCRIPTION, "Wrong description received");
  }

  @Test
  public void testRequireGradleHome() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    final List<Requirement> reqs = myRunType.getRunnerSpecificRequirements(params);
    assertEquals(reqs.size(), 1, "Wrong size");
    final Requirement req = reqs.get(0);
    assertEquals(req.getPropertyName(), "env.GRADLE_HOME");
    assertEquals(req.getType(), RequirementType.EXISTS);
  }

  @Test
  public void testDoNotRequireGradleHomeForPathOrWrapper() throws Exception {
    Map<String, String> params = new HashMap<String, String>();
    params.put(GradleRunnerConstants.GRADLE_WRAPPER_FLAG, "true");
    assertEquals(myRunType.getRunnerSpecificRequirements(params).size(), 0);

    params.clear();
    params.put(GradleRunnerConstants.GRADLE_HOME, "/path/to/gradle/home");
    assertEquals(myRunType.getRunnerSpecificRequirements(params).size(), 0);
  }
}