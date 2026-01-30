package fansirsqi.xposed.sesame.task.reserve

import org.json.JSONException
import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.ReserveEntity
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.ReserveaMap
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.data.Status
import kotlinx.coroutines.delay

class Reserve : ModelTask() {
    
    private var reserveList: SelectAndCountModelField? = null

    override fun getName(): String = "保护地"

    override fun getGroup(): ModelGroup = ModelGroup.FOREST

    override fun getIcon(): String = "Reserve.png"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(SelectAndCountModelField("reserveList", "保护地列表", LinkedHashMap(), ReserveEntity::getListAsMapperEntity).also { reserveList = it })
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
        GlobalThreadPools.execute {
            runSuspend()
        }
    }

    override suspend fun runSuspend() {
        try {
            Log.record(TAG, "开始保护地任务")
            initReserve()
            animalReserve()
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "保护地任务")
        }
    }

    private suspend fun animalReserve() {
        try {
            Log.record(TAG, "开始执行-${getName()}")
            var s: String? = ReserveRpcCall.queryTreeItemsForExchange()
            if (s == null) {
                delay(RandomUtil.delay().toLong())
                s = ReserveRpcCall.queryTreeItemsForExchange()
            }
            val jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("treeItems")
                for (i in 0 until ja.length()) {
                    val item = ja.getJSONObject(i)
                    if (!item.has("projectType")) {
                        continue
                    }
                    if (item.getString("projectType") != "RESERVE") {
                        continue
                    }
                    if (item.getString("applyAction") != "AVAILABLE") {
                        continue
                    }
                    val projectId = item.getString("itemId")
                    val itemName = item.getString("itemName")
                    val map = reserveList?.value ?: continue
                    val value = map[projectId]
                    if (value != null && value > 0 && Status.canReserveToday(projectId, value)) {
                        exchangeTree(projectId, itemName, value)
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "animalReserve err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "结束执行-${getName()}")
        }
    }

    private fun queryTreeForExchange(projectId: String): Boolean {
        try {
            val s = ReserveRpcCall.queryTreeForExchange(projectId)
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                val applyAction = jo.getString("applyAction")
                val currentEnergy = jo.getInt("currentEnergy")
                jo = jo.getJSONObject("exchangeableTree")
                return if (applyAction == "AVAILABLE") {
                    if (currentEnergy >= jo.getInt("energy")) {
                        true
                    } else {
                        Log.forest("领保护地🏕️[${jo.getString("projectName")}]#能量不足停止申请")
                        false
                    }
                } else {
                    Log.forest("领保护地🏕️[${jo.getString("projectName")}]#似乎没有了")
                    false
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryTreeForExchange err:")
            Log.printStackTrace(TAG, t)
        }
        return false
    }

    private suspend fun exchangeTree(projectId: String, itemName: String, count: Int) {
        try {
            var canApply = queryTreeForExchange(projectId)
            if (!canApply)
                return
            for (applyCount in 1..count) {
                val s = ReserveRpcCall.exchangeTree(projectId)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG, jo)) {
                    val vitalityAmount = jo.optInt("vitalityAmount", 0)
                    val appliedTimes = Status.getReserveTimes(projectId) + 1
                    val str = "领保护地🏕️[$itemName]#第${appliedTimes}次" +
                            if (vitalityAmount > 0) "-活力值+$vitalityAmount" else ""
                    Log.forest(str)
                    Status.reserveToday(projectId, 1)
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(jo.toString())
                    Log.forest("领保护地🏕️[$itemName]#发生未知错误，停止申请")
                    break
                }
                delay(300)
                canApply = queryTreeForExchange(projectId)
                if (!canApply) {
                    break
                } else {
                    delay(300)
                }
                if (!Status.canReserveToday(projectId, count))
                    break
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "exchangeTree err:")
            Log.printStackTrace(TAG, t)
        }
    }

    companion object {
        private val TAG = Reserve::class.java.simpleName

        @JvmStatic
        fun initReserve() {
            try {
                val response = ReserveRpcCall.queryTreeItemsForExchange()
                val jsonResponse = JSONObject(response)
                if (ResChecker.checkRes(TAG, jsonResponse)) {
                    val treeItems = jsonResponse.optJSONArray("treeItems")
                    if (treeItems != null) {
                        for (i in 0 until treeItems.length()) {
                            val item = treeItems.getJSONObject(i)
                            if (!item.has("projectType")) {
                                continue
                            }
                            if (item.getString("projectType") == "RESERVE" && item.getString("applyAction") == "AVAILABLE") {
                                val itemId = item.getString("itemId")
                                val itemName = item.getString("itemName")
                                val energy = item.getInt("energy")
                                IdMapManager.getInstance(ReserveaMap::class.java).add(itemId, "$itemName(${energy}g)")
                            }
                        }
                        Log.runtime(TAG, "初始化保护地任务成功。")
                    }
                    IdMapManager.getInstance(ReserveaMap::class.java).save()
                } else {
                    Log.runtime(jsonResponse.optString("resultDesc", "未知错误"))
                }
            } catch (e: JSONException) {
                Log.runtime(TAG, "JSON 解析错误：${e.message}")
                Log.printStackTrace(e)
                IdMapManager.getInstance(ReserveaMap::class.java).load()
            } catch (e: Exception) {
                Log.runtime(TAG, "初始化保护地任务时出错：${e.message}")
                Log.printStackTrace(e)
                IdMapManager.getInstance(ReserveaMap::class.java).load()
            }
        }
    }
}
