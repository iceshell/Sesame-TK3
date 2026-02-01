package fansirsqi.xposed.sesame.hook.rpc.bridge

import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private const val REQUEST_LOG_LIMIT = 1024
private const val RESPONSE_LOG_LIMIT = 4096

private fun truncateForLog(value: String?, limit: Int): String {
    if (value == null) return ""
    if (value.length <= limit) return value
    return value.substring(0, limit) + "..."
}

private fun getErrorValueAsString(obj: Any, key: String): String {
    return runCatching {
        XposedHelpers.callMethod(obj, "getString", key) as? String
    }.getOrNull()?.takeIf { it.isNotBlank() }
        ?: runCatching {
            XposedHelpers.callMethod(obj, "get", key)?.toString()
        }.getOrNull().orEmpty()
}

/**
 * 新版RPC接口，支持最低支付宝版本v10.3.96.8100
 * 记录RPC抓包，支持最低支付宝版本v10.3.96.8100
 */
class NewRpcBridge : RpcBridge {
    private var loader: ClassLoader? = null
    private var newRpcInstance: Any? = null
    private var parseObjectMethod: Method? = null
    private var bridgeCallbackClazzArray: Array<Class<*>>? = null
    private var newRpcCallMethod: Method? = null
    private val maxErrorCount = AtomicInteger(0)
    private val setMaxErrorCount: Int = BaseModel.setMaxErrorCount.value ?: 10

    private val errorMark = arrayListOf("1004", "46", "48")
    private val errorStringMark = arrayListOf("繁忙", "拒绝", "网络不可用", "重试")

    // 需要屏蔽错误日志的RPC方法列表
    private val silentErrorMethods = arrayListOf(
        "com.alipay.adexchange.ad.facade.xlightPlugin",  // 木兰集市 第一次
        "alipay.antforest.forest.h5.takeLook"  // 找能量
    )

    private val lastNullResponseLogAtMs = ConcurrentHashMap<String, Long>()
    private val nullResponseLogIntervalMs = NULL_RESPONSE_LOG_INTERVAL_MS

    private data class ErrorSummaryWindowStat(
        var windowStartMs: Long,
        var count: Int
    )

    private val errorSummaryWindowStats = ConcurrentHashMap<String, ErrorSummaryWindowStat>()
    private val errorSummaryWindowMs = ERROR_SUMMARY_WINDOW_MS
    private val lastCaptureLogAtMs = ConcurrentHashMap<String, Long>()
    private val captureLogIntervalMs = CAPTURE_LOG_INTERVAL_MS

    private val lastErrorNotifyAtMs = AtomicLong(0)
    private val errorNotifyIntervalMs = ERROR_NOTIFY_INTERVAL_MS

    private val lastReloginAtMs = AtomicLong(0)
    private val reloginIntervalMs = RELOGIN_INTERVAL_MS

    private fun offlineCooldownMs(): Long {
        return maxOf(
            BaseModel.checkInterval.value?.toLong() ?: 180000L,
            180000L
        )
    }

    private fun handleAuthLikeError(
        rpcEntity: RpcEntity,
        methodName: String?,
        statusText: String,
        notifyTitle: String,
        response: String?,
        reason: String,
        count: Int
    ): RpcEntity? {
        if (!shouldShowErrorLog(methodName)) {
            logNullResponse(rpcEntity, reason, count)
            return null
        }

        maxErrorCount.set(0)
        val wasOffline = fansirsqi.xposed.sesame.hook.ApplicationHookConstants.offline
        val cooldownMs = offlineCooldownMs()
        if (!wasOffline) {
            Notify.updateStatusText(statusText)
            if (BaseModel.errNotify.value == true &&
                shouldNotifyNow(lastErrorNotifyAtMs, errorNotifyIntervalMs)
            ) {
                Notify.sendErrorNotification(
                    "${TimeUtil.getTimeStr()} | $notifyTitle",
                    response.orEmpty()
                )
            }
        }

        if (!wasOffline) {
            fansirsqi.xposed.sesame.hook.ApplicationHookConstants.enterOffline(cooldownMs)
        }

        val shouldTryRelogin =
            BaseModel.timeoutRestart.value == true &&
                shouldNotifyNow(lastReloginAtMs, reloginIntervalMs)
        if (!wasOffline && shouldTryRelogin) {
            Log.record(TAG, "尝试重新登录")
            fansirsqi.xposed.sesame.hook.ApplicationHookUtils.reLoginByBroadcast()
        }

        logNullResponse(rpcEntity, reason, count)
        return null
    }

    /**
     * 检查指定的RPC方法是否应该显示错误日志
     *
     * @param methodName RPC方法名称
     * @return 如果应该显示错误日志返回true，否则返回false
     */
    private fun shouldShowErrorLog(methodName: String?): Boolean {
        return methodName != null && !silentErrorMethods.contains(methodName)
    }

    override fun getVersion(): RpcVersion = RpcVersion.NEW

    override fun load() {
        loader = fansirsqi.xposed.sesame.hook.ApplicationHookConstants.classLoader
        val classLoader = loader ?: run {
            Log.error(TAG, "ClassLoader为null，无法加载NewRpcBridge")
            return
        }
        
        try {
            val service = XposedHelpers.callStaticMethod(
                XposedHelpers.findClass("com.alipay.mobile.nebulacore.Nebula", classLoader),
                "getService"
            )
            val extensionManager = XposedHelpers.callMethod(service, "getExtensionManager")
            val getExtensionByName = extensionManager.javaClass.getDeclaredMethod(
                "createExtensionInstance",
                Class::class.java
            )
            getExtensionByName.isAccessible = true
            newRpcInstance = getExtensionByName.invoke(
                null,
                classLoader.loadClass("com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension")
            )

            if (newRpcInstance == null) {
                val nodeExtensionMap = XposedHelpers.callMethod(extensionManager, "getNodeExtensionMap")
                if (nodeExtensionMap != null) {
                    @Suppress("UNCHECKED_CAST")
                    val map = nodeExtensionMap as Map<Any, Map<String, Any>>
                    for ((_, innerMap) in map) {
                        for ((key, value) in innerMap) {
                            if (key == "com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension") {
                                newRpcInstance = value
                                break
                            }
                        }
                    }
                }
                if (newRpcInstance == null) {
                    Log.runtime(TAG, "get newRpcInstance null")
                    throw RuntimeException("get newRpcInstance is null")
                }
            }

            parseObjectMethod = classLoader.loadClass("com.alibaba.fastjson.JSON")
                .getMethod("parseObject", String::class.java)
            val bridgeCallbackClazz = classLoader.loadClass(
                "com.alibaba.ariver.engine.api.bridge.extension.BridgeCallback"
            )
            bridgeCallbackClazzArray = arrayOf(bridgeCallbackClazz)
            
            val rpcInstance = newRpcInstance ?: throw RuntimeException("newRpcInstance is null")
            newRpcCallMethod = rpcInstance.javaClass.getMethod(
                "rpc",
                String::class.java,
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                String::class.java,
                classLoader.loadClass(General.JSON_OBJECT_NAME),
                String::class.java,
                classLoader.loadClass(General.JSON_OBJECT_NAME),
                Boolean::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Boolean::class.javaPrimitiveType,
                String::class.java,
                classLoader.loadClass("com.alibaba.ariver.app.api.App"),
                classLoader.loadClass("com.alibaba.ariver.app.api.Page"),
                classLoader.loadClass("com.alibaba.ariver.engine.api.bridge.model.ApiContext"),
                bridgeCallbackClazz
            )
            Log.runtime(TAG, "get newRpcCallMethod successfully")
        } catch (e: Exception) {
            Log.runtime(TAG, "get newRpcCallMethod err:")
            throw e
        }
    }

    override fun unload() {
        newRpcCallMethod = null
        bridgeCallbackClazzArray = null
        parseObjectMethod = null
        newRpcInstance = null
        loader = null
    }

    override fun requestString(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): String? {
        val resRpcEntity = requestObject(rpcEntity, tryCount, retryInterval)
        return resRpcEntity?.responseString
    }

    override fun requestObject(rpcEntity: RpcEntity, tryCount: Int, retryInterval: Int): RpcEntity? {
        // 方法开始时，将成员变量赋值给局部变量，以避免在方法执行期间因其他线程的unload()调用而导致成员变量变为null
        var localNewRpcCallMethod = newRpcCallMethod
        var localParseObjectMethod = parseObjectMethod
        var localNewRpcInstance = newRpcInstance
        var localLoader = loader
        var localBridgeCallbackClazzArray = bridgeCallbackClazzArray

        if (fansirsqi.xposed.sesame.hook.ApplicationHookConstants.shouldBlockRpc()) {
            return null
        }

        // 如果RPC组件未准备好，尝试重新初始化一次
        if (localNewRpcCallMethod == null) {
            Log.debug(TAG, "RPC方法为null，尝试重新初始化...")
            try {
                load()
                // 重新加载初始化后的变量
                localNewRpcCallMethod = newRpcCallMethod
                localParseObjectMethod = parseObjectMethod
                localNewRpcInstance = newRpcInstance
                localLoader = loader
                localBridgeCallbackClazzArray = bridgeCallbackClazzArray
                Log.debug(TAG, "RPC重新初始化成功")
            } catch (e: Exception) {
                Log.error(TAG, "RPC重新初始化失败:")
                Log.printStackTrace(e)
                logNullResponse(rpcEntity, "RPC组件初始化失败", 0)
                return null
            }
        }

        if (localNewRpcCallMethod == null || localParseObjectMethod == null ||
            localNewRpcInstance == null || localLoader == null || localBridgeCallbackClazzArray == null
        ) {
            logNullResponse(rpcEntity, "RPC组件不完整", 0)
            return null
        }

        try {
            var count = 0
            do {
                count++
                try {
                    val requestMethod = rpcEntity.requestMethod ?: run {
                        Log.error(TAG, "requestMethod为null")
                        return null
                    }
                    RpcIntervalLimit.enterIntervalLimit(requestMethod)
                    val finalLocalBridgeCallbackClazzArray = localBridgeCallbackClazzArray
                    localNewRpcCallMethod.invoke(
                        localNewRpcInstance,
                        rpcEntity.requestMethod,
                        false,
                        false,
                        "json",
                        localParseObjectMethod.invoke(null, rpcEntity.rpcFullRequestData),
                        "",
                        null,
                        true,
                        false,
                        0,
                        false,
                        "",
                        null,
                        null,
                        null,
                        Proxy.newProxyInstance(
                            localLoader,
                            localBridgeCallbackClazzArray
                        ) { proxy, method, args ->
                            when (method.name) {
                                "equals" -> proxy === args?.get(0)
                                "hashCode" -> System.identityHashCode(proxy)
                                "toString" -> "Proxy for ${finalLocalBridgeCallbackClazzArray[0].name}"
                                "sendJSONResponse" -> {
                                    if (args != null && args.isNotEmpty()) {
                                        try {
                                            val obj = args[0]
                                            // 获取JSON字符串，失败时重试一次
                                            var jsonString: String? = null
                                            try {
                                                jsonString = XposedHelpers.callMethod(obj, "toJSONString") as String
                                            } catch (e: Exception) {
                                                // 第一次失败，尝试重试
                                                try {
                                                    GlobalThreadPools.sleepCompat(100L)
                                                    jsonString = XposedHelpers.callMethod(obj, "toJSONString") as String
                                                } catch (retryException: Exception) {
                                                    // 重试后仍失败，记录日志并标记错误，触发外层RPC重试
                                                    Log.runtime(TAG, "toJSONString 重试后仍然失败，将触发整个 RPC 请求重试: ${retryException.message}")
                                                    rpcEntity.setResponseObject(obj, null)
                                                    rpcEntity.setError()
                                                    return@newProxyInstance null
                                                }
                                            }

                                            rpcEntity.setResponseObject(obj, jsonString)
                                            if (!(XposedHelpers.callMethod(obj, "containsKey", "success") as Boolean) &&
                                                !(XposedHelpers.callMethod(obj, "containsKey", "isSuccess") as Boolean)
                                            ) {
                                                rpcEntity.setError()
                                                val methodName = rpcEntity.requestMethod ?: "unknown"
                                                val errorCode = getErrorValueAsString(obj, "error")
                                                val errorMessage = getErrorValueAsString(obj, "errorMessage")

                                                val isMarkedNetworkError =
                                                    errorMark.contains(errorCode) ||
                                                        errorStringMark.contains(errorMessage)
                                                if (isMarkedNetworkError) {
                                                    logErrorSummary(methodName, errorCode, errorMessage)
                                                } else if (shouldShowErrorLog(methodName)) {
                                                    val message = buildString {
                                                        append("new rpc response1 | id: ")
                                                        append(rpcEntity.hashCode())
                                                        append(" | method: ")
                                                        append(rpcEntity.requestMethod)
                                                        append("\n")
                                                        append("args: ")
                                                        append(truncateForLog(rpcEntity.requestData, REQUEST_LOG_LIMIT))
                                                        append(" |\n")
                                                        append("data: ")
                                                        append(
                                                            truncateForLog(
                                                                rpcEntity.responseString,
                                                                RESPONSE_LOG_LIMIT
                                                            )
                                                        )
                                                    }
                                                    Log.error(TAG, message)
                                                }
                                            }
                                        } catch (e: Exception) {
                                            rpcEntity.setError()
                                            Log.error(
                                                TAG,
                                                "new rpc response2 | id: ${rpcEntity.hashCode()} | method: ${rpcEntity.requestMethod} err:"
                                            )
                                            Log.printStackTrace(e)
                                        }
                                    }
                                    null
                                }
                                else -> null
                            }
                        }
                    )

                    if (!rpcEntity.hasResult) {
                        logNullResponse(rpcEntity, "无响应结果", count)
                        return null
                    }

                    if (!rpcEntity.hasError) {
                        maxErrorCount.set(0)
                        return rpcEntity
                    }

                    try {
                        val responseObj = rpcEntity.responseObject
                        val errorCode =
                            if (responseObj != null) getErrorValueAsString(responseObj, "error") else ""
                        val errorMessage =
                            if (responseObj != null) getErrorValueAsString(responseObj, "errorMessage") else ""
                        val response = rpcEntity.responseString
                        val methodName = rpcEntity.requestMethod

                        if (errorCode == "1009") {
                            return handleAuthLikeError(
                                rpcEntity = rpcEntity,
                                methodName = methodName,
                                statusText = "需要验证后继续执行",
                                notifyTitle = "需要验证后继续执行",
                                response = response,
                                reason = "需要验证: $errorCode/$errorMessage",
                                count = count
                            )
                        }

                        if (errorCode == "2000" && errorMessage.contains("登录超时")) {
                            return handleAuthLikeError(
                                rpcEntity = rpcEntity,
                                methodName = methodName,
                                statusText = "登录超时",
                                notifyTitle = "登录超时",
                                response = response,
                                reason = "登录超时: $errorCode/$errorMessage",
                                count = count
                            )
                        }

                        if (errorMark.contains(errorCode) || errorStringMark.contains(errorMessage)) {
                            val shouldHandleRecovery = shouldShowErrorLog(methodName)
                            val currentErrorCount = if (shouldHandleRecovery) {
                                maxErrorCount.incrementAndGet()
                            } else {
                                0
                            }
                            if (!fansirsqi.xposed.sesame.hook.ApplicationHookConstants.offline && shouldHandleRecovery) {
                                var enteredOffline = false
                                if (currentErrorCount > setMaxErrorCount) {
                                    fansirsqi.xposed.sesame.hook.ApplicationHookConstants.enterOffline(
                                        offlineCooldownMs()
                                    )
                                    enteredOffline = true
                                    Notify.updateStatusText("网络连接异常，已进入离线模式")
                                    if (BaseModel.errNotify.value == true) {
                                        Notify.sendErrorNotification(
                                            "${TimeUtil.getTimeStr()} | 网络异常次数超过阈值[$setMaxErrorCount]",
                                            response
                                        )
                                    }
                                }

                                if (BaseModel.errNotify.value == true &&
                                    shouldNotifyNow(lastErrorNotifyAtMs, errorNotifyIntervalMs)
                                ) {
                                    Notify.sendErrorNotification(
                                        "${TimeUtil.getTimeStr()} | 网络异常: $methodName",
                                        response
                                    )
                                }

                                val shouldTryRelogin =
                                    BaseModel.timeoutRestart.value == true &&
                                        shouldNotifyNow(lastReloginAtMs, reloginIntervalMs) &&
                                        errorCode != "1004" &&
                                        !enteredOffline
                                if (shouldTryRelogin) {
                                    Log.record(TAG, "尝试重新登录")
                                    fansirsqi.xposed.sesame.hook.ApplicationHookUtils.reLoginByBroadcast()
                                }
                            }

                            logNullResponse(rpcEntity, "网络错误: $errorCode/$errorMessage", count)
                            return null
                        }
                        return rpcEntity
                    } catch (e: Exception) {
                        Log.error(
                            TAG,
                            "new rpc response | id: ${rpcEntity.hashCode()} | method: ${rpcEntity.requestMethod} get err:"
                        )
                        Log.printStackTrace(e)
                    }

                    when {
                        retryInterval < 0 -> CoroutineUtils.sleepCompat((600 + RandomUtil.delay()).toLong())
                        retryInterval > 0 -> CoroutineUtils.sleepCompat(retryInterval.toLong())
                    }
                } catch (t: Throwable) {
                    Log.error(
                        TAG,
                        "new rpc request | id: ${rpcEntity.hashCode()} | method: ${rpcEntity.requestMethod} err:"
                    )
                    Log.printStackTrace(t)
                    when {
                        retryInterval < 0 -> CoroutineUtils.sleepCompat((600 + RandomUtil.delay()).toLong())
                        retryInterval > 0 -> CoroutineUtils.sleepCompat(retryInterval.toLong())
                    }
                }
            } while (count < tryCount)

            logNullResponse(rpcEntity, "重试次数耗尽", tryCount)
            return null
        } finally {
            // 仅在调试模式下打印堆栈
            if (BaseModel.debugMode.value == true) {
                val methodName = rpcEntity.requestMethod ?: "unknown"
                val now = System.currentTimeMillis()
                val last = lastCaptureLogAtMs[methodName]
                if (last == null || now - last >= captureLogIntervalMs) {
                    lastCaptureLogAtMs[methodName] = now
                    val captureMessage = buildString {
                        append("New RPC\n")
                        append("方法: ")
                        append(methodName)
                        append("\n")
                        append("参数: ")
                        append(truncateForLog(rpcEntity.requestData, REQUEST_LOG_LIMIT))
                        append("\n")
                        append("数据: ")
                        append(
                            truncateForLog(
                                rpcEntity.responseString,
                                RESPONSE_LOG_LIMIT
                            )
                        )
                    }
                    Log.capture(TAG, captureMessage)
                    Log.printStack(TAG)
                }
            }
        }
    }

    private fun logNullResponse(rpcEntity: RpcEntity?, reason: String, count: Int) {
        val methodName = rpcEntity?.requestMethod ?: "unknown"
        if (shouldShowErrorLog(methodName)) {
            val now = System.currentTimeMillis()
            val last = lastNullResponseLogAtMs[methodName]
            if (last == null || now - last >= nullResponseLogIntervalMs) {
                lastNullResponseLogAtMs[methodName] = now
                Log.error(TAG, "RPC返回null | 方法: $methodName | 原因: $reason | 重试: $count")
            }
        }
    }

    private fun logErrorSummary(methodName: String, errorCode: String, errorMessage: String) {
        if (!shouldShowErrorLog(methodName)) return

        val key = "$methodName|$errorCode"
        val now = System.currentTimeMillis()
        val stat = errorSummaryWindowStats.computeIfAbsent(key) {
            ErrorSummaryWindowStat(windowStartMs = now, count = 0)
        }

        var summaryLog: String? = null
        var shouldLogDetail = false
        synchronized(stat) {
            if (now - stat.windowStartMs >= errorSummaryWindowMs) {
                summaryLog = buildString {
                    append("RPC错误汇总 | 方法: ")
                    append(methodName)
                    append(" | 错误: ")
                    append(errorCode)
                    append("/")
                    append(errorMessage)
                    append(" | 次数: ")
                    append(stat.count)
                    append(" | 窗口: ")
                    append(errorSummaryWindowMs)
                    append("ms")
                }
                stat.windowStartMs = now
                stat.count = 0
                shouldLogDetail = true
            } else if (stat.count == 0) {
                shouldLogDetail = true
            }
            stat.count++
        }
        summaryLog?.let { Log.error(TAG, it) }
        if (shouldLogDetail) {
            Log.error(TAG, "RPC错误 | 方法: $methodName | 错误: $errorCode/$errorMessage")
        }
    }

    private fun shouldNotifyNow(lastMs: AtomicLong, intervalMs: Long): Boolean {
        val now = System.currentTimeMillis()
        val last = lastMs.get()
        if (now - last < intervalMs) return false
        return lastMs.compareAndSet(last, now)
    }

    companion object {
        private val TAG = NewRpcBridge::class.java.simpleName

        private const val NULL_RESPONSE_LOG_INTERVAL_MS: Long = 5_000L
        private const val ERROR_SUMMARY_WINDOW_MS: Long = 10 * 60_000L
        private const val ERROR_NOTIFY_INTERVAL_MS: Long = 60_000L
        private const val RELOGIN_INTERVAL_MS: Long = 120_000L
        private const val CAPTURE_LOG_INTERVAL_MS: Long = 2_000L
    }
}
