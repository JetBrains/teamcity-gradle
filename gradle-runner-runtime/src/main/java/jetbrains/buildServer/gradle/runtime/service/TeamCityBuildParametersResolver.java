package jetbrains.buildServer.gradle.runtime.service;

import java.io.File;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.gradle.GradleRunnerConstants.TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY;
import static jetbrains.buildServer.gradle.GradleRunnerConstants.TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY;

public class TeamCityBuildParametersResolver {

  @NotNull
  public static File getTcBuildParametersFile(@NotNull Map<String, String> environment) {
    String filePath = System.getProperty(TC_BUILD_PROPERTIES_SYSTEM_PROPERTY_KEY);
    if (null == filePath) {
      filePath = environment.get(TC_BUILD_PROPERTIES_SYSTEM_ENV_KEY);
    }

    if (filePath == null) {
      throw new RuntimeException("teamcity.build.parameters must be set");
    }
    File propertyFile = new File(filePath);
    if (!propertyFile.exists()) {
      throw new RuntimeException("teamcity.build.parameters file doesn't exist");
    }

    return propertyFile;
  }
}
