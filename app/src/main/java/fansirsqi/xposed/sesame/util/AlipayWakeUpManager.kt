package fansirsqi.xposed.sesame.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import fansirsqi.xposed.sesame.hook.ApplicationHook
import fansirsqi.xposed.sesame.util.Log

/**
 * 支付宝唤醒管理器
 * 
 * 功能：芝麻粒打开时自动后台唤醒支付宝进行能量收取
 * 
 * @author 芝麻粒优化版
 * @date 2025-10-19
 */
object AlipayWakeUpManager {
    private const val TAG = "AlipayWakeUpManager"
    
    // 支付宝包名
    private const val ALIPAY_PACKAGE = "com.eg.android.AlipayGphone"
    
    // 支付宝蚂蚁森林Activity
    private const val FOREST_ACTIVITY = "com.alipay.mobile.nebulax.integration.mpaas.activity.NebulaActivity"
    
    /**
     * 唤醒支付宝并跳转到蚂蚁森林
     */
    @JvmStatic
    fun wakeUpAlipayForEnergyCollection(context: Context?) {
        if (context == null) {
            Log.error(TAG, "Context为null，无法唤醒支付宝")
            return
        }
        
        try {
            // 检查支付宝是否已安装
            if (!isAlipayInstalled(context)) {
                Log.error(TAG, "支付宝未安装，无法唤醒")
                return
            }
            
            Log.record(TAG, "🚀 准备唤醒支付宝...")
            
            // 方案1：通过Intent启动蚂蚁森林
            val intent = Intent().apply {
                component = ComponentName(ALIPAY_PACKAGE, FOREST_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                
                // 蚂蚁森林的scheme
                action = Intent.ACTION_VIEW
                data = android.net.Uri.parse("alipays://platformapi/startapp?appId=60000002")
            }
            
            context.startActivity(intent)
            Log.record(TAG, "✅ 已发送唤醒支付宝指令")
            
        } catch (e: Exception) {
            Log.error(TAG, "唤醒支付宝失败: ${e.message}")
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
     * 在应用启动时自动唤醒支付宝
     */
    @JvmStatic
    fun autoWakeUpOnAppStart() {
        try {
            val context = ApplicationHook.getAppContext()
            if (context != null) {
                Log.record(TAG, "📱 芝麻粒启动，自动唤醒支付宝进行能量收取")
                
                // 延迟3秒后唤醒，避免应用启动时卡顿
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    wakeUpAlipayForEnergyCollection(context)
                }, 3000)
            } else {
                Log.error(TAG, "无法获取Application Context")
            }
        } catch (e: Exception) {
            Log.error(TAG, "自动唤醒失败: ${e.message}")
        }
    }
}
