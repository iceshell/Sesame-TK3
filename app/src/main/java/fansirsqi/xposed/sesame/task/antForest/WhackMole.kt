package fansirsqi.xposed.sesame.task.antForest

import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import org.json.JSONObject

/**
 * 打地鼠任务（6秒拼手速）
 *
 * @author Byseven
 * @date 2025/3/7
 */
object WhackMole {

    private const val TAG = "WhackMole"

    /**
     * 6秒拼手速 - 打地鼠
     */
    @JvmStatic
    fun startWhackMole() {
        try {
            val startTime = System.currentTimeMillis()
            
            var response = JSONObject(AntForestRpcCall.startWhackMole("senlinguangchangdadishu"))
            
            if (response.optBoolean("success")) {
                val moleInfoArray = response.optJSONArray("moleInfo")
                
                if (moleInfoArray != null) {
                    val moleIdList = mutableListOf<String>()
                    
                    // 收集每个地鼠的 ID
                    for (i in 0 until moleInfoArray.length()) {
                        val mole = moleInfoArray.getJSONObject(i)
                        val moleId = mole.getLong("id")
                        moleIdList.add(moleId.toString())
                    }
                    
                    if (moleIdList.isNotEmpty()) {
                        val token = response.getString("token") // 获取令牌
                        val elapsedTime = System.currentTimeMillis() - startTime // 计算已耗时间
                        
                        // 睡眠至6秒
                        GlobalThreadPools.sleepCompat((6000 - elapsedTime).coerceAtLeast(0))
                        
                        response = JSONObject(
                            AntForestRpcCall.settlementWhackMole(
                                token,
                                moleIdList,
                                "senlinguangchangdadishu"
                            )
                        )
                        
                        if (ResChecker.checkRes(TAG, response)) {
                            val totalEnergy = response.getInt("totalEnergy")
                            Log.forest("森林能量⚡️[获得:6秒拼手速能量 ${totalEnergy}g]")
                        }
                    }
                }
            } else {
                Log.runtime(TAG, response.getJSONObject("data").toString())
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "whackMole err")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 关闭6秒拼手速
     *
     * @return 是否成功关闭
     */
    @JvmStatic
    fun closeWhackMole(): Boolean {
        try {
            val response = JSONObject(AntForestRpcCall.closeWhackMole("senlinguangchangdadishu"))
            
            if (response.optBoolean("success")) {
                return true
            } else {
                Log.runtime(TAG, response.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.printStackTrace(t)
        }
        return false
    }
}
