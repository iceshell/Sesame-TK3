package fansirsqi.xposed.sesame.hook.xp82

import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage
import fansirsqi.xposed.sesame.data.General
import fansirsqi.xposed.sesame.hook.ApplicationHookEntry
import fansirsqi.xposed.sesame.hook.XposedEnv

/**
 * 旧版 Xposed 框架（如 EdXposed、LSPosed < 1.9）入口
 * 对应新框架 fansirsqi.xposed.sesame.hook.lsp100.HookEntry
 */
class HookEntry : IXposedHookLoadPackage {

    private val tag = "Xp82Entry"

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            // 只在目标应用执行
            if (lpparam.packageName != General.PACKAGE_NAME) return
            // 只在主进程执行（防止子进程重复初始化）
            XposedEnv.classLoader = lpparam.classLoader
            XposedEnv.appInfo = lpparam.appInfo
            XposedEnv.packageName = lpparam.packageName
            XposedEnv.processName = lpparam.processName

            XposedBridge.log("$tag: Hooking ${lpparam.packageName} in process ${lpparam.processName}")
            // 调用Kotlin迁移后的Hook逻辑
            ApplicationHookEntry.loadPackageCompat(lpparam)

        } catch (e: Throwable) {
            XposedBridge.log("$tag: Hook failed - ${e.message}")
            XposedBridge.log(e)
        }
    }
}
