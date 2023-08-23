package jetbrains.buildServer.gradle.agent;

/**
 * The mode in which the Gradle build will be launched
 */
public enum GradleLaunchMode {

  /**
   * Launch build via Gradle Tooling API.
   * For Gradle 8.0 and newer
   */
  GRADLE_TOOLING_API,

  /**
   * Launch build via Gradle (the old way).
   * For Gradle before 8.0
   */
  GRADLE,

  /**
   * Couldn't detect launch mode.
   */
  UNDEFINED
}
