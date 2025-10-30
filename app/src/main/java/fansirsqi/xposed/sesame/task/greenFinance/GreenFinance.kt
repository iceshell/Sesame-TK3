package fansirsqi.xposed.sesame.task.greenFinance

import fansirsqi.xposed.sesame.task.greenFinance.GreenFinanceRpcCall.taskQuery
import fansirsqi.xposed.sesame.task.greenFinance.GreenFinanceRpcCall.taskTrigger
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TreeMap
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.util.TimeUtil

class GreenFinance : ModelTask() {

    private var greenFinanceLsxd: BooleanModelField? = null
    private var greenFinanceLsbg: BooleanModelField? = null
    private var greenFinanceLscg: BooleanModelField? = null
    private var greenFinanceLswl: BooleanModelField? = null
    private var greenFinanceWdxd: BooleanModelField? = null
    private var greenFinanceDonation: BooleanModelField? = null
    private var greenFinancePointFriend: BooleanModelField? = null

    override fun getName(): String = "绿色经营"

    override fun getGroup(): ModelGroup = ModelGroup.OTHER

    override fun getIcon(): String = "GreenFinance.png"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(BooleanModelField("greenFinanceLsxd", "打卡 | 绿色行动", false).also { greenFinanceLsxd = it })
        modelFields.addField(BooleanModelField("greenFinanceLscg", "打卡 | 绿色采购", false).also { greenFinanceLscg = it })
        modelFields.addField(BooleanModelField("greenFinanceLsbg", "打卡 | 绿色办公", false).also { greenFinanceLsbg = it })
        modelFields.addField(BooleanModelField("greenFinanceWdxd", "打卡 | 绿色销售", false).also { greenFinanceWdxd = it })
        modelFields.addField(BooleanModelField("greenFinanceLswl", "打卡 | 绿色物流", false).also { greenFinanceLswl = it })
        modelFields.addField(BooleanModelField("greenFinancePointFriend", "收取 | 好友金币", false).also { greenFinancePointFriend = it })
        modelFields.addField(BooleanModelField("greenFinanceDonation", "捐助 | 快过期金币", false).also { greenFinanceDonation = it })
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
            val s = GreenFinanceRpcCall.greenFinanceIndex()
            var jo = JSONObject(s)
            if (!jo.optBoolean("success")) {
                Log.runtime(TAG, jo.optString("resultDesc"))
                return
            }
            val result = jo.getJSONObject("result")
            if (!result.getBoolean("greenFinanceSigned")) {
                Log.other("绿色经营📊未开通")
                return
            }
            val mcaGreenLeafResult = result.getJSONObject("mcaGreenLeafResult")
            val greenLeafList = mcaGreenLeafResult.getJSONArray("greenLeafList")
            var currentCode = ""
            var bsnIds = JSONArray()
            for (i in 0 until greenLeafList.length()) {
                val greenLeaf = greenLeafList.getJSONObject(i)
                val code = greenLeaf.getString("code")
                if (currentCode == code || bsnIds.length() == 0) {
                    bsnIds.put(greenLeaf.getString("bsnId"))
                } else {
                    batchSelfCollect(bsnIds)
                    bsnIds = JSONArray()
                }
            }
            if (bsnIds.length() > 0) {
                batchSelfCollect(bsnIds)
            }
            signIn("PLAY102632271")
            signIn("PLAY102232206")
            behaviorTick()
            donation()
            batchStealFriend()
            prizes()
            doTask("AP13159535", TAG, "绿色经营📊")
            GlobalThreadPools.sleepCompat(500)
        } catch (th: Throwable) {
            Log.runtime(TAG, "index err:")
            Log.printStackTrace(TAG, th)
        } finally {
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    private fun batchSelfCollect(bsnIds: JSONArray) {
        val s = GreenFinanceRpcCall.batchSelfCollect(bsnIds)
        try {
            val joSelfCollect = JSONObject(s)
            if (joSelfCollect.optBoolean("success")) {
                val totalCollectPoint = joSelfCollect.getJSONObject("result").getInt("totalCollectPoint")
                Log.other("绿色经营📊收集获得$totalCollectPoint")
            } else {
                Log.runtime("$TAG.batchSelfCollect", joSelfCollect.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "batchSelfCollect err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun signIn(sceneId: String) {
        try {
            var s = GreenFinanceRpcCall.signInQuery(sceneId)
            var jo = JSONObject(s)
            if (!jo.optBoolean("success")) {
                Log.runtime("$TAG.signIn.signInQuery", jo.optString("resultDesc"))
                return
            }
            val result = jo.getJSONObject("result")
            if (result.getBoolean("isTodaySignin")) {
                return
            }
            s = GreenFinanceRpcCall.signInTrigger(sceneId)
            GlobalThreadPools.sleepCompat(300)
            jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                Log.other("绿色经营📊签到成功")
            } else {
                Log.runtime("$TAG.signIn.signInTrigger", jo.optString("resultDesc"))
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "signIn err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun behaviorTick() {
        if (greenFinanceLsxd?.value == true) doTick("lsxd")
        if (greenFinanceLscg?.value == true) doTick("lscg")
        if (greenFinanceLswl?.value == true) doTick("lswl")
        if (greenFinanceLsbg?.value == true) doTick("lsbg")
        if (greenFinanceWdxd?.value == true) doTick("wdxd")
    }

    private fun doTick(type: String) {
        try {
            var str = GreenFinanceRpcCall.queryUserTickItem(type)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.doTick.queryUserTickItem", jsonObject.optString("resultDesc"))
                return
            }
            val jsonArray = jsonObject.getJSONArray("result")
            for (i in 0 until jsonArray.length()) {
                jsonObject = jsonArray.getJSONObject(i)
                if ("Y" == jsonObject.getString("status")) {
                    continue
                }
                str = GreenFinanceRpcCall.submitTick(type, jsonObject.getString("behaviorCode"))
                GlobalThreadPools.sleepCompat(1500)
                val obj = JSONObject(str)
                if (!obj.optBoolean("success") || 
                    JsonUtil.getValueByPath(obj, "result.result") != "true") {
                    Log.other("绿色经营📊[${jsonObject.getString("title")}]打卡失败")
                    break
                }
                Log.other("绿色经营📊[${jsonObject.getString("title")}]打卡成功")
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "doTick err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun donation() {
        if (greenFinanceDonation?.value != true) {
            return
        }
        try {
            var str = GreenFinanceRpcCall.queryExpireMcaPoint(1)
            GlobalThreadPools.sleepCompat(300)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.donation.queryExpireMcaPoint", jsonObject.optString("resultDesc"))
                return
            }
            val strAmount = JsonUtil.getValueByPath(jsonObject, "result.expirePoint.amount")
            if (strAmount.isEmpty() || !strAmount.matches(Regex("-?\\d+(\\.\\d+)?"))) {
                return
            }
            val amount = strAmount.toDouble()
            if (amount <= 0) {
                return
            }
            Log.other("绿色经营📊1天内过期的金币[$amount]")
            str = GreenFinanceRpcCall.queryAllDonationProjectNew()
            GlobalThreadPools.sleepCompat(300)
            jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.donation.queryAllDonationProjectNew", jsonObject.optString("resultDesc"))
                return
            }
            val result = jsonObject.getJSONArray("result")
            val dicId = TreeMap<String, String>()
            for (i in 0 until result.length()) {
                val obj = JsonUtil.getValueByPathObject(
                    result.getJSONObject(i),
                    "mcaDonationProjectResult.[0]"
                ) as? JSONObject ?: continue
                val pId = obj.optString("projectId")
                if (pId.isEmpty()) {
                    continue
                }
                dicId[pId] = obj.optString("projectName")
            }
            val r = calculateDeductions(amount.toInt(), dicId.size)
            var am = "200"
            for (i in 0 until r[0]) {
                val id = dicId.keys.elementAt(i)
                val name = dicId[id]
                if (i == r[0] - 1) {
                    am = r[1].toString()
                }
                str = GreenFinanceRpcCall.donation(id, am)
                GlobalThreadPools.sleepCompat(1000)
                jsonObject = JSONObject(str)
                if (!jsonObject.optBoolean("success")) {
                    Log.runtime("$TAG.donation.$id", jsonObject.optString("resultDesc"))
                    return
                }
                Log.other("绿色经营📊成功捐助[$name]${am}金币")
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "donation err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun prizes() {
        try {
            if (Status.canGreenFinancePrizesMap()) {
                return
            }
            val campId = "CP14664674"
            var str = GreenFinanceRpcCall.queryPrizes(campId)
            var jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.prizes.queryPrizes", jsonObject.optString("resultDesc"))
                return
            }
            val prizes = JsonUtil.getValueByPathObject(jsonObject, "result.prizes") as? JSONArray
            if (prizes != null) {
                for (i in 0 until prizes.length()) {
                    jsonObject = prizes.getJSONObject(i)
                    val bizTime = jsonObject.getString("bizTime")
                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                    val dateTime = formatter.parse(bizTime)
                    if (dateTime != null && TimeUtil.getWeekNumber(dateTime) == TimeUtil.getWeekNumber(Date())) {
                        Status.greenFinancePrizesMap()
                        return
                    }
                }
            }
            str = GreenFinanceRpcCall.campTrigger(campId)
            jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.prizes.campTrigger", jsonObject.optString("resultDesc"))
                return
            }
            val obj = JsonUtil.getValueByPathObject(jsonObject, "result.prizes.[0]") as? JSONObject ?: return
            Log.other("绿色经营🍬评级奖品[${obj.getString("prizeName")}]${obj.getString("price")}")
        } catch (th: Throwable) {
            Log.runtime(TAG, "prizes err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun batchStealFriend() {
        try {
            if (Status.canGreenFinancePointFriend() || greenFinancePointFriend?.value != true) {
                return
            }
            var n = 0
            while (true) {
                try {
                    var str = GreenFinanceRpcCall.queryRankingList(n)
                    GlobalThreadPools.sleepCompat(1500)
                    var jsonObject = JSONObject(str)
                    if (!jsonObject.optBoolean("success")) {
                        Log.other("绿色经营🙋，好友金币巡查失败")
                        break
                    }
                    val result = jsonObject.getJSONObject("result")
                    if (result.getBoolean("lastPage")) {
                        Log.other("绿色经营🙋，好友金币巡查完成")
                        Status.greenFinancePointFriend()
                        return
                    }
                    n = result.getInt("nextStartIndex")
                    val list = result.getJSONArray("rankingList")
                    for (i in 0 until list.length()) {
                        val obj = list.getJSONObject(i)
                        if (!obj.getBoolean("collectFlag")) {
                            continue
                        }
                        val friendId = obj.optString("uid")
                        if (friendId.isEmpty()) {
                            continue
                        }
                        str = GreenFinanceRpcCall.queryGuestIndexPoints(friendId)
                        GlobalThreadPools.sleepCompat(1000)
                        jsonObject = JSONObject(str)
                        if (!jsonObject.optBoolean("success")) {
                            Log.runtime("$TAG.batchStealFriend.queryGuestIndexPoints", jsonObject.optString("resultDesc"))
                            continue
                        }
                        val points = JsonUtil.getValueByPathObject(jsonObject, "result.pointDetailList") as? JSONArray ?: continue
                        val jsonArray = JSONArray()
                        for (j in 0 until points.length()) {
                            jsonObject = points.getJSONObject(j)
                            if (!jsonObject.getBoolean("collectFlag")) {
                                jsonArray.put(jsonObject.getString("bsnId"))
                            }
                        }
                        if (jsonArray.length() == 0) {
                            continue
                        }
                        str = GreenFinanceRpcCall.batchSteal(jsonArray, friendId)
                        GlobalThreadPools.sleepCompat(1000)
                        jsonObject = JSONObject(str)
                        if (!jsonObject.optBoolean("success")) {
                            Log.runtime("$TAG.batchStealFriend.batchSteal", jsonObject.optString("resultDesc"))
                            continue
                        }
                        Log.other("绿色经营🤩收[${obj.optString("nickName")}]${JsonUtil.getValueByPath(jsonObject, "result.totalCollectPoint")}金币")
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                    break
                }
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "batchStealFriend err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private fun calculateDeductions(amount: Int, maxDeductions: Int): IntArray {
        if (amount < 200) {
            return intArrayOf(1, 200)
        }
        var actualDeductions = minOf(maxDeductions, ((amount.toDouble() / 200).let { if (it > it.toInt()) it.toInt() + 1 else it.toInt() }))
        var remainingAmount = amount - actualDeductions * 200
        if (remainingAmount % 100 != 0) {
            remainingAmount = ((remainingAmount + 99) / 100) * 100
        }
        if (remainingAmount < 200) {
            remainingAmount = 200
        }
        if (remainingAmount < amount - actualDeductions * 200) {
            actualDeductions = (amount - remainingAmount) / 200
        }
        return intArrayOf(actualDeductions, remainingAmount)
    }

    companion object {
        private val TAG = GreenFinance::class.java.simpleName

        @JvmStatic
        fun doTask(appletId: String, tag: String, name: String) {
            try {
                var s = taskQuery(appletId)
                var jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.runtime("$tag.doTask.taskQuery", jo.optString("resultDesc"))
                    return
                }
                val result = jo.getJSONObject("result")
                val taskDetailList = result.getJSONArray("taskDetailList")
                for (i in 0 until taskDetailList.length()) {
                    val taskDetail = taskDetailList.getJSONObject(i)
                    val type = taskDetail.getString("sendCampTriggerType")
                    if (type != "USER_TRIGGER" && type != "EVENT_TRIGGER") {
                        continue
                    }
                    val status = taskDetail.getString("taskProcessStatus")
                    val taskId = taskDetail.getString("taskId")
                    when {
                        "TO_RECEIVE" == status -> {
                            s = taskTrigger(taskId, "receive", appletId)
                            jo = JSONObject(s)
                            if (!jo.optBoolean("success")) {
                                Log.runtime("$tag.doTask.receive", jo.optString("resultDesc"))
                                continue
                            }
                        }
                        "NONE_SIGNUP" == status -> {
                            s = taskTrigger(taskId, "signup", appletId)
                            jo = JSONObject(s)
                            if (!jo.optBoolean("success")) {
                                Log.runtime("$tag.doTask.signup", jo.optString("resultDesc"))
                                continue
                            }
                        }
                    }
                    if ("SIGNUP_COMPLETE" == status || "NONE_SIGNUP" == status) {
                        s = taskTrigger(taskId, "send", appletId)
                        jo = JSONObject(s)
                        if (!jo.optBoolean("success")) {
                            Log.runtime("$tag.doTask.send", jo.optString("resultDesc"))
                            continue
                        }
                    } else if ("TO_RECEIVE" != status) {
                        continue
                    }
                    Log.other("$name[${JsonUtil.getValueByPath(taskDetail, "taskExtProps.TASK_MORPHO_DETAIL.title")}]任务完成")
                }
            } catch (th: Throwable) {
                Log.runtime(tag, "doTask err:")
                Log.printStackTrace(tag, th)
            }
        }
    }
}
