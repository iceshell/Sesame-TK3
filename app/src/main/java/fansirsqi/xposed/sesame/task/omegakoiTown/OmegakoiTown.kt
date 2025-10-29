package fansirsqi.xposed.sesame.task.omegakoiTown

import org.json.JSONObject
import java.text.DecimalFormat
import java.text.NumberFormat
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.Log

class OmegakoiTown : ModelTask() {

    enum class RewardType(val rewardName: String) {
        gold("金币"), diamond("钻石"), dyestuff("颜料"), rubber("橡胶"),
        glass("玻璃"), certificate("合格证"), shipping("包邮券"), tpuPhoneCaseCertificate("TPU手机壳合格证"),
        glassPhoneCaseCertificate("玻璃手机壳合格证"), canvasBagCertificate("帆布袋合格证"), notebookCertificate("记事本合格证"),
        box("快递包装盒"), paper("纸张"), cotton("棉花")
    }

    enum class HouseType {
        houseTrainStation, houseStop, houseBusStation, houseGas, houseSchool, houseService, houseHospital, housePolice,
        houseBank, houseRecycle, houseWasteTreatmentPlant, houseMetro, houseKfc, houseManicureShop, housePhoto, house5g,
        houseGame, houseLucky, housePrint, houseBook, houseGrocery, houseScience, housemarket1, houseMcd,
        houseStarbucks, houseRestaurant, houseFruit, houseDessert, houseClothes, zhiketang, houseFlower, houseMedicine,
        housePet, houseChick, houseFamilyMart, houseHouse, houseFlat, houseVilla, houseResident, housePowerPlant,
        houseWaterPlant, houseDailyChemicalFactory, houseToyFactory, houseSewageTreatmentPlant, houseSports, houseCinema,
        houseCotton, houseMarket, houseStadium, houseHotel, housebusiness, houseOrchard, housePark, houseFurnitureFactory,
        houseChipFactory, houseChemicalPlant, houseThermalPowerPlant, houseExpressStation, houseDormitory, houseCanteen,
        houseAdministrationBuilding, houseGourmetPalace, housePaperMill, houseAuctionHouse, houseCatHouse, houseStarPickingPavilion
    }

    override fun getName(): String = "小镇"
    override fun getGroup(): ModelGroup = ModelGroup.OTHER
    override fun getFields(): ModelFields = ModelFields()
    override fun getIcon(): String = "OmegakoiTown.png"

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
                val executeTime = RuntimeInfo.getInstance().getLong("omegakoiTown", 0)
                System.currentTimeMillis() - executeTime >= 21600000
            }
        }
    }

    override fun runJava() {
        try {
            Log.record("开始执行-${getName()}")
            RuntimeInfo.getInstance().put("omegakoiTown", System.currentTimeMillis())
            getUserTasks()
            getSignInStatus()
            houseProduct()
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record("结束执行-${getName()}")
        }
    }

    private fun getUserTasks() {
        try {
            val s = OmegakoiTownRpcCall.getUserTasks()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val result = jo.getJSONObject("result")
                val tasks = result.getJSONArray("tasks")
                for (i in 0 until tasks.length()) {
                    jo = tasks.getJSONObject(i)
                    val done = jo.getBoolean("done")
                    val hasRewarded = jo.getBoolean("hasRewarded")
                    if (done && !hasRewarded) {
                        val task = jo.getJSONObject("task")
                        val name = task.getString("name")
                        val taskId = task.getString("taskId")
                        if ("dailyBuild" == taskId) continue
                        val amount = task.getJSONObject("reward").getInt("amount")
                        val itemId = task.getJSONObject("reward").getString("itemId")
                        try {
                            val rewardType = RewardType.valueOf(itemId)
                            jo = JSONObject(OmegakoiTownRpcCall.triggerTaskReward(taskId))
                            if (jo.optBoolean("success")) {
                                Log.other("小镇任务🌇[$name]#$amount[${rewardType.rewardName}]")
                            }
                        } catch (th: Throwable) {
                            Log.runtime(TAG, "spec RewardType:$itemId;未知的类型")
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "getUserTasks err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun getSignInStatus() {
        try {
            var s = OmegakoiTownRpcCall.getSignInStatus()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val signed = jo.getJSONObject("result").getBoolean("signed")
                if (!signed) {
                    jo = JSONObject(OmegakoiTownRpcCall.signIn())
                    val diffItem = jo.getJSONObject("result").getJSONArray("diffItems").getJSONObject(0)
                    val amount = diffItem.getInt("amount")
                    val itemId = diffItem.getString("itemId")
                    val rewardType = RewardType.valueOf(itemId)
                    Log.other("小镇签到[${rewardType.rewardName}]#$amount")
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "getSignInStatus err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private fun houseProduct() {
        try {
            val s = OmegakoiTownRpcCall.houseProduct()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val result = jo.getJSONObject("result")
                val userHouses = result.getJSONArray("userHouses")
                for (i in 0 until userHouses.length()) {
                    jo = userHouses.getJSONObject(i)
                    val extraInfo = jo.getJSONObject("extraInfo")
                    if (!extraInfo.has("toBeCollected")) continue
                    val toBeCollected = extraInfo.optJSONArray("toBeCollected")
                    if (toBeCollected != null && toBeCollected.length() > 0) {
                        val amount = toBeCollected.getJSONObject(0).getDouble("amount")
                        if (amount < 500) continue
                        val houseId = jo.getString("houseId")
                        val id = jo.getLong("id")
                        jo = JSONObject(OmegakoiTownRpcCall.collect(houseId, id))
                        if (jo.optBoolean("success")) {
                            val houseType = HouseType.valueOf(houseId)
                            val itemId = jo.getJSONObject("result").getJSONArray("rewards").getJSONObject(0)
                                .getString("itemId")
                            val rewardType = RewardType.valueOf(itemId)
                            val numberFormat = NumberFormat.getNumberInstance()
                            (numberFormat as DecimalFormat).applyPattern("#.00")
                            val formattedAmount = numberFormat.format(amount)
                            Log.other("小镇收金🌇[${houseType.name}]#$formattedAmount${rewardType.rewardName}")
                        }
                    }
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "getUserTasks err:")
            Log.printStackTrace(TAG, t)
        }
    }

    companion object {
        private val TAG = OmegakoiTown::class.java.simpleName
    }
}
