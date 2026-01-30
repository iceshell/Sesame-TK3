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
            var canSend = false

            if (!ResChecker.checkRes(TAG, response)) {
                val resultCode = response.optJSONObject("data")?.optString("resultCode").orEmpty()
                if (resultCode.isNotBlank()) {
                    Log.runtime(TAG, resultCode)
                }
                CoroutineUtils.sleepCompat(300)
            } else {
                val data = response.optJSONObject("data")
                canSend = data?.optBoolean("canSendEnergy", false) == true
            }

            if (canSend) {
                CoroutineUtils.sleepCompat(300)
                response = JSONObject(AntForestRpcCall.sendEnergyByAction(sourceType))
                if (ResChecker.checkRes(TAG, response)) {
                    val sendData = response.optJSONObject("data")
                    if (sendData?.optBoolean("canSendEnergy", false) == true) {
                        val receivedEnergyAmount = sendData.optInt("receivedEnergyAmount", 0)
                        if (receivedEnergyAmount > 0) {
                            Log.forest("集市逛街🛍[获得:能量${receivedEnergyAmount}g]")
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "sendEnergyByAction err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
