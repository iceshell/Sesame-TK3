package fansirsqi.xposed.sesame.util

import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import fansirsqi.xposed.sesame.util.Log

/**
 * 支付宝自动启动器（优化版本）
 * 
 * 功能：在应用启动时自动后台唤醒支付宝进行能量收取
 * 优化：
 * 1. 使用ADB命令启动支付宝
 * 2. 检查支付宝是否已运行，避免重复启动
 * 3. 优化错误提示逻辑
 * 
 * @author 芝麻粒优化版 v2
 * @date 2025-10-19
 */
object AlipayAutoLauncher {
    private const val TAG = "AlipayAutoLauncher"
    
    // 支付宝包名
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    
    // 支付宝启动Activity
    private const val ALIPAY_SCHEME_ACTIVITY = "com.alipay.mobile.framework.service.common.SchemeStartActivity"
    
    // 蚂蚁森林AppId
    private const val ANTFOREST_APPID = "60000002"
    
    // 标记是否已经自动启动过（避免重复启动）
    @Volatile
    private var hasAutoLaunched = false
    
    /**
     * 在应用启动时自动唤醒支付宝（优化版本）
     * 
     * 此方法应该在Activity的onResume中调用，而不是Application.onCreate
     * 
     * @param context Activity的Context
     * @param forceRelaunch 是否强制重新启动（默认false，只启动一次）
     */
    @JvmStatic
    fun autoLaunchOnAppStart(context: Context?, forceRelaunch: Boolean = false) {
        // 如果已经启动过且不强制重启，直接返回
        if (hasAutoLaunched && !forceRelaunch) {
            Log.debug(TAG, "已自动启动过支付宝，跳过")
            return
        }
        
        if (context == null) {
            Log.error(TAG, "Context为null，无法启动支付宝")
            return
        }
        
        try {
            // 检查支付宝是否已安装
            if (!isAlipayInstalled(context)) {
                Log.record(TAG, "⚠️ 支付宝未安装，无法自动唤醒")
                hasAutoLaunched = true // 标记为已尝试，避免重复检查
                return
            }
            
            // 检查支付宝是否已在运行
            if (isAlipayRunning(context)) {
                Log.record(TAG, "✅ 支付宝已在运行，跳过启动")
                hasAutoLaunched = true
                return
            }
            
            Log.record(TAG, "🚀 准备后台启动支付宝...")
            
            // 使用AM命令后台启动支付宝（推荐方式）
            val intent = Intent().apply {
                component = ComponentName(ALIPAY_PACKAGE, ALIPAY_SCHEME_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                // 不添加FLAG_ACTIVITY_BROUGHT_TO_FRONT，让它在后台启动
            }
            
            // 延迟3秒启动，避免应用启动时卡顿
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    context.startActivity(intent)
                    Log.record(TAG, "✅ 已后台启动支付宝（AM命令）")
                    hasAutoLaunched = true
                } catch (e: Exception) {
                    Log.error(TAG, "后台启动支付宝失败: ${e.message}")
                    // 降级方案：使用DeepLink启动
                    try {
                        val deepLinkIntent = Intent(Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("alipays://platformapi/startapp?appId=$ANTFOREST_APPID")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }
                        context.startActivity(deepLinkIntent)
                        Log.record(TAG, "✅ 已使用DeepLink启动支付宝（降级方案）")
                        hasAutoLaunched = true
                    } catch (ex: Exception) {
                        Log.error(TAG, "DeepLink启动也失败: ${ex.message}")
                    }
                }
            }, 3000)
            
        } catch (e: Exception) {
            Log.error(TAG, "自动唤醒支付宝异常: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }
    
    /**
     * 检查支付宝是否已安装
     */
    private fun isAlipayInstalled(context: Context): Boolean {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(ALIPAY_PACKAGE, 0)
            Log.debug(TAG, "支付宝已安装，版本: ${packageInfo.versionName}")
            true
        } catch (e: Exception) {
            Log.debug(TAG, "支付宝未安装")
            false
        }
    }
    
    /**
     * 检查支付宝是否正在运行
     */
    private fun isAlipayRunning(context: Context): Boolean {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            // Android 5.0+ 使用不同的方法
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val runningAppProcesses = activityManager.runningAppProcesses
                if (runningAppProcesses != null) {
                    for (processInfo in runningAppProcesses) {
                        if (processInfo.processName == ALIPAY_PACKAGE) {
                            Log.debug(TAG, "支付宝进程正在运行")
                            return true
                        }
                    }
                }
            } else {
                // Android 5.0以下使用旧方法
                @Suppress("DEPRECATION")
                val runningTasks = activityManager.getRunningTasks(100)
                for (taskInfo in runningTasks) {
                    if (taskInfo.baseActivity?.packageName == ALIPAY_PACKAGE) {
                        Log.debug(TAG, "支付宝任务正在运行")
                        return true
                    }
                }
            }
            
            Log.debug(TAG, "支付宝未运行")
            false
        } catch (e: Exception) {
            Log.error(TAG, "检查支付宝运行状态失败: ${e.message}")
            false
        }
    }
    
    /**
     * 重置自动启动标志（用于测试或重新启动）
     */
    @JvmStatic
    fun resetAutoLaunchFlag() {
        hasAutoLaunched = false
        Log.debug(TAG, "已重置自动启动标志")
    }
    
    /**
     * 手动启动支付宝到蚂蚁森林（立即启动，无延迟）
     */
    @JvmStatic
    fun launchAlipayToForest(context: Context?) {
        if (context == null) {
            Log.error(TAG, "Context为null，无法启动支付宝")
            return
        }
        
        try {
            if (!isAlipayInstalled(context)) {
                Log.record(TAG, "⚠️ 支付宝未安装")
                return
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("alipays://platformapi/startapp?appId=$ANTFOREST_APPID")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            context.startActivity(intent)
            Log.record(TAG, "✅ 已启动支付宝到蚂蚁森林")
        } catch (e: Exception) {
            Log.error(TAG, "手动启动支付宝失败: ${e.message}")
        }
    }
}
