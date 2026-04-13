package jetbrains.buildServer.gradle.agent.commandLineComposers

import jetbrains.buildServer.agent.runner.ProgramCommandLine
import jetbrains.buildServer.agent.runner.SimpleProgramCommandLine
import jetbrains.buildServer.gradle.agent.GradleLaunchMode
import jetbrains.buildServer.gradle.agent.tasks.GradleTasksComposer

class GradleSimpleV2CommandLineComposer(private val tasksComposer: GradleTasksComposer) : GradleCommandLineComposer {
    override fun getLaunchMode(): GradleLaunchMode = GradleLaunchMode.COMMAND_LINE_V2

    override fun compose(parameters: GradleCommandLineComposerParameters): ProgramCommandLine {
        val gradleParameters = buildList {
            addAll(parameters.initialGradleParams)
            addAll(
                tasksComposer.getGradleParameters(
                    getLaunchMode(),
                    parameters.runnerParameters,
                    parameters.gradleUserDefinedParams,
                    parameters.pluginsDir.toFile()
                )
            )
            addAll(parameters.gradleTasks)
        }

        return SimpleProgramCommandLine(
            parameters.env,
            parameters.workingDir.toString(),
            parameters.exePath,
            gradleParameters
        )
    }
}
