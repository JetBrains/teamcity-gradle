package jetbrains.buildServer.gradle.runtime;

import org.jetbrains.annotations.NotNull;

public class BuildContext {

  /**
   * Path to teamcity.build.parameters
   */
  private final String myTcBuildParametersPath;

  /**
   * Temporary directory of the build
   */
  private final String myBuildTempDir;

  /**
   * Temporary file with environment of the build
   */
  private final String myEnvTempFilePath;

  /**
   * Temporary file with Gradle build parameters
   */
  private final String myGradleParamsTempFilePath;

  /**
   * Temporary file with JVM arguments of the build
   */
  private final String myJvmArgsTempFilePath;

  /**
   * Temporary file with Gradle tasks to execute
   */
  private final String myGradleTasksTempFilePath;

  public BuildContext(@NotNull String tcBuildParametersPath,
                      @NotNull String buildTempDir,
                      @NotNull String envTempFilePath,
                      @NotNull String gradleParamsTempFilePath,
                      @NotNull String jvmArgsTempFilePath,
                      @NotNull String gradleTasksTempFilePath) {
    myTcBuildParametersPath = tcBuildParametersPath;
    myBuildTempDir = buildTempDir;
    myEnvTempFilePath = envTempFilePath;
    myGradleParamsTempFilePath = gradleParamsTempFilePath;
    myJvmArgsTempFilePath = jvmArgsTempFilePath;
    myGradleTasksTempFilePath = gradleTasksTempFilePath;
  }

  @NotNull
  public String getTcBuildParametersPath() {
    return myTcBuildParametersPath;
  }

  @NotNull
  public String getBuildTempDir() {
    return myBuildTempDir;
  }

  @NotNull
  public String getEnvTempFilePath() {
    return myEnvTempFilePath;
  }

  @NotNull
  public String getGradleParamsTempFilePath() {
    return myGradleParamsTempFilePath;
  }

  @NotNull
  public String getJvmArgsTempFilePath() {
    return myJvmArgsTempFilePath;
  }

  @NotNull
  public String getGradleTasksTempFilePath() {
    return myGradleTasksTempFilePath;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{" +
           "myTcBuildParametersPath=" + myTcBuildParametersPath +
           ", myBuildTempDir='" + myBuildTempDir + '\'' +
           ", myEnvTempFilePath='" + myEnvTempFilePath + '\'' +
           ", myGradleParamsTempFilePath='" + myGradleParamsTempFilePath + '\'' +
           ", myJvmArgsTempFilePath='" + myJvmArgsTempFilePath + '\'' +
           ", myGradleTasksTempFilePath='" + myGradleTasksTempFilePath + '\'' +
           '}';
  }
}
