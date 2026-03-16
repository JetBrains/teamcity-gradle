package jetbrains.buildServer.gradle.agent.obsolete

import jetbrains.buildServer.gradle.agent.GradleRunnerContext
import org.gradle.tooling.GradleConnector
import java.io.Closeable

@Deprecated("Remove in a few releases after 2026.1 if the new version / user home detection implementations work without problems")
class GradleConnectorProvider(private val gradleRunnerContext: GradleRunnerContext) : Closeable {
    private val gradleConnectorDelegate: Lazy<GradleConnector?> = lazy { initConnector() }

    fun getConnector(): GradleConnector? = gradleConnectorDelegate.value

    override fun close() {
        if (gradleConnectorDelegate.isInitialized()) {
            gradleConnectorDelegate.value?.disconnect()
        }
    }

    private fun initConnector(): GradleConnector? = try {
        GradleToolingConnectorFactory.instantiate(
            gradleRunnerContext.workingDirectory,
            gradleRunnerContext.useWrapper,
            gradleRunnerContext.gradleHome,
            gradleRunnerContext.gradleWrapperProperties,
            gradleRunnerContext.buildRunnerContext.configParameters
        )
    } catch (t: Throwable) {
        gradleRunnerContext.flowLogger.warning("Unable to obtain project connector: " + t.message)
        null
    }
}
