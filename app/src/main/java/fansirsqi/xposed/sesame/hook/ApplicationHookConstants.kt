package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.os.Handler
import android.os.PowerManager
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.hook.keepalive.AlipayComponentHelper
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcBridge
import fansirsqi.xposed.sesame.hook.rpc.bridge.RpcVersion
import fansirsqi.xposed.sesame.hook.server.ModuleHttpServer
import fansirsqi.xposed.sesame.task.BaseTask
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.util.Calendar
import java.util.concurrent.atomic.AtomicInteger
 import java.util.concurrent.atomic.AtomicReference

/**
 * ApplicationHook 常量和静态字段
 * 第一部分迁移：所有静态字段、常量和基础getter/setter
 */
object ApplicationHookConstants {
    const val TAG = "ApplicationHook"
    
    // 模块版本
    val modelVersion: String = BuildConfig.VERSION_NAME
    
    // 最大不活动时间：1小时
    const val MAX_INACTIVE_TIME = 3600000L
    
    // 支付宝版本
    @JvmStatic
    var alipayVersion: AlipayVersion = AlipayVersion("")
        private set
    
    @JvmStatic
    fun setAlipayVersion(version: AlipayVersion) {
        alipayVersion = version
    }
    
    // ClassLoader
    @JvmStatic
    var classLoader: ClassLoader? = null
        private set
    
    @JvmStatic
    fun setClassLoader(loader: ClassLoader?) {
        classLoader = loader
    }
    
    // MicroApplication Context
    @JvmStatic
    var microApplicationContextObject: Any? = null
        private set
    
    @JvmStatic
    fun setMicroApplicationContextObject(obj: Any?) {
        microApplicationContextObject = obj
    }
    
    // Application Context
    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    var appContext: Context? = null
        private set
    
    @JvmStatic
    fun setAppContext(context: Context?) {
        appContext = context
    }
    
    // AlipayComponentHelper
    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    var alipayComponentHelper: AlipayComponentHelper? = null
        private set
    
    @JvmStatic
    fun setAlipayComponentHelper(helper: AlipayComponentHelper?) {
        alipayComponentHelper = helper
    }
    
    // Hook状态
    @Volatile
    @JvmStatic
    var hooked: Boolean = false
        private set
    
    @JvmStatic
    fun setHooked(value: Boolean) {
        hooked = value
    }
    
    // 初始化状态
    @Volatile
    @JvmStatic
    var init: Boolean = false
        private set
    
    @JvmStatic
    fun setInit(value: Boolean) {
        init = value
    }
    
    // 日历
    @Volatile
    @JvmStatic
    var dayCalendar: Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }
    
    // 离线状态
    @Volatile
    @JvmStatic
    var offline: Boolean = false

    @Volatile
    @JvmStatic
    var offlineUntilMs: Long = 0L

    @JvmStatic
    fun enterOffline(cooldownMs: Long) {
        offline = true
        offlineUntilMs = if (cooldownMs > 0) {
            System.currentTimeMillis() + cooldownMs
        } else {
            0L
        }
    }

    @JvmStatic
    fun exitOffline() {
        offline = false
        offlineUntilMs = 0L
    }

    @JvmStatic
    fun shouldBlockRpc(): Boolean {
        if (!offline) return false

        val untilMs = offlineUntilMs
        if (untilMs <= 0L) return true

        val now = System.currentTimeMillis()
        if (now < untilMs) return true

        exitOffline()
        return false
    }
    
    // 闹钟触发标志
    @Volatile
    @JvmStatic
    var alarmTriggeredFlag: Boolean = false
        private set
    
    @JvmStatic
    fun setAlarmTriggeredFlag(value: Boolean) {
        alarmTriggeredFlag = value
    }

    enum class TriggerSource {
        ALARM,
        EXECUTE_BROADCAST,
        ON_RESUME,
        UNKNOWN
    }

    data class TriggerInfo(
        val source: TriggerSource,
        val requestCode: Int = -1,
        val isBackupAlarm: Boolean = false
    )

    private val pendingTrigger = AtomicReference<TriggerInfo?>(null)

    @JvmStatic
    fun setPendingTrigger(triggerInfo: TriggerInfo) {
        pendingTrigger.set(triggerInfo)
    }

    @JvmStatic
    fun consumePendingTrigger(): TriggerInfo? {
        return pendingTrigger.getAndSet(null)
    }
    
    // 重登录计数
    @JvmStatic
    val reLoginCount: AtomicInteger = AtomicInteger(0)
    
    // Service
    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    var service: Service? = null
        private set
    
    @JvmStatic
    fun setService(svc: Service?) {
        service = svc
    }
    
    // Main Handler
    @JvmStatic
    var mainHandler: Handler? = null
        private set
    
    @JvmStatic
    fun setMainHandler(handler: Handler?) {
        mainHandler = handler
    }
    
    // Main Task
    @JvmStatic
    var mainTask: BaseTask? = null
        private set
    
    @JvmStatic
    fun setMainTask(task: BaseTask?) {
        mainTask = task
    }
    
    // RPC Bridge
    @Volatile
    @JvmStatic
    var rpcBridge: RpcBridge? = null
        private set
    
    @JvmStatic
    val rpcBridgeLock: Any = Any()
    
    @JvmStatic
    fun setRpcBridge(bridge: RpcBridge?) {
        synchronized(rpcBridgeLock) {
            rpcBridge = bridge
        }
    }
    
    // RPC Version
    @JvmStatic
    var rpcVersion: RpcVersion? = null
        private set
    
    @JvmStatic
    fun setRpcVersion(version: RpcVersion?) {
        rpcVersion = version
    }
    
    // WakeLock
    @JvmStatic
    var wakeLock: PowerManager.WakeLock? = null
        private set
    
    @JvmStatic
    fun setWakeLock(lock: PowerManager.WakeLock?) {
        wakeLock = lock
    }
    
    // 执行时间相关
    @Volatile
    @JvmStatic
    var lastExecTime: Long = 0
        private set
    
    @JvmStatic
    fun setLastExecTime(time: Long) {
        lastExecTime = time
    }
    
    @Volatile
    @JvmStatic
    var nextExecutionTime: Long = 0

    // CoroutineTaskRunner 运行计数（用于阻止 AlarmScheduler 备份重启误伤正在运行的任务）
    @JvmStatic
    val taskRunnerRunningCount: AtomicInteger = AtomicInteger(0)

    @JvmStatic
    fun markTaskRunnerStart() {
        taskRunnerRunningCount.incrementAndGet()
    }

    @JvmStatic
    fun markTaskRunnerFinish() {
        val newValue = taskRunnerRunningCount.decrementAndGet()
        if (newValue < 0) {
            taskRunnerRunningCount.set(0)
        }
    }
    
    // Xposed Interface
    @JvmStatic
    var xposedInterface: XposedInterface? = null
        private set
    
    @JvmStatic
    fun setXposedInterface(iface: XposedInterface?) {
        xposedInterface = iface
    }
    
    // HTTP Server
    @JvmStatic
    var httpServer: ModuleHttpServer? = null
    
    // Deoptimize Method
    @JvmStatic
    val deoptimizeMethod: Method? by lazy {
        try {
            de.robv.android.xposed.XposedBridge::class.java.getDeclaredMethod(
                "deoptimizeMethod",
                java.lang.reflect.Member::class.java
            )
        } catch (t: Throwable) {
            de.robv.android.xposed.XposedBridge.log("E/$TAG ${android.util.Log.getStackTraceString(t)}")
            null
        }
    }
}
