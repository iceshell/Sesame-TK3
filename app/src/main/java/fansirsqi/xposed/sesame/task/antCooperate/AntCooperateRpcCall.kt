package fansirsqi.xposed.sesame.task.antCooperate

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * 合种RPC调用
 */
object AntCooperateRpcCall {

    private const val VERSION = "20230501"

    /**
     * 查询用户合种列表
     *
     * @return RPC响应字符串
     */
    @JvmStatic
    fun queryUserCooperatePlantList(): String {
        return RequestManager.requestString("alipay.antmember.forest.h5.queryUserCooperatePlantList", "[{}]")
    }

    /**
     * 查询合种详情
     *
     * @param coopId 合种ID
     * @return RPC响应字符串
     */
    @JvmStatic
    fun queryCooperatePlant(coopId: String): String {
        val args = "[{\"cooperationId\":\"$coopId\"}]"
        return RequestManager.requestString("alipay.antmember.forest.h5.queryCooperatePlant", args)
    }

    /**
     * 合种浇水
     *
     * @param uid 用户ID
     * @param coopId 合种ID
     * @param count 浇水克数
     * @return RPC响应字符串
     */
    @JvmStatic
    fun cooperateWater(uid: String, coopId: String, count: Int): String {
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.cooperateWater",
            "[{\"bizNo\":\"${uid}_${coopId}_${System.currentTimeMillis()}\",\"cooperationId\":\"$coopId\",\"energyCount\":$count,\"source\":\"\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 获取合种浇水量排行
     *
     * @param bizType 参数：D/A，"D"为查询当天，"A"为查询所有
     * @param coopId 合种ID
     * @return RPC响应字符串
     */
    @JvmStatic
    fun queryCooperateRank(bizType: String, coopId: String): String {
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.queryCooperateRank",
            "[{\"bizType\":\"$bizType\",\"cooperationId\":\"$coopId\",\"source\":\"chInfo_ch_url-https://render.alipay.com/p/yuyan/180020010001247580/home.html\"}]"
        )
    }

    /**
     * 召唤队友浇水
     *
     * @param userId 用户ID
     * @param cooperationId 合种ID
     * @return RPC响应字符串
     * @throws JSONException JSON构建异常
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun sendCooperateBeckon(userId: String, cooperationId: String): String {
        val params = JSONObject().apply {
            put("bizImage", "https://gw.alipayobjects.com/zos/rmsportal/gzYPfxdAxLrkzFUeVkiY.jpg")
            put("link", "alipays://platformapi/startapp?appId=66666886&url=%2Fwww%2Fcooperation%2Findex.htm%3FcooperationId%3D$cooperationId%26sourceName%3Dcard")
            put("midTitle", "快来给我们的树苗浇水，让它快快长大。")
            put("noticeLink", "alipays://platformapi/startapp?appId=60000002&url=https%3A%2F%2Frender.alipay.com%2Fp%2Fc%2F17ussbd8vtfg%2Fmessage.html%3FsourceName%3Dcard&showOptionMenu=NO&transparentTitle=NO")
            put("topTitle", "树苗需要你的呵护")
            put("source", "chInfo_ch_url-https://render.alipay.com/p/yuyan/180020010001247580/home.html")
            put("cooperationId", cooperationId)
            put("userId", userId)
        }
        
        return RequestManager.requestString(
            "alipay.antmember.forest.h5.sendCooperateBeckon",
            JSONArray().put(params).toString()
        )
    }
}
