package fansirsqi.xposed.sesame.util

import android.util.Log as AndroidLog
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.model.BaseModel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * 日志工具类，负责初始化和管理各种类型的日志记录器，并提供日志输出方法。
 *
 * **迁移说明**:
 * - 保持所有方法的Java兼容性 (@JvmStatic)
 * - 使用Kotlin的对象单例特性
 * - 优化字符串处理
 */
@Suppress("TooGenericExceptionCaught")
object Log {

    private const val TAG = ""
    private val RUNTIME_LOGGER: Logger
    private val SYSTEM_LOGGER: Logger
    private val RECORD_LOGGER: Logger
    private val DEBUG_LOGGER: Logger
    private val FOREST_LOGGER: Logger
    private val FARM_LOGGER: Logger
    private val OTHER_LOGGER: Logger
    private val ERROR_LOGGER: Logger
    private val CAPTURE_LOGGER: Logger

    // 错误去重机制：记录错误特征和出现次数
    private val errorCountMap = ConcurrentHashMap<String, AtomicInteger>()
    private const val MAX_DUPLICATE_ERRORS = 3 // 最多打印3次相同错误
    private val lastPrintStackAtMs = ConcurrentHashMap<String, Long>()
    private const val PRINT_STACK_INTERVAL_MS = 30_000L

    init {
        try {
            Logback.configureLogbackDirectly()
        } catch (e: Exception) {
            AndroidLog.e("Log", "configureLogbackDirectly failed: ${e.message}", e)
        } catch (e: LinkageError) {
            AndroidLog.e("Log", "configureLogbackDirectly failed: ${e.message}", e)
        }
        RUNTIME_LOGGER = LoggerFactory.getLogger("runtime")
        SYSTEM_LOGGER = LoggerFactory.getLogger("system")
        RECORD_LOGGER = LoggerFactory.getLogger("record")
        DEBUG_LOGGER = LoggerFactory.getLogger("debug")
        FOREST_LOGGER = LoggerFactory.getLogger("forest")
        FARM_LOGGER = LoggerFactory.getLogger("farm")
        OTHER_LOGGER = LoggerFactory.getLogger("other")
        ERROR_LOGGER = LoggerFactory.getLogger("error")
        CAPTURE_LOGGER = LoggerFactory.getLogger("capture")
    }

    private fun truncateLogmsg(msg: String): String {
        return if (msg.length > 16) {
            msg.substring(0, 16) + "..."
        } else {
            msg
        }
    }

    @JvmStatic
    fun system(msg: String) {
        SYSTEM_LOGGER.info("$TAG{}", msg)
    }

    @JvmStatic
    fun system(TAG: String, msg: String) {
        system("[$TAG]: $msg")
    }

    @JvmStatic
    fun runtime(msg: String) {
        if (msg.contains("[SESAME_TK_READY]")) {
            RUNTIME_LOGGER.info("$TAG{}", msg)
            return
        }
        if (BaseModel.runtimeLog.value == true || BuildConfig.DEBUG) {
            RUNTIME_LOGGER.info("$TAG{}", msg)
        }
    }

    @JvmStatic
    fun runtime(TAG: String, msg: String) {
        runtime("[$TAG]: $msg")
    }

    @JvmStatic
    fun record(msg: String) {
        runtime(msg)
        if (BaseModel.recordLog.value == true) {
            RECORD_LOGGER.info("$TAG{}", msg)
        }
    }

    @JvmStatic
    fun record(TAG: String, msg: String) {
        record("[$TAG]: $msg")
    }

    @JvmStatic
    fun forest(msg: String) {
        record(msg)
        FOREST_LOGGER.info("{}", msg)
    }

    @JvmStatic
    fun forest(TAG: String, msg: String) {
        forest("[$TAG]: $msg")
    }

    @JvmStatic
    fun farm(msg: String) {
        record(msg)
        FARM_LOGGER.info("{}", msg)
    }

    @JvmStatic
    fun farm(TAG: String, msg: String) {
        farm("[$TAG]: $msg")
    }

    @JvmStatic
    fun other(msg: String) {
        record(msg)
        OTHER_LOGGER.info("{}", msg)
    }

    @JvmStatic
    fun other(TAG: String, msg: String) {
        other("[$TAG]: $msg")
    }

    @JvmStatic
    fun debug(msg: String) {
        runtime(msg)
        DEBUG_LOGGER.info("{}", msg)
    }

    @JvmStatic
    fun debug(TAG: String, msg: String) {
        debug("[$TAG]: $msg")
    }

    @JvmStatic
    fun error(msg: String) {
        runtime(msg)
        ERROR_LOGGER.error("$TAG{}", msg)
    }

    @JvmStatic
    fun error(TAG: String, msg: String) {
        error("[$TAG]: $msg")
    }

    @JvmStatic
    fun capture(msg: String) {
        CAPTURE_LOGGER.info("$TAG{}", msg)
    }

    @JvmStatic
    fun capture(TAG: String, msg: String) {
        capture("[$TAG]: $msg")
    }

    /**
     * 检查是否应该打印此错误（去重机制）
     *
     * @param th 异常对象
     * @return true=应该打印，false=已重复太多次
     */
    private fun shouldPrintError(th: Throwable?): Boolean {
        if (th == null) return true

        // 提取错误特征（类名+消息的前50个字符）
        val message = th.message
        val errorSignature = if (message != null) {
            // 特殊处理：JSON解析空字符串错误
            if (message.contains("End of input at character 0")) {
                "JSONException:EmptyResponse"
            } else {
                "${th.javaClass.simpleName}:${message.substring(0, minOf(50, message.length))}"
            }
        } else {
            "${th.javaClass.simpleName}:null"
        }

        val count = errorCountMap.computeIfAbsent(errorSignature) { AtomicInteger(0) }
        val currentCount = count.incrementAndGet()

        // 如果是第3次，记录一个汇总信息
        if (currentCount == MAX_DUPLICATE_ERRORS) {
            runtime("⚠️ 错误【$errorSignature】已出现${currentCount}次，后续将不再打印详细堆栈")
            return false
        }

        // 超过最大次数后不再打印
        if (currentCount > MAX_DUPLICATE_ERRORS) {
            return false
        }

        return true
    }

    @JvmStatic
    fun printStackTrace(th: Throwable?) {
        if (!shouldPrintError(th)) return
        val stackTrace = "error: ${AndroidLog.getStackTraceString(th)}"
        error(stackTrace)
    }

    @JvmStatic
    fun printStackTrace(msg: String?, th: Throwable?) {
        if (!shouldPrintError(th)) return
        val stackTrace = "Throwable error: ${AndroidLog.getStackTraceString(th)}"
        error(msg ?: "null", stackTrace)
    }

    @JvmStatic
    fun printStackTrace(TAG: String, msg: String?, th: Throwable?) {
        if (!shouldPrintError(th)) return
        val stackTrace = "[$TAG] Throwable error: ${AndroidLog.getStackTraceString(th)}"
        error(msg ?: "null", stackTrace)
    }

    @JvmStatic
    fun printStackTrace(e: Exception?) {
        if (!shouldPrintError(e)) return
        val stackTrace = "Exception error: ${AndroidLog.getStackTraceString(e)}"
        error(stackTrace)
    }

    @JvmStatic
    fun printStackTrace(msg: String?, e: Exception?) {
        if (!shouldPrintError(e)) return
        val stackTrace = "Throwable error: ${AndroidLog.getStackTraceString(e)}"
        error(msg ?: "null", stackTrace)
    }

    @JvmStatic
    fun printStackTrace(TAG: String, msg: String?, e: Exception?) {
        if (!shouldPrintError(e)) return
        val stackTrace = "[$TAG] Throwable error: ${AndroidLog.getStackTraceString(e)}"
        error(msg ?: "null", stackTrace)
    }

    /**
     * 清除错误计数缓存（可在任务重新开始时调用）
     */
    @JvmStatic
    fun clearErrorCount() {
        errorCountMap.clear()
    }

    @JvmStatic
    fun printStack(TAG: String) {
        val now = System.currentTimeMillis()
        val last = lastPrintStackAtMs[TAG]
        if (last != null && now - last < PRINT_STACK_INTERVAL_MS) return
        lastPrintStackAtMs[TAG] = now
        val stackTrace = "stack: ${AndroidLog.getStackTraceString(Exception("获取当前堆栈$TAG:"))}"
        capture(stackTrace)
    }

    /**
     * 设置当前用户ID到MDC（Mapped Diagnostic Context）
     * 用于在日志中显示用户标识
     *
     * @param userId 用户ID，传null则清除
     */
    @JvmStatic
    fun setCurrentUser(userId: String?) {
        if (userId != null && userId.length >= 4) {
            // 只显示用户ID的后4位，保护隐私
            val shortId = userId.substring(userId.length - 4)
            MDC.put("userId", shortId)
            // 保存到ThreadLocal，以便子线程继承
            currentUserId.set(shortId)
        } else {
            MDC.remove("userId")
            currentUserId.remove()
        }
    }
    
    // 用于在子线程中恢复MDC
    private val currentUserId = ThreadLocal<String>()

    /**
     * 清除当前用户ID
     */
    @JvmStatic
    fun clearCurrentUser() {
        MDC.remove("userId")
    }
}
