package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.Log
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/**
 * 能量收取优化器
 * 
 * 功能：
 * 1. 时间段动态间隔
 * 2. 自适应批量大小
 * 3. 收取统计
 * 4. 智能预测引擎
 * 
 * @author 芝麻粒优化版
 * @date 2025-10-19
 */
object EnergyCollectionOptimizer {
    private const val TAG = "EnergyCollectionOptimizer"
    
    // ==================== 1. 时间段动态间隔 ====================
    
    /**
     * 根据时间段计算收取间隔
     * 
     * 时间段规则：
     * - 7-8点（早高峰）：2分钟
     * - 12-13点（午休）：2分钟
     * - 17-18点（晚高峰）：2分钟
     * - 0-1点（午夜能量成熟）：2分钟
     * - 2-6点（深夜）：30分钟
     * - 其他时间：5分钟
     */
    @JvmStatic
    fun calculateDynamicInterval(): Long {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        
        return when (hour) {
            in 7..8 -> 2 * 60 * 1000L      // 早高峰：2分钟
            in 12..13 -> 2 * 60 * 1000L    // 午休：2分钟
            in 17..18 -> 2 * 60 * 1000L    // 晚高峰：2分钟
            0, 1 -> 2 * 60 * 1000L         // 午夜：2分钟（0-1点能量成熟）
            in 2..6 -> 30 * 60 * 1000L     // 深夜：30分钟
            else -> 5 * 60 * 1000L         // 其他：5分钟
        }
    }
    
    /**
     * 获取下次检查时间（用于日志显示）
     */
    @JvmStatic
    fun getNextCheckTimeDescription(): String {
        val interval = calculateDynamicInterval()
        val nextTime = System.currentTimeMillis() + interval
        val calendar = Calendar.getInstance().apply { timeInMillis = nextTime }
        
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val reason = when (hour) {
            in 7..8 -> "早高峰"
            in 12..13 -> "午休"
            in 17..18 -> "晚高峰"
            0, 1 -> "午夜成熟"
            in 2..6 -> "深夜"
            else -> "常规"
        }
        
        return String.format(
            "%02d:%02d:%02d [%s期，间隔%d分钟]",
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            calendar.get(Calendar.SECOND),
            reason,
            interval / 60000
        )
    }
    
    // ==================== 2. 自适应批量大小 ====================
    
    private var lastRpcLatency = 0L
    private val latencyHistory = mutableListOf<Long>()
    
    enum class NetworkQuality {
        EXCELLENT,  // 优秀 (延迟<200ms)
        GOOD,       // 良好 (200-500ms)
        POOR,       // 较差 (>500ms)
        UNKNOWN     // 未知
    }
    
    /**
     * 记录RPC延迟
     */
    @JvmStatic
    fun recordRpcLatency(latency: Long) {
        lastRpcLatency = latency
        latencyHistory.add(latency)
        
        // 只保留最近10次记录
        if (latencyHistory.size > 10) {
            latencyHistory.removeAt(0)
        }
    }
    
    /**
     * 检测网络质量
     */
    @JvmStatic
    fun detectNetworkQuality(): NetworkQuality {
        if (latencyHistory.isEmpty()) {
            return NetworkQuality.UNKNOWN
        }
        
        val avgLatency = latencyHistory.average()
        
        return when {
            avgLatency < 200 -> NetworkQuality.EXCELLENT
            avgLatency < 500 -> NetworkQuality.GOOD
            else -> NetworkQuality.POOR
        }
    }
    
    /**
     * 计算最优批量大小
     * 
     * @param totalBubbles 总能量球数量
     * @return 推荐的批量大小
     */
    @JvmStatic
    fun calculateOptimalBatchSize(totalBubbles: Int): Int {
        val quality = detectNetworkQuality()
        
        val baseBatchSize = when (quality) {
            NetworkQuality.EXCELLENT -> 10  // 网络优秀：10个
            NetworkQuality.GOOD -> 6        // 网络良好：6个
            NetworkQuality.POOR -> 3        // 网络较差：3个
            NetworkQuality.UNKNOWN -> 6     // 未知：默认6个
        }
        
        return min(baseBatchSize, totalBubbles)
    }
    
    // ==================== 3. 收取统计 ====================
    
    data class CollectStats(
        var batchCount: Int = 0,           // 批量收取次数
        var singleCount: Int = 0,          // 单个收取次数
        var batchSuccessCount: Int = 0,    // 批量成功次数
        var batchFailCount: Int = 0,       // 批量失败次数
        var totalCollected: Int = 0,       // 总收取能量
        var totalBatchTime: Long = 0,      // 批量总耗时
        var totalSingleTime: Long = 0,     // 单个总耗时
        var lastResetTime: Long = System.currentTimeMillis()
    )
    
    private val stats = CollectStats()
    
    /**
     * 记录批量收取
     */
    @JvmStatic
    fun recordBatchCollect(success: Boolean, duration: Long, energyCollected: Int) {
        stats.batchCount++
        if (success) {
            stats.batchSuccessCount++
        } else {
            stats.batchFailCount++
        }
        stats.totalBatchTime += duration
        stats.totalCollected += energyCollected
    }
    
    /**
     * 记录单个收取
     */
    @JvmStatic
    fun recordSingleCollect(duration: Long, energyCollected: Int) {
        stats.singleCount++
        stats.totalSingleTime += duration
        stats.totalCollected += energyCollected
    }
    
    /**
     * 打印统计信息
     */
    @JvmStatic
    fun printStats() {
        if (stats.batchCount == 0 && stats.singleCount == 0) {
            Log.record(TAG, "📊 暂无收取统计数据")
            return
        }
        
        val runtime = (System.currentTimeMillis() - stats.lastResetTime) / 1000 / 60 // 分钟
        val batchSuccessRate = if (stats.batchCount > 0) {
            stats.batchSuccessCount * 100.0 / stats.batchCount
        } else 0.0
        
        val avgBatchTime = if (stats.batchCount > 0) {
            stats.totalBatchTime / stats.batchCount
        } else 0
        
        val avgSingleTime = if (stats.singleCount > 0) {
            stats.totalSingleTime / stats.singleCount
        } else 0
        
        val networkQuality = detectNetworkQuality()
        val networkDesc = when (networkQuality) {
            NetworkQuality.EXCELLENT -> "优秀"
            NetworkQuality.GOOD -> "良好"
            NetworkQuality.POOR -> "较差"
            NetworkQuality.UNKNOWN -> "未知"
        }
        
        Log.record(TAG, """
            |
            |📊 ========== 能量收取统计报告 ==========
            |⏱️  运行时长: ${runtime}分钟
            |💰 总收取能量: ${stats.totalCollected}g
            |
            |🎯 批量收取:
            |   - 收取次数: ${stats.batchCount}
            |   - 成功次数: ${stats.batchSuccessCount}
            |   - 失败次数: ${stats.batchFailCount}
            |   - 成功率: ${String.format("%.1f", batchSuccessRate)}%
            |   - 平均耗时: ${avgBatchTime}ms
            |
            |🔹 单个收取:
            |   - 收取次数: ${stats.singleCount}
            |   - 平均耗时: ${avgSingleTime}ms
            |
            |🌐 网络质量: $networkDesc
            |📈 平均延迟: ${if (latencyHistory.isNotEmpty()) "${latencyHistory.average().toInt()}ms" else "未知"}
            |==========================================
        """.trimMargin())
    }
    
    /**
     * 重置统计
     */
    @JvmStatic
    fun resetStats() {
        stats.batchCount = 0
        stats.singleCount = 0
        stats.batchSuccessCount = 0
        stats.batchFailCount = 0
        stats.totalCollected = 0
        stats.totalBatchTime = 0
        stats.totalSingleTime = 0
        stats.lastResetTime = System.currentTimeMillis()
        
        Log.record(TAG, "📊 统计数据已重置")
    }
    
    // ==================== 4. 智能预测引擎 ====================
    
    /**
     * 能量成熟时间段
     */
    enum class EnergyMaturityPeriod(val hourRange: IntRange) {
        MORNING(7..8),      // 早晨
        NOON(12..13),       // 午休
        EVENING(17..18),    // 傍晚
        MIDNIGHT(0..1);     // 午夜
        
        companion object {
            fun getCurrentPeriod(): EnergyMaturityPeriod? {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                return values().find { hour in it.hourRange }
            }
            
            fun getNextPeriod(): EnergyMaturityPeriod {
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                return when {
                    hour < 7 -> MORNING
                    hour < 12 -> NOON
                    hour < 17 -> EVENING
                    hour < 24 -> MIDNIGHT
                    else -> MORNING
                }
            }
        }
    }
    
    /**
     * 好友能量记录
     */
    private data class FriendEnergyRecord(
        var lastMorningEnergy: Long = 0,   // 最近一次早晨能量时间
        var lastNoonEnergy: Long = 0,      // 最近一次午休能量时间
        var lastEveningEnergy: Long = 0,   // 最近一次傍晚能量时间
        var lastMidnightEnergy: Long = 0   // 最近一次午夜能量时间
    )
    
    private val friendEnergyHistory = ConcurrentHashMap<String, FriendEnergyRecord>()
    
    /**
     * 记录好友能量收取
     */
    @JvmStatic
    fun recordFriendEnergy(userId: String) {
        val record = friendEnergyHistory.getOrPut(userId) { FriendEnergyRecord() }
        val period = EnergyMaturityPeriod.getCurrentPeriod()
        val now = System.currentTimeMillis()
        
        when (period) {
            EnergyMaturityPeriod.MORNING -> record.lastMorningEnergy = now
            EnergyMaturityPeriod.NOON -> record.lastNoonEnergy = now
            EnergyMaturityPeriod.EVENING -> record.lastEveningEnergy = now
            EnergyMaturityPeriod.MIDNIGHT -> record.lastMidnightEnergy = now
            null -> {} // 不在能量成熟时间段
        }
    }
    
    /**
     * 预测下次能量成熟时间
     */
    @JvmStatic
    fun predictNextEnergyTime(): Long {
        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY)
        val currentMinute = now.get(Calendar.MINUTE)
        
        // 找到下一个能量成熟时间段
        val nextPeriod = when {
            currentHour < 7 -> EnergyMaturityPeriod.MORNING
            currentHour == 7 && currentMinute < 30 -> EnergyMaturityPeriod.MORNING
            currentHour < 12 -> EnergyMaturityPeriod.NOON
            currentHour == 12 && currentMinute < 30 -> EnergyMaturityPeriod.NOON
            currentHour < 17 -> EnergyMaturityPeriod.EVENING
            currentHour == 17 && currentMinute < 30 -> EnergyMaturityPeriod.EVENING
            currentHour < 24 -> EnergyMaturityPeriod.MIDNIGHT
            else -> EnergyMaturityPeriod.MORNING
        }
        
        // 计算到该时间段开始的时间
        val targetCalendar = Calendar.getInstance()
        val targetHour = nextPeriod.hourRange.first
        
        if (currentHour < targetHour) {
            // 今天
            targetCalendar.set(Calendar.HOUR_OF_DAY, targetHour)
            targetCalendar.set(Calendar.MINUTE, 0)
            targetCalendar.set(Calendar.SECOND, 0)
        } else {
            // 明天
            targetCalendar.add(Calendar.DAY_OF_MONTH, 1)
            targetCalendar.set(Calendar.HOUR_OF_DAY, targetHour)
            targetCalendar.set(Calendar.MINUTE, 0)
            targetCalendar.set(Calendar.SECOND, 0)
        }
        
        return targetCalendar.timeInMillis
    }
    
    /**
     * 判断当前是否在能量成熟高峰期
     */
    @JvmStatic
    fun isInPeakPeriod(): Boolean {
        return EnergyMaturityPeriod.getCurrentPeriod() != null
    }
    
    /**
     * 获取统计快照（用于外部访问）
     */
    @JvmStatic
    fun getStats(): Map<String, Any> {
        return mapOf(
            "batchCount" to stats.batchCount,
            "singleCount" to stats.singleCount,
            "totalCollected" to stats.totalCollected,
            "batchSuccessRate" to if (stats.batchCount > 0) {
                stats.batchSuccessCount * 100.0 / stats.batchCount
            } else 0.0,
            "networkQuality" to detectNetworkQuality().name
        )
    }
}
