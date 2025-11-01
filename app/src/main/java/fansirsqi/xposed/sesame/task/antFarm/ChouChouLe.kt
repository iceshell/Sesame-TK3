package fansirsqi.xposed.sesame.task.antFarm

import org.json.JSONArray
import org.json.JSONObject
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.ResChecker

class ChouChouLe {

    enum class TaskStatus {
        TODO, FINISHED, RECEIVED, DONATION
    }

    private data class TaskInfo(
        var taskStatus: String = "",
        var title: String = "",
        var taskId: String = "",
        var innerAction: String = "",
        var rightsTimes: Int = 0,
        var rightsTimesLimit: Int = 0,
        var awardType: String = "",
        var awardCount: Int = 0
    ) {
        fun getRemainingTimes(): Int = maxOf(0, rightsTimesLimit - rightsTimes)
    }

    fun chouchoule() {
        try {
            val response = AntFarmRpcCall.queryLoveCabin(UserMap.currentUid ?: return)
            val jo = JSONObject(response)
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }

            val drawMachineInfo = jo.optJSONObject("drawMachineInfo")
            if (drawMachineInfo == null) {
                Log.error(TAG, "抽抽乐🎁[获取抽抽乐活动信息失败]")
                return
            }

            if (drawMachineInfo.has("dailyDrawMachineActivityId")) {
                doChouchoule("dailyDraw")
            }
            if (drawMachineInfo.has("ipDrawMachineActivityId")) {
                doChouchoule("ipDraw")
            }

        } catch (t: Throwable) {
            Log.printStackTrace("chouchoule err:", t)
        }
    }

    private fun doChouchoule(drawType: String) {
        var doubleCheck: Boolean
        do {
            doubleCheck = false
            try {
                val jo = JSONObject(AntFarmRpcCall.chouchouleListFarmTask(drawType))
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.error(TAG, if (drawType == "ipDraw") "IP抽抽乐任务列表获取失败" else "抽抽乐任务列表获取失败")
                    continue
                }
                val farmTaskList = jo.getJSONArray("farmTaskList")
                val tasks = parseTasks(farmTaskList)
                for (task in tasks) {
                    if (TaskStatus.FINISHED.name == task.taskStatus) {
                        if (task.awardType == "ALLPURPOSE" && task.awardCount + AntFarm.foodStock > AntFarm.foodStockLimit) {
                            Log.record(TAG, "抽抽乐任务[${task.title}]的奖励领取后会使饲料超出上限，暂不领取")
                            continue
                        }
                        if (receiveTaskAward(drawType, task.taskId)) {
                            GlobalThreadPools.sleepCompat(5 * 1000L)
                            doubleCheck = true
                        }
                    } else if (TaskStatus.TODO.name == task.taskStatus) {
                        if (task.getRemainingTimes() > 0 && "DONATION" != task.innerAction) {
                            if (doChouTask(drawType, task)) {
                                doubleCheck = true
                            }
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace("doChouchoule err:", t)
            }
        } while (doubleCheck)

        if ("ipDraw" == drawType) {
            handleIpDraw()
        } else {
            handleDailyDraw()
        }
    }

    @Throws(Exception::class)
    private fun parseTasks(array: JSONArray): List<TaskInfo> {
        val list = ArrayList<TaskInfo>()
        for (i in 0 until array.length()) {
            val item = array.getJSONObject(i)
            list.add(TaskInfo(
                taskStatus = item.getString("taskStatus"),
                title = item.getString("title"),
                taskId = item.getString("bizKey"),
                innerAction = item.optString("innerAction"),
                rightsTimes = item.optInt("rightsTimes", 0),
                rightsTimesLimit = item.optInt("rightsTimesLimit", 0),
                awardType = item.optString("awardType"),
                awardCount = item.optInt("awardCount", 0)
            ))
        }
        return list
    }

    private fun doChouTask(drawType: String, task: TaskInfo): Boolean {
        return try {
            val s = AntFarmRpcCall.chouchouleDoFarmTask(drawType, task.taskId)
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                Log.farm("${if (drawType == "ipDraw") "IP抽抽乐" else "抽抽乐"}🧾️[任务: ${task.title}]")
                GlobalThreadPools.sleepCompat(if (task.title == "消耗饲料换机会") 1000L else 5000L)
                true
            } else false
        } catch (t: Throwable) {
            Log.printStackTrace("执行抽抽乐任务 err:", t)
            false
        }
    }

    private fun receiveTaskAward(drawType: String, taskId: String): Boolean {
        return try {
            val jo = JSONObject(AntFarmRpcCall.chouchouleReceiveFarmTaskAward(drawType, taskId))
            ResChecker.checkRes(TAG, jo)
        } catch (t: Throwable) {
            Log.printStackTrace("receiveFarmTaskAward err:", t)
            false
        }
    }

    private fun handleIpDraw() {
        try {
            val jo = JSONObject(AntFarmRpcCall.queryDrawMachineActivity())
            if (!ResChecker.checkRes(TAG, jo)) return

            val activity = jo.getJSONObject("drawMachineActivity")
            val endTime = activity.getLong("endTime")
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[${activity.optString("activityId")}]抽奖活动已结束")
                return
            }

            val drawTimes = jo.optInt("drawTimes", 0)
            repeat(drawTimes) {
                drawPrize("IP抽抽乐", AntFarmRpcCall.drawMachine())
                GlobalThreadPools.sleepCompat(5000L)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("handleIpDraw err:", t)
        }
    }

    private fun handleDailyDraw() {
        try {
            val jo = JSONObject(AntFarmRpcCall.enterDrawMachine())
            if (!ResChecker.checkRes(TAG, jo)) {
                Log.record(TAG, "抽奖活动进入失败")
                return
            }

            val userInfo = jo.getJSONObject("userInfo")
            val drawActivityInfo = jo.getJSONObject("drawActivityInfo")
            val endTime = drawActivityInfo.getLong("endTime")
            if (System.currentTimeMillis() > endTime) {
                Log.record(TAG, "该[${drawActivityInfo.optString("activityId")}]抽奖活动已结束")
                return
            }

            val leftDrawTimes = userInfo.optInt("leftDrawTimes", 0)
            val activityId = drawActivityInfo.optString("activityId")

            repeat(leftDrawTimes) {
                val call = if (activityId == "null") AntFarmRpcCall.DrawPrize() else AntFarmRpcCall.DrawPrize(activityId)
                drawPrize("抽抽乐", call)
                GlobalThreadPools.sleepCompat(5000L)
            }
        } catch (t: Throwable) {
            Log.printStackTrace("handleDailyDraw err:", t)
        }
    }

    private fun drawPrize(prefix: String, response: String) {
        try {
            val jo = JSONObject(response)
            if (ResChecker.checkRes(TAG, jo)) {
                val title = jo.getString("title")
                val prizeNum = jo.optInt("prizeNum", 1)
                Log.farm("$prefix🎁[领取: $title*$prizeNum]")
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        private val TAG = ChouChouLe::class.java.simpleName
    }
}
