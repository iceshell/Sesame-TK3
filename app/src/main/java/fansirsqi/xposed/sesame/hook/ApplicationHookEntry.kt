package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.RunType
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.entity.AlipayVersion
import fansirsqi.xposed.sesame.hook.keepalive.AlipayComponentHelper
import fansirsqi.xposed.sesame.hook.rpc.debug.DebugRpc
import fansirsqi.xposed.sesame.hook.server.ModuleHttpServerManager
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.task.BaseTask
import fansirsqi.xposed.sesame.task.TaskRunnerAdapter
import fansirsqi.xposed.sesame.util.AssetUtil
import fansirsqi.xposed.sesame.util.Detector
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.util.maps.UserMap
import io.github.libxposed.api.XposedModuleInterface
import org.luckypray.dexkit.DexKitBridge
import java.io.File
import java.util.Calendar
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

private fun getCurrentProcessName(context: Context): String {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Application.getProcessName()
        } else {
            val pid = Process.myPid()
            val cmdlineFile = File("/proc/$pid/cmdline")
            if (cmdlineFile.exists()) {
                cmdlineFile.readText().trim('\u0000')
            } else {
                context.packageName
            }
        }
    } catch (_: Exception) {
        context.packageName
    }
}

private fun isMainProcess(context: Context): Boolean {
    val processName = getCurrentProcessName(context)
    return processName == General.PACKAGE_NAME
}

private object EntryDispatcher {
    private const val TAG = "ApplicationHook"
    private const val MIN_ENTRY_INTERVAL_MS = 1_500L

    private val lastEntryAtMsByAction = ConcurrentHashMap<String, AtomicLong>()

    private val entryExecutor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "SesameEntry").apply { isDaemon = true }
    }

    fun submit(action: String, block: () -> Unit) {
        try {
            entryExecutor.submit {
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

    fun submitDebounced(action: String, block: () -> Unit) {
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
        submit(action, block)
    }
}

/**
 * ApplicationHook 入口类
 * 第四部分迁移：Hook入口、广播接收器和主要Hook逻辑
 */
class ApplicationHookEntry {
    companion object {
        private const val TAG = "ApplicationHook"

        @Volatile
        private var broadcastReceiverRegistered: Boolean = false

        @Volatile
        private var broadcastReceiverSkipLogged: Boolean = false

        private val broadcastReceiverInstance: AlipayBroadcastReceiver by lazy {
            AlipayBroadcastReceiver()
        }

 
        
        /**
         * ✅ 原有新版入口：LibXposed / LSPosed ≥ 1.9 使用
         */
        @JvmStatic
        fun loadPackage(lpparam: XposedModuleInterface.PackageLoadedParam) {
            Log.runtime(TAG, "xposed start loadPackage: ${lpparam.packageName}")
            if (General.PACKAGE_NAME != lpparam.packageName) return
            
            ApplicationHookConstants.setClassLoader(lpparam.classLoader)
            handleHookLogic(
                lpparam.classLoader,
                lpparam.packageName,
                lpparam.applicationInfo.sourceDir,
                null
            )
        }

        /**
         * ✅ 新增旧版兼容入口：传统 Xposed / EdXposed / LSPosed < 1.9 使用
         */
        @JvmStatic
        fun loadPackageCompat(lpparam: XC_LoadPackage.LoadPackageParam) {
            Log.runtime(TAG, "xp82 start loadPackageCompat: ${lpparam.packageName}")
            XposedBridge.log("$TAG|Hook in ${lpparam.packageName} in process ${lpparam.processName}")
            
            if (General.PACKAGE_NAME != lpparam.packageName) return
            
            ApplicationHookConstants.setClassLoader(lpparam.classLoader)
            val apkPath = lpparam.appInfo?.sourceDir
            handleHookLogic(lpparam.classLoader, lpparam.packageName, apkPath, lpparam)
        }

        /**
         * 主Hook逻辑处理
         */
        @SuppressLint("PrivateApi")
        private fun handleHookLogic(
            classLoader: ClassLoader,
            packageName: String,
            apkPath: String?,
            rawParam: Any?
        ) {
            XposedBridge.log("$TAG|handleHookLogic $packageName success!")
            
            if (ApplicationHookConstants.hooked) return
            ApplicationHookConstants.setHooked(true)
            
            // Hook验证码关闭功能
            try {
                CaptchaHook.hookCaptcha(classLoader)
                Log.runtime(TAG, "验证码Hook已启用")
            } catch (t: Throwable) {
                Log.runtime(TAG, "验证码Hook启用失败")
                Log.printStackTrace(TAG, t)
            }
            
            try {
                // 在Hook Application.attach 之前，先 deoptimize LoadedApk.makeApplicationInner
                try {
                    val loadedApkClass = classLoader.loadClass("android.app.LoadedApk")
                    ApplicationHookUtils.deoptimizeMethod(loadedApkClass)
                } catch (t: Throwable) {
                    Log.runtime(TAG, "deoptimize makeApplicationInner err:")
                    Log.printStackTrace(TAG, t)
                }
                
                XposedHelpers.findAndHookMethod(
                    Application::class.java,
                    "attach",
                    Context::class.java,
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val mainHandler = Handler(Looper.getMainLooper())
                            ApplicationHookConstants.setMainHandler(mainHandler)
                            
                            val appContext = param.args[0] as Context
                            ApplicationHookConstants.setAppContext(appContext)

                            registerBroadcastReceiver(appContext)
                            
                            // 设置AlarmSchedulerManager依赖项
                            val alarmManager = ApplicationHookCore.getAlarmManager()
                            alarmManager.mainHandler = mainHandler
                            alarmManager.appContext = appContext
                            alarmManager.initializeAlarmScheduler(appContext)

                            // 初始化支付宝组件帮助类
                            val alipayHelper = AlipayComponentHelper(appContext)
                            alipayHelper.setupKeepAlive()
                            ApplicationHookConstants.setAlipayComponentHelper(alipayHelper)
                            Log.runtime(TAG, "✅ 已初始化支付宝组件帮助类")

                            // 设置支付宝版本号
                            try {
                                val pInfo: PackageInfo? = appContext.packageManager.getPackageInfo(packageName, 0)
                                Log.runtime(TAG, "PackageInfo versionName: ${pInfo?.versionName ?: "pInfo is null"}")
                                
                                val versionName = pInfo?.versionName
                                if (!versionName.isNullOrEmpty()) {
                                    val alipayVersion = AlipayVersion(versionName)
                                    ApplicationHookConstants.setAlipayVersion(alipayVersion)
                                    Log.runtime(TAG, "✅ 支付宝版本号设置成功: ${alipayVersion.versionString}")
                                } else {
                                    Log.runtime(TAG, "⚠️ 无法获取支付宝版本号，pInfo.versionName为空")
                                }
                            } catch (e: Throwable) {
                                Log.runtime(TAG, "❌ 获取支付宝版本号异常: ${e.message}")
                                Log.printStackTrace(TAG, e)
                            }
                            
                            Log.runtime(TAG, "handleLoadPackage alipayVersion: ${ApplicationHookConstants.alipayVersion.versionString}")
                            
                            ApplicationHookUtils.loadNativeLibs(appContext, AssetUtil.checkerDestFile)
                            ApplicationHookUtils.loadNativeLibs(appContext, AssetUtil.dexkitDestFile)
                            
                            val pInfo = try {
                                appContext.packageManager.getPackageInfo(packageName, 0)
                            } catch (e: Exception) {
                                null
                            }
                            
                            if (pInfo?.versionName != null) {
                                if (pInfo.versionName == "10.7.26.8100") {
                                    HookUtil.fuckAccounLimit(classLoader)
                                }
                                Log.runtime(TAG, "${ApplicationHookConstants.alipayVersion.versionString} Not support fuck")
                            }

                            if (BuildConfig.DEBUG && BaseModel.debugMode.value == true) {
                                try {
                                    Log.runtime(TAG, "start service for debug rpc")
                                    ModuleHttpServerManager.startIfNeeded(
                                        8080,
                                        "ET3vB^#td87sQqKaY*eMUJXP",
                                        XposedEnv.processName,
                                        General.PACKAGE_NAME
                                    )
                                } catch (e: Throwable) {
                                    Log.printStackTrace(e)
                                }
                            }
                            
                            super.afterHookedMethod(param)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }

            hookOnResume(classLoader)
            hookServiceLifecycle(classLoader, apkPath)
            HookUtil.hookOtherService(classLoader)
            
            ApplicationHookConstants.setHooked(true)
        }

        /**
         * Hook LauncherActivity.onResume
         */
        private fun hookOnResume(classLoader: ClassLoader) {
            try {
                XposedHelpers.findAndHookMethod(
                    "com.alipay.mobile.quinox.LauncherActivity",
                    classLoader,
                    "onResume",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            Log.runtime(TAG, "hook onResume after start")
                            
                            val currentUid = UserMap.currentUid
                            val resolvedUid = UserSessionProvider.resolveUserId(
                                classLoader = classLoader,
                                retryCount = 2,
                                retryDelayMs = 100L
                            )

                            Log.runtime(TAG, "onResume resolvedUid: $resolvedUid")

                            if (resolvedUid == null) {
                                Log.record(TAG, "onResume:用户未登录")
                                Toast.show("用户未登录")
                                Log.clearCurrentUser()
                                return
                            }
                            
                            if (!ApplicationHookConstants.init) {
                                Log.setCurrentUser(resolvedUid)

                                val initOk = ApplicationHookCore.execOrInit(
                                    forceInit = true,
                                    allowDeferWhenServiceNotReady = true
                                )
                                Log.runtime(TAG, if (initOk) { "execOrInit success" } else { "execOrInit deferred/failed" })
                                return
                            }

                            Log.runtime(TAG, "onResume currentUid: $currentUid")

                            if (resolvedUid != currentUid) {
                                if (currentUid != null) {
                                    // 用户切换日志
                                    Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    Log.record(TAG, "🔄 检测到用户切换")
                                    Log.record(TAG, "   旧用户: $currentUid")
                                    Log.record(TAG, "   新用户: $resolvedUid")
                                    Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    
                                    Log.setCurrentUser(resolvedUid)
                                    UserSessionProvider.recordUserId(resolvedUid)
                                    ApplicationHookCore.initHandler(true)
                                    ApplicationHookConstants.setLastExecTime(0)
                                    
                                    Log.record(TAG, "✅ 用户切换完成，已重新初始化")
                                    Toast.show("用户已切换")
                                    return
                                }
                                HookUtil.hookUser(classLoader)
                            }
                            
                            if (ApplicationHookConstants.offline) {
                                ApplicationHookConstants.exitOffline()
                                val activity = param.thisObject as Activity
                                EntryDispatcher.submitDebounced("onResume") {
                                    ApplicationHookConstants.setPendingTrigger(
                                        ApplicationHookConstants.TriggerInfo(
                                            source = ApplicationHookConstants.TriggerSource.ON_RESUME
                                        )
                                    )
                                    ApplicationHookCore.execHandler()
                                    ApplicationHookConstants.mainHandler?.post { activity.finish() }
                                }
                                
                                return
                            }
                            
                            EntryDispatcher.submitDebounced("onResume") {
                                ApplicationHookConstants.setPendingTrigger(
                                    ApplicationHookConstants.TriggerInfo(
                                        source = ApplicationHookConstants.TriggerSource.ON_RESUME
                                    )
                                )
                                ApplicationHookCore.execOrInit()
                            }
                            Log.runtime(TAG, "hook onResume after end")
                        }
                    }
                )
                Log.runtime(TAG, "hook login successfully")
            } catch (t: Throwable) {
                Log.runtime(TAG, "hook login err")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * Hook Service生命周期
         */
        private fun hookServiceLifecycle(classLoader: ClassLoader, apkPath: String?) {
            // Hook onCreate
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Service",
                    classLoader,
                    "onCreate",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val appService = param.thisObject as Service
                            if (General.CURRENT_USING_SERVICE != appService.javaClass.canonicalName) {
                                return
                            }

                            Log.runtime(TAG, "Service onCreate")
                            val appContext = appService.applicationContext
                            ApplicationHookConstants.setAppContext(appContext)
                            
                            val isok = Detector.isLegitimateEnvironment(appContext)
                            if (isok) {
                                Detector.dangerous(appContext)
                                return
                            }
                            
                            try {
                                DexKitBridge.create(apkPath ?: "").use {
                                    Log.runtime(TAG, "hook dexkit successfully")
                                }
                            } catch (e: Exception) {
                                Log.printStackTrace(TAG, e)
                            }
                            
                            ApplicationHookConstants.setService(appService)
                            
                            val mainTask = BaseTask.newInstance("MAIN_TASK") {
                                try {
                                    val trigger = ApplicationHookConstants.consumePendingTrigger()
                                    val triggerSource = trigger?.source
                                        ?: ApplicationHookConstants.TriggerSource.UNKNOWN

                                    val isAlarmTriggered = ApplicationHookConstants.alarmTriggeredFlag ||
                                        (triggerSource == ApplicationHookConstants.TriggerSource.ALARM)
                                    if (isAlarmTriggered) {
                                        ApplicationHookConstants.setAlarmTriggeredFlag(false)
                                    }

                                    if (!ApplicationHookConstants.init) {
                                        Log.record(TAG, "️🐣跳过执行-未初始化")
                                        return@newInstance
                                    }
                                    
                                    if (!Config.isLoaded()) {
                                        Log.record(TAG, "️⚙跳过执行-用户模块配置未加载")
                                        return@newInstance
                                    }

                                    if (isAlarmTriggered) {
                                        Log.record(TAG, "⏰ 开始新一轮任务 (闹钟触发)")
                                    } else {
                                        val lastExecTime = ApplicationHookConstants.lastExecTime
                                        if (lastExecTime == 0L) {
                                            Log.record(TAG, "▶️ 首次手动触发，开始运行")
                                        } else {
                                            val shouldApplyManualGate =
                                                triggerSource == ApplicationHookConstants.TriggerSource.ON_RESUME
                                            if (shouldApplyManualGate) {
                                                if (BaseModel.manualTriggerAutoSchedule.value == true) {
                                                    Log.record(TAG, "手动APP触发，已开启")
                                                } else {
                                                    Log.record(TAG, "手动APP触发，已关闭")
                                                    return@newInstance
                                                }
                                            }
                                        }
                                    }

                                    val currentTime = System.currentTimeMillis()
                                    val MIN_EXEC_INTERVAL = 2000L
                                    val timeSinceLastExec = currentTime - ApplicationHookConstants.lastExecTime

                                    if (isAlarmTriggered && timeSinceLastExec < MIN_EXEC_INTERVAL) {
                                        Log.record(TAG, "⚠️ 闹钟触发间隔较短(${timeSinceLastExec}ms)，跳过执行，安排下次执行")
                                        val hasRunningTask = ApplicationHookConstants.taskRunnerRunningCount.get() > 0
                                        val hasNextSchedule = ApplicationHookConstants.nextExecutionTime > currentTime
                                        if (!hasRunningTask && !hasNextSchedule) {
                                            ApplicationHookCore.getAlarmManager().scheduleDelayedExecutionWithRetry(
                                                BaseModel.checkInterval.value?.toLong() ?: 180000L,
                                                "跳过执行后的重新调度"
                                            )
                                        }
                                        return@newInstance
                                    }

                                    val currentUid = UserMap.currentUid
                                    val targetUid = UserSessionProvider.resolveUserId(
                                        classLoader = classLoader,
                                        retryCount = 1,
                                        retryDelayMs = 100L
                                    )
                                    
                                    if (targetUid == null || targetUid != currentUid) {
                                        Log.record(TAG, "用户切换或为空，重新登录")
                                        ApplicationHookCore.reLogin()
                                        return@newInstance
                                    }
                                    
                                    ApplicationHookConstants.setLastExecTime(currentTime)
                                    TaskRunnerAdapter().run()
                                    ApplicationHookCore.scheduleNextExecution(currentTime)
                                } catch (e: Exception) {
                                    Log.record(TAG, "❌执行异常")
                                    Log.printStackTrace(TAG, e)
                                } finally {
                                    AlarmScheduler.releaseWakeLock()

                                    if (ApplicationHookConstants.hasPendingAlarmTriggers()) {
                                        ApplicationHookConstants.mainHandler?.postDelayed({
                                            EntryDispatcher.submit("alarm_drain") {
                                                ApplicationHookCore.execOrInit(
                                                    forceInit = true,
                                                    allowDeferWhenServiceNotReady = true
                                                )
                                            }
                                        }, 200L)
                                    }
                                }
                            }
                            
                            ApplicationHookConstants.setMainTask(mainTask)
                            ApplicationHookConstants.dayCalendar = java.util.Calendar.getInstance()
                            
                            if (ApplicationHookCore.initHandler(true)) {
                                ApplicationHookConstants.setInit(true)
                            }
                        }
                    }
                )
                Log.runtime(TAG, "hook service onCreate successfully")
            } catch (t: Throwable) {
                Log.runtime(TAG, "hook service onCreate err")
                Log.printStackTrace(TAG, t)
            }

            // Hook onDestroy
            try {
                XposedHelpers.findAndHookMethod(
                    "android.app.Service",
                    classLoader,
                    "onDestroy",
                    object : XC_MethodHook() {
                        override fun afterHookedMethod(param: MethodHookParam) {
                            val service = param.thisObject as Service
                            if (General.CURRENT_USING_SERVICE != service.javaClass.canonicalName) {
                                return
                            }
                            
                            Log.record(TAG, "支付宝前台服务被销毁")
                            Notify.updateStatusText("支付宝前台服务被销毁")
                            ApplicationHookCore.destroyHandler(true)
                            
                            try {
                                ModuleHttpServerManager.stopIfRunning()
                            } catch (ignore: Throwable) {
                            }
                            
                            EntryDispatcher.submitDebounced("serviceOnDestroyRestart") {
                                ApplicationHookUtils.restartByBroadcast()
                            }
                        }
                    }
                )
            } catch (t: Throwable) {
                Log.runtime(TAG, "hook service onDestroy err")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * 注册广播接收器
         */
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        private fun registerBroadcastReceiver(context: Context) {
            try {
                val appContext = runCatching { context.applicationContext }.getOrNull() ?: context

                if (!isMainProcess(appContext)) {
                    if (!broadcastReceiverSkipLogged) {
                        broadcastReceiverSkipLogged = true
                        val processName = getCurrentProcessName(appContext)
                        Log.runtime(TAG, "非主进程跳过注册Sesame广播: $processName")
                    }
                    return
                }

                val markerKey = "sesame_broadcast_receiver_registered"
                val alreadyRegistered = XposedHelpers.getAdditionalInstanceField(appContext, markerKey) as? Boolean
                if (alreadyRegistered != true && !broadcastReceiverRegistered) {
                    val intentFilter = createIntentFilter()
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        appContext.registerReceiver(
                            broadcastReceiverInstance,
                            intentFilter,
                            Context.RECEIVER_EXPORTED
                        )
                    } else {
                        appContext.registerReceiver(broadcastReceiverInstance, intentFilter)
                    }

                    broadcastReceiverRegistered = true

                    XposedHelpers.setAdditionalInstanceField(appContext, markerKey, true)
                    
                    Log.runtime(TAG, "hook registerBroadcastReceiver successfully")
                }
            } catch (th: Throwable) {
                Log.runtime(TAG, "hook registerBroadcastReceiver err:")
                Log.printStackTrace(TAG, th)
            }
        }

        /**
         * 创建IntentFilter
         */
        private fun createIntentFilter(): IntentFilter {
            return IntentFilter().apply {
                addAction("com.eg.android.AlipayGphone.sesame.restart")
                addAction("com.eg.android.AlipayGphone.sesame.execute")
                addAction("com.eg.android.AlipayGphone.sesame.reLogin")
                addAction("com.eg.android.AlipayGphone.sesame.status")
                addAction("com.eg.android.AlipayGphone.sesame.rpctest")
                addCategory("fansirsqi.xposed.sesame.ALARM_CATEGORY")
            }
        }
    }

    /**
     * 支付宝广播接收器
     */
    class AlipayBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val appContext = runCatching { context.applicationContext }.getOrNull() ?: context
                if (!isMainProcess(appContext)) {
                    return
                }
                val action = intent.action
                Log.runtime(TAG, "Alipay got Broadcast $action intent:$intent")
                
                when (action) {
                    "com.eg.android.AlipayGphone.sesame.restart" -> {
                        if (BaseModel.debugMode.value == true) {
                            Log.printStack(TAG)
                        }
                        val configReload = intent.getBooleanExtra("configReload", false)
                        EntryDispatcher.submitDebounced("restart") { ApplicationHookCore.initHandler(!configReload) }
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.execute" -> {
                        if (BaseModel.debugMode.value == true) {
                            Log.printStack(TAG)
                        }

                        val isAlarmTriggered = intent.getBooleanExtra("alarm_triggered", false)
                        if (isAlarmTriggered) {
                            ApplicationHookConstants.setAlarmTriggeredFlag(true)
                        }

                        val requestCode = intent.getIntExtra("request_code", -1)
                        val isBackupAlarm = intent.getBooleanExtra("is_backup_alarm", false)
                        ApplicationHookConstants.setPendingTrigger(
                            ApplicationHookConstants.TriggerInfo(
                                source = if (isAlarmTriggered) {
                                    ApplicationHookConstants.TriggerSource.ALARM
                                } else {
                                    ApplicationHookConstants.TriggerSource.EXECUTE_BROADCAST
                                },
                                requestCode = requestCode,
                                isBackupAlarm = isBackupAlarm
                            )
                        )

                        if (isAlarmTriggered) {
                            EntryDispatcher.submit("execute_alarm") {
                                ApplicationHookCore.execOrInit(
                                    forceInit = true,
                                    allowDeferWhenServiceNotReady = true
                                )
                            }
                        } else {
                            EntryDispatcher.submitDebounced("execute") {
                                ApplicationHookCore.execOrInit()
                            }
                        }
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.reLogin" -> {
                        if (BaseModel.debugMode.value == true) {
                            Log.printStack(TAG)
                        }
                        EntryDispatcher.submitDebounced("reLogin") { ApplicationHookCore.reLogin() }
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.status" -> {
                        if (BaseModel.debugMode.value == true) {
                            Log.printStack(TAG)
                        }
                        if (ViewAppInfo.getRunType() == RunType.DISABLE) {
                            val replyIntent = Intent("fansirsqi.xposed.sesame.status").apply {
                                putExtra("EXTRA_RUN_TYPE", RunType.ACTIVE.nickName)
                                setPackage(General.MODULE_PACKAGE_NAME)
                            }
                            context.sendBroadcast(replyIntent)
                            Log.system(TAG, "Replied with status: ${RunType.ACTIVE.nickName}")
                        }
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.rpctest" -> {
                        Thread {
                            try {
                                val method = intent.getStringExtra("method")
                                val data = intent.getStringExtra("data")
                                val type = intent.getStringExtra("type")
                                Log.runtime(TAG, "收到RPC测试请求 - Method: $method, Type: $type")
                                
                                if (method != null && data != null && type != null) {
                                    if (type == "Rpc" && !BuildConfig.DEBUG && BaseModel.debugMode.value != true) {
                                        Log.runtime(TAG, "已拦截 Rpc 调试请求：非 Debug 构建且未开启 debugMode")
                                        return@Thread
                                    }
                                    val rpcInstance = DebugRpc()
                                    rpcInstance.start(method, data, type)
                                } else {
                                    Log.runtime(TAG, "RPC测试请求参数不完整")
                                }
                            } catch (th: Throwable) {
                                Log.runtime(TAG, "sesame 测试RPC请求失败:")
                                Log.printStackTrace(TAG, th)
                            }
                        }.start()
                    }
                    
                    else -> {
                        val requestCode = intent.getIntExtra("request_code", -1)
                        val isBackupAlarm = intent.getBooleanExtra("is_backup_alarm", false)

                        ApplicationHookConstants.setPendingTrigger(
                            ApplicationHookConstants.TriggerInfo(
                                source = ApplicationHookConstants.TriggerSource.ALARM,
                                requestCode = requestCode,
                                isBackupAlarm = isBackupAlarm
                            )
                        )

                        EntryDispatcher.submit("execute_alarm") {
                            ApplicationHookCore.execOrInit(
                                forceInit = true,
                                allowDeferWhenServiceNotReady = true
                            )
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "AlipayBroadcastReceiver.onReceive err:", t)
            }
        }
    }
}
