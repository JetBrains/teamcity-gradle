package jetbrains.buildServer.gradle.agent.propertySplit;

import com.google.common.annotations.VisibleForTesting;
import java.io.File;
import java.io.IOException;
import java.util.*;
import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil;
import org.jetbrains.annotations.NotNull;

import static jetbrains.buildServer.gradle.agent.propertySplit.InitScriptParametersConstants.*;
import static jetbrains.buildServer.gradle.agent.propertySplit.SplitPropertiesFilenameBuilder.buildStaticPropertiesFilename;

public class TeamCityBuildPropertiesGradleSplitter implements GradleBuildPropertiesSplitter {

  private static final String TEAMCITY_BUILD_PROPERTIES_ENV_KEY = "TEAMCITY_BUILD_PROPERTIES_FILE";

  private static final String TEAMCITY_CONFIGURATION_PROPERTIES_KEY = "teamcity.configuration.properties.file";

  @Override
  public SplitablePropertyFile getType() {
    return SplitablePropertyFile.TEAMCITY_BUILD_PROPERTIES;
  }

  @Override
  public void split(@NotNull Map<String, String> environment,
                    @NotNull File buildTempDir) throws RunBuildException {
    String propertyFilePath = environment.get(TEAMCITY_BUILD_PROPERTIES_ENV_KEY);
    if (propertyFilePath == null) return;
    File propertyFile = new File(propertyFilePath);
    if (!propertyFile.exists()) return;

    File propertyFileParent = propertyFile.getParentFile();

    File staticPropertyFile = new File(propertyFileParent, buildStaticPropertiesFilename(propertyFile.getName()));

    Properties teamCityBuildProperties = readProperties(propertyFile);

    splitInternal(teamCityBuildProperties, staticPropertyFile, buildTempDir);
  }

  private void splitInternal(@NotNull Properties teamCityBuildProperties,
                             @NotNull File staticPropertyFile,
                             @NotNull File buildTempDir) throws RunBuildException {
    GradleBuildProperties staticProperties = new GradleBuildProperties();

    teamCityBuildProperties.forEach((propertyKey, propertyValue) -> {
      if (!isDynamicProperty((String)propertyKey)) {
        staticProperties.put(propertyKey, propertyValue);
      }
    });

    addStaticPropertiesFromConfigurationFile(staticProperties, teamCityBuildProperties);

    predefineDefaultValues(staticProperties);

    storeProperties(staticPropertyFile, staticProperties, buildTempDir);
  }

  /**
   * Determines if a property is dynamic.
   * "Dynamic" means that the property changes from build to build. E.g.: "build.number".
   *
   * To determine if dependency parameters are dynamic, we must remove the dep.<btID> prefix from them and check as regular parameters.
   * E.g.: there is dependency parameter "dep.buildType.teamcity.idea.home".
   * First of all, we remove the prefix from it, so it turns into "teamcity.idea.home".
   * Then we process it as regular parameter.
   * more about dependency parameters: https://www.jetbrains.com/help/teamcity/predefined-build-parameters.html#Dependency+Parameters
   *
   * @return true - the property is dynamic
   */
  @VisibleForTesting
  public boolean isDynamicProperty(@NotNull String propertyKey) {
    if (propertyKey.startsWith("dep.")) {
      propertyKey = propertyKey.substring(propertyKey.indexOf('.', 4) + 1);
    }

    if (propertyKey.startsWith("build.")) {
      return true;
    }

    if (propertyKey.startsWith("teamcity.build.id")) {
      return true;
    }

    if (propertyKey.startsWith("teamcity.")) {
      if (propertyKey.startsWith("teamcity.build.")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.buildConfName")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.buildType.")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.configuration.")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.runner.")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.projectName")) {
        return false;
      }
      if (propertyKey.startsWith("teamcity.tests.")) {
        return false;
      }
      return true;
    }

    return false;
  }

  /**
   * Adds configuration properties to the static file properties in order not to use teamcity.config.parameters in the init script.
   * teamcity.config.parameters changes from build to build, its usage will cause inability to use Gradle configuration-cache.
   */
  private void addStaticPropertiesFromConfigurationFile(@NotNull Map<Object, Object> staticProperties,
                                                        @NotNull Properties teamCityBuildProperties) throws RunBuildException {

    if (!teamCityBuildProperties.containsKey(TEAMCITY_CONFIGURATION_PROPERTIES_KEY)) return;

    String tcConfigPropertiesFilePath = (String) teamCityBuildProperties.get(TEAMCITY_CONFIGURATION_PROPERTIES_KEY);
    if (tcConfigPropertiesFilePath == null) return;
    File tcConfigPropertiesFile = new File(tcConfigPropertiesFilePath);
    if (!tcConfigPropertiesFile.exists()) return;

    Properties tcConfigFileProperties = readProperties(tcConfigPropertiesFile);
    if (tcConfigFileProperties.containsKey(TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY)) {
      staticProperties.put(TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY, tcConfigFileProperties.get(TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY));
    }
    if (tcConfigFileProperties.containsKey(TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY)) {
      staticProperties.put(TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY, tcConfigFileProperties.get(TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY));
    }
    if (tcConfigFileProperties.containsKey(TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY)) {
      staticProperties.put(TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY, tcConfigFileProperties.get(TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY));
    }
  }

  /**
   * For the parameters used in init_since_8.gradle, we should define default values.
   * This is needed in order not to read from teamcity.build.parameters in {@link GradleBuildPropertiesContainer}.
   * Reading from teamcity.build.parameters always causes inability to use configuration-cache,
   * because this file changes from build to build.
   */
  private void predefineDefaultValues(@NotNull Map<Object, Object> staticProperties) {
    putIfKeyAbsent(staticProperties, TEAMCITY_BUILD_GRADLE_TEST_JVM_ARGS_KEY, "");
    putIfKeyAbsent(staticProperties, TEAMCITY_CONFIGURATION_USE_TEST_RETRY_PLUGIN_KEY, "true");
    putIfKeyAbsent(staticProperties, TEAMCITY_CONFIGURATION_TEST_NAME_FORMAT_KEY, "");
    putIfKeyAbsent(staticProperties, TEAMCITY_CONFIGURATION_IGNORE_SUITE_FORMAT_KEY, "");
    putIfKeyAbsent(staticProperties, TEAMCITY_BUILD_STACKTRACE_LOG_DIR_KEY, "");
    putIfKeyAbsent(staticProperties, TEAMCITY_BUILD_CHANGED_FILES_KEY, "");
  }

  private void putIfKeyAbsent(@NotNull Map<Object, Object> target,
                              @NotNull Object key,
                              @NotNull Object value) {
    if (!target.containsKey(key)) {
      target.put(key, value);
    }
  }

  private Properties readProperties(@NotNull File propertyFile) throws RunBuildException {
    try {
      return GradleRunnerFileUtil.readProperties(propertyFile);
    } catch (IOException e) {
      throw new RunBuildException("Couldn't read properties from file: name=" + propertyFile.getName(), e);
    }
  }

  private void storeProperties(@NotNull File destination,
                               @NotNull GradleBuildProperties properties,
                               @NotNull File buildTempDir) throws RunBuildException {
    try {
      GradleRunnerFileUtil.storeProperties(buildTempDir, properties, destination);
    } catch (IOException e) {
      throw new RunBuildException("Couldn't store properties to file: name=" + destination.getName(), e);
    }
  }
}
