package jetbrains.buildServer.gradle.depcache

import com.intellij.openapi.diagnostic.Logger
import jetbrains.buildServer.agent.AgentLifeCycleAdapter
import jetbrains.buildServer.agent.AgentLifeCycleListener
import jetbrains.buildServer.agent.BuildAgent
import jetbrains.buildServer.gradle.agent.GradleRunnerCacheDirectoryProvider
import jetbrains.buildServer.util.EventDispatcher

class GradleCacheDirectoryAllowedListAdder(
    private val agentLifecycleEventDispatcher: EventDispatcher<AgentLifeCycleListener>
) : AgentLifeCycleAdapter() {

    fun register() {
        agentLifecycleEventDispatcher.addListener(this)
    }

    public override fun afterAgentConfigurationLoaded(agent: BuildAgent) {
        // we need to add Gradle cache directory to the allowed list in order to be able to restore the agent-wide Gradle dependencies
        super.beforeAgentConfigurationLoaded(agent)
        val agentConfiguration = agent.configuration
        var allowedList = agentConfiguration.configurationParameters.get(ARTIFACTS_ALLOWED_LIST_PARAMETER_KEY)
        val gradleCacheDirectory = GradleRunnerCacheDirectoryProvider.getGradleRunnerCacheDirectory(agentConfiguration).path

        LOG.info("Adding Gradle cache directory $gradleCacheDirectory into the allowedList $allowedList")

        allowedList = if (allowedList.isNullOrBlank()) {
            gradleCacheDirectory
        } else "${allowedList.trim()}$ARTIFACTS_ALLOWED_LIST_SEPARATOR$gradleCacheDirectory"

        agentConfiguration.addConfigurationParameter(ARTIFACTS_ALLOWED_LIST_PARAMETER_KEY, allowedList!!)
    }

    fun unregister() {
        agentLifecycleEventDispatcher.removeListener(this)
    }

    companion object {
        private val LOG = Logger.getInstance(GradleCacheDirectoryAllowedListAdder::class.java)
        private val ARTIFACTS_ALLOWED_LIST_PARAMETER_KEY = "teamcity.artifactDependenciesResolution.allowedList"
        private val ARTIFACTS_ALLOWED_LIST_SEPARATOR = ","
    }
}