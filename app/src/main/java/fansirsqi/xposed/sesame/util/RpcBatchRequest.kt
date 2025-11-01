package fansirsqi.xposed.sesame.util

import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.RequestManager
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * RPC 批量请求工具
 * 支持并发执行多个RPC请求，提升执行效率
 * 
 * @author Cascade AI
 * @date 2024-11-02
 */
object RpcBatchRequest {
    
    private const val TAG = "RpcBatchRequest"
    
    /**
     * 批量执行RPC请求（并发）
     * @param entities RPC实体列表
     * @return 响应结果列表，顺序与输入一致
     */
    suspend fun batchRequest(entities: List<RpcEntity>): List<String> = coroutineScope {
        if (entities.isEmpty()) return@coroutineScope emptyList()
        
        Log.runtime(TAG, "开始批量请求: ${entities.size}个")
        val startTime = System.currentTimeMillis()
        
        val results = entities.map { entity ->
            async(Dispatchers.IO) {
                try {
                    RequestManager.requestString(entity)
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "批量请求失败: ${entity.methodName}", e)
                    ""
                }
            }
        }.awaitAll()
        
        val duration = System.currentTimeMillis() - startTime
        Log.runtime(TAG, "批量请求完成: ${entities.size}个, 耗时: ${duration}ms")
        
        results
    }
    
    /**
     * 批量执行RPC请求（带限流）
     * @param entities RPC实体列表
     * @param concurrency 最大并发数，默认5
     * @return 响应结果列表
     */
    suspend fun batchRequestWithLimit(
        entities: List<RpcEntity>,
        concurrency: Int = 5
    ): List<String> = coroutineScope {
        if (entities.isEmpty()) return@coroutineScope emptyList()
        
        Log.runtime(TAG, "开始限流批量请求: ${entities.size}个, 并发数: $concurrency")
        val startTime = System.currentTimeMillis()
        
        // 使用信号量限制并发数
        val semaphore = Semaphore(concurrency)
        
        val results = entities.map { entity ->
            async(Dispatchers.IO) {
                semaphore.withPermit {
                    try {
                        RequestManager.requestString(entity)
                    } catch (e: Exception) {
                        Log.printStackTrace(TAG, "批量请求失败: ${entity.methodName}", e)
                        ""
                    }
                }
            }
        }.awaitAll()
        
        val duration = System.currentTimeMillis() - startTime
        Log.runtime(TAG, "限流批量请求完成: ${entities.size}个, 耗时: ${duration}ms")
        
        results
    }
    
    /**
     * 批量执行简单RPC请求
     * @param requests 请求列表，每项包含 (method, data)
     * @return 响应结果列表
     */
    suspend fun batchRequestSimple(
        requests: List<Pair<String, String>>
    ): List<String> = coroutineScope {
        if (requests.isEmpty()) return@coroutineScope emptyList()
        
        Log.runtime(TAG, "开始简单批量请求: ${requests.size}个")
        val startTime = System.currentTimeMillis()
        
        val results = requests.map { (method, data) ->
            async(Dispatchers.IO) {
                try {
                    RequestManager.requestString(method, data)
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "简单批量请求失败: $method", e)
                    ""
                }
            }
        }.awaitAll()
        
        val duration = System.currentTimeMillis() - startTime
        Log.runtime(TAG, "简单批量请求完成: ${requests.size}个, 耗时: ${duration}ms")
        
        results
    }
}
