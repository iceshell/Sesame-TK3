package fansirsqi.xposed.sesame.newutil

import android.os.Build
import androidx.annotation.RequiresApi
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import java.io.File
import java.nio.file.ClosedWatchServiceException
import java.nio.file.StandardWatchEventKinds
import java.nio.file.WatchService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.concurrent.locks.LockSupport
import kotlin.concurrent.read
import kotlin.concurrent.write

object DataStore {
    private const val SAVE_DEBOUNCE_MS = 1_500L

    private val mapper = jacksonObjectMapper()
    private val data = ConcurrentHashMap<String, Any>()
    private val lock = ReentrantReadWriteLock()
    private lateinit var storageFile: File

    private val saveScope = CoroutineScope(
        Dispatchers.IO + SupervisorJob() + CoroutineName("DataStoreSaver")
    )

    @Volatile
    private var pendingSaveJob: Job? = null

    @Volatile
    private var watcherJob: Job? = null

    @Volatile
    private var watchService: WatchService? = null

    fun init(dir: File) {
        storageFile = File(dir, "DataStore.json").apply {
            if (!exists()) createNewFile()
        }
        loadFromDisk()
        
        // 初始化完成后，如果内存中有数据（可能是初始化前保存的），立即写入磁盘
        if (data.isNotEmpty()) {
            flushPendingSave()
        }
        
        // ✅ minSdk 26+: 直接使用NIO WatchService
        startWatcherNio()
    }

    inline fun <reified T : Any> DataStore.getOrCreate(key: String) = getOrCreate(key, object : TypeReference<T>() {})

    private fun checkInit() {
        if (!::storageFile.isInitialized)
            throw IllegalStateException("DataStore.init(dir) must be called first!")
    }

    fun <T> get(key: String, clazz: Class<T>): T? = lock.read {
        data[key]?.let { mapper.convertValue(it, clazz) }
    }


    /* -------------------------------------------------- */
    /*  类型安全读取：Class 版（基本 / 自定义对象）         */
    /* -------------------------------------------------- */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrCreate(key: String, clazz: Class<T>): T = lock.write {
        data[key]?.let { return mapper.convertValue(it, clazz) }
        val default: T = when (clazz) {
            /* 基本容器 */
            java.util.List::class.java -> mutableListOf<Any>() as T
            java.util.Set::class.java -> mutableSetOf<Any>() as T
            java.util.Map::class.java -> mutableMapOf<String, Any>() as T

            /* 基本包装类型 */
            String::class.java -> "" as T
            Boolean::class.java -> false as T
            Int::class.java -> 0 as T
            Long::class.java -> 0L as T
            Double::class.java -> 0.0 as T
            Float::class.java -> 0f as T

            /* 其它：尝试无参构造 */
            else -> clazz.getDeclaredConstructor().newInstance()
        }
        data[key] = default
        scheduleSave()
        default
    }

    /* -------------------------------------------------- */
    /*  类型安全读取：TypeReference 版（支持嵌套泛型）       */
    /* -------------------------------------------------- */
    fun <T : Any> getOrCreate(key: String, typeRef: TypeReference<T>): T = lock.write {
        data[key]?.let { return mapper.convertValue(it, typeRef) }
        val default: T = createDefault(typeRef)
        data[key] = default
        scheduleSave()
        default
    }


    /* 根据 TypeReference 创建默认实例（支持嵌套） */
    @Suppress("UNCHECKED_CAST")
    private fun <T> createDefault(typeRef: TypeReference<T>): T {
        mapper.typeFactory.constructType(typeRef)
        val raw = mapper.typeFactory.constructType(typeRef).rawClass
        return when (raw) {
            java.util.List::class.java -> mutableListOf<Any>() as T
            java.util.Set::class.java -> mutableSetOf<Any>() as T
            java.util.Map::class.java -> mutableMapOf<String, Any>() as T
            else -> raw.getDeclaredConstructor().newInstance() as T
        }
    }

    private fun loadFromDisk() {
        if (!::storageFile.isInitialized) return
        if (storageFile.length() == 0L) return
        lock.write {
            try {
                val loaded: Map<String, Any> = mapper.readValue(storageFile)
                data.clear()
                data.putAll(loaded)
            } catch (e: MismatchedInputException) {
                // Ignore, may be caused by file being written
            }
        }
    }

    private val prettyPrinter = DefaultPrettyPrinter().apply {
        indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE)   // 数组换行
        indentObjectsWith(DefaultIndenter("    ", DefaultIndenter.SYS_LF)) // 对象换行 + 4 空格
    }

    private fun saveToDisk() {
        if (!::storageFile.isInitialized) {
            // DataStore 尚未初始化，跳过保存（仅保存在内存中）
            return
        }
        val tempFile = File(storageFile.parentFile, storageFile.name + ".tmp")
        val bakFile = File(storageFile.parentFile, storageFile.name + ".bak")
        try {
            tempFile.writeText(mapper.writer(prettyPrinter).writeValueAsString(data))

            if (bakFile.exists()) {
                bakFile.delete()
            }

            if (storageFile.exists() && !storageFile.renameTo(bakFile)) {
                tempFile.delete()
                return
            }

            if (!tempFile.renameTo(storageFile)) {
                if (bakFile.exists()) {
                    bakFile.renameTo(storageFile)
                }
                tempFile.delete()
                return
            }

            if (bakFile.exists()) {
                bakFile.delete()
            }
        } catch (e: Exception) {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun scheduleSave() {
        if (!::storageFile.isInitialized) {
            return
        }
        pendingSaveJob?.cancel()
        pendingSaveJob = saveScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            saveToDisk()
        }
    }

    fun flushPendingSave() {
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        saveToDisk()
    }

    fun shutdown() {
        pendingSaveJob?.cancel()
        pendingSaveJob = null
        watcherJob?.cancel()
        watcherJob = null
        saveScope.cancel()
        watchService?.close()
        watchService = null
    }

    private fun startWatcher() {
        watcherJob?.cancel()
        watcherJob = GlobalThreadPools.execute(Dispatchers.IO) {
            var last = storageFile.lastModified()
            while (isActive) {
                LockSupport.parkNanos(1_000_000_000L)
                val current = storageFile.lastModified()
                if (current > last) {
                    last = current
                    loadFromDisk()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startWatcherNio() {
        watcherJob?.cancel()
        watcherJob = GlobalThreadPools.execute(Dispatchers.IO) {
            val path = storageFile.toPath().parent
            watchService?.close()
            val watch = path.fileSystem.newWatchService()
            watchService = watch
            path.register(watch, StandardWatchEventKinds.ENTRY_MODIFY)
            while (isActive) {
                try {
                    val key = watch.take()
                    key.pollEvents().forEach {
                        if (it.context().toString() == storageFile.name) loadFromDisk()
                    }
                    key.reset()
                } catch (_: ClosedWatchServiceException) {
                    return@execute
                } catch (_: InterruptedException) {
                    return@execute
                }
            }
        }
    }

    /* -------------------------------------------------- */
    /*  简易 put / remove（可选）                          */
    /* -------------------------------------------------- */
    fun put(key: String, value: Any) = lock.write {
        data[key] = value
        scheduleSave()
    }

    fun remove(key: String) = lock.write {
        data.remove(key)
        scheduleSave()
    }
}