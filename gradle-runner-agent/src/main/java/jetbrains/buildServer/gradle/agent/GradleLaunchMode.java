package jetbrains.buildServer.gradle.agent;

/**
 * The mode in which the Gradle build will be launched
 */
public enum GradleLaunchMode {

  /**
   * Launch build via Gradle Tooling API.
   * For Gradle 8.0 and newer
   */
  TOOLING_API,

  /**
   * Launch build via Gradle command line (the old way).
   * For Gradle before 8.0
   */
  COMMAND_LINE
}
