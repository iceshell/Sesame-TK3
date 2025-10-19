package fansirsqi.xposed.sesame

import android.app.Application
import android.os.Process
import fansirsqi.xposed.sesame.util.Log

/**
 * 芝麻粒应用主类
 * 
 * 负责应用初始化
 */
class SesameApplication : Application() {

    companion object {
        private const val TAG = "SesameApplication"
    }

    override fun onCreate() {
        super.onCreate()
        
        val processName = getCurrentProcessName()
        Log.runtime(TAG, "🚀 应用启动 | 进程: $processName | PID: ${Process.myPid()}")
        
        // 自动唤醒支付宝进行能量收取
        try {
            fansirsqi.xposed.sesame.util.AlipayWakeUpManager.autoWakeUpOnAppStart()
        } catch (e: Exception) {
            Log.error(TAG, "自动唤醒支付宝失败: ${e.message}")
        }
    }

    /**
     * 获取当前进程名
     */
    private fun getCurrentProcessName(): String {
        return try {
            // Android 9.0+ 可直接获取
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                android.app.Application.getProcessName()
            } else {
                // 通过读取 /proc/self/cmdline 获取
                val pid = Process.myPid()
                val cmdlineFile = java.io.File("/proc/$pid/cmdline")
                if (cmdlineFile.exists()) {
                    cmdlineFile.readText().trim('\u0000')
                } else {
                    packageName
                }
            }
        } catch (e: Exception) {
            packageName
        }
    }
}