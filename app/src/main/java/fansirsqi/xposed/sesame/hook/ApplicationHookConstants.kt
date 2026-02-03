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
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.task.BaseTask
import fansirsqi.xposed.sesame.util.Log
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Method
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * ApplicationHook 常量和静态字段
 * 第一部分迁移：所有静态字段、常量和基础getter/setter
 */
object ApplicationHookConstants {
    const val TAG = "ApplicationHook"

    private const val MIN_ENTRY_INTERVAL_MS = 1_500L

    @Volatile
    @JvmStatic
    var nowProvider: () -> Long = { System.currentTimeMillis() }

    private val lastEntryAtMsByAction = ConcurrentHashMap<String, AtomicLong>()

    private val entryExecutorLock = Any()

    @Volatile
    private var entryExecutor: ExecutorService? = null

    private fun newEntryExecutor(): ExecutorService {
        return Executors.newSingleThreadExecutor { r ->
            Thread(r, "SesameEntry").apply { isDaemon = true }
        }
    }

    private fun getEntryExecutor(): ExecutorService {
        val current = entryExecutor
        if (current != null && !current.isShutdown && !current.isTerminated) {
            return current
        }
        return synchronized(entryExecutorLock) {
            val existing = entryExecutor
            if (existing != null && !existing.isShutdown && !existing.isTerminated) {
                existing
            } else {
                newEntryExecutor().also { entryExecutor = it }
            }
        }
    }

    @JvmStatic
    fun shutdownEntryExecutor() {
        val executorToShutdown = synchronized(entryExecutorLock) {
            val existing = entryExecutor
            entryExecutor = null
            existing
        }
        executorToShutdown?.shutdown()
    }

    @JvmStatic
    fun submitEntry(action: String, block: () -> Unit) {
        try {
            getEntryExecutor().submit {
                val thread = Thread.currentThread()
                val oldName = thread.name
                thread.name = "SesameEntry:$action"
                try {
                    runCatching { block() }
                        .onFailure { Log.printStackTrace(TAG, it) }
                } finally {
                    thread.name = oldName
                }
            }
        } catch (e: java.util.concurrent.RejectedExecutionException) {
            Log.printStackTrace(TAG, e)
        }
    }

    @JvmStatic
    fun submitEntryDebounced(action: String, block: () -> Unit) {
        val now = System.currentTimeMillis()
        val lastEntryAtMs = lastEntryAtMsByAction.getOrPut(action) { AtomicLong(0) }
        val last = lastEntryAtMs.get()
        if (now - last < MIN_ENTRY_INTERVAL_MS) {
            Log.runtime(TAG, "入口处理过于频繁，已跳过: $action")
            return
        }
        if (!lastEntryAtMs.compareAndSet(last, now)) {
            return
        }
        submitEntry(action, block)
    }
    
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

    @Volatile
    @JvmStatic
    var offlineReason: String? = null

    @Volatile
    @JvmStatic
    var offlineReasonDetail: String? = null

    @JvmStatic
    val offlineEnterCount: AtomicInteger = AtomicInteger(0)

    @JvmStatic
    val offlineExitCount: AtomicInteger = AtomicInteger(0)

    @Volatile
    @JvmStatic
    var lastOfflineEnterAtMs: Long = 0L

    @Volatile
    @JvmStatic
    var lastOfflineExitAtMs: Long = 0L

    @Volatile
    @JvmStatic
    var lastOfflineEnterReason: String? = null

    @Volatile
    @JvmStatic
    var lastOfflineEnterReasonDetail: String? = null

    enum class OfflineEventType {
        ENTER,
        REFRESH,
        EXIT,
        AUTO_EXIT
    }

    data class OfflineEvent(
        val type: OfflineEventType,
        val atMs: Long,
        val cooldownMs: Long,
        val untilMs: Long,
        val reason: String?,
        val detail: String?
    )

    private const val OFFLINE_EVENT_MAX = 64
    private val offlineEvents = ConcurrentLinkedQueue<OfflineEvent>()
    private val offlineEventSize = AtomicInteger(0)

    private fun addOfflineEvent(event: OfflineEvent) {
        offlineEvents.add(event)
        val size = offlineEventSize.incrementAndGet()
        if (size <= OFFLINE_EVENT_MAX) return
        while (offlineEventSize.get() > OFFLINE_EVENT_MAX) {
            val removed = offlineEvents.poll() ?: break
            offlineEventSize.decrementAndGet()
            if (removed === event) break
        }
    }

    @JvmStatic
    fun getOfflineEventsSnapshot(): List<OfflineEvent> {
        return offlineEvents.toList()
    }

    @JvmStatic
    fun enterOffline(cooldownMs: Long) {
        enterOffline(cooldownMs, null)
    }

    @JvmStatic
    fun enterOffline(cooldownMs: Long, reason: String?) {
        enterOffline(cooldownMs, reason, null)
    }

    @JvmStatic
    fun enterOffline(cooldownMs: Long, reason: String?, detail: String?) {
        val wasOffline = offline
        val now = nowProvider()
        offline = true
        offlineReason = reason
        offlineReasonDetail = detail

        if (!wasOffline) {
            offlineEnterCount.incrementAndGet()
            lastOfflineEnterAtMs = now
            lastOfflineEnterReason = reason
            lastOfflineEnterReasonDetail = detail
        }

        offlineUntilMs = if (cooldownMs > 0) {
            now + cooldownMs
        } else {
            0L
        }

        addOfflineEvent(
            OfflineEvent(
                type = if (wasOffline) OfflineEventType.REFRESH else OfflineEventType.ENTER,
                atMs = now,
                cooldownMs = cooldownMs,
                untilMs = offlineUntilMs,
                reason = reason,
                detail = detail
            )
        )

        ModuleStatusReporter.requestUpdate(if (wasOffline) "offline_refresh" else "offline_enter")
    }

    @JvmStatic
    fun getOfflineCooldownMs(): Long {
        val configured = BaseModel.offlineCooldown.value?.toLong() ?: 0L
        if (configured > 0L) {
            return maxOf(configured, 180000L)
        }

        return maxOf(
            BaseModel.checkInterval.value?.toLong() ?: 180000L,
            180000L
        )
    }

    @JvmStatic
    fun exitOffline() {
        exitOfflineInternal(OfflineEventType.EXIT)
    }

    private fun exitOfflineInternal(type: OfflineEventType) {
        val now = nowProvider()
        val enterAtMs = lastOfflineEnterAtMs
        val durationMs = if (enterAtMs > 0L) (now - enterAtMs).coerceAtLeast(0L) else -1L
        val enterReason = offlineReason

        val enterDetail = offlineReasonDetail

        offline = false
        offlineUntilMs = 0L
        offlineReason = null
        offlineReasonDetail = null

        offlineExitCount.incrementAndGet()
        lastOfflineExitAtMs = now

        addOfflineEvent(
            OfflineEvent(
                type = type,
                atMs = now,
                cooldownMs = 0L,
                untilMs = 0L,
                reason = enterReason,
                detail = enterDetail
            )
        )

        Log.record(
            TAG,
            "exitOffline: durationMs=$durationMs reason=${enterReason ?: "null"} detail=${enterDetail ?: "null"}"
        )

        ModuleStatusReporter.requestUpdate(
            when (type) {
                OfflineEventType.EXIT -> "offline_exit"
                OfflineEventType.AUTO_EXIT -> "offline_auto_exit"
                else -> "offline_exit"
            }
        )
    }

    @JvmStatic
    fun shouldBlockRpc(): Boolean {
        if (!offline) return false

        val untilMs = offlineUntilMs
        if (untilMs <= 0L) return true

        val now = nowProvider()
        if (now < untilMs) return true

        exitOfflineInternal(OfflineEventType.AUTO_EXIT)
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

     private fun getTriggerPriority(source: TriggerSource): Int {
         return when (source) {
             TriggerSource.ALARM -> 3
             TriggerSource.EXECUTE_BROADCAST -> 2
             TriggerSource.ON_RESUME -> 1
             TriggerSource.UNKNOWN -> 0
         }
     }

    data class TriggerInfo(
        val source: TriggerSource,
        val requestCode: Int = -1,
        val isBackupAlarm: Boolean = false
    )

    private val alarmTriggerQueue = ConcurrentLinkedQueue<TriggerInfo>()
    private val pendingNonAlarmTrigger = AtomicReference<TriggerInfo?>(null)

    private val alarmQueueSize = AtomicInteger(0)
    private val hasPendingNonAlarmTrigger = AtomicInteger(0)

    private const val TRIGGER_LOG_INTERVAL_MS = 5_000L
    private val lastTriggerEnqueueLogAtMs = AtomicLong(0)
    private val lastTriggerDequeueLogAtMs = AtomicLong(0)

    private fun tryLogTriggerEnqueue(triggerInfo: TriggerInfo) {
        val now = System.currentTimeMillis()
        val last = lastTriggerEnqueueLogAtMs.get()
        if (now - last < TRIGGER_LOG_INTERVAL_MS) return
        if (!lastTriggerEnqueueLogAtMs.compareAndSet(last, now)) return

        Log.runtime(
            TAG,
            "trigger enqueue: src=${triggerInfo.source} rc=${triggerInfo.requestCode} backup=${triggerInfo.isBackupAlarm} alarmQ=${alarmQueueSize.get()} nonAlarm=${hasPendingNonAlarmTrigger.get()}"
        )
    }

    private fun tryLogTriggerDequeue(triggerInfo: TriggerInfo) {
        val now = System.currentTimeMillis()
        val last = lastTriggerDequeueLogAtMs.get()
        if (now - last < TRIGGER_LOG_INTERVAL_MS) return
        if (!lastTriggerDequeueLogAtMs.compareAndSet(last, now)) return

        Log.runtime(
            TAG,
            "trigger dequeue: src=${triggerInfo.source} rc=${triggerInfo.requestCode} backup=${triggerInfo.isBackupAlarm} alarmQ=${alarmQueueSize.get()} nonAlarm=${hasPendingNonAlarmTrigger.get()}"
        )
    }

    @JvmStatic
    fun setPendingTrigger(triggerInfo: TriggerInfo) {
        if (triggerInfo.source == TriggerSource.ALARM) {
            alarmTriggerQueue.add(triggerInfo)
            alarmQueueSize.incrementAndGet()
            tryLogTriggerEnqueue(triggerInfo)
            return
        }

        while (!Thread.currentThread().isInterrupted) {
            val current = pendingNonAlarmTrigger.get()
            if (current == null) {
                if (pendingNonAlarmTrigger.compareAndSet(null, triggerInfo)) {
                    hasPendingNonAlarmTrigger.set(1)
                    tryLogTriggerEnqueue(triggerInfo)
                    return
                }
                continue
            }

            val currentPriority = getTriggerPriority(current.source)
            val newPriority = getTriggerPriority(triggerInfo.source)

            val shouldReplace = when {
                newPriority > currentPriority -> true
                newPriority < currentPriority -> false
                current.source == TriggerSource.ALARM && triggerInfo.source == TriggerSource.ALARM -> {
                    current.isBackupAlarm && !triggerInfo.isBackupAlarm
                }
                else -> true
            }

            if (!shouldReplace) {
                Log.runtime(TAG, "忽略低优先级触发: new=${triggerInfo.source} current=${current.source}")
                return
            }

            if (pendingNonAlarmTrigger.compareAndSet(current, triggerInfo)) {
                hasPendingNonAlarmTrigger.set(1)
                tryLogTriggerEnqueue(triggerInfo)
                return
            }
        }
    }

    @JvmStatic
    fun consumePendingTrigger(): TriggerInfo? {
        val alarmTrigger = alarmTriggerQueue.poll()
        if (alarmTrigger != null) {
            alarmQueueSize.decrementAndGet()
            tryLogTriggerDequeue(alarmTrigger)
            return alarmTrigger
        }
        val nonAlarm = pendingNonAlarmTrigger.getAndSet(null)
        if (nonAlarm != null) {
            hasPendingNonAlarmTrigger.set(0)
            tryLogTriggerDequeue(nonAlarm)
        }
        return nonAlarm
    }

    @JvmStatic
    fun hasPendingAlarmTriggers(): Boolean {
        return alarmQueueSize.get() > 0
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
