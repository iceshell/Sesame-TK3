package fansirsqi.xposed.sesame.task.consumeGold

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.TimeUtil

class ConsumeGold : ModelTask() {

    private var lastExecutionInterval: IntegerModelField? = null
    private var consumeGoldSign: BooleanModelField? = null
    private var consumeGoldAward: BooleanModelField? = null
    private var consumeGoldGainRepair: BooleanModelField? = null
    private var consumeGoldRepairSign: BooleanModelField? = null
    private var consumeGoldRepairSignUseLimit: IntegerModelField? = null
    private var consumeGoldGainTask: BooleanModelField? = null
    private var eachTaskDelay: IntegerModelField? = null
    private var watchAdDelay: IntegerModelField? = null

    override fun getName(): String = "消费金"

    override fun getGroup(): ModelGroup = ModelGroup.OTHER

    override fun getIcon(): String = "ConsumeGold.svg"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(IntegerModelField("lastExecutionInterval", "距上次执行间隔不小于（毫秒，默认6小时）", 21600000, 0, 86400000).also { lastExecutionInterval = it })
        modelFields.addField(BooleanModelField("consumeGoldSign", "签到", false).also { consumeGoldSign = it })
        modelFields.addField(BooleanModelField("consumeGoldAward", "抽奖（每日免费三次）", false).also { consumeGoldAward = it })
        modelFields.addField(BooleanModelField("consumeGoldGainRepair", "领取补签卡", false).also { consumeGoldGainRepair = it })
        modelFields.addField(BooleanModelField("consumeGoldRepairSign", "使用补签卡", false).also { consumeGoldRepairSign = it })
        modelFields.addField(IntegerModelField("consumeGoldRepairSignUseLimit", "补签卡每日使用次数（当日过期）", 1, 1, 10).also { consumeGoldRepairSignUseLimit = it })
        modelFields.addField(BooleanModelField("consumeGoldGainTask", "完成积分任务", false).also { consumeGoldGainTask = it })
        modelFields.addField(IntegerModelField("eachTaskDelay", "执行下一项任务的延时（毫秒，默认200）", 200).also { eachTaskDelay = it })
        modelFields.addField(IntegerModelField("watchAdDelay", "观看15s广告任务执行延时（毫秒，默认16000）", 16000).also { watchAdDelay = it })
        return modelFields
    }

    override fun check(): Boolean? {
        return when {
            TaskCommon.IS_ENERGY_TIME -> {
                Log.record(TAG, "⏸ 当前为只收能量时间【${BaseModel.energyTime.value}】，停止执行${getName()}任务！")
                false
            }
            TaskCommon.IS_MODULE_SLEEP_TIME -> {
                Log.record(TAG, "💤 模块休眠时间【${BaseModel.modelSleepTime.value}】停止执行${getName()}任务！")
                false
            }
            else -> {
                val executeTime = RuntimeInfo.getInstance().getLong("consumeGold", 0)
                System.currentTimeMillis() - executeTime >= (lastExecutionInterval?.value ?: 21600000)
            }
        }
    }

    override fun runJava() {
        try {
            Log.record(TAG, "执行开始-${getName()}")
            RuntimeInfo.getInstance().put("consumeGold", System.currentTimeMillis())
            if (consumeGoldSign?.value == true) {
                consumeGoldSign()
                GlobalThreadPools.sleepCompat((eachTaskDelay?.value ?: 200).toLong())
            }
            if (consumeGoldAward?.value == true) {
                consumeGoldAward()
                GlobalThreadPools.sleepCompat((eachTaskDelay?.value ?: 200).toLong())
            }
            if (consumeGoldGainRepair?.value == true) {
                consumeGoldGainRepair()
                GlobalThreadPools.sleepCompat((eachTaskDelay?.value ?: 200).toLong())
            }
            if (consumeGoldRepairSign?.value == true) {
                consumeGoldRepairSign()
                GlobalThreadPools.sleepCompat((eachTaskDelay?.value ?: 200).toLong())
            }
            if (consumeGoldGainTask?.value == true) {
                consumeGoldGainTask()
                GlobalThreadPools.sleepCompat((eachTaskDelay?.value ?: 200).toLong())
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.run", t)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    private fun consumeGoldSign() {
        try {
            var s = ConsumeGoldRpcCall.signinCalendar()
            GlobalThreadPools.sleepCompat(200)
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldSign.signinCalendar", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldSign.signinCalendar", "消费金🪙[响应失败]#$s")
                return
            }
            if (jo.optBoolean("isSignInToday")) {
                return
            }
            s = ConsumeGoldRpcCall.taskV2Index("CG_SIGNIN_AD_FEEDS")
            GlobalThreadPools.sleepCompat(200)
            jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldSign.taskV2Index", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldSign.taskV2Index", "消费金🪙[响应失败]#$s")
                return
            }
            val taskList = jo.getJSONArray("taskList")
            if (taskList.length() == 0) {
                return
            }
            jo = taskList.getJSONObject(0)
            val taskId = jo.getJSONObject("extInfo").getString("actionBizId")
            s = ConsumeGoldRpcCall.taskV2Trigger(taskId, "CG_SIGNIN_AD_FEEDS", "SIGN_UP")
            GlobalThreadPools.sleepCompat(200)
            jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldSign.taskV2Trigger", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldSign.taskV2Trigger", "消费金🪙[响应失败]#$s")
                return
            }
            s = ConsumeGoldRpcCall.taskOpenBoxAward()
            GlobalThreadPools.sleepCompat(500)
            jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldSign.taskOpenBoxAward", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldSign.taskOpenBoxAward", "消费金🪙[响应失败]#$s")
                return
            }
            val amount = jo.getInt("amount")
            Log.other("消费金🪙[签到]#获得$amount")
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.consumeGoldSign", t)
        }
    }

    private fun consumeGoldAward() {
        try {
            var s = ConsumeGoldRpcCall.promoIndex()
            GlobalThreadPools.sleepCompat(500)
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldAward.promoIndex", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldAward.promoIndex", "消费金🪙[响应失败]#$s")
                return
            }
            jo = jo.getJSONObject("homePromoInfoDTO")
            val homePromoTokenDTOList = jo.getJSONArray("homePromoTokenDTOList")
            var tokenTotalAmount = 0
            var tokenLeftAmount = 0
            for (i in 0 until homePromoTokenDTOList.length()) {
                jo = homePromoTokenDTOList.getJSONObject(i)
                if ("FREE" == jo.getString("tokenType")) {
                    tokenTotalAmount = jo.getInt("tokenTotalAmount")
                    tokenLeftAmount = jo.getInt("tokenLeftAmount")
                    break
                }
            }
            if (tokenLeftAmount <= 0) {
                return
            }
            for (j in (tokenTotalAmount - tokenLeftAmount) until tokenTotalAmount) {
                s = ConsumeGoldRpcCall.promoTrigger()
                GlobalThreadPools.sleepCompat(1000)
                jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                if (!jo.optBoolean("success")) {
                    Log.other("$TAG.consumeGoldAward.promoTrigger", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                    Log.error("$TAG.consumeGoldAward.promoTrigger", "消费金🪙[响应失败]#$s")
                    return
                }
                jo = jo.getJSONObject("homePromoPrizeInfoDTO")
                val quantity = jo.getInt("quantity")
                Log.other("消费金🪙[抽奖(${j + 1}/$tokenTotalAmount)]#获得$quantity")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.consumeGoldAward", t)
        }
    }

    @Suppress("ReturnCount")
    private fun consumeGoldGainRepair() {
        try {
            var s = ConsumeGoldRpcCall.signinCalendar()
            GlobalThreadPools.sleepCompat(200)
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldGainRepair.signinCalendar", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldGainRepair.signinCalendar", "消费金🪙[响应失败]#$s")
                return
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "REPAIR_SIGN_TOKEN", "领取补签卡", true, true, true)
            }
            s = ConsumeGoldRpcCall.taskV2Index("REPAIR_SIGN_XLIGHT")
            jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldGainRepair.taskV2Index", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldGainRepair.taskV2Index", "消费金🪙[响应失败]#$s")
                return
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "REPAIR_SIGN_XLIGHT", "领取补签卡", true, true, false)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.consumeGoldGainRepair", t)
        }
    }

    private fun consumeGoldRepairSign() {
        try {
            val currentDate = TimeUtil.getFormatDate()
            if (currentDate != RuntimeInfo.getInstance().getString("consumeGoldRepairSignDate")) {
                RuntimeInfo.getInstance().put("consumeGoldRepairSignUsed", 0)
                RuntimeInfo.getInstance().put("consumeGoldRepairSignDate", currentDate)
            }
            var consumeGoldRepairUseLimit = RuntimeInfo.getInstance().getLong("consumeGoldRepairSignUsed", 0)
            var s = ConsumeGoldRpcCall.signinCalendar()
            GlobalThreadPools.sleepCompat(200)
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldRepairSign.signinCalendar", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldRepairSign.signinCalendar", "消费金🪙[响应失败]#$s")
                return
            }
            val repairSignInInfo = jo.getJSONObject("repairSignInInfo")
            val canRepair = repairSignInInfo.optBoolean("repair")
            var repairCardNum = repairSignInInfo.getInt("repairCardTokenNum")
            if (!canRepair || repairCardNum == 0) {
                return
            }
            val calendarGroup = jo.getJSONArray("calendarGroup")
            val calendarMap = HashMap<String, Boolean>()
            for (i in 0 until calendarGroup.length()) {
                val tempArray = calendarGroup.getJSONObject(i).getJSONArray("dateList")
                for (j in 0 until tempArray.length()) {
                    jo = tempArray.getJSONObject(j)
                    calendarMap[jo.getString("date")] = jo.optBoolean("isRepairable") && !jo.optBoolean("isSignIn")
                }
            }
            val repairDateList = ArrayList<String>()
            var offset = -1
            while (offset >= -calendarMap.size && repairDateList.size < repairCardNum && 
                   consumeGoldRepairUseLimit < (consumeGoldRepairSignUseLimit?.value ?: 1)) {
                val tempTime = TimeUtil.getFormatTime(offset, "yyyy-MM-dd")
                if (!calendarMap.containsKey(tempTime)) {
                    return
                }
                if (calendarMap[tempTime] == true) {
                    repairDateList.add(tempTime.replace("-", ""))
                    consumeGoldRepairUseLimit++
                }
                offset--
            }
            if (repairDateList.isEmpty()) {
                return
            }
            consumeGoldRepairUseLimit = RuntimeInfo.getInstance().getLong("consumeGoldRepairSignUsed", 0)
            for (repairDate in repairDateList) {
                s = ConsumeGoldRpcCall.signinTrigger("check", repairDate)
                GlobalThreadPools.sleepCompat(500)
                jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                if (!jo.optBoolean("success")) {
                    Log.other("$TAG.consumeGoldRepairSign.signinTrigger.check", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                    Log.error("$TAG.consumeGoldRepairSign.signinTrigger.check", "消费金🪙[响应失败]#$s")
                    return
                }
                s = ConsumeGoldRpcCall.signinTrigger("repair", repairDate)
                GlobalThreadPools.sleepCompat(500)
                jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                if (!jo.optBoolean("success")) {
                    Log.other("$TAG.consumeGoldRepairSign.signinTrigger.repair", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                    Log.error("$TAG.consumeGoldRepairSign.signinTrigger.repair", "消费金🪙[响应失败]#$s")
                    return
                }
                Log.other("消费金🪙[补签${repairDate}成功]#补签卡剩余${--repairCardNum}张")
                RuntimeInfo.getInstance().put("consumeGoldRepairSignUsed", ++consumeGoldRepairUseLimit)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.consumeGoldRepairSign", t)
        }
    }

    private fun consumeGoldGainTask() {
        try {
            val s = ConsumeGoldRpcCall.taskV2Index("ALL_DAILY_TASK_LIST")
            GlobalThreadPools.sleepCompat(200)
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.consumeGoldGainTask.taskV2Index", "消费金🪙[响应失败]#${jo.getString("errorMsg")}")
                Log.error("$TAG.consumeGoldGainTask.taskV2Index", "消费金🪙[响应失败]#$s")
                return
            }
            if (jo.has("taskList")) {
                execTask(jo.getJSONArray("taskList"), "ALL_DAILY_TASK_LIST", "消费金任务", true, true, true)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.consumeGoldGainTask", t)
        }
    }

    @Throws(JSONException::class)
    private fun execTask(taskList: JSONArray, taskSceneCode: String, execType: String, 
                        needSignUp: Boolean, needSend: Boolean, needReceive: Boolean) {
        var s: String
        var jo: JSONObject
        for (i in 0 until taskList.length()) {
            var task = taskList.getJSONObject(i)
            val amount = if (task.has("prizeInfoList")) {
                task.getJSONArray("prizeInfoList").getJSONObject(0).getInt("prizeModulus")
            } else {
                task.getInt("pointNum")
            }
            val type = task.getString("type")
            if ("BROWSER" == type || "CLICK_DIRECT_FINISH" == type) {
                continue
            }
            task = task.getJSONObject("extInfo")
            val taskId = task.getString("actionBizId")
            val title = task.getString("title")
            when (task.getString("taskStatus")) {
                "NONE_SIGNUP" -> {
                    if (needSignUp) {
                        GlobalThreadPools.sleepCompat(200)
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "SIGN_UP")
                        jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                        if (!jo.optBoolean("success")) {
                            Log.other("$TAG.execTask.taskV2Trigger.SIGN_UP", "消费金🪙[响应失败]#$s")
                            continue
                        }
                    }
                }
                "SIGNUP_COMPLETE" -> {
                    if (needSend) {
                        GlobalThreadPools.sleepCompat((watchAdDelay?.value ?: 16000).toLong())
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "SEND")
                        jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                        if (!jo.optBoolean("success")) {
                            Log.other("$TAG.execTask.taskV2Trigger.SEND", "消费金🪙[响应失败]#$s")
                            continue
                        }
                    }
                }
                "TO_RECEIVE" -> {
                    if (needReceive) {
                        GlobalThreadPools.sleepCompat(200)
                        s = ConsumeGoldRpcCall.taskV2Trigger(taskId, taskSceneCode, "RECEIVE")
                        jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                        if (!jo.optBoolean("success")) {
                            Log.other("$TAG.execTask.taskV2Trigger.RECEIVE", "消费金🪙[响应失败]#$s")
                        }
                    }
                }
                "RECEIVE_SUCCESS" -> continue
            }
            Log.other("消费金🪙[$execType($title)]#获得$amount")
        }
    }

    companion object {
        private val TAG = ConsumeGold::class.java.simpleName
    }
}
