package fansirsqi.xposed.sesame.task.antStall

import fansirsqi.xposed.sesame.hook.RequestManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * 蚂蚁新村 RPC调用
 * @author Constanline
 * @since 2023/08/22
 */
object AntStallRpcCall {
    private const val VERSION = "0.1.2508141056.25"
    
    @JvmStatic
    fun home(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.home",
            "[{\"arouseAppParams\":{},\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun settle(assetId: String, settleCoin: Int): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.settle",
            "[{\"assetId\":\"$assetId\",\"coinType\":\"MASTER\",\"settleCoin\":$settleCoin,\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun shopList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.shop.list",
            "[{\"freeTop\":false,\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun preOneKeyClose(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close.preOneKey",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun oneKeyClose(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyClose",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun preShopClose(shopId: String, billNo: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close.pre",
            "[{\"billNo\":\"$billNo\",\"shopId\":\"$shopId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun shopClose(shopId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.close",
            "[{\"shopId\":\"$shopId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun oneKeyOpen(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyOpen",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun shopOpen(friendSeatId: String, friendUserId: String, shopId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.open",
            "[{\"friendSeatId\":\"$friendSeatId\",\"friendUserId\":\"$friendUserId\",\"shopId\":\"$shopId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun rankCoinDonate(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.coin.donate",
            "[{\"source\":\"ANTFARM\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun friendHome(userId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.home",
            "[{\"arouseAppParams\":{},\"friendUserId\":\"$userId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun taskList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.list",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun signToday(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.sign.today",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun finishTask(outBizNo: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.finishTask",
            "[{\"outBizNo\":\"$outBizNo\",\"requestType\":\"RPC\",\"sceneCode\":\"ANTSTALL_TASK\",\"source\":\"AST\",\"systemType\":\"android\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun xlightPlugin(): String {
        return RequestManager.requestString(
            "com.alipay.adexchange.ad.facade.xlightPlugin",
            "[{\"positionRequest\":{\"extMap\":{\"xlightPlayInstanceId\":\"300004\"},\"referInfo\":{},\"spaceCode\":\"ANT_FARM_NEW_VILLAGE\"},\"sdkPageInfo\":{\"adComponentType\":\"FEEDS\",\"adComponentVersion\":\"4.11.13\",\"enableFusion\":true,\"networkType\":\"WIFI\",\"pageFrom\":\"ch_url-https://68687809.h5app.alipay.com/www/game.html\",\"pageNo\":1,\"pageUrl\":\"https://render.alipay.com/p/yuyan/180020010001256918/multi-stage-task.html?caprMode=sync&spaceCodeFeeds=ANT_FARM_NEW_VILLAGE&usePlayLink=true&xlightPlayInstanceId=300004\",\"session\":\"u_54b721d9fffd6_1904b8eba8f\",\"unionAppId\":\"2060090000304921\",\"usePlayLink\":\"true\",\"xlightSDKType\":\"h5\",\"xlightSDKVersion\":\"4.11.13\"}}]"
        )
    }
    
    @JvmStatic
    fun finish(playBizId: String, jsonObject: JSONObject): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.interaction.finish",
            "[{\"extendInfo\":{\"iepTaskSceneCode\":\"ANTSTALL_TASK\",\"iepTaskType\":\"ANTSTALL_XLIGHT_VARIABLE_AWARD\"},\"playBizId\":\"$playBizId\",\"playEventInfo\":$jsonObject,\"source\":\"adx\"}]"
        )
    }
    
    @JvmStatic
    fun queryCallAppSchema(sceneCode: String): String {
        return RequestManager.requestString(
            "alipay.antmember.callApp.queryCallAppSchema",
            "[{\"sceneCode\":\"$sceneCode\"}]"
        )
    }
    
    @JvmStatic
    fun receiveTaskAward(taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.receiveTaskAward",
            "[{\"ignoreLimit\":true,\"requestType\":\"RPC\",\"sceneCode\":\"ANTSTALL_TASK\",\"source\":\"AST\",\"systemType\":\"android\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun taskFinish(taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.finish",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun taskAward(amount: String, prizeId: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.award",
            "[{\"amount\":$amount,\"prizeId\":\"$prizeId\",\"source\":\"search\",\"systemType\":\"android\",\"taskType\":\"$taskType\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun taskBenefit(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.task.benefit",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun collectManure(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.collectManure",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun queryManureInfo(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.queryManureInfo",
            "[{\"queryManureType\":\"ANTSTALL\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun projectList(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.list",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun projectDetail(projectId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.detail",
            "[{\"projectId\":\"$projectId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun projectDonate(projectId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.project.donate",
            "[{\"bizNo\":\"${UUID.randomUUID()}\",\"projectId\":\"$projectId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun roadmap(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.village.roadmap",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun nextVillage(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.ast.next.village",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun rankInviteRegister(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.invite.register",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun friendInviteRegister(friendUserId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.invite.register",
            "[{\"friendUserId\":\"$friendUserId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    // ==================== 助力好友 ====================
    
    @JvmStatic
    fun shareP2P(): String {
        return RequestManager.requestString(
            "com.alipay.antiep.shareP2P",
            "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTSTALL_P2P_SHARER\",\"source\":\"ANTSTALL\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun achieveBeShareP2P(shareId: String): String {
        return RequestManager.requestString(
            "com.alipay.antiep.achieveBeShareP2P",
            "[{\"requestType\":\"RPC\",\"sceneCode\":\"ANTSTALL_P2P_SHARER\",\"shareId\":\"$shareId\",\"source\":\"ANTSTALL\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun shopSendBackPre(billNo: String, seatId: String, shopId: String, shopUserId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.shop.sendback.pre",
            "[{\"billNo\":\"$billNo\",\"seatId\":\"$seatId\",\"shopId\":\"$shopId\",\"shopUserId\":\"$shopUserId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun shopSendBack(seatId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.shop.sendback",
            "[{\"seatId\":\"$seatId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun rankInviteOpen(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.rank.invite.open",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun oneKeyInviteOpenShop(friendUserId: String, mySeatId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.user.shop.oneKeyInviteOpenShop",
            "[{\"friendUserId\":\"$friendUserId\",\"mySeatId\":\"$mySeatId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun dynamicLoss(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.dynamic.loss",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun throwManure(dynamicList: JSONArray): String {
        return RequestManager.requestString(
            "com.alipay.antstall.manure.throwManure",
            "[{\"dynamicList\":$dynamicList,\"sendMsg\":false,\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    @JvmStatic
    fun settleReceivable(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.self.settle.receivable",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    /**
     * 查找下一个可以贴罚单的好友
     */
    @JvmStatic
    fun nextTicketFriend(): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.nextTicketFriend",
            "[{\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
    
    /**
     * 贴罚单
     */
    @JvmStatic
    fun ticket(billNo: String, seatId: String, shopId: String, shopUserId: String, seatUserId: String): String {
        return RequestManager.requestString(
            "com.alipay.antstall.friend.paste.ticket",
            "[{\"billNo\":\"$billNo\",\"seatId\":\"$seatId\",\"shopId\":\"$shopId\",\"shopUserId\":\"$shopUserId\",\"seatUserId\":\"$seatUserId\",\"source\":\"search\",\"systemType\":\"android\",\"version\":\"$VERSION\"}]"
        )
    }
}
