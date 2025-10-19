package fansirsqi.xposed.sesame.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import fansirsqi.xposed.sesame.util.Log

/**
 * 支付宝自动启动器（简化安全版本）
 * 
 * 功能：在应用启动时自动后台唤醒支付宝进行能量收取
 * 注意：此类不使用任何Hook方法，确保在应用启动时可以安全使用
 * 
 * @author 芝麻粒优化版
 * @date 2025-10-19
 */
object AlipayAutoLauncher {
    private const val TAG = "AlipayAutoLauncher"
    
    // 支付宝包名
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    
    // 蚂蚁森林AppId
    private const val ANTFOREST_APPID = "60000002"
    
    // 标记是否已经自动启动过（避免重复启动）
    @Volatile
    private var hasAutoLaunched = false
    
    /**
     * 在应用启动时自动唤醒支付宝（安全版本）
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
            
            Log.record(TAG, "🚀 准备自动唤醒支付宝...")
            
            // 使用支付宝的DeepLink跳转到蚂蚁森林
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse("alipays://platformapi/startapp?appId=$ANTFOREST_APPID")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            
            // 延迟3秒启动，避免应用启动时卡顿
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                try {
                    context.startActivity(intent)
                    Log.record(TAG, "✅ 已发送唤醒支付宝指令，3秒后将打开蚂蚁森林")
                    hasAutoLaunched = true
                } catch (e: Exception) {
                    Log.error(TAG, "启动支付宝失败: ${e.message}")
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
            context.packageManager.getPackageInfo(ALIPAY_PACKAGE, 0)
            true
        } catch (e: Exception) {
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
