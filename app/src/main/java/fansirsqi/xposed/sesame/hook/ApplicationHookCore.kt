package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.data.RunType
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.ViewAppInfo
import fansirsqi.xposed.sesame.hook.rpc.bridge.NewRpcBridge
import fansirsqi.xposed.sesame.hook.rpc.bridge.OldRpcBridge
import fansirsqi.xposed.sesame.hook.rpc.intervallimit.RpcIntervalLimit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.NetworkUtils
import fansirsqi.xposed.sesame.util.Notify
import fansirsqi.xposed.sesame.util.PermissionUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.util.Calendar

/**
 * ApplicationHook 核心逻辑方法
 * 第三部分迁移：初始化、任务调度、定时器等核心功能
 */
object ApplicationHookCore {
    private const val TAG = "ApplicationHook"
    
    // AlarmScheduler管理器
    private val alarmManager = AlarmSchedulerManager()

    /**
     * 获取AlarmScheduler管理器实例
     */
    @JvmStatic
    fun getAlarmManager(): AlarmSchedulerManager = alarmManager

    /**
     * 调度定时执行
     */
    @JvmStatic
    fun scheduleNextExecution(lastExecTime: Long) {
        try {
            // 检查长时间未执行的情况
            checkInactiveTime()
            
            val checkInterval = BaseModel.checkInterval.value ?: 180000
            val execAtTimeList = BaseModel.execAtTimeList.value
            
            if (execAtTimeList?.contains("-1") == true) {
                Log.record(TAG, "定时执行未开启")
                return
            }

            var delayMillis = checkInterval.toLong()
            var targetTime = 0L

            try {
                execAtTimeList?.let { timeList ->
                    val lastExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime)
                    val nextExecTimeCalendar = TimeUtil.getCalendarByTimeMillis(lastExecTime + checkInterval)
                    
                    for (execAtTime in timeList) {
                        val execAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(execAtTime)
                        if (execAtTimeCalendar != null && 
                            lastExecTimeCalendar.compareTo(execAtTimeCalendar) < 0 && 
                            nextExecTimeCalendar.compareTo(execAtTimeCalendar) > 0) {
                            Log.record(TAG, "设置定时执行:$execAtTime")
                            targetTime = execAtTimeCalendar.timeInMillis
                            delayMillis = targetTime - lastExecTime
                            break
                        }
                    }
                }
            } catch (e: Exception) {
                Log.runtime(TAG, "execAtTime err:：${e.message}")
                Log.printStackTrace(TAG, e)
            }

            // 使用统一的闹钟调度器
            ApplicationHookConstants.nextExecutionTime = if (targetTime > 0) targetTime else (lastExecTime + delayMillis)
            alarmManager.scheduleExactExecution(delayMillis, ApplicationHookConstants.nextExecutionTime)
        } catch (e: Exception) {
            Log.runtime(TAG, "scheduleNextExecution：${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 检查长时间未执行的情况，如果超过阈值则自动重启
     */
    @JvmStatic
    fun checkInactiveTime() {
        try {
            val lastExecTime = ApplicationHookConstants.lastExecTime
            if (lastExecTime == 0L) {
                return // 首次执行，跳过检查
            }
            
            val currentTime = System.currentTimeMillis()
            val inactiveTime = currentTime - lastExecTime
            
            // 检查是否经过了0点
            val lastExecCalendar = Calendar.getInstance().apply {
                timeInMillis = lastExecTime
            }
            val currentCalendar = Calendar.getInstance().apply {
                timeInMillis = currentTime
            }
            
            val crossedMidnight = lastExecCalendar.get(Calendar.DAY_OF_YEAR) != currentCalendar.get(Calendar.DAY_OF_YEAR) ||
                    lastExecCalendar.get(Calendar.YEAR) != currentCalendar.get(Calendar.YEAR)
            
            // 如果超过最大不活动时间或者跨越了0点但已经过了一段时间
            if (inactiveTime > ApplicationHookConstants.MAX_INACTIVE_TIME ||
                (crossedMidnight && currentCalendar.get(Calendar.HOUR_OF_DAY) >= 1)) {
                Log.record(TAG, "⚠️ 检测到长时间未执行(${inactiveTime / 60000}分钟)，可能跨越0点，尝试重新登录")
                reLogin()
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "checkInactiveTime err:${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 设置定时唤醒
     */
    @JvmStatic
    fun setWakenAtTimeAlarm() {
        setWakenAtTimeAlarmWithRetry(0)
    }

    /**
     * 设置定时唤醒（带重试机制）
     */
    @JvmStatic
    fun setWakenAtTimeAlarmWithRetry(retryCount: Int) {
        try {
            // 检查AlarmScheduler是否已初始化
            if (!alarmManager.isAlarmSchedulerAvailable) {
                if (retryCount < 3) {
                    // 延迟重试，最多3次
                    val currentRetry = retryCount + 1
                    Log.runtime(TAG, "AlarmScheduler未初始化，延迟${currentRetry * 2}秒后重试设置定时唤醒 (第${currentRetry}次)")
                    ApplicationHookConstants.mainHandler?.postDelayed(
                        { setWakenAtTimeAlarmWithRetry(currentRetry) },
                        (currentRetry * 2000).toLong()
                    )
                } else {
                    Log.error(TAG, "AlarmScheduler初始化超时，放弃设置定时唤醒")
                }
                return
            }

            val wakenAtTimeList = BaseModel.wakenAtTimeList.value
            if (wakenAtTimeList?.contains("-1") == true) {
                Log.record(TAG, "定时唤醒未开启")
                return
            }

            // 清理旧唤醒闹钟
            unsetWakenAtTimeAlarm()

            // 设置0点唤醒
            val calendar = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val success = alarmManager.scheduleWakeupAlarm(calendar.timeInMillis, 0, true)
            if (success) {
                Log.record(TAG, "⏰ 设置0点定时唤醒成功")
            } else {
                Log.runtime(TAG, "⏰ 设置0点定时唤醒失败")
            }

            // 设置自定义时间点唤醒
            wakenAtTimeList?.let { timeList ->
                if (timeList.isNotEmpty()) {
                    val nowCalendar = Calendar.getInstance()
                    var successCount = 0
                    
                    for (i in 1 until timeList.size) {
                        try {
                            val wakenAtTime = timeList[i]
                            val wakenAtTimeCalendar = TimeUtil.getTodayCalendarByTimeStr(wakenAtTime)
                            
                            if (wakenAtTimeCalendar != null && wakenAtTimeCalendar.compareTo(nowCalendar) > 0) {
                                val customSuccess = alarmManager.scheduleWakeupAlarm(
                                    wakenAtTimeCalendar.timeInMillis,
                                    i,
                                    false
                                )
                                if (customSuccess) {
                                    successCount++
                                    Log.record(TAG, "⏰ 设置定时唤醒成功: $wakenAtTime")
                                }
                            }
                        } catch (e: Exception) {
                            Log.runtime(TAG, "设置自定义唤醒时间失败: ${e.message}")
                        }
                    }
                    
                    if (successCount > 0) {
                        Log.record(TAG, "⏰ 共设置了 $successCount 个自定义定时唤醒")
                    }
                }
            }
        } catch (e: Exception) {
            Log.runtime(TAG, "setWakenAtTimeAlarm err:")
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 取消所有定时唤醒
     */
    @JvmStatic
    fun unsetWakenAtTimeAlarm() {
        if (alarmManager.isAlarmSchedulerAvailable) {
            Log.debug(TAG, "取消定时唤醒将由destroyHandler统一处理")
        }
    }

    /**
     * 初始化处理程序
     */
    @JvmStatic
    @Synchronized
    fun initHandler(force: Boolean): Boolean {
        try {
            if (ApplicationHookConstants.init && !force) {
                Log.runtime(TAG, "initHandler: 已初始化，跳过")
                return true
            }

            if (ApplicationHookConstants.init) {
                Log.runtime(TAG, "initHandler: 强制重新初始化")
                destroyHandler(true)
            }

            // AlarmScheduler 确保可用
            val appContext = ApplicationHookConstants.appContext
            if (!alarmManager.isAlarmSchedulerAvailable && appContext != null) {
                alarmManager.initializeAlarmScheduler(appContext)
            }

            Model.initAllModel()

            if (force) {
                val classLoader = ApplicationHookConstants.classLoader ?: return false
                val userId = UserSessionProvider.resolveUserId(
                    classLoader = classLoader,
                    retryCount = 3,
                    retryDelayMs = 150L
                )

                if (userId.isNullOrEmpty()) {
                    Log.record(TAG, "initHandler: 用户未登录")
                    Toast.show("用户未登录")
                    return false
                }

                HookUtil.hookUser(classLoader)
                
                // 初始化日志 - 开始
                Log.record(TAG, "━━━━━━━━━━ 初始化开始 ━━━━━━━━━━")
                Log.record(TAG, "芝麻粒-TK 开始初始化...")
                Log.record(TAG, "⚙️模块版本：${ApplicationHookConstants.modelVersion}")
                Log.record(TAG, "📦应用版本：${ApplicationHookConstants.alipayVersion.versionString}")
                Log.record(TAG, "📶网络类型：${NetworkUtils.getNetworkType()}")
                Log.record(TAG, "👤用户ID：$userId")
                Log.record(TAG, "🕐初始化时间：${TimeUtil.getCommonDate(System.currentTimeMillis())}")
                Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

                Config.load(userId)
                if (!Config.isLoaded()) {
                    Log.record(TAG, "用户模块配置加载失败")
                    Toast.show("用户模块配置加载失败")
                    return false
                }

                // 闹钟权限检查（非阻塞性）
                if (!PermissionUtil.checkAlarmPermissions()) {
                    Log.record(TAG, "⚠️ 支付宝无闹钟权限（将影响定时任务执行）")
                    ApplicationHookConstants.mainHandler?.postDelayed({
                        if (!PermissionUtil.checkOrRequestAlarmPermissions(appContext!!)) {
                            Toast.show("请授予支付宝使用闹钟权限以启用定时任务")
                        }
                    }, 2000)
                    // 不阻止初始化继续，允许手动触发任务
                }

                // 后台运行权限检查
                if (!ApplicationHookConstants.init && !PermissionUtil.checkBatteryPermissions()) {
                    Log.record(TAG, "支付宝无始终在后台运行权限")
                    ApplicationHookConstants.mainHandler?.postDelayed({
                        if (!PermissionUtil.checkOrRequestBatteryPermissions(appContext!!)) {
                            Toast.show("请授予支付宝始终在后台运行权限")
                        }
                    }, 2000)
                }

                // 检查service是否可用
                val service = ApplicationHookConstants.service
                val notifyContext = service ?: appContext
                if (service == null) {
                    Log.record(TAG, "initHandler: service为空，通知能力将降级")
                    Toast.show("服务未就绪，通知可能不可用")
                }

                if (notifyContext != null) {
                    Notify.start(notifyContext)

                    try {
                        val pm = notifyContext.getSystemService(Context.POWER_SERVICE) as PowerManager
                        val wakeLockTag = service?.javaClass?.name ?: TAG
                        val wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag)
                        wakeLock.acquire(10 * 60 * 1000L) // 10分钟
                        ApplicationHookConstants.setWakeLock(wakeLock)
                    } catch (t: Throwable) {
                        Log.record(TAG, "唤醒锁申请失败:")
                        Log.printStackTrace(t)
                    }
                }

                setWakenAtTimeAlarm()

                synchronized(ApplicationHookConstants.rpcBridgeLock) {
                    val rpcBridge = if (BaseModel.newRpc.value == true) {
                        NewRpcBridge()
                    } else {
                        OldRpcBridge()
                    }
                    rpcBridge.load()
                    ApplicationHookConstants.setRpcBridge(rpcBridge)
                    ApplicationHookConstants.setRpcVersion(rpcBridge.getVersion())
                }

                Model.bootAllModel(classLoader)
                Status.load(userId)
                DataStore.init(Files.CONFIG_DIR)
                updateDay(userId)

                // 抓包Hook
                if (BaseModel.newRpc.value == true && BaseModel.debugMode.value == true) {
                    try {
                        Log.runtime(TAG, "开始初始化RPC抓包Hook...")
                        HookUtil.hookRpcBridgeExtension(
                            classLoader,
                            BaseModel.sendHookData.value ?: false,
                            BaseModel.sendHookDataUrl.value ?: ""
                        )
                        HookUtil.hookDefaultBridgeCallback(classLoader)
                        Log.runtime(TAG, "✅ RPC抓包Hook初始化成功")
                    } catch (t: Throwable) {
                        Log.runtime(TAG, "❌ RPC抓包Hook初始化失败: ${t.message}")
                        Log.printStackTrace(TAG, t)
                    }
                }
                
                // 初始化日志 - 完成
                Log.record(TAG, "━━━━━━━━━━ 初始化完成 ━━━━━━━━━━")
                Log.record(TAG, "✅ 芝麻粒-TK 加载成功✨")
                Log.record(TAG, "[SESAME_TK_READY]")
                Log.runtime(TAG, "[SESAME_TK_READY]")
                ModuleStatusReporter.requestUpdate("ready")
                Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                Toast.show("芝麻粒-TK 加载成功✨")
            }

            ApplicationHookConstants.exitOffline()
            execHandler()
            ApplicationHookConstants.setInit(true)
            return true
        } catch (th: Throwable) {
            Log.printStackTrace(TAG, "startHandler", th)
            Toast.show("芝麻粒加载失败 🎃")
            return false
        }
    }

    /**
     * 销毁处理程序
     */
    @JvmStatic
    @Synchronized
    fun destroyHandler(force: Boolean) {
        try {
            if (force) {
                val service = ApplicationHookConstants.service
                if (service != null) {
                    stopHandler()
                    BaseModel.destroyData()
                    try {
                        Status.flushPendingSave()
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, "flushPendingSave err", t)
                    }
                    try {
                        DataStore.flushPendingSave()
                        DataStore.shutdown()
                    } catch (t: Throwable) {
                        Log.printStackTrace(TAG, "DataStore shutdown err", t)
                    }
                    Status.unload()
                    Notify.stop()
                    RpcIntervalLimit.clearIntervalLimit()
                    Config.unload()
                    UserMap.unload()
                }
                
                // 清理AlarmScheduler协程资源
                alarmManager.cleanupAlarmScheduler()
                
                val wakeLock = ApplicationHookConstants.wakeLock
                wakeLock?.release()
                ApplicationHookConstants.setWakeLock(null)
                
                synchronized(ApplicationHookConstants.rpcBridgeLock) {
                    val rpcBridge = ApplicationHookConstants.rpcBridge
                    if (rpcBridge != null) {
                        ApplicationHookConstants.setRpcVersion(null)
                        rpcBridge.unload()
                        ApplicationHookConstants.setRpcBridge(null)
                    }
                }

                ApplicationHookConstants.shutdownEntryExecutor()
            } else {
                ModelTask.stopAllTask()
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "stopHandler err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 停止处理程序
     */
    @JvmStatic
    fun stopHandler() {
        ApplicationHookConstants.mainTask?.stopTask()
        ModelTask.stopAllTask()
    }

    /**
     * 执行处理程序
     */
    @JvmStatic
    fun execHandler() {
        if (ApplicationHookConstants.taskRunnerRunningCount.get() > 0) {
            Log.runtime(TAG, "execHandler: 检测到任务执行中，跳过本次触发")
            return
        }

        val mainTask = ApplicationHookConstants.mainTask
        if (mainTask == null) {
            Log.runtime(TAG, "⚠️ mainTask未初始化，跳过执行")
            return
        }

        if (mainTask.thread?.isAlive == true) {
            Log.runtime(TAG, "execHandler: mainTask线程运行中，跳过本次触发")
            return
        }
        
        // 任务执行前唤醒支付宝进程
        ApplicationHookConstants.alipayComponentHelper?.let { helper ->
            try {
                helper.wakeupAlipayLite()
            } catch (e: Exception) {
                Log.runtime(TAG, "唤醒支付宝进程失败: ${e.message}")
            }
        }
        
        mainTask.startTask(false)
    }

    @JvmStatic
    fun execOrInit(
        forceInit: Boolean = true,
        allowDeferWhenServiceNotReady: Boolean = false
    ): Boolean {
        if (ApplicationHookConstants.init) {
            execHandler()
            return true
        }

        if (allowDeferWhenServiceNotReady && ApplicationHookConstants.service == null) {
            Log.runtime(TAG, "execOrInit: service未就绪，等待下次触发")
            return false
        }

        return initHandler(forceInit)
    }

    /**
     * 更新日期
     */
    @JvmStatic
    fun updateDay(userId: String) {
        val nowCalendar = Calendar.getInstance()
        try {
            var dayCalendar = ApplicationHookConstants.dayCalendar
            
            val nowYear = nowCalendar.get(Calendar.YEAR)
            val nowMonth = nowCalendar.get(Calendar.MONTH)
            val nowDay = nowCalendar.get(Calendar.DAY_OF_MONTH)
            
            if (dayCalendar.get(Calendar.YEAR) != nowYear || 
                dayCalendar.get(Calendar.MONTH) != nowMonth || 
                dayCalendar.get(Calendar.DAY_OF_MONTH) != nowDay) {
                
                dayCalendar = nowCalendar.clone() as Calendar
                dayCalendar.apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }
                ApplicationHookConstants.dayCalendar = dayCalendar
                Log.record(TAG, "日期更新为：$nowYear-${nowMonth + 1}-$nowDay")
                setWakenAtTimeAlarm()
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }

        try {
            Status.save(nowCalendar)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 重新登录
     */
    @JvmStatic
    fun reLogin() {
        ApplicationHookConstants.mainHandler?.post {
            val reLoginCount = ApplicationHookConstants.reLoginCount
            val delayMillis = if (reLoginCount.get() < 5) {
                reLoginCount.getAndIncrement() * 5000L
            } else {
                ApplicationHookConstants.getOfflineCooldownMs()
            }

            // 使用统一的闹钟调度器
            alarmManager.scheduleDelayedExecution(delayMillis)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setClassName(General.PACKAGE_NAME, General.CURRENT_USING_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            ApplicationHookConstants.enterOffline(delayMillis, "relogin", "ApplicationHookCore.reLogin")
            ApplicationHookConstants.appContext?.startActivity(intent)
        }
    }
}
