package fansirsqi.xposed.sesame.util

import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.RequestManager
import kotlinx.coroutines.delay

/**
 * RPC 重试策略
 * 提供智能重试机制，包括指数退避等策略
 * 
 * @author Cascade AI
 * @date 2024-11-02
 */
object RpcRetryStrategy {
    
    private const val TAG = "RpcRetryStrategy"
    
    /**
     * 使用指数退避策略请求
     * @param rpcEntity RPC实体
     * @param maxRetries 最大重试次数，默认3次
     * @param initialDelay 初始延迟（毫秒），默认500ms
     * @param maxDelay 最大延迟（毫秒），默认8000ms
     * @param factor 退避因子，默认2.0
     * @return RPC响应结果
     */
    suspend fun requestWithExponentialBackoff(
        rpcEntity: RpcEntity,
        maxRetries: Int = 3,
        initialDelay: Long = 500,
        maxDelay: Long = 8000,
        factor: Double = 2.0
    ): String {
        var currentDelay = initialDelay
        var lastError: String? = null
        
        repeat(maxRetries) { attempt ->
            try {
                val result = RequestManager.requestString(rpcEntity)
                
                // 如果成功返回
                if (result.isNotEmpty()) {
                    if (attempt > 0) {
                        Log.runtime(TAG, "重试成功: ${rpcEntity.methodName}, 尝试次数: ${attempt + 1}")
                    }
                    return result
                }
                
                lastError = "RPC返回空响应"
            } catch (e: Exception) {
                lastError = e.message ?: "未知错误"
                Log.printStackTrace(TAG, "RPC请求异常: ${rpcEntity.methodName}", e)
            }
            
            // 如果不是最后一次尝试，等待后重试
            if (attempt < maxRetries - 1) {
                Log.runtime(TAG, "重试 ${attempt + 1}/${maxRetries}: ${rpcEntity.methodName}, 等待 ${currentDelay}ms")
                delay(currentDelay)
                
                // 计算下次延迟时间（指数增长，但不超过最大值）
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        
        Log.record(TAG, "重试失败: ${rpcEntity.methodName}, 已尝试 $maxRetries 次, 最后错误: $lastError")
        return ""
    }
    
    /**
     * 使用线性退避策略请求
     * @param rpcEntity RPC实体
     * @param maxRetries 最大重试次数，默认3次
     * @param delay 每次重试延迟（毫秒），默认1000ms
     * @return RPC响应结果
     */
    suspend fun requestWithLinearBackoff(
        rpcEntity: RpcEntity,
        maxRetries: Int = 3,
        delay: Long = 1000
    ): String {
        repeat(maxRetries) { attempt ->
            try {
                val result = RequestManager.requestString(rpcEntity)
                
                if (result.isNotEmpty()) {
                    if (attempt > 0) {
                        Log.runtime(TAG, "重试成功: ${rpcEntity.methodName}, 尝试次数: ${attempt + 1}")
                    }
                    return result
                }
            } catch (e: Exception) {
                Log.printStackTrace(TAG, "RPC请求异常: ${rpcEntity.methodName}", e)
            }
            
            // 如果不是最后一次尝试，等待固定时间
            if (attempt < maxRetries - 1) {
                Log.runtime(TAG, "重试 ${attempt + 1}/${maxRetries}: ${rpcEntity.methodName}, 等待 ${delay}ms")
                delay(delay)
            }
        }
        
        Log.record(TAG, "重试失败: ${rpcEntity.methodName}, 已尝试 $maxRetries 次")
        return ""
    }
    
    /**
     * 根据错误类型选择重试策略
     * @param rpcEntity RPC实体
     * @param errorHandler 错误处理器，返回true表示应该重试
     * @return RPC响应结果
     */
    suspend fun requestWithSmartRetry(
        rpcEntity: RpcEntity,
        maxRetries: Int = 3,
        errorHandler: (String) -> Boolean = { true }
    ): String {
        repeat(maxRetries) { attempt ->
            try {
                val result = RequestManager.requestString(rpcEntity)
                
                if (result.isNotEmpty()) {
                    if (attempt > 0) {
                        Log.runtime(TAG, "智能重试成功: ${rpcEntity.methodName}, 尝试次数: ${attempt + 1}")
                    }
                    return result
                }
                
                // 空响应，检查是否应该重试
                if (!errorHandler("空响应")) {
                    Log.runtime(TAG, "错误不可重试，放弃: ${rpcEntity.methodName}")
                    return ""
                }
            } catch (e: Exception) {
                val error = e.message ?: "未知错误"
                Log.printStackTrace(TAG, "RPC请求异常: ${rpcEntity.methodName}", e)
                
                // 检查是否应该重试此错误
                if (!errorHandler(error)) {
                    Log.runtime(TAG, "错误不可重试，放弃: ${rpcEntity.methodName}, 错误: $error")
                    return ""
                }
            }
            
            // 根据尝试次数动态调整延迟
            if (attempt < maxRetries - 1) {
                val delay = when (attempt) {
                    0 -> 500L
                    1 -> 1000L
                    else -> 2000L
                }
                Log.runtime(TAG, "智能重试 ${attempt + 1}/${maxRetries}: ${rpcEntity.methodName}, 等待 ${delay}ms")
                delay(delay)
            }
        }
        
        Log.record(TAG, "智能重试失败: ${rpcEntity.methodName}, 已尝试 $maxRetries 次")
        return ""
    }
}
