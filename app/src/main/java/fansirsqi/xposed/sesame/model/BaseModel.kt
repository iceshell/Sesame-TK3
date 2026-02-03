package fansirsqi.xposed.sesame.model

import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ListModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager

/**
 * 基础配置模块
 */
class BaseModel : Model() {
    
    override fun getName(): String = "基础"
    
    override fun getGroup(): ModelGroup = ModelGroup.BASE
    
    override fun getIcon(): String = "BaseModel.png"
    
    override val enableFieldName: String
        get() = "启用模块"
    
    override fun getFields(): ModelFields {
        return ModelFields().apply {
            addField(stayAwake)
            addField(manualTriggerAutoSchedule)
            addField(checkInterval)
            addField(offlineCooldown)
            addField(taskExecutionRounds)
            addField(modelSleepTime)
            addField(execAtTimeList)
            addField(wakenAtTimeList)
            addField(energyTime)
            addField(timedTaskModel)
            addField(timeoutRestart)
            addField(waitWhenException)
            addField(errNotify)
            addField(setMaxErrorCount)
            addField(newRpc)
            addField(debugMode)
            addField(sendHookData)
            addField(sendHookDataUrl)
            addField(batteryPerm)
            addField(enableCaptchaHook)
            addField(captchaHookLevel)
            addField(recordLog)
            addField(runtimeLog)
            addField(showToast)
            addField(enableOnGoing)
            addField(languageSimplifiedChinese)
            addField(toastOffsetY)
        }
    }
    
    companion object {
        private const val TAG = "BaseModel"
        
        @JvmStatic
        val stayAwake = BooleanModelField("stayAwake", "保持唤醒", true)
        
        @JvmStatic
        val manualTriggerAutoSchedule = BooleanModelField("manualTriggerAutoSchedule", "手动触发支付宝运行", false)
        
        @JvmStatic
        val checkInterval = IntegerModelField.MultiplyIntegerModelField(
            "checkInterval", "执行间隔(分钟)", 50, 1, 12 * 60, 60_000
        )

        @JvmStatic
        val offlineCooldown = IntegerModelField.MultiplyIntegerModelField(
            "offlineCooldown", "离线冷却(分钟,0=随执行间隔)", 0, 0, 24 * 60, 60_000
        )
        
        @JvmStatic
        val taskExecutionRounds = IntegerModelField("taskExecutionRounds", "任务执行轮数", 2, 1, 99)
        
        @JvmStatic
        val execAtTimeList = ListModelField.ListJoinCommaToStringModelField(
            "execAtTimeList", "定时执行(关闭:-1)", 
            ArrayList(listOf("0010", "0030", "0100", "0700", "0730", "1200", "1230", "1700", "1730", "2000", "2030", "2359"))
        )
        
        @JvmStatic
        val wakenAtTimeList = ListModelField.ListJoinCommaToStringModelField(
            "wakenAtTimeList", "定时唤醒(关闭:-1)", 
            ArrayList(listOf("0010", "0030", "0100", "0650", "2350"))
        )
        
        @JvmStatic
        val energyTime = ListModelField.ListJoinCommaToStringModelField(
            "energyTime", "只收能量时间(范围|关闭:-1)", ArrayList(listOf("0700-0730"))
        )
        
        @JvmStatic
        val modelSleepTime = ListModelField.ListJoinCommaToStringModelField(
            "modelSleepTime", "模块休眠时间(范围|关闭:-1)", ArrayList(listOf("0200-0201"))
        )
        
        @JvmStatic
        val timedTaskModel = ChoiceModelField("timedTaskModel", "定时任务模式", TimedTaskModel.SYSTEM, TimedTaskModel.nickNames)
        
        @JvmStatic
        val timeoutRestart = BooleanModelField("timeoutRestart", "超时重启", true)
        
        @JvmStatic
        val waitWhenException = IntegerModelField.MultiplyIntegerModelField(
            "waitWhenException", "异常等待时间(分钟)", 60, 0, 24 * 60, 60_000
        )
        
        @JvmStatic
        val errNotify = BooleanModelField("errNotify", "开启异常通知", false)
        
        @JvmStatic
        val setMaxErrorCount = IntegerModelField("setMaxErrorCount", "异常次数阈值", 8)
        
        @JvmStatic
        val newRpc = BooleanModelField("newRpc", "使用新接口(最低支持v10.3.96.8100)", true)
        
        @JvmStatic
        val debugMode = BooleanModelField("debugMode", "开启抓包(基于新接口)", false)
        
        @JvmStatic
        val sendHookData = BooleanModelField("sendHookData", "启用Hook数据转发", false)
        
        @JvmStatic
        val sendHookDataUrl = StringModelField("sendHookDataUrl", "Hook数据转发地址", "http://127.0.0.1:9527/hook")
        
        @JvmStatic
        val batteryPerm = BooleanModelField("batteryPerm", "为支付宝申请后台运行权限", true)
        
        @JvmStatic
        val enableCaptchaHook = BooleanModelField("enableCaptchaHook", "启用验证码拦截", false)
        
        @JvmStatic
        val captchaHookLevel = ChoiceModelField("captchaHookLevel", "验证码拦截级别", CaptchaHookLevel.SLIDE_CAPTCHA, CaptchaHookLevel.nickNames)
        
        @JvmStatic
        val recordLog = BooleanModelField("recordLog", "全部 | 记录record日志", true)
        
        @JvmStatic
        val runtimeLog = BooleanModelField("runtimeLog", "全部 | 记录runtime日志", false)
        
        @JvmStatic
        val showToast = BooleanModelField("showToast", "气泡提示", true)
        
        @JvmStatic
        val toastOffsetY = IntegerModelField("toastOffsetY", "气泡纵向偏移", 99)
        
        @JvmStatic
        val languageSimplifiedChinese = BooleanModelField("languageSimplifiedChinese", "只显示中文并设置时区", true)
        
        @JvmStatic
        val enableOnGoing = BooleanModelField("enableOnGoing", "开启状态栏禁删", false)
        
        @JvmStatic
        fun destroyData() {
            try {
                Log.runtime(TAG, "🧹清理所有数据")
                IdMapManager.getInstance(BeachMap::class.java).clear()
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }
    
    object TimedTaskModel {
        const val SYSTEM = 0
        const val PROGRAM = 1
        @JvmField
        val nickNames = arrayOf("🤖系统计时", "📦程序计时")
    }
    
    object CaptchaHookLevel {
        const val NORMAL_CAPTCHA = 0
        const val SLIDE_CAPTCHA = 1
        @JvmField
        val nickNames = arrayOf("🔓普通验证(放行滑块)", "🛡️滑块验证(屏蔽所有)")
    }
}
