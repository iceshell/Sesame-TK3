package fansirsqi.xposed.sesame.hook

import android.Manifest
import androidx.annotation.RequiresPermission
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.util.Log
import java.util.concurrent.ConcurrentHashMap
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

    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    private fun getRpcBridge(): RpcBridge? {
        if (ApplicationHookConstants.shouldBlockRpc()) {
            if (shouldLogRpcBlocked()) {
                val untilMs = ApplicationHookConstants.offlineUntilMs
                val remainMs = if (untilMs > 0L) (untilMs - System.currentTimeMillis()).coerceAtLeast(0L) else -1L
                Log.runtime("RequestManager", "RPC 被离线冷却阻断 remainMs=$remainMs untilMs=$untilMs")
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
        // 尝试从缓存获取
        val cached = fansirsqi.xposed.sesame.util.RpcCache.get(rpcEntity.methodName, rpcEntity.requestData)
        if (cached != null) return cached
        
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, RpcBridge.DEFAULT_TRY_COUNT, RpcBridge.DEFAULT_RETRY_INTERVAL)
        val checkedResult = checkResult(result, rpcEntity.methodName)
        
        // 缓存成功的响应
        if (checkedResult.isNotEmpty()) {
            fansirsqi.xposed.sesame.util.RpcCache.put(rpcEntity.methodName, rpcEntity.requestData, checkedResult)
        }
        return checkedResult
    }

    @JvmStatic
    fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String {
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(rpcEntity, tryCount, retryInterval)
        return checkResult(result, rpcEntity.methodName)
    }

    @JvmStatic
    fun requestString(method: String?, data: String?): String {
        // 尝试从缓存获取
        val cached = fansirsqi.xposed.sesame.util.RpcCache.get(method, data)
        if (cached != null) return cached
        
        val rpcBridge = getRpcBridge() ?: return ""
        val result = rpcBridge.requestString(method, data)
        val checkedResult = checkResult(result, method)
        
        // 缓存成功的响应
        if (checkedResult.isNotEmpty()) {
            fansirsqi.xposed.sesame.util.RpcCache.put(method, data, checkedResult)
        }
        return checkedResult
    }

    @JvmStatic
    fun requestString(method: String?, data: String?, relation: String?): String {
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
