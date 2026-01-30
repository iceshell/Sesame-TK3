package fansirsqi.xposed.sesame.util.maps

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log

/**
 * 抽象通用映射工具类，支持任意类型的Key和Value。
 */
abstract class IdMaps private constructor() {
    companion object {
        private const val TAG = "IdMaps"
        private val instances = ConcurrentHashMap<Class<out IdMaps>, IdMaps>()

        fun <T : IdMaps> getInstance(clazz: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return instances.computeIfAbsent(clazz) {
                try {
                    clazz.getDeclaredConstructor().newInstance() as IdMaps
                } catch (e: Exception) {
                    throw RuntimeException("Failed to create instance for ${clazz.name}", e)
                }
            } as T
        }
    }

    protected abstract fun thisFileName(): String

    private val idMap = ConcurrentHashMap<Any, Any>()
    val map: Map<Any, Any> = Collections.unmodifiableMap(idMap)

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: Any): T? {
        return try {
            idMap[key] as? T
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "Get value error for key: $key", e)
            null
        }
    }

    fun put(key: Any, value: Any) {
        idMap[key] = value
    }

    fun remove(key: Any) {
        idMap.remove(key)
    }

    fun clear() {
        idMap.clear()
    }

    @Synchronized
    fun load(userId: String? = null) {
        if (userId.isNullOrEmpty()) {
            Log.runtime(TAG, "Skip loading map for empty userId")
            doLoadGlobal()
        } else {
            idMap.clear()
            try {
                val file = Files.getTargetFileofUser(userId, thisFileName())
                val body = file?.let { Files.readFromFile(it) }.orEmpty()
                if (body.isNotBlank()) {
                    val newMap = ObjectMapper().readValue(body, object : TypeReference<Map<Any, Any>>() {})
                    idMap.putAll(newMap)
                }
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    private fun doLoadGlobal() {
        idMap.clear()
        try {
            val file = Files.getTargetFileofDir(Files.MAIN_DIR, thisFileName())
            val body = file?.let { Files.readFromFile(it) }.orEmpty()
            if (body.isNotBlank()) {
                val newMap = ObjectMapper().readValue(body, object : TypeReference<Map<Any, Any>>() {})
                idMap.putAll(newMap)
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    @Synchronized
    fun save(userId: String): Boolean {
        return try {
            val json = JsonUtil.formatJson(idMap)
            val file = Files.getTargetFileofUser(userId, thisFileName()) ?: return false
            Files.write2File(json, file)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            false
        }
    }

    @Synchronized
    fun save(): Boolean {
        return try {
            val json = JsonUtil.formatJson(idMap)
            val file = Files.getTargetFileofDir(Files.MAIN_DIR, thisFileName())
            Files.write2File(json, file)
        } catch (e: Exception) {
            Log.printStackTrace(e)
            false
        }
    }
}