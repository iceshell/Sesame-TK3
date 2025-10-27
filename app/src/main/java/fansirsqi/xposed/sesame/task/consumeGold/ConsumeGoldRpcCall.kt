package fansirsqi.xposed.sesame.task.consumeGold

import fansirsqi.xposed.sesame.hook.RequestManager
import java.util.UUID

/**
 * 消费金RPC调用
 */
object ConsumeGoldRpcCall {

    private const val ALIPAY_VERSION = "10.6.80.8000"

    /**
     * 生成请求ID
     */
    private fun getRequestId(): String {
        return UUID.randomUUID().toString()
            .split("-")
            .joinToString("") { it.substring(it.length / 2) }
            .uppercase()
    }

    /**
     * 生成客户端追踪ID
     */
    private fun getClientTraceId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 获取签到日历状态
     */
    @JvmStatic
    fun signinCalendar(): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.task.signin.calendar",
            "[{\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"clientTraceId\":\"${getClientTraceId()}\",\"clientVersion\":\"6.5.0\"}]"
        )
    }

    /**
     * 签到领取奖励
     */
    @JvmStatic
    fun taskOpenBoxAward(): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.task.openBoxAward",
            "[{\"actionAwardDetails\":[{\"actionType\":\"date_sign_start\"}],\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"bizType\":\"CONSUME_GOLD\",\"boxType\":\"CONSUME_GOLD_SIGN_DATE\",\"timeScaleType\":0,\"userType\":\"old\"}]"
        )
    }

    /**
     * 消费金首页
     */
    @JvmStatic
    fun promoIndex(): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.home.promo.index",
            "[{\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\"${getClientTraceId()}\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"UnFavorite\"}]"
        )
    }

    /**
     * 消费金抽奖触发
     */
    @JvmStatic
    fun promoTrigger(): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.index.promo.trigger",
            "[{\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\"${UUID.randomUUID()}\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"UnFavorite\",\"requestId\":\"${getRequestId()}\"}]"
        )
    }

    /**
     * 任务V2首页
     *
     * @param taskSceneCode 任务场景代码
     */
    @JvmStatic
    fun taskV2Index(taskSceneCode: String): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.taskV2.index",
            "[{\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"cacheMap\":{},\"clientTraceId\":\"${getClientTraceId()}\",\"clientVersion\":\"6.5.0\",\"favoriteStatus\":\"\",\"taskSceneCode\":\"$taskSceneCode\"}]"
        )
    }

    /**
     * 任务V2触发
     *
     * @param taskId 任务ID
     * @param taskSceneCode 任务场景代码
     * @param action 动作
     */
    @JvmStatic
    fun taskV2Trigger(taskId: String, taskSceneCode: String, action: String): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.taskV2.trigger",
            "[{\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"clientTraceId\":\"${getClientTraceId()}\",\"clientVersion\":\"6.5.0\",\"taskId\":\"$taskId\",\"taskSceneCode\":\"$taskSceneCode\",\"triggerAction\":\"$action\"}]"
        )
    }

    /**
     * 补签
     *
     * @param actionType 动作类型
     *                   - check 表示[二次确认]
     *                   - repair 表示[确认]
     * @param repairDate 补签日期
     */
    @JvmStatic
    fun signinTrigger(actionType: String, repairDate: String): String {
        return RequestManager.requestString(
            "alipay.mobile.ipsponsorprod.consume.gold.repair.signin.trigger",
            "[{\"actionType\":\"$actionType\",\"alipayAppVersion\":\"$ALIPAY_VERSION\",\"appClient\":\"Android\",\"appSource\":\"consumeGold\",\"bizType\":\"CONSUME_GOLD\",\"boxType\":\"CONSUME_GOLD_SIGN_DATE\",\"clientTraceId\":\"${getClientTraceId()}\",\"clientVersion\":\"6.5.0\",\"repairDate\":\"$repairDate\"}]"
        )
    }
}
