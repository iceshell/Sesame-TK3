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
import java.util.Calendar

/**
 * ApplicationHook 入口类
 * 第四部分迁移：Hook入口、广播接收器和主要Hook逻辑
 */
class ApplicationHookEntry {
    companion object {
        private const val TAG = "ApplicationHook"
        
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

                            if (BuildConfig.DEBUG) {
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
                            
                            val targetUid = ApplicationHookUtils.getUserId()
                            Log.runtime(TAG, "onResume targetUid: $targetUid")
                            
                            if (targetUid == null) {
                                Log.record(TAG, "onResume:用户未登录")
                                Toast.show("用户未登录")
                                Log.clearCurrentUser()
                                return
                            }
                            
                            if (!ApplicationHookConstants.init) {
                                Log.setCurrentUser(targetUid)
                                
                                if (ApplicationHookConstants.service == null) {
                                    Log.runtime(TAG, "onResume: service未就绪，等待下次触发")
                                    return
                                }
                                
                                if (ApplicationHookCore.initHandler(true)) {
                                    ApplicationHookConstants.setInit(true)
                                    Log.runtime(TAG, "initHandler success")
                                } else {
                                    Log.runtime(TAG, "initHandler failed")
                                }
                                return
                            }
                            
                            val currentUid = UserMap.currentUid
                            Log.runtime(TAG, "onResume currentUid: $currentUid")
                            
                            if (targetUid != currentUid) {
                                if (currentUid != null) {
                                    // 用户切换日志
                                    Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    Log.record(TAG, "🔄 检测到用户切换")
                                    Log.record(TAG, "   旧用户: $currentUid")
                                    Log.record(TAG, "   新用户: $targetUid")
                                    Log.record(TAG, "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                                    
                                    Log.setCurrentUser(targetUid)
                                    ApplicationHookCore.initHandler(true)
                                    ApplicationHookConstants.setLastExecTime(0)
                                    
                                    Log.record(TAG, "✅ 用户切换完成，已重新初始化")
                                    Toast.show("用户已切换")
                                    return
                                }
                                HookUtil.hookUser(classLoader)
                            }
                            
                            if (ApplicationHookConstants.offline) {
                                ApplicationHookConstants.offline = false
                                ApplicationHookCore.execHandler()
                                (param.thisObject as Activity).finish()
                                Log.runtime(TAG, "Activity reLogin")
                                return
                            }
                            
                            ApplicationHookCore.execHandler()
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
                                    val isAlarmTriggered = ApplicationHookConstants.alarmTriggeredFlag
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
                                            if (BaseModel.manualTriggerAutoSchedule.value == true) {
                                                Log.record(TAG, "手动APP触发，已开启")
                                                TaskRunnerAdapter().run()
                                            } else {
                                                Log.record(TAG, "手动APP触发，已关闭")
                                                return@newInstance
                                            }
                                        }
                                    }

                                    val currentTime = System.currentTimeMillis()
                                    val MIN_EXEC_INTERVAL = 2000L
                                    val timeSinceLastExec = currentTime - ApplicationHookConstants.lastExecTime

                                    if (isAlarmTriggered && timeSinceLastExec < MIN_EXEC_INTERVAL) {
                                        Log.record(TAG, "⚠️ 闹钟触发间隔较短(${timeSinceLastExec}ms)，跳过执行，安排下次执行")
                                        ApplicationHookCore.getAlarmManager().scheduleDelayedExecutionWithRetry(
                                            BaseModel.checkInterval.value?.toLong() ?: 180000L,
                                            "跳过执行后的重新调度"
                                        )
                                        return@newInstance
                                    }

                                    val currentUid = UserMap.currentUid
                                    val targetUid = HookUtil.getUserId(classLoader)
                                    
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
                            
                            ApplicationHookUtils.restartByBroadcast()
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
                val intentFilter = createIntentFilter()
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.registerReceiver(
                        AlipayBroadcastReceiver(),
                        intentFilter,
                        Context.RECEIVER_EXPORTED
                    )
                } else {
                    context.registerReceiver(AlipayBroadcastReceiver(), intentFilter)
                }
                
                Log.runtime(TAG, "hook registerBroadcastReceiver successfully")
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
            }
        }
    }

    /**
     * 支付宝广播接收器
     */
    class AlipayBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val action = intent.action
                Log.runtime(TAG, "Alipay got Broadcast $action intent:$intent")
                
                when (action) {
                    "com.eg.android.AlipayGphone.sesame.restart" -> {
                        Log.printStack(TAG)
                        val configReload = intent.getBooleanExtra("configReload", false)
                        Thread {
                            ApplicationHookCore.initHandler(!configReload)
                        }.start()
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.execute" -> {
                        Log.printStack(TAG)
                        if (intent.getBooleanExtra("alarm_triggered", false)) {
                            ApplicationHookConstants.setAlarmTriggeredFlag(true)
                        }
                        Thread {
                            ApplicationHookCore.initHandler(false)
                        }.start()
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.reLogin" -> {
                        Log.printStack(TAG)
                        Thread { ApplicationHookCore.reLogin() }.start()
                    }
                    
                    "com.eg.android.AlipayGphone.sesame.status" -> {
                        Log.printStack(TAG)
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
                        // 处理闹钟相关的广播
                        val alarmManager = ApplicationHookCore.getAlarmManager()
                        if (alarmManager.isAlarmSchedulerAvailable) {
                            val requestCode = intent.getIntExtra("request_code", -1)
                            val alarmThread = Thread {
                                alarmManager.handleAlarmTrigger(requestCode)
                            }.apply {
                                name = "AlarmTriggered_$requestCode"
                            }
                            alarmThread.start()
                            Log.record(TAG, "闹钟广播触发，创建处理线程: ${alarmThread.name}")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "AlipayBroadcastReceiver.onReceive err:", t)
            }
        }

        companion object {
            private const val TAG = "ApplicationHook"
        }
    }
}
