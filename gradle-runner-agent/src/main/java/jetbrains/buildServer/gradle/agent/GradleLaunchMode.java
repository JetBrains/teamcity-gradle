package jetbrains.buildServer.gradle.agent;

/**
 * The mode in which the Gradle build will be launched
 */
public enum GradleLaunchMode {

  /**
   * Launch the build via the Gradle Tooling API.
   * Can be used with Gradle 8.1 or newer.
   */
  TOOLING_API,

  /**
   * Launch the build via the Gradle command line using the old init script.
   * Used for Gradle versions earlier than 8.1.
   */
  COMMAND_LINE,

  /**
   * Launch the build via the Gradle command line using the new init script.
   * Can be used with Gradle 8.1 or newer.
   */
  COMMAND_LINE_V2
}
