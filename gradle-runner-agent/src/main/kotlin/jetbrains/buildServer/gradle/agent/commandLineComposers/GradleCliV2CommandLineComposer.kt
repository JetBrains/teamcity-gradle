package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer

class GradleCliV2CommandLineComposer(tasksComposer: GradleTasksComposer) : GradleCliCommandLineComposerBase(tasksComposer) {
    override fun getLaunchMode(): GradleLaunchMode = GradleLaunchMode.COMMAND_LINE_V2
}
