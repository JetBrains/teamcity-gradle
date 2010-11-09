package jetbrains.buildServer.gradle.agent.init;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import jetbrains.buildServer.agent.AgentRuntimeProperties;
import org.apache.log4j.Logger;
import org.gradle.api.Project;
import org.gradle.api.ProjectEvaluationListener;
import org.gradle.api.ProjectState;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.testing.Test;

/**
 * Author: Nikita.Skvortsov
 * Date: 10/26/10
 */
public class PropertiesLoadListener implements ProjectEvaluationListener {

  private static Logger log = Logger.getLogger(PropertiesLoadListener.class);

  public void beforeEvaluate(Project project) {
    loadProps(project);
  }

  public void afterEvaluate(Project project, ProjectState projectState) {
    // TODO refactor this "multicast"
    final Map teamcityProperties = (Map)project.getProperties().get("teamcity");
    if (null != teamcityProperties) {
      String jvmArgs = (String) teamcityProperties.get("gradle.test.jvmargs");
      passJvmArgsToTest(project, jvmArgs);
    }
  }


  private void passJvmArgsToTest(Project project, String jvmArgs) {
    TaskCollection<Test> testTasks = project.getTasks().withType(Test.class);
    if (null != jvmArgs && null != testTasks) {
      for (Test task : testTasks) {
        final String[] arguments = jvmArgs.split("\n");
        task.jvmArgs(arguments);
      }
    }
  }

  private void loadProps(Project p) {
    String propsFilePath = System.getProperty(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_PROP);
    if (null == propsFilePath) {
      propsFilePath = System.getenv(AgentRuntimeProperties.AGENT_BUILD_PARAMS_FILE_ENV);
    }
    if (null != propsFilePath) {
      File propsFile = p.file(propsFilePath);
      Properties props = new Properties();
      InputStream inStream = null;

      try {
        inStream = new FileInputStream(propsFile);
        props.load(inStream);
      } catch (FileNotFoundException e) {
        log.error(e.getMessage(), e);
      } catch (IOException e) {
        log.error(e.getMessage(), e);
      } finally {
        if (null != inStream) {
          try {
            inStream.close();
          } catch (IOException e) {
            log.error("Failed to close file stream!", e);
          }
        }
      }

      p.setProperty("teamcity", props);
    } else {
      p.setProperty("teamcity", new HashMap());
    }
  }
}
