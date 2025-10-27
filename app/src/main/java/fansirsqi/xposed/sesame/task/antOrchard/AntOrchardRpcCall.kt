package fansirsqi.xposed.sesame.task.antOrchard

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * 芭芭农场RPC调用
 */
object AntOrchardRpcCall {

    private const val VERSION = "0.1.2401111000.31"
    private const val SOURCE = "ch_appcenter__chsub_9patch"
    private const val REQUEST_TYPE = "NORMAL"
    private const val SCENE_CODE_ORCHARD = "ORCHARD"

    /**
     * 获取农场首页
     */
    @JvmStatic
    fun orchardIndex(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardIndex",
            "[{\"inHomepage\":\"true\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 除草信息
     */
    @JvmStatic
    fun mowGrassInfo(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.mowGrassInfo",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"showRanking\":true,\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 批量雇佣动物推荐
     *
     * @param orchardUserId 农场用户ID
     */
    @JvmStatic
    fun batchHireAnimalRecommend(orchardUserId: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.batchHireAnimalRecommend",
            "[{\"orchardUserId\":\"$orchardUserId\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"sceneType\":\"weed\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 批量雇佣动物
     *
     * @param recommendGroupList 推荐组列表
     */
    @JvmStatic
    fun batchHireAnimal(recommendGroupList: List<String>): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.batchHireAnimal",
            "[{\"recommendGroupList\":[${recommendGroupList.joinToString(",")}],\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"sceneType\":\"weed\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 获取额外信息
     */
    @JvmStatic
    fun extraInfoGet(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoGet",
            "[{\"from\":\"entry\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"FUGUO\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 设置额外信息（查询肥料包）
     */
    @JvmStatic
    fun extraInfoSet(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.extraInfoSet",
            "[{\"bizCode\":\"fertilizerPacket\",\"bizParam\":{\"action\":\"queryCollectFertilizerPacket\"},\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询子地块活动
     *
     * @param treeLevel 树等级
     */
    @JvmStatic
    fun querySubplotsActivity(treeLevel: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.querySubplotsActivity",
            "[{\"activityType\":[\"WISH\",\"BATTLE\",\"HELP_FARMER\",\"DEFOLIATION\",\"CAMP_TAKEOVER\"],\"inHomepage\":false,\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"treeLevel\":\"$treeLevel\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 触发子地块活动
     *
     * @param activityId 活动ID
     * @param activityType 活动类型
     * @param optionKey 选项键
     */
    @JvmStatic
    fun triggerSubplotsActivity(activityId: String, activityType: String, optionKey: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.triggerSubplotsActivity",
            "[{\"activityId\":\"$activityId\",\"activityType\":\"$activityType\",\"optionKey\":\"$optionKey\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 领取农场权益
     *
     * @param activityId 活动ID
     * @param activityType 活动类型
     */
    @JvmStatic
    fun receiveOrchardRights(activityId: String, activityType: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.receiveOrchardRights",
            "[{\"activityId\":\"$activityId\",\"activityType\":\"$activityType\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 七日礼包抽奖
     */
    @JvmStatic
    fun drawLottery(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.drawLottery",
            "[{\"lotteryScene\":\"receiveLotteryPlus\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 同步农场首页
     */
    @JvmStatic
    fun orchardSyncIndex(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.orchardSyncIndex",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"syncIndexTypes\":\"QUERY_MAIN_ACCOUNT_INFO\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 施肥
     *
     * @param wua 肥料标识
     */
    @JvmStatic
    fun orchardSpreadManure(wua: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSpreadManure",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"useWua\":true,\"version\":\"$VERSION\",\"wua\":\"$wua\"}]"
        )
    }

    /**
     * 领取任务奖励
     *
     * @param sceneCode 场景代码
     * @param taskType 任务类型
     */
    @JvmStatic
    fun receiveTaskAward(sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":false,\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$sceneCode\",\"source\":\"$SOURCE\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 获取任务列表
     */
    @JvmStatic
    fun orchardListTask(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardListTask",
            "[{\"plantHiddenMMC\":\"false\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 签到
     */
    @JvmStatic
    fun orchardSign(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSign",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"signScene\":\"ANTFARM_ORCHARD_SIGN_V2\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 完成任务
     *
     * @param userId 用户ID
     * @param sceneCode 场景代码
     * @param taskType 任务类型
     */
    @JvmStatic
    fun finishTask(userId: String, sceneCode: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[{\"outBizNo\":\"$userId${System.currentTimeMillis()}\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$sceneCode\",\"source\":\"$SOURCE\",\"taskType\":\"$taskType\",\"userId\":\"$userId\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 触发淘宝任务
     *
     * @param taskId 任务ID
     * @param taskPlantType 任务种植类型
     */
    @JvmStatic
    fun triggerTbTask(taskId: String, taskPlantType: String): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.triggerTbTask",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"taskId\":\"$taskId\",\"taskPlantType\":\"$taskPlantType\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 选择种子
     */
    @JvmStatic
    fun orchardSelectSeed(): String {
        return RequestManager.requestString(
            "com.alipay.antfarm.orchardSelectSeed",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"seedCode\":\"rp\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    // ==================== 砸金蛋游戏 ====================

    /**
     * 查询游戏中心
     */
    @JvmStatic
    fun queryGameCenter(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.queryGameCenter",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 通知游戏
     *
     * @param appId 应用ID
     */
    @JvmStatic
    fun noticeGame(appId: String): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.noticeGame",
            "[{\"appId\":\"$appId\",\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    /**
     * 提交用户动作
     *
     * @param gameId 游戏ID
     */
    @JvmStatic
    fun submitUserAction(gameId: String): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.submitUserAction",
            "[{\"actionCode\":\"enterGame\",\"gameId\":\"$gameId\",\"paladinxVersion\":\"2.0.13\",\"source\":\"gameFramework\"}]"
        )
    }

    /**
     * 提交用户游戏时长动作
     *
     * @param gameAppId 游戏应用ID
     * @param source 来源
     */
    @JvmStatic
    fun submitUserPlayDurationAction(gameAppId: String, source: String): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.submitUserPlayDurationAction",
            "[{\"gameAppId\":\"$gameAppId\",\"playTime\":32,\"source\":\"$source\",\"statisticTag\":\"\"}]"
        )
    }

    /**
     * 砸金蛋
     */
    @JvmStatic
    fun smashedGoldenEgg(): String {
        return RequestManager.requestString(
            "com.alipay.antorchard.smashedGoldenEgg",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"seneCode\":\"$SCENE_CODE_ORCHARD\",\"source\":\"$SOURCE\",\"version\":\"$VERSION\"}]"
        )
    }

    // ==================== 助力好友 ====================

    /**
     * 达成被分享P2P
     *
     * @param shareId 分享ID
     */
    @JvmStatic
    fun achieveBeShareP2P(shareId: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.achieveBeShareP2P",
            "[{\"requestType\":\"$REQUEST_TYPE\",\"sceneCode\":\"ANTFARM_ORCHARD_SHARE_P2P\",\"shareId\":\"$shareId\",\"source\":\"share\",\"version\":\"$VERSION\"}]"
        )
    }
}
