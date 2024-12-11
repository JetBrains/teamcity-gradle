package jetbrains.buildServer.gradle.depcache

import jetbrains.buildServer.agent.cache.depcache.DependencyCache
import jetbrains.buildServer.util.FileUtil
import jetbrains.buildServer.util.Predicate
import java.io.File
import java.security.MessageDigest
import java.util.regex.Pattern

class GradleDependencyCacheInvalidationDataCollector {

    fun collect(workDir: File, cache: DependencyCache, depthLimit: Int): Result<Map<String, String>> {
        return runCatching {
            val files = ArrayList<String>()
            FileUtil.listFilesRecursively(workDir, File.separator, false, depthLimit, FILE_FILTER, files)
            val checksums = buildChecksums(files, workDir, cache)

            return Result.success(checksums)
        }
    }

    private fun buildChecksums(files: List<String>, workDir: File, cache: DependencyCache): Map<String, String> {
        val result = HashMap<String, String>()
        val messageDigest = MessageDigest.getInstance("SHA-256")

        for (filePath in files) {
            val targetFile = File(workDir, filePath)
            if (!targetFile.exists() || !targetFile.isFile) {
                cache.logWarning("File not found or is not a valid file: $targetFile")
                continue
            }

            val content = targetFile.readBytes()
            val digest = messageDigest.digest(content)

            result[filePath.replace(File.separatorChar, '/')] = digest.toHex()
        }

        return result
    }

    private fun ByteArray.toHex(): String {
        return joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val REGEX: String = "(.*\\.gradle(\\.kts)?)" +  // Matches any .gradle or .gradle.kts files
                "|(gradle-wrapper\\.properties)" +                    // Matches gradle-wrapper.properties
                "|(Versions\\.kt)" +                                  // Matches Versions.kt
                "|(Dependencies\\.kt)" +                              // Matches Dependencies.kt
                "|(.*\\.versions\\.toml)" +                           // Matches any file ending with .versions.toml
                "|(versions\\.properties)"                            // Matches versions.properties
        private val FILENAME_PATTERN = Pattern.compile(REGEX)
        private val FILE_FILTER = Predicate({ file: File ->
            file.isDirectory || FILENAME_PATTERN.matcher(file.name).matches()
        })
    }
}