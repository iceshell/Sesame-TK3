package fansirsqi.xposed.sesame.hook

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.util.AssetUtil
import fansirsqi.xposed.sesame.util.Detector
import fansirsqi.xposed.sesame.util.Log
import java.io.File
import java.lang.reflect.InvocationTargetException

/**
 * ApplicationHook 工具方法
 * 第二部分迁移：工具和辅助方法
 */
object ApplicationHookUtils {
    private const val TAG = "ApplicationHook"

    @Volatile
    private var lastReLoginBroadcastAt: Long = 0L

    @Volatile
    private var lastReLoginBroadcastSkipLogAt: Long = 0L

    private const val RELOGIN_BROADCAST_MIN_INTERVAL_MS = 10_000L

    @Volatile
    private var lastRestartBroadcastAt: Long = 0L

    @Volatile
    private var lastRestartBroadcastSkipLogAt: Long = 0L

    private const val RESTART_BROADCAST_MIN_INTERVAL_MS = 10_000L

    /**
     * 对类中的特定方法进行反优化处理
     * 用于确保Xposed hook能够正确工作
     */
    @JvmStatic
    fun deoptimizeMethod(clazz: Class<*>) {
        try {
            val deoptimizeMethod = ApplicationHookConstants.deoptimizeMethod ?: return
            
            clazz.declaredMethods.forEach { method ->
                if (method.name == "makeApplicationInner") {
                    deoptimizeMethod.invoke(null, method)
                    if (BuildConfig.DEBUG) {
                        XposedBridge.log("D/$TAG Deoptimized ${method.name}")
                    }
                }
            }
        } catch (e: InvocationTargetException) {
            Log.printStackTrace(TAG, e)
        } catch (e: IllegalAccessException) {
            Log.printStackTrace(TAG, e)
        }
    }

    /**
     * 加载Native库文件
     */
    @SuppressLint("UnsafeDynamicallyLoadedCode")
    @JvmStatic
    fun loadNativeLibs(context: Context, soFile: File) {
        try {
            val finalSoFile = AssetUtil.copyStorageSoFileToPrivateDir(context, soFile)
            if (finalSoFile != null) {
                System.load(finalSoFile.absolutePath)
                Log.runtime(TAG, "Loading ${soFile.name} from :${finalSoFile.absolutePath}")
            } else {
                Detector.loadLibrary(soFile.name.replace(".so", "").replace("lib", ""))
            }
        } catch (e: Exception) {
            Log.error(TAG, "载入so库失败！！")
            Log.printStackTrace(e)
        }
    }

    /**
     * 获取MicroApplicationContext对象
     */
    @JvmStatic
    fun getMicroApplicationContext(): Any? {
        var microContext = ApplicationHookConstants.microApplicationContextObject
        if (microContext == null) {
            try {
                val classLoader = ApplicationHookConstants.classLoader ?: return null
                val alipayApplicationClass = XposedHelpers.findClass(
                    "com.alipay.mobile.framework.AlipayApplication",
                    classLoader
                )
                val alipayApplicationInstance = XposedHelpers.callStaticMethod(
                    alipayApplicationClass,
                    "getInstance"
                ) ?: return null
                
                microContext = XposedHelpers.callMethod(
                    alipayApplicationInstance,
                    "getMicroApplicationContext"
                )
                ApplicationHookConstants.setMicroApplicationContextObject(microContext)
            } catch (t: Throwable) {
                Log.printStackTrace(t)
            }
        }
        return microContext
    }

    /**
     * 根据服务接口名称获取服务对象
     */
    @JvmStatic
    fun getServiceObject(service: String): Any? {
        return try {
            XposedHelpers.callMethod(getMicroApplicationContext(), "findServiceByInterface", service)
        } catch (th: Throwable) {
            Log.runtime(TAG, "getServiceObject err")
            Log.printStackTrace(TAG, th)
            null
        }
    }

    /**
     * 获取用户对象
     */
    @JvmStatic
    fun getUserObject(): Any? {
        return try {
            val classLoader = ApplicationHookConstants.classLoader ?: return null
            val serviceClass = XposedHelpers.findClass(
                "com.alipay.mobile.personalbase.service.SocialSdkContactService",
                classLoader
            )
            XposedHelpers.callMethod(
                getServiceObject(serviceClass.name),
                "getMyAccountInfoModelByLocal"
            )
        } catch (th: Throwable) {
            Log.runtime(TAG, "getUserObject err")
            Log.printStackTrace(TAG, th)
            null
        }
    }

    /**
     * 获取当前登录用户的ID
     */
    @JvmStatic
    fun getUserId(): String? {
        return try {
            val userObject = getUserObject()
            if (userObject != null) {
                XposedHelpers.getObjectField(userObject, "userId") as? String
            } else {
                null
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "getUserId err")
            Log.printStackTrace(TAG, th)
            null
        }
    }

    /**
     * 通过广播发送重新登录指令
     */
    @JvmStatic
    fun reLoginByBroadcast() {
        try {
            val now = System.currentTimeMillis()
            val shouldSend = synchronized(this) {
                if (now - lastReLoginBroadcastAt < RELOGIN_BROADCAST_MIN_INTERVAL_MS) {
                    false
                } else {
                    lastReLoginBroadcastAt = now
                    true
                }
            }

            if (!shouldSend) {
                val nowSkip = System.currentTimeMillis()
                if (nowSkip - lastReLoginBroadcastSkipLogAt >= RELOGIN_BROADCAST_MIN_INTERVAL_MS) {
                    lastReLoginBroadcastSkipLogAt = nowSkip
                    Log.runtime(TAG, "reLogin广播发送过于频繁，已跳过")
                }
                return
            }

            ApplicationHookConstants.appContext?.sendBroadcast(
                Intent("com.eg.android.AlipayGphone.sesame.reLogin")
            )
        } catch (th: Throwable) {
            Log.runtime(TAG, "sesame sendBroadcast reLogin err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 通过广播发送重启模块服务的指令
     */
    @JvmStatic
    fun restartByBroadcast() {
        try {
            val now = System.currentTimeMillis()
            val shouldSend = synchronized(this) {
                if (now - lastRestartBroadcastAt < RESTART_BROADCAST_MIN_INTERVAL_MS) {
                    false
                } else {
                    lastRestartBroadcastAt = now
                    true
                }
            }

            if (!shouldSend) {
                val nowSkip = System.currentTimeMillis()
                if (nowSkip - lastRestartBroadcastSkipLogAt >= RESTART_BROADCAST_MIN_INTERVAL_MS) {
                    lastRestartBroadcastSkipLogAt = nowSkip
                    Log.runtime(TAG, "restart广播发送过于频繁，已跳过")
                }
                return
            }
            ApplicationHookConstants.appContext?.sendBroadcast(
                Intent("com.eg.android.AlipayGphone.sesame.restart")
            )
        } catch (th: Throwable) {
            Log.runtime(TAG, "发送重启广播时出错:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 通过广播发送立即执行一次任务的指令
     */
    @JvmStatic
    fun executeByBroadcast() {
        try {
            ApplicationHookConstants.appContext?.sendBroadcast(
                Intent("com.eg.android.AlipayGphone.sesame.execute")
            )
        } catch (th: Throwable) {
            Log.runtime(TAG, "发送执行广播时出错:")
            Log.printStackTrace(TAG, th)
        }
    }
}
