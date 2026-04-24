package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer

class GradleCliCommandLineComposer(tasksComposer: GradleTasksComposer) : GradleCommandLineComposerBase(tasksComposer) {
    override fun getLaunchMode(): GradleLaunchMode = GradleLaunchMode.COMMAND_LINE
}
