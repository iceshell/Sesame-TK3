package fansirsqi.xposed.sesame.task.antSports

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * 运动 RPC调用
 */
object AntSportsRpcCall {
    private const val CH_INFO = "ch_appcenter__chsub_9patch"
    private const val TIME_ZONE = "Asia\\/Shanghai"
    private const val VERSION = "3.0.1.2"
    private const val ALIPAY_APP_VERSION = "0.0.852"
    private const val CITY_CODE = "330100"
    private const val APP_ID = "2021002116659397"
    
    private const val FEATURES = """[
            "DAILY_STEPS_RANK_V2",
            "STEP_BATTLE",
            "CLUB_HOME_CARD",
            "NEW_HOME_PAGE_STATIC",
            "CLOUD_SDK_AUTH",
            "STAY_ON_COMPLETE",
            "EXTRA_TREASURE_BOX",
            "NEW_HOME_PAGE_STATIC",
            "SUPPORT_AI",
            "SUPPORT_TAB3",
            "SUPPORT_FLYRABBIT",
            "SUPPORT_NEW_MATCH",
            "EXTERNAL_ADVERTISEMENT_TASK",
            "PROP",
            "PROPV2",
            "ASIAN_GAMES"
        ]"""
    
    // ==================== 运动任务查询 ====================
    
    @JvmStatic
    fun queryCoinTaskPanel(): String {
        val args = """[{
            "canAddHome": false,
            "chInfo": "$CH_INFO",
            "clientAuthStatus": "not_support",
            "clientOS": "android",
            "features": $FEATURES,
            "topTaskId": ""
        }]"""
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.queryCoinTaskPanel", args)
    }
    
    @JvmStatic
    fun completeExerciseTasks(taskId: String): String {
        val args = """[{
            "chInfo": "$CH_INFO",
            "clientOS": "android",
            "features": $FEATURES,
            "taskAction": "JUMP",
            "taskId": "$taskId"
        }]"""
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.completeTask", args)
    }
    
    @JvmStatic
    fun sportsCheck_in(): String {
        val args = """[{
            "chInfo": "homecard",
            "clientOS": "android",
            "features": $FEATURES,
            "operatorType": "signIn"
        }]"""
        return RequestManager.requestString("com.alipay.sportshealth.biz.rpc.SportsHealthCoinTaskRpc.signInCoinTask", args)
    }
    
    @JvmStatic
    @JvmOverloads
    fun pickBubbleTaskEnergy(medEnergyBallInfoRecordId: String, pickAllEnergyBall: Boolean = false): String {
        val args = """[{
            "apiVersion": "energy",
            "chInfo": "medical_health",
            "clientOS": "android",
            "features": $FEATURES,
            "medEnergyBallInfoRecordIds": ["$medEnergyBallInfoRecordId"],
            "pickAllEnergyBall": $pickAllEnergyBall,
            "source": "SPORT"
        }]"""
        return RequestManager.requestString("com.alipay.neverland.biz.rpc.pickBubbleTaskEnergy", args)
    }
    
    @JvmStatic
    fun queryCoinBubbleModule(): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.sportsHealthHomeRpc.queryCoinBubbleModule",
            "[{\"bubbleId\":\"\",\"canAddHome\":false,\"chInfo\":\"$CH_INFO\",\"clientAuthStatus\":\"not_support\",\"clientOS\":\"android\",\"distributionChannel\":\"\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_AI\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]"
        )
    }
    
    @JvmStatic
    fun receiveCoinAsset(assetId: String, coinAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthCoinCenterRpc.receiveCoinAsset",
            "[{\"assetId\":\"$assetId\",\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"coinAmount\":$coinAmount,\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"tracertPos\":\"首页金币收集\"}]"
        )
    }
    
    @JvmStatic
    fun queryMyHomePage(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.queryMyHomePage",
            "[{\"alipayAppVersion\":\"$ALIPAY_APP_VERSION\",\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"pathListUsePage\":true,\"timeZone\":\"$TIME_ZONE\"}]"
        )
    }
    
    @JvmStatic
    fun join(pathId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.join",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"pathId\":\"$pathId\"}]"
        )
    }
    
    @JvmStatic
    fun openAndJoinFirst(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.user.openAndJoinFirst",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]"
        )
    }
    
    @JvmStatic
    fun go(day: String, rankCacheKey: String, stepCount: Int): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.map.go",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"day\":\"$day\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"needAllBox\":true,\"rankCacheKey\":\"$rankCacheKey\",\"timeZone\":\"$TIME_ZONE\",\"useStepCount\":$stepCount}]"
        )
    }
    
    @JvmStatic
    fun openTreasureBox(boxNo: String, userId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.treasureBox.openTreasureBox",
            "[{\"boxNo\":\"$boxNo\",\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"userId\":\"$userId\"}]"
        )
    }
    
    @JvmStatic
    fun queryBaseList(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.path.queryBaseList",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]"
        )
    }
    
    @JvmStatic
    fun queryProjectList(index: Int): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.charity.queryProjectList",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"index\":$index,\"projectListUseVertical\":true}]"
        )
    }
    
    @JvmStatic
    fun donate(donateCharityCoin: Int, projectId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.charity.donate",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"donateCharityCoin\":$donateCharityCoin,\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"projectId\":\"$projectId\"}]"
        )
    }
    
    @JvmStatic
    fun queryWalkStep(): String {
        return RequestManager.requestString(
            "alipay.antsports.walk.user.queryWalkStep",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"NEW_HOME_PAGE_STATIC\",\"SUPPORT_TAB3\",\"SUPPORT_FLYRABBIT\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"timeZone\":\"$TIME_ZONE\"}]"
        )
    }
    
    @JvmStatic
    fun walkDonateSignInfo(count: Int): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.walkDonateSignInfo",
            "[{\"needDonateAction\":false,\"source\":\"walkDonateHome\",\"steps\":$count,\"timezoneId\":\"$TIME_ZONE\"}]"
        )
    }
    
    @JvmStatic
    fun donateWalkHome(count: Int): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.home",
            "[{\"module\":\"3\",\"steps\":$count,\"timezoneId\":\"$TIME_ZONE\"}]"
        )
    }
    
    @JvmStatic
    fun exchange(actId: String, count: Int, donateToken: String): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.walk.exchange",
            "[{\"actId\":\"$actId\",\"count\":$count,\"donateToken\":\"$donateToken\",\"timezoneId\":\"$TIME_ZONE\",\"ver\":0}]"
        )
    }
    
    // ==================== 运动币兑好礼 ====================
    
    @JvmStatic
    fun queryItemDetail(itemId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryItemDetail",
            "[{\"itemId\":\"$itemId\"}]"
        )
    }
    
    @JvmStatic
    fun exchangeItem(itemId: String, coinAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.exchangeItem",
            "[{\"coinAmount\":$coinAmount,\"itemId\":\"$itemId\"}]"
        )
    }
    
    @JvmStatic
    fun queryExchangeRecordPage(exchangeRecordId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportshealth.biz.rpc.SportsHealthItemCenterRpc.queryExchangeRecordPage",
            "[{\"exchangeRecordId\":\"$exchangeRecordId\"}]"
        )
    }
    
    // ==================== 新版走路线 ====================
    
    @JvmStatic
    fun queryUser(): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryUser",
            "[{\"source\":\"$CH_INFO\",\"timeZone\":\"$TIME_ZONE\"}]"
        )
    }
    
    @JvmStatic
    fun queryThemeList(): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.theme.queryThemeList",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"]}]"
        )
    }
    
    @JvmStatic
    fun queryWorldMap(themeId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryWorldMap",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"themeId\":\"$themeId\"}]"
        )
    }
    
    @JvmStatic
    fun queryCityPath(cityId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryCityPath",
            "[{\"chInfo\":\"$CH_INFO\",\"clientOS\":\"android\",\"features\":[\"DAILY_STEPS_RANK_V2\",\"STEP_BATTLE\",\"CLUB_HOME_CARD\",\"NEW_HOME_PAGE_STATIC\",\"CLOUD_SDK_AUTH\",\"STAY_ON_COMPLETE\",\"EXTRA_TREASURE_BOX\",\"SUPPORT_AI\",\"SUPPORT_FLYRABBIT\",\"SUPPORT_NEW_MATCH\",\"EXTERNAL_ADVERTISEMENT_TASK\",\"PROP\",\"PROPV2\",\"ASIAN_GAMES\"],\"cityId\":\"$cityId\"}]"
        )
    }
    
    @JvmStatic
    fun queryPath(appId: String, date: String, pathId: String): String {
        val wufuRewardType = "WUFU_CARD"
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryPath",
            "[{\"appId\":\"$appId\",\"date\":\"$date\",\"pathId\":\"$pathId\",\"source\":\"$CH_INFO\",\"timeZone\":\"$TIME_ZONE\",\"wufuRewardType\":\"$wufuRewardType\"}]"
        )
    }
    
    @JvmStatic
    fun joinPath(pathId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.joinPath",
            "[{\"pathId\":\"$pathId\",\"source\":\"$CH_INFO\"}]"
        )
    }
    
    @JvmStatic
    fun walkGo(appId: String, date: String, pathId: String, useStepCount: Int): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.go",
            "[{\"appId\":\"$appId\",\"date\":\"$date\",\"pathId\":\"$pathId\",\"source\":\"$CH_INFO\",\"timeZone\":\"$TIME_ZONE\",\"useStepCount\":\"$useStepCount\"}]"
        )
    }
    
    @JvmStatic
    fun receiveEvent(eventBillNo: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.receiveEvent",
            "[{\"eventBillNo\":\"$eventBillNo\"}]"
        )
    }
    
    @JvmStatic
    fun queryPathReward(appId: String, pathId: String): String {
        return RequestManager.requestString(
            "com.alipay.sportsplay.biz.rpc.walk.queryPathReward",
            "[{\"appId\":\"$appId\",\"pathId\":\"$pathId\",\"source\":\"$CH_INFO\"}]"
        )
    }
    
    @JvmStatic
    fun exchangeSuccess(exchangeId: String): String {
        return RequestManager.requestString(
            "alipay.charity.mobile.donate.exchange.success",
            "[{\"exchangeId\":\"$exchangeId\",\"timezone\":\"GMT+08:00\",\"version\":\"$VERSION\"}]"
        )
    }
    
    // ==================== 文体中心 ====================
    
    @JvmStatic
    fun userTaskGroupQuery(groupId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTaskGroup.query",
            "[{\"cityCode\":\"$CITY_CODE\",\"groupId\":\"$groupId\"}]"
        )
    }
    
    @JvmStatic
    fun userTaskComplete(bizType: String, taskId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTask.complete",
            "[{\"bizType\":\"$bizType\",\"cityCode\":\"$CITY_CODE\",\"completedTime\":${System.currentTimeMillis()},\"taskId\":\"$taskId\"}]"
        )
    }
    
    @JvmStatic
    fun userTaskRightsReceive(taskId: String, userTaskId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.sports.userTaskRights.receive",
            "[{\"taskId\":\"$taskId\",\"userTaskId\":\"$userTaskId\"}]"
        )
    }
    
    @JvmStatic
    fun queryAccount(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.user.asset.query.account",
            "[{\"accountType\":\"TIYU_SEED\"}]"
        )
    }
    
    @JvmStatic
    fun queryRoundList(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.wenti.walk.queryRoundList",
            "[{}]"
        )
    }
    
    @JvmStatic
    fun participate(bettingPoints: Int, InstanceId: String, ResultId: String, roundId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.wenti.walk.participate",
            "[{\"bettingPoints\":$bettingPoints,\"guessInstanceId\":\"$InstanceId\",\"guessResultId\":\"$ResultId\",\"newParticipant\":false,\"roundId\":\"$roundId\",\"stepTimeZone\":\"Asia/Shanghai\"}]"
        )
    }
    
    @JvmStatic
    fun pathFeatureQuery(): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.feature.query",
            "[{\"appId\":\"$APP_ID\",\"features\":[\"USER_CURRENT_PATH_SIMPLE\"],\"sceneCode\":\"wenti_shijiebei\"}]"
        )
    }
    
    @JvmStatic
    fun pathMapJoin(pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.join",
            "[{\"appId\":\"$APP_ID\",\"pathId\":\"$pathId\"}]"
        )
    }
    
    @JvmStatic
    fun pathMapHomepage(pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.homepage",
            "[{\"appId\":\"$APP_ID\",\"pathId\":\"$pathId\"}]"
        )
    }
    
    @JvmStatic
    fun stepQuery(countDate: String, pathId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.step.query",
            "[{\"appId\":\"$APP_ID\",\"countDate\":\"$countDate\",\"pathId\":\"$pathId\",\"timeZone\":\"Asia/Shanghai\"}]"
        )
    }
    
    @JvmStatic
    fun tiyubizGo(countDate: String, goStepCount: Int, pathId: String, userPathRecordId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.go",
            "[{\"appId\":\"$APP_ID\",\"countDate\":\"$countDate\",\"goStepCount\":$goStepCount,\"pathId\":\"$pathId\",\"timeZone\":\"Asia/Shanghai\",\"userPathRecordId\":\"$userPathRecordId\"}]"
        )
    }
    
    @JvmStatic
    fun rewardReceive(pathId: String, userPathRewardId: String): String {
        return RequestManager.requestString(
            "alipay.tiyubiz.path.map.reward.receive",
            "[{\"appId\":\"$APP_ID\",\"pathId\":\"$pathId\",\"userPathRewardId\":\"$userPathRewardId\"}]"
        )
    }
    
    // ==================== 抢好友大战 ====================
    
    @JvmStatic
    fun queryClubHome(): String {
        return RequestManager.requestString(
            "alipay.antsports.club.home.queryClubHome",
            "[{\"chInfo\":\"healthstep\",\"timeZone\":\"Asia/Shanghai\"}]"
        )
    }
    
    @JvmStatic
    fun collectBubble(bubbleId: String) {
        RequestManager.requestString(
            "alipay.antsports.club.home.collectBubble",
            "[{\"bubbleId\":\"$bubbleId\",\"chInfo\":\"healthstep\"}]"
        )
    }
    
    @JvmStatic
    fun queryTrainItem(): String {
        return RequestManager.requestString(
            "alipay.antsports.club.train.queryTrainItem",
            "[{\"chInfo\":\"healthstep\"}]"
        )
    }
    
    @JvmStatic
    fun trainMember(itemType: String, memberId: String, originBossId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.train.trainMember",
            "[{\"chInfo\":\"healthstep\",\"itemType\":\"$itemType\",\"memberId\":\"$memberId\",\"originBossId\":\"$originBossId\"}]"
        )
    }
    
    @JvmStatic
    fun queryMemberPriceRanking(coinBalance: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.ranking.queryMemberPriceRanking",
            "[{\"buyMember\":\"true\",\"chInfo\":\"healthstep\",\"coinBalance\":\"$coinBalance\"}]"
        )
    }
    
    @JvmStatic
    fun queryClubMember(memberId: String, originBossId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.trade.queryClubMember",
            "[{\"chInfo\":\"healthstep\",\"memberId\":\"$memberId\",\"originBossId\":\"$originBossId\"}]"
        )
    }
    
    @JvmStatic
    fun buyMember(currentBossId: String, memberId: String, originBossId: String, priceInfo: String, roomId: String): String {
        return RequestManager.requestString(
            "alipay.antsports.club.trade.buyMember",
            "[{\"chInfo\":\"healthstep\",\"currentBossId\":\"$currentBossId\",\"memberId\":\"$memberId\",\"originBossId\":\"$originBossId\",\"priceInfo\":$priceInfo,\"roomId\":\"$roomId\"}]"
        )
    }
}
