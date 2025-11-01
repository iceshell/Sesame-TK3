package fansirsqi.xposed.sesame.hook.rpc.bridge

import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.entity.RpcEntity
import fansirsqi.xposed.sesame.hook.ApplicationHook
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
import java.util.concurrent.atomic.AtomicInteger

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

    private val errorMark = arrayListOf("1004", "1009", "2000", "46", "48")
    private val errorStringMark = arrayListOf("繁忙", "拒绝", "网络不可用", "重试")

    // 需要屏蔽错误日志的RPC方法列表
    private val silentErrorMethods = arrayListOf(
        "com.alipay.adexchange.ad.facade.xlightPlugin",  // 木兰集市 第一次
        "alipay.antforest.forest.h5.takeLook"  // 找能量
    )

    /**
     * 检查指定的RPC方法是否应该显示错误日志
     *
     * @param methodName RPC方法名称
     * @return 如果应该显示错误日志返回true，否则返回false
     */
    private fun shouldShowErrorLog(methodName: String?): Boolean {
        return methodName != null && !silentErrorMethods.contains(methodName)
    }

    /**
     * 记录RPC请求返回null的原因
     *
     * @param rpcEntity RPC请求实体
     * @param reason 返回null的原因
     * @param count 当前重试次数
     */
    private fun logNullResponse(rpcEntity: RpcEntity?, reason: String, count: Int) {
        val methodName = rpcEntity?.requestMethod ?: "unknown"
        if (shouldShowErrorLog(methodName)) {
            Log.error(TAG, "RPC返回null | 方法: $methodName | 原因: $reason | 重试: $count")
        }
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

        if (ApplicationHook.isOffline()) {
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
                                                if (shouldShowErrorLog(rpcEntity.requestMethod)) {
                                                    Log.error(
                                                        TAG,
                                                        "new rpc response1 | id: ${rpcEntity.hashCode()} | method: ${rpcEntity.requestMethod}\n " +
                                                                "args: ${rpcEntity.requestData} |\n data: ${rpcEntity.responseString}"
                                                    )
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
                        return rpcEntity
                    }

                    try {
                        val errorCode = XposedHelpers.callMethod(
                            rpcEntity.responseObject,
                            "getString",
                            "error"
                        ) as? String ?: ""
                        val errorMessage = XposedHelpers.callMethod(
                            rpcEntity.responseObject,
                            "getString",
                            "errorMessage"
                        ) as? String ?: ""
                        val response = rpcEntity.responseString
                        val methodName = rpcEntity.requestMethod

                        if (errorMark.contains(errorCode) || errorStringMark.contains(errorMessage)) {
                            val currentErrorCount = maxErrorCount.incrementAndGet()
                            if (!ApplicationHook.isOffline()) {
                                if (currentErrorCount > setMaxErrorCount) {
                                    ApplicationHook.setOffline(true)
                                    Notify.updateStatusText("网络连接异常，已进入离线模式")
                                    if (BaseModel.errNotify.value == true) {
                                        Notify.sendErrorNotification(
                                            "${TimeUtil.getTimeStr()} | 网络异常次数超过阈值[$setMaxErrorCount]",
                                            response
                                        )
                                    }
                                }
                                if (BaseModel.errNotify.value == true) {
                                    Notify.sendErrorNotification(
                                        "${TimeUtil.getTimeStr()} | 网络异常: $methodName",
                                        response
                                    )
                                }
                                if (BaseModel.timeoutRestart.value == true) {
                                    Log.record(TAG, "尝试重新登录")
                                    ApplicationHook.reLoginByBroadcast()
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
                Log.system(TAG, "New RPC\n方法: ${rpcEntity.requestMethod}\n参数: ${rpcEntity.requestData}\n数据: ${rpcEntity.responseString}")
                Log.printStack(TAG)
            }
        }
    }

    companion object {
        private val TAG = NewRpcBridge::class.java.simpleName
    }
}
