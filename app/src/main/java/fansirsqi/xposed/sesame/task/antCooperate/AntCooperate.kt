package fansirsqi.xposed.sesame.task.antCooperate

import org.json.JSONObject
import fansirsqi.xposed.sesame.entity.CooperateEntity
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.util.TimeUtil

class AntCooperate : ModelTask() {

    private val cooperateWaterList = SelectAndCountModelField(
        "cooperateWaterList", "合种浇水列表", LinkedHashMap(),
        CooperateEntity.getList(), "开启合种浇水后执行一次重载"
    )
    private val cooperateWaterTotalLimitList = SelectAndCountModelField(
        "cooperateWaterTotalLimitList", "浇水总量限制列表",
        LinkedHashMap(), CooperateEntity.getList()
    )
    private val cooperateSendCooperateBeckon = BooleanModelField(
        "cooperateSendCooperateBeckon", "合种 | 召唤队友浇水| 仅队长 ", false
    )

    override fun getName(): String = "合种"

    override fun getGroup(): ModelGroup = ModelGroup.FOREST

    override fun getIcon(): String = "AntCooperate.png"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(cooperateWaterList)
        modelFields.addField(cooperateWaterTotalLimitList)
        modelFields.addField(cooperateSendCooperateBeckon)
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

            val s = AntCooperateRpcCall.queryUserCooperatePlantList()
            var jo = JSONObject(s)
            if (ResChecker.checkRes(TAG, jo)) {
                Log.runtime(TAG, "获取合种列表成功")
                val userCurrentEnergy = jo.getInt("userCurrentEnergy")
                val ja = jo.getJSONArray("cooperatePlants")
                for (i in 0 until ja.length()) {
                    jo = ja.getJSONObject(i)
                    val cooperationId = jo.getString("cooperationId")
                    if (!jo.has("name")) {
                        val response = AntCooperateRpcCall.queryCooperatePlant(cooperationId)
                        jo = JSONObject(response).getJSONObject("cooperatePlant")
                    }
                    val admin = jo.getString("admin")
                    val name = jo.getString("name")
                    if (cooperateSendCooperateBeckon.value == true && UserMap.currentUid == admin) {
                        cooperateSendCooperateBeckon(cooperationId, name)
                    }
                    val waterDayLimit = jo.getInt("waterDayLimit")
                    Log.runtime(TAG, "合种[$name]: 日限额:$waterDayLimit")
                    IdMapManager.getInstance(CooperateMap::class.java).add(cooperationId, name)
                    
                    val currentUid = UserMap.currentUid
                    if (currentUid != null && !Status.canCooperateWaterToday(currentUid, cooperationId)) {
                        Log.runtime(TAG, "[$name]今日已浇水💦")
                        continue
                    }
                    
                    var waterId = cooperateWaterList.value?.get(cooperationId)
                    if (waterId != null) {
                        val limitNum = cooperateWaterTotalLimitList.value?.get(cooperationId)
                        if (limitNum != null) {
                            val cumulativeWaterAmount = calculatedWaterNum(cooperationId)
                            if (cumulativeWaterAmount < 0) {
                                Log.runtime(TAG, "当前用户[$currentUid]的累计浇水能量获取失败,跳过本次浇水！")
                                continue
                            }
                            waterId = limitNum - cumulativeWaterAmount
                            Log.runtime(TAG, "[$name] 调整后的浇水数量: $waterId")
                        }
                        if (waterId > waterDayLimit) {
                            waterId = waterDayLimit
                        }
                        if (waterId > userCurrentEnergy) {
                            waterId = userCurrentEnergy
                        }
                        if (waterId > 0) {
                            cooperateWater(cooperationId, waterId, name)
                        } else {
                            Log.runtime(TAG, "浇水数量为0，跳过[$name]")
                        }
                    } else {
                        Log.runtime(TAG, "浇水列表中没有为[$name]配置")
                    }
                }
            } else {
                Log.error(TAG, "获取合种列表失败:")
                Log.runtime(TAG + "获取合种列表失败:", jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            UserMap.currentUid?.let { IdMapManager.getInstance(CooperateMap::class.java).save(it) }
            Log.record(TAG, "执行结束-${getName()}")
        }
    }

    companion object {
        private val TAG = AntCooperate::class.java.simpleName

        @JvmStatic
        private fun cooperateWater(coopId: String, count: Int, name: String) {
            try {
                val currentUid = UserMap.currentUid ?: return
                val s = AntCooperateRpcCall.cooperateWater(currentUid, coopId, count)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG, jo)) {
                    Log.forest("合种浇水🚿[$name]${jo.getString("barrageText")}")
                    Status.cooperateWaterToday(currentUid, coopId)
                } else {
                    Log.runtime(TAG, "浇水失败[$name]: ${jo.getString("resultDesc")}")
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "cooperateWater err:")
                Log.printStackTrace(TAG, t)
            } finally {
                GlobalThreadPools.sleepCompat(1500)
            }
        }

        @JvmStatic
        private fun calculatedWaterNum(coopId: String): Int {
            try {
                val s = AntCooperateRpcCall.queryCooperateRank("A", coopId)
                val jo = JSONObject(s)
                if (jo.optBoolean("success", false)) {
                    val jaList = jo.getJSONArray("cooperateRankInfos")
                    for (i in 0 until jaList.length()) {
                        val joItem = jaList.getJSONObject(i)
                        val userId = joItem.getString("userId")
                        if (userId == UserMap.currentUid) {
                            val energySummation = joItem.optInt("energySummation", -1)
                            if (energySummation >= 0) {
                                Log.runtime(TAG, "当前用户[$userId]的累计浇水能量: $energySummation")
                            }
                            return energySummation
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "calculatedWaterNum err:")
                Log.printStackTrace(TAG, t)
            }
            return -1
        }

        @JvmStatic
        private fun cooperateSendCooperateBeckon(cooperationId: String, name: String) {
            try {
                if (TimeUtil.isNowBeforeTimeStr("1800")) {
                    return
                }
                TimeUtil.sleepCompat(500)
                var jo = JSONObject(AntCooperateRpcCall.queryCooperateRank("D", cooperationId))
                if (ResChecker.checkRes(TAG, jo)) {
                    val cooperateRankInfos = jo.getJSONArray("cooperateRankInfos")
                    for (i in 0 until cooperateRankInfos.length()) {
                        val rankInfo = cooperateRankInfos.getJSONObject(i)
                        if (rankInfo.getBoolean("canBeckon")) {
                            jo = JSONObject(AntCooperateRpcCall.sendCooperateBeckon(rankInfo.getString("userId"), cooperationId))
                            if (ResChecker.checkRes(TAG, jo)) {
                                Log.forest("合种🚿[$name]#召唤队友[${rankInfo.getString("displayName")}]成功")
                            }
                            TimeUtil.sleepCompat(1000)
                        }
                    }
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "cooperateSendCooperateBeckon err:")
                Log.printStackTrace(TAG, t)
            }
        }
    }
}
