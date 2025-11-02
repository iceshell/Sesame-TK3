package fansirsqi.xposed.sesame.util

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * 性能监控工具
 * 
 * 提供方法执行时间监控、内存监控、统计功能
 * 
 * @author AI Code Quality Assistant
 * @since 2025-11-02 (rc161)
 */
object PerformanceMonitor {
    
    const val TAG = "PerformanceMonitor"
    
    // 方法执行统计
    data class MethodStats(
        var callCount: AtomicLong = AtomicLong(0),
        var totalTime: AtomicLong = AtomicLong(0),
        var minTime: AtomicLong = AtomicLong(Long.MAX_VALUE),
        var maxTime: AtomicLong = AtomicLong(0),
        var errorCount: AtomicLong = AtomicLong(0)
    ) {
        fun avgTime(): Long = if (callCount.get() > 0) {
            totalTime.get() / callCount.get()
        } else 0
    }
    
    val methodStats = ConcurrentHashMap<String, MethodStats>()
    
    // 启用/禁用监控
    @Volatile
    var enabled: Boolean = true
    
    // 慢方法阈值（毫秒）
    @Volatile
    var slowMethodThreshold: Long = 1000
    
    /**
     * 监控方法执行时间
     * 
     * @param methodName 方法名
     * @param block 要执行的代码块
     * @return 执行结果
     */
    inline fun <T> monitor(methodName: String, block: () -> T): T {
        if (!enabled) {
            return block()
        }
        
        val startTime = System.currentTimeMillis()
        var hasError = false
        
        return try {
            block()
        } catch (e: Exception) {
            hasError = true
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordMethodCall(methodName, duration, hasError)
            
            // 记录慢方法
            if (duration > slowMethodThreshold) {
                Log.runtime(TAG, "⚠️ 慢方法检测: $methodName 耗时 ${duration}ms")
            }
        }
    }
    
    /**
     * 监控协程方法执行
     * 
     * @param methodName 方法名
     * @param block 协程代码块
     * @return 执行结果
     */
    suspend inline fun <T> monitorSuspend(methodName: String, crossinline block: suspend () -> T): T {
        if (!enabled) {
            return block()
        }
        
        val startTime = System.currentTimeMillis()
        var hasError = false
        
        return try {
            block()
        } catch (e: Exception) {
            hasError = true
            throw e
        } finally {
            val duration = System.currentTimeMillis() - startTime
            recordMethodCall(methodName, duration, hasError)
            
            if (duration > slowMethodThreshold) {
                Log.runtime(TAG, "⚠️ 慢协程方法检测: $methodName 耗时 ${duration}ms")
            }
        }
    }
    
    /**
     * 记录方法调用统计
     */
    fun recordMethodCall(methodName: String, duration: Long, hasError: Boolean) {
        val stats = methodStats.getOrPut(methodName) { MethodStats() }
        
        stats.callCount.incrementAndGet()
        stats.totalTime.addAndGet(duration)
        
        // 更新最小时间
        var currentMin = stats.minTime.get()
        while (duration < currentMin) {
            if (stats.minTime.compareAndSet(currentMin, duration)) {
                break
            }
            currentMin = stats.minTime.get()
        }
        
        // 更新最大时间
        var currentMax = stats.maxTime.get()
        while (duration > currentMax) {
            if (stats.maxTime.compareAndSet(currentMax, duration)) {
                break
            }
            currentMax = stats.maxTime.get()
        }
        
        if (hasError) {
            stats.errorCount.incrementAndGet()
        }
    }
    
    /**
     * 获取方法统计信息
     */
    fun getMethodStats(methodName: String): String? {
        val stats = methodStats[methodName] ?: return null
        
        return buildString {
            append("方法: $methodName\n")
            append("调用次数: ${stats.callCount.get()}\n")
            append("总耗时: ${stats.totalTime.get()}ms\n")
            append("平均耗时: ${stats.avgTime()}ms\n")
            append("最小耗时: ${stats.minTime.get()}ms\n")
            append("最大耗时: ${stats.maxTime.get()}ms\n")
            append("错误次数: ${stats.errorCount.get()}")
        }
    }
    
    /**
     * 获取所有方法统计报告
     */
    fun generateReport(): String {
        if (methodStats.isEmpty()) {
            return "暂无性能数据"
        }
        
        val sortedStats = methodStats.entries
            .sortedByDescending { it.value.totalTime.get() }
            .take(20) // 只取前20个最耗时的方法
        
        return buildString {
            appendLine("========== 性能监控报告 ==========")
            appendLine("监控状态: ${if (enabled) "启用" else "禁用"}")
            appendLine("慢方法阈值: ${slowMethodThreshold}ms")
            appendLine("总方法数: ${methodStats.size}")
            appendLine()
            appendLine("TOP 20 耗时方法:")
            appendLine()
            
            sortedStats.forEachIndexed { index, (methodName, stats) ->
                appendLine("${index + 1}. $methodName")
                appendLine("   调用次数: ${stats.callCount.get()}")
                appendLine("   总耗时: ${stats.totalTime.get()}ms")
                appendLine("   平均耗时: ${stats.avgTime()}ms")
                appendLine("   最小/最大: ${stats.minTime.get()}ms / ${stats.maxTime.get()}ms")
                appendLine("   错误次数: ${stats.errorCount.get()}")
                appendLine()
            }
            
            appendLine("========== 报告结束 ==========")
        }
    }
    
    /**
     * 清空统计数据
     */
    fun clearStats() {
        methodStats.clear()
        Log.record(TAG, "性能统计数据已清空")
    }
    
    /**
     * 获取内存使用情况
     */
    fun getMemoryInfo(): String {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory() / (1024 * 1024)
        val freeMemory = runtime.freeMemory() / (1024 * 1024)
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory() / (1024 * 1024)
        
        return buildString {
            appendLine("========== 内存使用情况 ==========")
            appendLine("已用内存: ${usedMemory}MB")
            appendLine("空闲内存: ${freeMemory}MB")
            appendLine("总内存: ${totalMemory}MB")
            appendLine("最大内存: ${maxMemory}MB")
            appendLine("内存使用率: ${(usedMemory * 100 / maxMemory)}%")
        }
    }
    
    /**
     * 记录当前性能快照到日志
     */
    fun logSnapshot() {
        Log.record(TAG, getMemoryInfo())
        Log.record(TAG, generateReport())
    }
    
    /**
     * 监控代码块并自动记录到日志
     */
    inline fun <T> monitorAndLog(methodName: String, block: () -> T): T {
        return monitor(methodName) {
            try {
                block()
            } catch (e: Exception) {
                Log.error(TAG, "$methodName 执行异常: ${e.message}")
                throw e
            }
        }.also {
            // 如果方法执行超过阈值，自动记录详细信息
            methodStats[methodName]?.let { stats ->
                if (stats.maxTime.get() > slowMethodThreshold) {
                    Log.runtime(TAG, getMethodStats(methodName) ?: "")
                }
            }
        }
    }
}

/**
 * 性能监控DSL扩展
 */
inline fun <T> monitored(methodName: String, block: () -> T): T {
    return PerformanceMonitor.monitor(methodName, block)
}

suspend inline fun <T> monitoredSuspend(methodName: String, crossinline block: suspend () -> T): T {
    return PerformanceMonitor.monitorSuspend(methodName, block)
}
