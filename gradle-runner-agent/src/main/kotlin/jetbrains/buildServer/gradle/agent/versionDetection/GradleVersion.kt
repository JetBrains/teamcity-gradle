package jetbrains.buildServer.gradle.agent.versionDetection

data class GradleVersion(val version: String) {
    override fun toString(): String = version
}
