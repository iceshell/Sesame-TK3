package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.CoroutineUtils
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import org.json.JSONObject

/**
 * 绿色生活任务
 */
object GreenLife {

    private const val TAG = "GreenLife"

    /**
     * 森林集市 - 通过逛街获取能量
     *
     * @param sourceType 来源类型
     */
    @JvmStatic
    fun ForestMarket(sourceType: String) {
        try {
            var response = JSONObject(AntForestRpcCall.consultForSendEnergyByAction(sourceType))
            
            if (ResChecker.checkRes(TAG, response)) {
                var data = response.getJSONObject("data")
                
                if (data.optBoolean("canSendEnergy", false)) {
                    CoroutineUtils.sleepCompat(300)
                    
                    response = JSONObject(AntForestRpcCall.sendEnergyByAction(sourceType))
                    
                    if (ResChecker.checkRes(TAG, response)) {
                        data = response.getJSONObject("data")
                        
                        if (data.optBoolean("canSendEnergy", false)) {
                            val receivedEnergyAmount = data.getInt("receivedEnergyAmount")
                            Log.forest("集市逛街🛍[获得:能量${receivedEnergyAmount}g]")
                        }
                    }
                }
            } else {
                Log.runtime(TAG, response.getJSONObject("data").getString("resultCode"))
                CoroutineUtils.sleepCompat(300)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "sendEnergyByAction err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
