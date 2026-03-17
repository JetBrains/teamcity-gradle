package jetbrains.buildServer.gradle.runtime.listening.event;

/**
 * Describes what kind of failure happened during the build
 */
public enum FailureKind {

  /**
   * Compilation error
   */
  COMPILATION,

  /**
   * Undefined problem
   */
  UNDEFINED
}
