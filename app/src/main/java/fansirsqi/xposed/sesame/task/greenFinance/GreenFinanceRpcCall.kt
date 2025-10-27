package fansirsqi.xposed.sesame.task.greenFinance

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.maps.UserMap.currentUid
import org.json.JSONArray

/**
 * 绿色经营RPC调用
 *
 * @author xiong
 */
object GreenFinanceRpcCall {

    /**
     * 查询任务
     *
     * @param appletId 小程序ID
     */
    @JvmStatic
    fun taskQuery(appletId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.task.taskQuery",
            "[{\"appletId\":\"$appletId\",\"completedBottom\":true}]"
        )
    }

    /**
     * 触发任务
     *
     * @param appletId 小程序ID
     * @param stageCode 阶段代码
     * @param taskCenId 任务中心ID
     */
    @JvmStatic
    fun taskTrigger(appletId: String, stageCode: String, taskCenId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.task.taskTrigger",
            "[{\"appletId\":\"$appletId\",\"stageCode\":\"$stageCode\",\"taskCenId\":\"$taskCenId\"}]"
        )
    }

    /**
     * 触发签到
     *
     * @param sceneId 场景ID
     */
    @JvmStatic
    fun signInTrigger(sceneId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.signin.trigger",
            "[{\"extInfo\":{},\"sceneId\":\"$sceneId\"}]"
        )
    }

    /**
     * 绿色经营首页
     */
    @JvmStatic
    fun greenFinanceIndex(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinancePageQueryService.indexV2",
            "[{\"clientVersion\":\"VERSION2\",\"custType\":\"MERCHANT\"}]"
        )
    }

    /**
     * 批量收取
     *
     * @param bsnIds 业务单号列表
     */
    @JvmStatic
    fun batchSelfCollect(bsnIds: JSONArray): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinancePointCollectService.batchSelfCollect",
            "[{\"bsnIds\":$bsnIds,\"clientVersion\":\"VERSION2\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 签到查询
     *
     * @param sceneId 场景ID
     */
    @JvmStatic
    fun signInQuery(sceneId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.signin.query",
            "[{\"cycleCount\":7,\"cycleType\":\"d\",\"extInfo\":{},\"needContinuous\":1,\"sceneId\":\"$sceneId\"}]"
        )
    }

    /**
     * 查询打卡记录
     *
     * @param firstBehaviorType 打卡类型
     */
    @JvmStatic
    fun queryUserTickItem(firstBehaviorType: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceTickService.queryUserTickItem",
            "[{\"custType\":\"MERCHANT\",\"firstBehaviorType\":\"$firstBehaviorType\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 提交打卡
     *
     * @param firstBehaviorType 打卡类型
     * @param behaviorCode 行为编码
     */
    @JvmStatic
    fun submitTick(firstBehaviorType: String, behaviorCode: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceTickService.submitTick",
            "[{\"custType\":\"MERCHANT\",\"firstBehaviorType\":\"$firstBehaviorType\",\"uid\":\"${currentUid}\",\"behaviorCode\":\"$behaviorCode\"}]"
        )
    }

    /**
     * 查询即将过期的金�?     *
     * @param day 多少天后过期
     */
    @JvmStatic
    fun queryExpireMcaPoint(day: Long): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinancePageQueryService.queryExpireMcaPoint",
            "[{\"custType\":\"MERCHANT\",\"profitType\":\"MYBK_LOAN_DISCOUNT\",\"uid\":\"${currentUid}\",\"expireDate\":\"${System.currentTimeMillis() + day * 24 * 60 * 60 * 1000}\"}]"
        )
    }

    /**
     * 查询所有捐赠项�?     */
    @JvmStatic
    fun queryAllDonationProjectNew(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceDonationService.queryAllDonationProjectNew",
            "[{\"custType\":\"MERCHANT\",\"subjectType\":\"ALL_DONATION\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 捐赠
     *
     * @param projectId 项目ID
     * @param amount 金额
     */
    @JvmStatic
    fun donation(projectId: String, amount: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceDonationService.donation",
            "[{\"custType\":\"MERCHANT\",\"donationGold\":\"$amount\",\"uid\":\"${currentUid}\",\"outbizNo\":\"${System.currentTimeMillis()}\",\"projectId\":\"$projectId\"}]"
        )
    }

    /**
     * 查询证明任务列表
     */
    @JvmStatic
    fun consultProveTaskList(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.consultProveTaskList",
            "[{\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询证明任务
     *
     * @param bizId 业务ID
     */
    @JvmStatic
    fun proveTaskQuery(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.proveTaskQuery",
            "[{\"bizId\":\"$bizId\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 证明任务触发
     *
     * @param bizId 业务ID
     */
    @JvmStatic
    fun proveTaskTrigger(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.proveTaskTrigger",
            "[{\"bizId\":\"$bizId\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 收集证明任务
     *
     * @param bizId 业务ID
     */
    @JvmStatic
    fun proveTaskCollect(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.proveTaskCollect",
            "[{\"bizId\":\"$bizId\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 收集
     *
     * @param bsnId 业务单号
     */
    @JvmStatic
    fun collect(bsnId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinancePointCollectService.collect",
            "[{\"bsnId\":\"$bsnId\",\"clientVersion\":\"VERSION2\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询签到证书
     */
    @JvmStatic
    fun queryCertificate(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceCertificationService.queryCertificate",
            "[{\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 生成签到证书
     */
    @JvmStatic
    fun generateCertificate(): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceCertificationService.generateCertificate",
            "[{\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询特权奖品
     *
     * @param campId 活动ID
     */
    @JvmStatic
    fun queryPrizes(campId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.camp.queryPrizes",
            "[{\"campIds\":[\"$campId\"]}]"
        )
    }

    /**
     * 绿色特权奖品领取触发
     *
     * @param campId 活动ID
     */
    @JvmStatic
    fun campTrigger(campId: String): String {
        return RequestManager.requestString(
            "com.alipay.loanpromoweb.promo.camp.trigger",
            "[{\"campId\":\"$campId\"}]"
        )
    }

    /**
     * 绿色评级任务
     *
     * @param bizType 业务类型（ECO_FRIENDLY_BAG_PROVE、classifyTrashCanProve�?     * @param imageUrl 图片URL
     */
    @JvmStatic
    fun proveTask(bizType: String, imageUrl: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.proveTask",
            "[{\"bizType\":\"$bizType\",\"custType\":\"MERCHANT\",\"imageUrl\":\"$imageUrl\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询证明任务状�?     *
     * @param taskId 任务ID
     */
    @JvmStatic
    fun queryProveTaskStatus(taskId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.newservice.GreenFinanceProveTaskService.queryProveTaskStatus",
            "[{\"taskId\":\"$taskId\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询好友排行�?     *
     * @param startIndex 起始索引
     */
    @JvmStatic
    fun queryRankingList(startIndex: Int): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinanceUserInteractionQueryService.queryRankingList",
            "[{\"clientVersion\":\"VERSION2\",\"custType\":\"MERCHANT\",\"includeMe\":true,\"onlyRealFriend\":true,\"pageLimit\":10,\"rankingScene\":\"FRIEND\",\"rankingType\":\"OVERALL\",\"startIndex\":$startIndex,\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 查询访客金币点数
     *
     * @param guestId 访客ID
     */
    @JvmStatic
    fun queryGuestIndexPoints(guestId: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinanceUserInteractionQueryService.queryGuestIndexPoints",
            "[{\"clientVersion\":\"VERSION2\",\"custType\":\"MERCHANT\",\"guestCustType\":\"MERCHANT\",\"guestUid\":\"$guestId\",\"uid\":\"${currentUid}\"}]"
        )
    }

    /**
     * 批量偷取金币
     *
     * @param bsnIds 业务单号列表
     * @param collectedUid 被收取用户ID
     */
    @JvmStatic
    fun batchSteal(bsnIds: JSONArray, collectedUid: String): String {
        return RequestManager.requestString(
            "com.alipay.mcaplatformunit.common.mobile.service.GreenFinancePointCollectService.batchSteal",
            "[{\"bsnIds\":$bsnIds,\"clientVersion\":\"VERSION2\",\"collectedCustType\":\"MERCHANT\",\"collectedUid\":\"$collectedUid\",\"custType\":\"MERCHANT\",\"uid\":\"${currentUid}\"}]"
        )
    }
}

