package fansirsqi.xposed.sesame.task.antOrchard

import android.util.Base64
import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.data.Status

class AntOrchard : ModelTask() {
    
    private var userId: String? = null
    private var treeLevel: String? = null
    private var wuaList: Array<String>? = null
    private var executeIntervalInt: Int = 0
    
    private var executeInterval: IntegerModelField? = null
    private var receiveOrchardTaskAward: BooleanModelField? = null
    private var orchardSpreadManureCount: IntegerModelField? = null
    private var assistFriendList: SelectModelField? = null

    override fun getName(): String = "农场"

    override fun getGroup(): ModelGroup = ModelGroup.ORCHARD

    override fun getIcon(): String = "AntOrchard.png"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(IntegerModelField("executeInterval", "执行间隔(毫秒)", 500).also { executeInterval = it })
        modelFields.addField(BooleanModelField("receiveOrchardTaskAward", "收取农场任务奖励", false).also { receiveOrchardTaskAward = it })
        modelFields.addField(IntegerModelField("orchardSpreadManureCount", "农场每日施肥次数", 0).also { orchardSpreadManureCount = it })
        modelFields.addField(SelectModelField("assistFriendList", "助力好友列表", LinkedHashSet(), AlipayUser::getListAsMapperEntity).also { assistFriendList = it })
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
            else -> true
        }
    }

    override fun runJava() {
        try {
            Log.record(TAG, "执行开始-${getName()}")
            executeIntervalInt = maxOf(executeInterval?.value ?: 500, 500)
            val s = AntOrchardRpcCall.orchardIndex()
            var jo = JSONObject(s)
            if ("100" == jo.getString("resultCode")) {
                if (jo.optBoolean("userOpenOrchard")) {
                    val taobaoData = JSONObject(jo.getString("taobaoData"))
                    treeLevel = taobaoData.getJSONObject("gameInfo")
                        .getJSONObject("plantInfo").getJSONObject("seedStage").getInt("stageLevel").toString()
                    val joo = JSONObject(AntOrchardRpcCall.mowGrassInfo())
                    if ("100" == joo.optString("resultCode")) {
                        userId = joo.optString("userId").ifBlank { null }
                        if (userId.isNullOrBlank()) {
                            Log.runtime(TAG, "mowGrassInfo userId 为空，跳过本轮农场任务")
                            return
                        }
                        if (jo.has("lotteryPlusInfo")) drawLotteryPlus(jo.getJSONObject("lotteryPlusInfo"))
                        extraInfoGet()
                        if (receiveOrchardTaskAward?.value == true) {
                            userId?.let { doOrchardDailyTask(it) }
                            triggerTbTask()
                        }
                        val orchardSpreadManureCountValue = orchardSpreadManureCount?.value ?: 0
                        if (orchardSpreadManureCountValue > 0 && userId?.let { Status.canSpreadManureToday(it) } == true) {
                            orchardSpreadManure()
                        }
                        when {
                            orchardSpreadManureCountValue >= 10 -> querySubplotsActivity(10)
                            orchardSpreadManureCountValue >= 3 -> querySubplotsActivity(3)
                        }
                        orchardassistFriend()
                    } else {
                        Log.record(joo.optString("resultDesc"))
                        Log.runtime(joo.toString())
                    }
                } else {
                    enableField?.value = false
                    Log.other("请先开启芭芭农场！")
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    private fun getWua(): String {
        if (wuaList == null) {
            try {
                val content = Files.readFromFile(Files.getWuaFile())
                wuaList = content.split("\n").toTypedArray()
            } catch (ignored: Throwable) {
                wuaList = emptyArray()
            }
        }
        return if ((wuaList?.size ?: 0) > 0) {
            wuaList!![RandomUtil.nextInt(0, wuaList!!.size - 1)]
        } else {
            "null"
        }
    }

    private fun canSpreadManureContinue(stageBefore: Int, stageAfter: Int): Boolean {
        if (stageAfter - stageBefore > 1) {
            return true
        }
        Log.record(TAG, "施肥只加0.01%进度今日停止施肥！")
        return false
    }

    private fun orchardSpreadManure() {
        try {
            while (true) {
                try {
                    var jo = JSONObject(AntOrchardRpcCall.orchardIndex())
                    if ("100" != jo.getString("resultCode")) {
                        Log.runtime(TAG, jo.getString("resultDesc"))
                        return
                    }
                    if (jo.has("spreadManureActivity")) {
                        val spreadManureStage = jo.getJSONObject("spreadManureActivity").getJSONObject("spreadManureStage")
                        if ("FINISHED" == spreadManureStage.getString("status")) {
                            val sceneCode = spreadManureStage.getString("sceneCode")
                            val taskType = spreadManureStage.getString("taskType")
                            val awardCount = spreadManureStage.getInt("awardCount")
                            val joo = JSONObject(AntOrchardRpcCall.receiveTaskAward(sceneCode, taskType))
                            if (joo.optBoolean("success")) {
                                Log.farm("丰收礼包🎁[肥料*$awardCount]")
                            } else {
                                Log.record(joo.getString("desc"))
                                Log.runtime(joo.toString())
                            }
                        }
                    }
                    val taobaoData = jo.getString("taobaoData")
                    jo = JSONObject(taobaoData)
                    val plantInfo = jo.getJSONObject("gameInfo").getJSONObject("plantInfo")
                    val canExchange = plantInfo.getBoolean("canExchange")
                    if (canExchange) {
                        Log.farm("🎉 农场果树似乎可以兑换了！")
                        return
                    }
                    val seedStage = plantInfo.getJSONObject("seedStage")
                    treeLevel = seedStage.getInt("stageLevel").toString()
                    val accountInfo = jo.getJSONObject("gameInfo").getJSONObject("accountInfo")
                    val happyPoint = accountInfo.getString("happyPoint").toInt()
                    val wateringCost = accountInfo.getInt("wateringCost")
                    val wateringLeftTimes = accountInfo.getInt("wateringLeftTimes")
                    if (happyPoint > wateringCost && wateringLeftTimes > 0 && 
                        (200 - wateringLeftTimes < (orchardSpreadManureCount?.value ?: 0))) {
                        jo = JSONObject(AntOrchardRpcCall.orchardSpreadManure(getWua()))
                        if ("100" != jo.getString("resultCode")) {
                            Log.record(jo.getString("resultDesc"))
                            Log.runtime(jo.toString())
                            return
                        }
                        jo = JSONObject(jo.getString("taobaoData"))
                        val stageText = jo.getJSONObject("currentStage").getString("stageText")
                        Log.farm("农场施肥💩[$stageText]")
                        if (!canSpreadManureContinue(
                                seedStage.getInt("totalValue"),
                                jo.getJSONObject("currentStage").getInt("totalValue")
                            )) {
                            userId?.let { Status.spreadManureToday(it) }
                            return
                        }
                        continue
                    }
                } finally {
                    GlobalThreadPools.sleepCompat(executeIntervalInt.toLong())
                }
                break
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "orchardSpreadManure err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun extraInfoGet() {
        try {
            val s = AntOrchardRpcCall.extraInfoGet()
            var jo = JSONObject(s)
            if ("100" == jo.getString("resultCode")) {
                val fertilizerPacket = jo.getJSONObject("data").getJSONObject("extraData").getJSONObject("fertilizerPacket")
                if ("todayFertilizerWaitTake" != fertilizerPacket.getString("status")) return
                val todayFertilizerNum = fertilizerPacket.getInt("todayFertilizerNum")
                jo = JSONObject(AntOrchardRpcCall.extraInfoSet())
                if ("100" == jo.getString("resultCode")) {
                    Log.farm("每日肥料💩[${todayFertilizerNum}g]")
                } else {
                    Log.runtime(jo.getString("resultDesc"), jo.toString())
                }
            } else {
                Log.runtime(jo.getString("resultDesc"), jo.toString())
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "extraInfoGet err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun drawLotteryPlus(lotteryPlusInfo: JSONObject) {
        try {
            if (!lotteryPlusInfo.has("userSevenDaysGiftsItem")) return
            val itemId = lotteryPlusInfo.getString("itemId")
            var jo = lotteryPlusInfo.getJSONObject("userSevenDaysGiftsItem")
            val ja = jo.getJSONArray("userEverydayGiftItems")
            for (i in 0 until ja.length()) {
                jo = ja.getJSONObject(i)
                if (jo.getString("itemId") == itemId) {
                    if (!jo.getBoolean("received")) {
                        jo = JSONObject(AntOrchardRpcCall.drawLottery())
                        if ("100" == jo.getString("resultCode")) {
                            val userEverydayGiftItems = jo.getJSONObject("lotteryPlusInfo")
                                .getJSONObject("userSevenDaysGiftsItem")
                                .getJSONArray("userEverydayGiftItems")
                            for (j in 0 until userEverydayGiftItems.length()) {
                                jo = userEverydayGiftItems.getJSONObject(j)
                                if (jo.getString("itemId") == itemId) {
                                    val awardCount = jo.optInt("awardCount", 1)
                                    Log.farm("七日礼包🎁[获得肥料]#${awardCount}g")
                                    break
                                }
                            }
                        } else {
                            Log.runtime(jo.getString("resultDesc"), jo.toString())
                        }
                    } else {
                        Log.record(TAG, "七日礼包已领取")
                    }
                    break
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "drawLotteryPlus err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun doOrchardDailyTask(userId: String) {
        try {
            val s = AntOrchardRpcCall.orchardListTask()
            var jo = JSONObject(s)
            if ("100" == jo.getString("resultCode")) {
                if (jo.has("signTaskInfo")) {
                    val signTaskInfo = jo.getJSONObject("signTaskInfo")
                    orchardSign(signTaskInfo)
                }
                val jaTaskList = jo.getJSONArray("taskList")
                for (i in 0 until jaTaskList.length()) {
                    jo = jaTaskList.getJSONObject(i)
                    if ("TODO" != jo.getString("taskStatus")) continue
                    val title = jo.getJSONObject("taskDisplayConfig").getString("title")
                    if ("TRIGGER" == jo.getString("actionType") || 
                        "ADD_HOME" == jo.getString("actionType") ||
                        "PUSH_SUBSCRIBE" == jo.getString("actionType")) {
                        val taskId = jo.getString("taskId")
                        val sceneCode = jo.getString("sceneCode")
                        jo = JSONObject(AntOrchardRpcCall.finishTask(userId, sceneCode, taskId))
                        if (jo.optBoolean("success")) {
                            Log.farm("农场任务🧾[$title]")
                        } else {
                            Log.record(jo.getString("desc"))
                            Log.runtime(jo.toString())
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultCode"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "doOrchardDailyTask err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun orchardSign(signTaskInfo: JSONObject) {
        try {
            val currentSignItem = signTaskInfo.getJSONObject("currentSignItem")
            if (!currentSignItem.getBoolean("signed")) {
                val joSign = JSONObject(AntOrchardRpcCall.orchardSign())
                if ("100" == joSign.getString("resultCode")) {
                    val awardCount = joSign.getJSONObject("signTaskInfo")
                        .getJSONObject("currentSignItem").getInt("awardCount")
                    Log.farm("农场签到📅[获得肥料]#${awardCount}g")
                } else {
                    Log.runtime(joSign.getString("resultDesc"), joSign.toString())
                }
            } else {
                Log.record(TAG, "农场今日已签到")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "orchardSign err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun querySubplotsActivity(taskRequire: Int) {
        try {
            val currentTreeLevel = treeLevel ?: return
            val s = AntOrchardRpcCall.querySubplotsActivity(currentTreeLevel)
            var jo = JSONObject(s)
            if ("100" == jo.getString("resultCode")) {
                val subplotsActivityList = jo.getJSONArray("subplotsActivityList")
                for (i in 0 until subplotsActivityList.length()) {
                    jo = subplotsActivityList.getJSONObject(i)
                    if ("WISH" != jo.getString("activityType")) continue
                    val activityId = jo.getString("activityId")
                    when (jo.getString("status")) {
                        "NOT_STARTED" -> {
                            val extend = jo.getString("extend")
                            jo = JSONObject(extend)
                            val wishActivityOptionList = jo.getJSONArray("wishActivityOptionList")
                            var optionKey: String? = null
                            for (j in 0 until wishActivityOptionList.length()) {
                                jo = wishActivityOptionList.getJSONObject(j)
                                if (taskRequire == jo.getInt("taskRequire")) {
                                    optionKey = jo.getString("optionKey")
                                    break
                                }
                            }
                            if (optionKey != null) {
                                jo = JSONObject(AntOrchardRpcCall.triggerSubplotsActivity(activityId, "WISH", optionKey))
                                if ("100" == jo.getString("resultCode")) {
                                    Log.farm("农场许愿✨[每日施肥${taskRequire}次]")
                                } else {
                                    Log.record(jo.getString("resultDesc"))
                                    Log.runtime(jo.toString())
                                }
                            }
                        }
                        "FINISHED" -> {
                            jo = JSONObject(AntOrchardRpcCall.receiveOrchardRights(activityId, "WISH"))
                            if ("100" == jo.getString("resultCode")) {
                                Log.farm("许愿奖励✨[肥料${jo.getInt("amount")}g]")
                                querySubplotsActivity(taskRequire)
                                return
                            } else {
                                Log.record(jo.getString("resultDesc"))
                                Log.runtime(jo.toString())
                            }
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "triggerTbTask err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun orchardassistFriend() {
        try {
            if (!Status.canAntOrchardAssistFriendToday()) {
                return
            }
            val friendSet = assistFriendList?.value ?: return
            for (uid in friendSet) {
                val shareId = Base64.encodeToString(
                    ("$uid-${RandomUtil.getRandomInt(5)}ANTFARM_ORCHARD_SHARE_P2P").toByteArray(),
                    Base64.NO_WRAP
                )
                val str = AntOrchardRpcCall.achieveBeShareP2P(shareId)
                val jsonObject = JSONObject(str)
                GlobalThreadPools.sleepCompat(800)
                val name = UserMap.getMaskName(uid)
                if (!jsonObject.optBoolean("success")) {
                    val code = jsonObject.getString("code")
                    if ("600000027" == code) {
                        Log.record(TAG, "农场助力💪今日助力他人次数上限")
                        Status.antOrchardAssistFriendToday()
                        return
                    }
                    Log.record(TAG, "农场助力😔失败[$name]${jsonObject.optString("desc")}")
                    continue
                }
                Log.farm("农场助力💪[助力:$name]")
            }
            Status.antOrchardAssistFriendToday()
        } catch (t: Throwable) {
            Log.runtime(TAG, "orchardassistFriend err:")
            Log.printStackTrace(TAG, t)
        }
    }

    companion object {
        private val TAG = AntOrchard::class.java.simpleName

        @JvmStatic
        private fun triggerTbTask() {
            try {
                val s = AntOrchardRpcCall.orchardListTask()
                var jo = JSONObject(s)
                if ("100" == jo.getString("resultCode")) {
                    val jaTaskList = jo.getJSONArray("taskList")
                    for (i in 0 until jaTaskList.length()) {
                        jo = jaTaskList.getJSONObject(i)
                        if ("FINISHED" != jo.getString("taskStatus")) continue
                        val title = jo.getJSONObject("taskDisplayConfig").getString("title")
                        val awardCount = jo.optInt("awardCount", 0)
                        val taskId = jo.getString("taskId")
                        val taskPlantType = jo.getString("taskPlantType")
                        jo = JSONObject(AntOrchardRpcCall.triggerTbTask(taskId, taskPlantType))
                        if ("100" == jo.getString("resultCode")) {
                            Log.farm("领取奖励🎖️[$title]#${awardCount}g肥料")
                        } else {
                            Log.record(jo.getString("resultDesc"))
                            Log.runtime(jo.toString())
                        }
                    }
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(s)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "triggerTbTask err:")
                Log.printStackTrace(TAG, t)
            }
        }
    }
}
