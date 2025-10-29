package fansirsqi.xposed.sesame.task.antBookRead

import org.json.JSONObject
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.StringUtil

class AntBookRead : ModelTask() {

    override fun getName(): String = "读书听书"

    override fun getGroup(): ModelGroup = ModelGroup.OTHER

    override fun getIcon(): String = "AntBookRead.png"

    override fun getFields(): ModelFields = ModelFields()

    override fun check(): Boolean? {
        if (TaskCommon.IS_ENERGY_TIME || !TaskCommon.IS_AFTER_8AM) {
            return false
        }
        val executeTime = RuntimeInfo.getInstance().getLong("consumeGold", 0)
        return System.currentTimeMillis() - executeTime >= 21600000
    }

    override fun runJava() {
        try {
            Log.record("执行开始-${getName()}")
            RuntimeInfo.getInstance().put("consumeGold", System.currentTimeMillis())
            queryTaskCenterPage()
            queryTask()
            queryTreasureBox()
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record("执行结束-${getName()}")
        }
    }

    companion object {
        private val TAG = AntBookRead::class.java.simpleName

        @JvmStatic
        private fun queryTaskCenterPage() {
            try {
                var s = AntBookReadRpcCall.queryTaskCenterPage()
                var jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    val data = jo.getJSONObject("data")
                    val todayPlayDurationText = data.getJSONObject("benefitAggBlock").getString("todayPlayDurationText")
                    val PlayDuration = StringUtil.getSubString(todayPlayDurationText, "今日听读时长", "分钟").toInt()
                    if (PlayDuration < 450) {
                        jo = JSONObject(AntBookReadRpcCall.queryHomePage())
                        if (jo.optBoolean("success")) {
                            val bookList = jo.getJSONObject("data").getJSONArray("dynamicCardList").getJSONObject(0)
                                .getJSONObject("data").getJSONArray("bookList")
                            val bookListLength = bookList.length()
                            val postion = RandomUtil.nextInt(0, bookListLength - 1)
                            val book = bookList.getJSONObject(postion)
                            val bookId = book.getString("bookId")
                            jo = JSONObject(AntBookReadRpcCall.queryReaderContent(bookId))
                            if (jo.optBoolean("success")) {
                                val nextChapterId = jo.getJSONObject("data").getString("nextChapterId")
                                val name = jo.getJSONObject("data").getJSONObject("readerHomePageVO").getString("name")
                                for (i in 0 until 17) {
                                    var energy = 0
                                    jo = JSONObject(AntBookReadRpcCall.syncUserReadInfo(bookId, nextChapterId))
                                    if (jo.optBoolean("success")) {
                                        jo = JSONObject(AntBookReadRpcCall.queryReaderForestEnergyInfo(bookId))
                                        if (jo.optBoolean("success")) {
                                            val tips = jo.getJSONObject("data").getString("tips")
                                            if (tips.contains("已得")) {
                                                energy = StringUtil.getSubString(tips, "已得", "g").toInt()
                                            }
                                            Log.forest("阅读书籍📚[$name]#累计能量${energy}g")
                                        }
                                    }
                                    if (energy >= 150) {
                                        break
                                    } else {
                                        GlobalThreadPools.sleepCompat(1500L)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(s)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "queryTaskCenterPage err:")
                Log.printStackTrace(TAG, t)
            }
        }

        @JvmStatic
        private fun queryTask() {
            var doubleCheck = false
            try {
                val s = AntBookReadRpcCall.queryTaskCenterPage()
                var jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    val data = jo.getJSONObject("data")
                    val userTaskGroupList = data.getJSONObject("userTaskListModuleVO")
                        .getJSONArray("userTaskGroupList")
                    for (i in 0 until userTaskGroupList.length()) {
                        jo = userTaskGroupList.getJSONObject(i)
                        val userTaskList = jo.getJSONArray("userTaskList")
                        for (j in 0 until userTaskList.length()) {
                            var taskInfo = userTaskList.getJSONObject(j)
                            val taskStatus = taskInfo.getString("taskStatus")
                            val taskType = taskInfo.getString("taskType")
                            val title = taskInfo.getString("title")
                            when {
                                "TO_RECEIVE" == taskStatus -> {
                                    if ("READ_MULTISTAGE" == taskType) {
                                        val multiSubTaskList = taskInfo.getJSONArray("multiSubTaskList")
                                        for (k in 0 until multiSubTaskList.length()) {
                                            taskInfo = multiSubTaskList.getJSONObject(k)
                                            val subTaskStatus = taskInfo.getString("taskStatus")
                                            if ("TO_RECEIVE" == subTaskStatus) {
                                                val taskId = taskInfo.getString("taskId")
                                                collectTaskPrize(taskId, taskType, title)
                                            }
                                        }
                                    } else {
                                        val taskId = taskInfo.getString("taskId")
                                        collectTaskPrize(taskId, taskType, title)
                                    }
                                }
                                "NOT_DONE" == taskStatus -> {
                                    when (taskType) {
                                        "AD_VIDEO_TASK" -> {
                                            val taskId = taskInfo.getString("taskId")
                                            repeat(5) {
                                                taskFinish(taskId, taskType)
                                                GlobalThreadPools.sleepCompat(1500L)
                                                collectTaskPrize(taskId, taskType, title)
                                                GlobalThreadPools.sleepCompat(1500L)
                                            }
                                        }
                                        "FOLLOW_UP", "JUMP" -> {
                                            val taskId = taskInfo.getString("taskId")
                                            taskFinish(taskId, taskType)
                                            doubleCheck = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (doubleCheck)
                        queryTask()
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(s)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "queryTask err:")
                Log.printStackTrace(TAG, t)
            }
        }

        @JvmStatic
        private fun collectTaskPrize(taskId: String, taskType: String, name: String) {
            try {
                val s = AntBookReadRpcCall.collectTaskPrize(taskId, taskType)
                val jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    val coinNum = jo.getJSONObject("data").getInt("coinNum")
                    Log.other("阅读任务📖[$name]#$coinNum")
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "collectTaskPrize err:")
                Log.printStackTrace(TAG, t)
            }
        }

        @JvmStatic
        private fun taskFinish(taskId: String, taskType: String) {
            try {
                val s = AntBookReadRpcCall.taskFinish(taskId, taskType)
                val jo = JSONObject(s)
                jo.optBoolean("success")
            } catch (t: Throwable) {
                Log.runtime(TAG, "taskFinish err:")
                Log.printStackTrace(TAG, t)
            }
        }

        @JvmStatic
        private fun queryTreasureBox() {
            try {
                var s = AntBookReadRpcCall.queryTreasureBox()
                var jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    val treasureBoxVo = jo.getJSONObject("data").getJSONObject("treasureBoxVo")
                    if (treasureBoxVo.has("countdown"))
                        return
                    val status = treasureBoxVo.getString("status")
                    if ("CAN_OPEN" == status) {
                        jo = JSONObject(AntBookReadRpcCall.openTreasureBox())
                        if (jo.optBoolean("success")) {
                            val coinNum = jo.getJSONObject("data").getInt("coinNum")
                            Log.other("阅读任务📖[打开宝箱]#$coinNum")
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "queryTreasureBox err:")
                Log.printStackTrace(TAG, t)
            }
        }
    }
}
