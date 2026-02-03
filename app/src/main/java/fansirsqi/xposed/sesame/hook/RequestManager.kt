package fansirsqi.xposed.sesame.hook

import android.Manifest
import androidx.annotation.RequiresPermission
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.util.Log
import java.util.concurrent.Callable
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.concurrent.atomic.AtomicLong

/**
 * @author Byseven
 * @date 2025/1/6
 * @apiNote
 */
@Suppress("TooManyFunctions")
object RequestManager {
    private val lastEmptyResponseLogAtMs = ConcurrentHashMap<String, Long>()
    private val lastRpcBridgeNullLogAtMs = AtomicLong(0)
    private val lastRpcBlockedLogAtMs = AtomicLong(0)

    private val inFlight = ConcurrentHashMap<String, FutureTask<String>>()

    private const val EMPTY_RESPONSE_LOG_INTERVAL_MS: Long = 5_000L
    private const val RPC_BRIDGE_NULL_LOG_INTERVAL_MS: Long = 5_000L
    private const val RPC_BLOCKED_LOG_INTERVAL_MS: Long = 5_000L

    private fun shouldLogEmptyResponse(method: String?): Boolean {
        val key = method ?: "unknown"
        val now = System.currentTimeMillis()
        val last = lastEmptyResponseLogAtMs[key]
        return if (last == null || now - last >= EMPTY_RESPONSE_LOG_INTERVAL_MS) {
            lastEmptyResponseLogAtMs[key] = now
            true
        } else {
            false
        }
    }

    private fun shouldLogRpcBridgeNull(): Boolean {
        val now = System.currentTimeMillis()
        val last = lastRpcBridgeNullLogAtMs.get()
        return if (last == 0L || now - last >= RPC_BRIDGE_NULL_LOG_INTERVAL_MS) {
            lastRpcBridgeNullLogAtMs.set(now)
            true
        } else {
            false
        }
    }

    private fun shouldLogRpcBlocked(): Boolean {
        val now = System.currentTimeMillis()
        val last = lastRpcBlockedLogAtMs.get()
        return if (last == 0L || now - last >= RPC_BLOCKED_LOG_INTERVAL_MS) {
            lastRpcBlockedLogAtMs.set(now)
            true
        } else {
            false
        }
    }

    private fun checkResult(result: String?, method: String?): String {
        // 处理 null 返回值，避免 NullPointerException
        if (result == null) {
            if (shouldLogEmptyResponse(method)) {
                Log.runtime("RequestManager", "RPC 返回 null: $method")
            }
            return ""
        }
        // 检查是否为空字符串
        if (result.trim { it <= ' ' }.isEmpty()) {
            if (shouldLogEmptyResponse(method)) {
                Log.runtime("RequestManager", "RPC 返回空字符串: $method")
            }
            return ""
        }
        return result
    }

    private fun shouldUseRpcCache(method: String?): Boolean {
        val m = method?.lowercase() ?: return false
        val allow = m.contains("query") || m.contains("list") || m.contains("get")
        if (!allow) return false

        val deny = m.contains("send") ||
            m.contains("finish") ||
            m.contains("receive") ||
            m.contains("draw") ||
            m.contains("exchange") ||
            m.contains("apply") ||
            m.contains("submit") ||
            m.contains("sign") ||
            m.contains("use")
        return !deny
    }

    private fun generateKey(method: String?, data: String?): String {
        val dataHash = data?.hashCode() ?: 0
        return "${method}_${dataHash}"
    }

    private fun requestWithInFlight(key: String, supplier: () -> String): String {
        val task = FutureTask(Callable { supplier() })
        val existing = inFlight.putIfAbsent(key, task)
        val toWait = existing ?: task
        if (existing == null) {
            try {
                task.run()
            } finally {
                inFlight.remove(key, task)
            }
        }

        return try {
            toWait.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            ""
        } catch (e: ExecutionException) {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getRpcBridge(): RpcBridge? {
        if (ApplicationHookConstants.shouldBlockRpc()) {
            if (shouldLogRpcBlocked()) {
                val untilMs = ApplicationHookConstants.offlineUntilMs
                val remainMs = if (untilMs > 0L) (untilMs - System.currentTimeMillis()).coerceAtLeast(0L) else -1L
                val reason = ApplicationHookConstants.offlineReason
                Log.runtime("RequestManager", "RPC 被离线冷却阻断 remainMs=$remainMs untilMs=$untilMs reason=${reason ?: "null"}")
            }
            return null
        }

        val rpcBridge = ApplicationHookConstants.rpcBridge
        if (rpcBridge == null) {
            if (shouldLogRpcBridgeNull()) {
                Log.runtime("RequestManager", "RpcBridge 为空，跳过本次请求")
            }
            return null
        }
        return rpcBridge
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity): String {
        val method = rpcEntity.requestMethod
        val data = rpcEntity.requestData
        val cacheKeyData = if (rpcEntity.requestRelation.isNullOrEmpty()) {
            data
        } else {
            (data ?: "") + "\u0001rel=" + rpcEntity.requestRelation
        }

        if (shouldUseRpcCache(method)) {
            val cached = fansirsqi.xposed.sesame.util.RpcCache.get(method, cacheKeyData)
            if (cached != null) return cached

            val key = generateKey(method, cacheKeyData)
            return requestWithInFlight(key) {
                val rpcBridge = getRpcBridge() ?: return@requestWithInFlight ""
                val result = rpcBridge.requestString(rpcEntity, RpcBridge.DEFAULT_TRY_COUNT, RpcBridge.DEFAULT_RETRY_INTERVAL)
                val checkedResult = checkResult(result, method)
                if (checkedResult.isNotEmpty()) {
                    fansirsqi.xposed.sesame.util.RpcCache.put(method, cacheKeyData, checkedResult)
                }
                checkedResult
            }
        }

        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, RpcBridge.DEFAULT_TRY_COUNT, RpcBridge.DEFAULT_RETRY_INTERVAL)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        return checkResult(result, rpcEntity.requestMethod)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?): String {
        if (shouldUseRpcCache(method)) {
            val cached = fansirsqi.xposed.sesame.util.RpcCache.get(method, data)
            if (cached != null) return cached

            val key = generateKey(method, data)
            return requestWithInFlight(key) {
                val rpcBridge = getRpcBridge() ?: return@requestWithInFlight ""
                val result = rpcBridge.requestString(method, data)
                val checkedResult = checkResult(result, method)
                if (checkedResult.isNotEmpty()) {
                    fansirsqi.xposed.sesame.util.RpcCache.put(method, data, checkedResult)
                }
                checkedResult
            }
        }

        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, relation: String?): String {
        val cacheKeyData = if (relation.isNullOrEmpty()) {
            data
        } else {
            (data ?: "") + "\u0001rel=" + relation
        }

        if (shouldUseRpcCache(method)) {
            val cached = fansirsqi.xposed.sesame.util.RpcCache.get(method, cacheKeyData)
            if (cached != null) return cached

            val key = generateKey(method, cacheKeyData)
            return requestWithInFlight(key) {
                val rpcBridge = getRpcBridge() ?: return@requestWithInFlight ""
                val result = rpcBridge.requestString(method, data, relation)
                val checkedResult = checkResult(result, method)
                if (checkedResult.isNotEmpty()) {
                    fansirsqi.xposed.sesame.util.RpcCache.put(method, cacheKeyData, checkedResult)
                }
                checkedResult
            }
        }

        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, appName: String?, methodName: String?, facadeName: String?): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, appName, methodName, facadeName)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, tryCount, retryInterval)
        return checkResult(result, method)
    }

    fun requestString(method: String?, data: String?, relation: String?, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data, relation, tryCount, retryInterval)
        return checkResult(result, method)
    }

    @JvmStatic
    fun requestObject(rpcEntity: RpcEntity?, tryCount: Int, retryInterval: Int) {
        val rpcBridge = getRpcBridge() ?: return
        if (rpcEntity != null) {
            rpcBridge.requestObject(rpcEntity, tryCount, retryInterval)
        }
    }

}
