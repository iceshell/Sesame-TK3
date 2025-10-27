package fansirsqi.xposed.sesame.task.antBookRead

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.RandomUtil

/**
 * 读书听书RPC调用
 *
 * @author cwj851
 * @since 2024/01/18
 */
object AntBookReadRpcCall {

    private const val VERSION = "1.0.1397"
    private const val CH_INFO = "ch_appcenter__chsub_9patch"
    private const val MINI_CLIENT_VERSION = "1.0.0"

    // ==================== 读书相关 ====================

    /**
     * 查询任务中心页面
     */
    @JvmStatic
    fun queryTaskCenterPage(): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.taskcenter.queryTaskCenterPage",
            "[{\"bannerId\":\"\",\"chInfo\":\"$CH_INFO\",\"hasAddHome\":false,\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"supportFeatures\":[\"prize_task_20230831\"],\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询迷你任务中心信息
     */
    @JvmStatic
    fun queryMiniTaskCenterInfo(): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.minitaskcenter.queryMiniTaskCenterInfo",
            "[{\"chInfo\":\"$CH_INFO\",\"hasAddHome\":false,\"isFromSync\":false,\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"needInfos\":\"\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 同步用户阅读信息
     *
     * @param bookId 书籍ID
     * @param chapterId 章节ID
     */
    @JvmStatic
    fun syncUserReadInfo(bookId: String, chapterId: String): String {
        val readCount = RandomUtil.nextInt(40, 200)
        val readTime = RandomUtil.nextInt(160, 170) * 10000
        return RequestManager.requestString(
            "com.alipay.antbookread.biz.mgw.syncUserReadInfo",
            "[{\"bookId\":\"$bookId\",\"chInfo\":\"$CH_INFO\",\"chapterId\":\"$chapterId\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"readCount\":$readCount,\"readTime\":$readTime,\"timeStamp\":${System.currentTimeMillis()},\"volumeId\":\"\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询阅读森林能量信息
     *
     * @param bookId 书籍ID
     */
    @JvmStatic
    fun queryReaderForestEnergyInfo(bookId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbookread.biz.mgw.queryReaderForestEnergyInfo",
            "[{\"bookId\":\"$bookId\",\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询首页
     */
    @JvmStatic
    fun queryHomePage(): String {
        return RequestManager.requestString(
            "com.alipay.antbookread.biz.mgw.queryHomePage",
            "[{\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询书籍目录信息
     *
     * @param bookId 书籍ID
     */
    @JvmStatic
    fun queryBookCatalogueInfo(bookId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbookread.biz.mgw.queryBookCatalogueInfo",
            "[{\"bookId\":\"$bookId\",\"chInfo\":\"$CH_INFO\",\"isInit\":true,\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"order\":1,\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询阅读内容
     *
     * @param bookId 书籍ID
     */
    @JvmStatic
    fun queryReaderContent(bookId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbookread.biz.mgw.queryReaderContent",
            "[{\"bookId\":\"$bookId\",\"chInfo\":\"$CH_INFO\",\"isInit\":true,\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"queryRecommend\":false,\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    // ==================== 任务相关 ====================

    /**
     * 查询宝箱
     */
    @JvmStatic
    fun queryTreasureBox(): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.taskcenter.queryTreasureBox",
            "[{\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 完成任务
     *
     * @param taskId 任务ID
     * @param taskType 任务类型
     */
    @JvmStatic
    fun taskFinish(taskId: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.taskcenter.taskFinish",
            "[{\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"taskId\":\"$taskId\",\"taskType\":\"$taskType\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 领取任务奖励
     *
     * @param taskId 任务ID
     * @param taskType 任务类型
     */
    @JvmStatic
    fun collectTaskPrize(taskId: String, taskType: String): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.taskcenter.collectTaskPrize",
            "[{\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"taskId\":\"$taskId\",\"taskType\":\"$taskType\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询应用层
     */
    @JvmStatic
    fun queryApplayer(): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.applayer.query",
            "[{\"spaceCode\":\"adPosId#2023112024200071171##sceneCode#null##mediaScene#42##rewardNum#1##spaceCode#READ_LISTEN_BOOK_TREASURE_FEEDS_FUSION##expCode#\"}]"
        )
    }

    /**
     * 完成服务任务
     *
     * @param bizId 业务ID
     */
    @JvmStatic
    fun serviceTaskFinish(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            "[{\"bizId\":\"$bizId\"}]"
        )
    }

    /**
     * 查询服务任务
     *
     * @param bizId 业务ID
     */
    @JvmStatic
    fun serviceTaskQuery(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.query",
            "[{\"bizId\":\"$bizId\"}]"
        )
    }

    /**
     * 打开宝箱
     */
    @JvmStatic
    fun openTreasureBox(): String {
        return RequestManager.requestString(
            "com.alipay.antbookpromo.taskcenter.openTreasureBox",
            "[{\"chInfo\":\"$CH_INFO\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    // ==================== 听书相关 ====================

    /**
     * 查询晚安森林主页
     */
    @JvmStatic
    fun queryEveningForestMainPage(): String {
        return RequestManager.requestString(
            "com.alipay.antbooks.biz.mgw.queryEveningForestMainPage",
            "[{\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询专辑详情页
     *
     * @param albumId 专辑ID
     */
    @JvmStatic
    fun queryAlbumDetailPage(albumId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbooks.biz.mgw.queryAlbumDetailPage",
            "[{\"albumId\":$albumId,\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询音频URL
     *
     * @param albumId 专辑ID
     * @param soundId 音频ID
     */
    @JvmStatic
    fun querySoundUrl(albumId: String, soundId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbooks.biz.mgw.querySoundUrl",
            "[{\"albumId\":$albumId,\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"sceneId\":\"EVENING_FOREST\",\"soundId\":$soundId,\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 同步用户播放数据
     *
     * @param albumId 专辑ID
     * @param soundId 音频ID
     */
    @JvmStatic
    fun syncUserPlayData(albumId: String, soundId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbooks.biz.mgw.syncUserPlayData",
            "[{\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"syncingPlayRecordRequestList\":[{\"albumId\":$albumId,\"position\":720,\"soundId\":$soundId,\"timestamp\":${System.currentTimeMillis()}}],\"yuyanVersion\":\"$VERSION\"}]"
        )
    }

    /**
     * 查询播放页
     *
     * @param albumId 专辑ID
     * @param soundId 音频ID
     */
    @JvmStatic
    fun queryPlayPage(albumId: String, soundId: String): String {
        return RequestManager.requestString(
            "com.alipay.antbooks.biz.mgw.queryPlayPage",
            "[{\"albumId\":$albumId,\"chInfo\":\"sy_wanansenlin_shouye\",\"miniClientVersion\":\"$MINI_CLIENT_VERSION\",\"sceneId\":\"EVENING_FOREST\",\"soundId\":$soundId,\"yuyanVersion\":\"$VERSION\"}]"
        )
    }
}
