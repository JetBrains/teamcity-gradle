package jetbrains.buildServer.gradle.depcache

import com.google.gson.Gson
import jetbrains.buildServer.agent.cache.depcache.invalidation.Serializable
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Objects

class GradleDependencies(private val cacheRootDependencies: Map<String, Set<String>>) : Serializable {

    override fun serialize(): ByteArray {
        return GSON.toJson(this).toByteArray(JSON_CHARSET)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        var that = other as GradleDependencies
        return cacheRootDependencies == that.cacheRootDependencies
    }

    override fun hashCode(): Int {
        return Objects.hash(cacheRootDependencies)
    }

    companion object {
        private val JSON_CHARSET: Charset = StandardCharsets.UTF_8
        private val GSON = Gson()

        fun deserialize(bytes: ByteArray): GradleDependencies {
            return GSON.fromJson(String(bytes, JSON_CHARSET), GradleDependencies::class.java)
        }
    }
}