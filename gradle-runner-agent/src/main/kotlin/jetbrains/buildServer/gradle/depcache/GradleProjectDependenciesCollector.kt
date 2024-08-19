package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.gradle.agent.GradleRunnerFileUtil
import jetbrains.buildServer.gradle.agent.GradleRunnerService
import org.gradle.tooling.GradleConnector
import org.gradle.tooling.ProjectConnection
import org.gradle.tooling.model.GradleProject
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.regex.Pattern

class GradleProjectDependenciesCollector {

    fun collectProjectDependencies(buildTempDirectory: File, projectConnector: GradleConnector): Result<MutableSet<String>> {
        var dependencies: MutableSet<String> = HashSet()
        var outputFile: File = File(buildTempDirectory, DEPENDENCIES_OUTPUT_FILE)

        try {
            GradleRunnerFileUtil.createFileInBuildTempDirectory(buildTempDirectory, outputFile)

            projectConnector.connect().use({ connection ->
                FileOutputStream(outputFile, true).use({ outputStream ->
                    var rootProject: GradleProject = connection.getModel<GradleProject>(GradleProject::class.java)
                    collectDependencies(connection, rootProject, outputStream)
                })
            })

            extractDependencies(outputFile, dependencies)
            return Result.success(dependencies)
        } catch (e: Throwable) {
            return Result.failure(e)
        }
    }

    private fun collectDependencies(connection: ProjectConnection, project: GradleProject, outputStream: OutputStream?) {
        val taskPath = "${if (isRootProject(project)) "" else project.path}:dependencies"
        connection.newBuild()
            .forTasks(taskPath)
            .setStandardOutput(outputStream)
            .run()

        for (subProject in project.children) {
            collectDependencies(connection, subProject, outputStream)
        }
    }

    private fun extractDependencies(outputFile: File, dependencies: MutableSet<String>) {
        var pattern = Pattern.compile("([a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+:[a-zA-Z0-9_.-]+)")
        BufferedReader(FileReader(outputFile)).use({ reader ->
            reader.forEachLine { line ->
                if (isProjectOrTaskPath(line)) return@forEachLine
                val matcher = pattern.matcher(line)
                while (matcher.find()) {
                    dependencies.add(matcher.group(1))
                }
            }
        })
    }

    private fun isRootProject(project: GradleProject): Boolean {
        return project.path == ":"
    }

    private fun isProjectOrTaskPath(line: String): Boolean {
        return line.startsWith(":") || line.contains(":dependencies")
    }

    companion object {
        const val DEPENDENCIES_OUTPUT_FILE = "gradle-dependecies-tree.txt"
    }
}