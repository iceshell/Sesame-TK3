package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.TimeUtil
import org.json.JSONArray
import org.json.JSONObject

/**
 * 医疗健康任务（绿色医疗、电子小票）
 *
 * @author Byseven
 * @date 2025/3/7
 */
object Healthcare {

    private const val TAG = "Healthcare"

    /**
     * 查询并收取森林能量
     *
     * @param scene 场景类型（FEEDS=绿色医疗，其他=电子小票）
     */
    @JvmStatic
    fun queryForestEnergy(scene: String) {
        try {
            var response = JSONObject(AntForestRpcCall.queryForestEnergy(scene))
            
            if (!ResChecker.checkRes(TAG, response)) {
                return
            }
            
            response = response.getJSONObject("data").getJSONObject("response")
            var energyList = response.getJSONArray("energyGeneratedList")
            
            // 收取已有的能量球
            if (energyList.length() > 0) {
                harvestForestEnergy(scene, energyList)
            }
            
            // 处理剩余的能量球
            val remainBubble = response.optInt("remainBubble")
            repeat(remainBubble) {
                energyList = produceForestEnergy(scene)
                if (energyList.length() == 0 || !harvestForestEnergy(scene, energyList)) {
                    return
                }
                TimeUtil.sleepCompat(1000)
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "queryForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
    }

    /**
     * 产生森林能量
     *
     * @param scene 场景类型
     * @return 产生的能量列表
     */
    private fun produceForestEnergy(scene: String): JSONArray {
        var energyGeneratedList = JSONArray()
        try {
            val response = JSONObject(AntForestRpcCall.produceForestEnergy(scene))
            
            if (ResChecker.checkRes(TAG, response)) {
                val data = response.getJSONObject("data").getJSONObject("response")
                energyGeneratedList = data.getJSONArray("energyGeneratedList")
                
                if (energyGeneratedList.length() > 0) {
                    val title = if (scene == "FEEDS") "绿色医疗" else "电子小票"
                    val cumulativeEnergy = data.getInt("cumulativeEnergy")
                    Log.forest("医疗健康🚑完成[$title]#产生[${cumulativeEnergy}g能量]")
                }
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "produceForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
        return energyGeneratedList
    }

    /**
     * 收取森林能量
     *
     * @param scene 场景类型
     * @param bubbles 能量球列表
     * @return 是否收取成功
     */
    private fun harvestForestEnergy(scene: String, bubbles: JSONArray): Boolean {
        try {
            val response = JSONObject(AntForestRpcCall.harvestForestEnergy(scene, bubbles))
            
            if (!ResChecker.checkRes(TAG, response)) {
                return false
            }
            
            val data = response.getJSONObject("data").getJSONObject("response")
            val collectedEnergy = data.getInt("collectedEnergy")
            
            if (collectedEnergy > 0) {
                val title = if (scene == "FEEDS") "绿色医疗" else "电子小票"
                Log.forest("医疗健康🚑收取[$title]#获得[${collectedEnergy}g能量]")
                return true
            }
        } catch (th: Throwable) {
            Log.runtime(TAG, "harvestForestEnergy err:")
            Log.printStackTrace(TAG, th)
        }
        return false
    }
}
