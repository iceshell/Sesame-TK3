package fansirsqi.xposed.sesame.task.omegakoiTown

import fansirsqi.xposed.sesame.hook.RequestManager
import java.util.UUID

/**
 * 新村RPC调用
 */
object OmegakoiTownRpcCall {

    private const val VERSION = "2.0"

    /**
     * 生成UUID（仅取后半部分）
     */
    private fun getUuid(): String {
        return UUID.randomUUID().toString()
            .split("-")
            .joinToString("") { it.substring(it.length / 2) }
    }

    /**
     * 房屋生产
     */
    @JvmStatic
    fun houseProduct(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.house.product",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\",\"shouldScoreReward\":true}]"
        )
    }

    /**
     * 建造房屋
     */
    @JvmStatic
    fun houseBuild(groundId: String, houseId: String): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.house.build",
            "[{\"groundId\":\"$groundId\",\"houseId\":\"$houseId\",\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取用户积分
     */
    @JvmStatic
    fun getUserScore(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.user.getUserScore",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取可收集的气球
     */
    @JvmStatic
    fun getBalloonsReadyToCollect(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.balloon.getBalloonsReadyToCollect",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取用户任务
     */
    @JvmStatic
    fun getUserQuests(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.scenario.getUserQuests",
            "[{\"disableQuests\":true,\"outBizNo\":\"${UUID.randomUUID()}\",\"scenarioId\":\"shopNewestTips\"}]"
        )
    }

    /**
     * 完成任务
     */
    @JvmStatic
    fun completeQuest(questId: String, scenarioId: String): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.scenario.completeQuest",
            "[{\"optionIndex\":0,\"outBizNo\":\"${UUID.randomUUID()}\",\"questId\":\"$questId\",\"scenarioId\":\"$scenarioId\",\"showType\":\"mayor\"}]"
        )
    }

    /**
     * 购买地皮
     */
    @JvmStatic
    fun groundBuy(groundId: String): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.ground.buy",
            "[{\"groundId\":\"$groundId\",\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 根据目标获取当前气球
     */
    @JvmStatic
    fun getCurrentBalloonsByTarget(groundId: String): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.balloon.getCurrentBalloonsByTarget",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取用户任务列表
     */
    @JvmStatic
    fun getUserTasks(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.task.getUserTasks",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 查询应用信息
     */
    @JvmStatic
    fun queryAppInfo(appId: String): String {
        return RequestManager.requestString(
            "alipay.mappconfig.queryAppInfo",
            "[{\"baseInfoReq\":{\"appIds\":[\"$appId\"],\"platform\":\"ANDROID\",\"pre\":false,\"width\":0},\"packInfoReq\":{\"bundleid\":\"com.alipay.alipaywallet\",\"channel\":\"offical\",\"client\":\"10.5.36.8100\",\"env\":\"production\",\"platform\":\"android\",\"protocol\":\"1.0\",\"query\":\"{\\\"$appId\\\":{\\\"app_id\\\":\\\"$appId\\\",\\\"version\\\":\\\"*\\\",\\\"isTarget\\\":\\\"YES\\\"}}\",\"reqmode\":\"async\",\"sdk\":\"1.3.0.0\",\"system\":\"10\"},\"reqType\":2}]"
        )
    }

    /**
     * 触发任务奖励
     */
    @JvmStatic
    fun triggerTaskReward(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.task.triggerTaskReward",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\",\"taskId\":\"$taskId\"}]"
        )
    }

    /**
     * 获取分享ID
     */
    @JvmStatic
    fun getShareId(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.user.getShareId",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取凤蝶数据
     */
    @JvmStatic
    fun getFengdieData(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.user.getFengdieData",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取签到状态
     */
    @JvmStatic
    fun getSignInStatus(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.signIn.getSignInStatus",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 签到
     */
    @JvmStatic
    fun signIn(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.signIn.signIn",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取商品
     */
    @JvmStatic
    fun getProduct(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.shop.getProduct",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取用户地皮
     */
    @JvmStatic
    fun getUserGrounds(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.ground.getUserGrounds",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 获取用户房屋
     */
    @JvmStatic
    fun getUserHouses(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.house.getUserHouses",
            "[{\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 收集
     */
    @JvmStatic
    fun collect(houseId: String, id: Long): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.town.v2.house.collect",
            "[{\"houseId\":\"$houseId\",\"id\":$id,\"outBizNo\":\"${UUID.randomUUID()}\"}]"
        )
    }

    /**
     * 匹配人群
     */
    @JvmStatic
    fun matchCrowd(): String {
        return RequestManager.requestString(
            "com.alipay.omegakoi.common.user.matchCrowd",
            "[{\"crowdCodes\":[\"OUW7WQPH7\",\"OM9K933XZ\"],\"outBizNo\":\"60123460-b6ac-11ee-95b2-3be423343437\"}]"
        )
    }
}
