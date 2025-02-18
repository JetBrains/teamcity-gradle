package jetbrains.buildServer.gradle.depcache

import com.google.gson.Gson
import jetbrains.buildServer.agent.cache.depcache.invalidation.Serializable
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

data class GradleDependencyCacheProjectFilesChecksum(private val absoluteCachesPathToChecksum: Map<String, String>) : Serializable {

    override fun serialize(): ByteArray {
        return GSON.toJson(this).toByteArray(JSON_CHARSET)
    }

    companion object {
        private val JSON_CHARSET: Charset = StandardCharsets.UTF_8
        private val GSON = Gson()

        fun deserialize(bytes: ByteArray): GradleDependencyCacheProjectFilesChecksum {
            return GSON.fromJson(String(bytes, JSON_CHARSET), GradleDependencyCacheProjectFilesChecksum::class.java)
        }
    }
}