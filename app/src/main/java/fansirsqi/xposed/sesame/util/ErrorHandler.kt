package fansirsqi.xposed.sesame.util

/**
 * 统一错误处理工具类
 * 
 * 提供安全的错误处理模式，减少重复代码，提高代码健壮性
 * 
 * @author AI Code Quality Assistant
 * @since 2025-10-27
 */
object ErrorHandler {
    
    /**
     * 安全执行代码块，捕获异常并返回结果或fallback值
     * 
     * @param T 返回值类型
     * @param tag 日志标签
     * @param errorMsg 错误消息前缀
     * @param fallback 失败时的fallback值
     * @param block 要执行的代码块
     * @return 执行结果或fallback值
     * 
     * 示例:
     * ```kotlin
     * val result = ErrorHandler.safely("TAG", "操作失败", fallback = false) {
     *     // 可能抛出异常的操作
     *     performOperation()
     * }
     * ```
     */
    inline fun <T> safely(
        tag: String,
        errorMsg: String = "操作失败",
        fallback: T? = null,
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            Log.error(tag, "$errorMsg: ${e.message}")
            Log.printStackTrace(tag, e)
            fallback
        }
    }
    
    /**
     * 安全执行无返回值的代码块，捕获并记录异常
     * 
     * @param tag 日志标签
     * @param errorMsg 错误消息前缀
     * @param block 要执行的代码块
     * 
     * 示例:
     * ```kotlin
     * ErrorHandler.safelyRun("TAG", "通知创建失败") {
     *     createNotification()
     * }
     * ```
     */
    inline fun safelyRun(
        tag: String,
        errorMsg: String = "操作失败",
        block: () -> Unit
    ) {
        try {
            block()
        } catch (e: Exception) {
            Log.error(tag, "$errorMsg: ${e.message}")
            Log.printStackTrace(tag, e)
        }
    }
    
    /**
     * 安全执行代码块，针对特定异常类型进行处理
     * 
     * @param T 返回值类型
     * @param E 异常类型
     * @param tag 日志标签
     * @param errorMsg 错误消息前缀
     * @param fallback fallback值
     * @param onError 异常处理回调
     * @param block 要执行的代码块
     * @return 执行结果或fallback值
     */
    inline fun <T, reified E : Exception> safelyWithHandler(
        tag: String,
        errorMsg: String = "操作失败",
        fallback: T? = null,
        crossinline onError: (E) -> Unit = {},
        block: () -> T
    ): T? {
        return try {
            block()
        } catch (e: Exception) {
            when (e) {
                is E -> {
                    Log.error(tag, "$errorMsg: ${e.message}")
                    onError(e)
                }
                else -> {
                    Log.error(tag, "$errorMsg (未处理的异常): ${e.message}")
                    Log.printStackTrace(tag, e)
                }
            }
            fallback
        }
    }
    
    /**
     * 带重试机制的安全执行
     * 
     * @param T 返回值类型
     * @param tag 日志标签
     * @param maxRetries 最大重试次数
     * @param retryDelay 重试延迟(毫秒)
     * @param errorMsg 错误消息前缀
     * @param fallback fallback值
     * @param block 要执行的代码块
     * @return 执行结果或fallback值
     * 
     * 示例:
     * ```kotlin
     * val data = ErrorHandler.safelyWithRetry("TAG", maxRetries = 3, retryDelay = 1000) {
     *     fetchDataFromNetwork()
     * }
     * ```
     */
    inline fun <T> safelyWithRetry(
        tag: String,
        maxRetries: Int = 3,
        retryDelay: Long = 1000,
        errorMsg: String = "操作失败",
        fallback: T? = null,
        block: () -> T
    ): T? {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    Log.runtime(tag, "$errorMsg，第${attempt + 1}次重试...")
                    Thread.sleep(retryDelay)
                }
            }
        }
        Log.error(tag, "$errorMsg，已重试${maxRetries}次: ${lastException?.message}")
        lastException?.let { Log.printStackTrace(tag, it) }
        return fallback
    }
    
    /**
     * 检查参数有效性，无效时抛出IllegalArgumentException
     * 
     * @param condition 条件
     * @param lazyMessage 错误消息生成函数
     * @throws IllegalArgumentException 条件不满足时
     * 
     * 示例:
     * ```kotlin
     * ErrorHandler.require(userId != null) { "userId不能为空" }
     * ErrorHandler.require(count > 0) { "count必须大于0，当前值: $count" }
     * ```
     */
    inline fun require(condition: Boolean, lazyMessage: () -> String) {
        if (!condition) {
            throw IllegalArgumentException(lazyMessage())
        }
    }
    
    /**
     * 检查状态有效性，无效时抛出IllegalStateException
     * 
     * @param condition 条件
     * @param lazyMessage 错误消息生成函数
     * @throws IllegalStateException 条件不满足时
     */
    inline fun check(condition: Boolean, lazyMessage: () -> String) {
        if (!condition) {
            throw IllegalStateException(lazyMessage())
        }
    }
}
