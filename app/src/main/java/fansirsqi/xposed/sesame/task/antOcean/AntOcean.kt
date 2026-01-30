package fansirsqi.xposed.sesame.task.antOcean

import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.entity.AlipayBeach
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectAndCountModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.newutil.DataStore
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.TaskStatus
import fansirsqi.xposed.sesame.task.antForest.AntForestRpcCall
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.ResChecker
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author Constanline
 * @since 2023/08/01
 */
class AntOcean : ModelTask() {

    /**
     * 申请动作枚举
     */
    enum class ApplyAction(val code: Int, val desc: String) {
        AVAILABLE(0, "可用"),
        NO_STOCK(1, "无库存"),
        ENERGY_LACK(2, "能量不足");

        companion object {
            /**
             * 根据字符串获取对应枚举
             */
            fun fromString(value: String): ApplyAction? {
                for (action in values()) {
                    if (action.name.equals(value, ignoreCase = true)) {
                        return action
                    }
                }
                Log.error("ApplyAction", "Unknown applyAction: $value")
                return null
            }
        }
    }

    /**
     * 保护类型接口常量
     */
    object ProtectType {
        const val DONT_PROTECT = 0
        const val PROTECT_ALL = 1
        const val PROTECT_BEACH = 2
        val nickNames = arrayOf("不保护", "保护全部", "仅保护沙滩")
    }

    /**
     * 清理类型接口常量
     */
    object CleanOceanType {
        const val CLEAN = 0
        const val DONT_CLEAN = 1
        val nickNames = arrayOf("选中清理", "选中不清理")
    }

    companion object {
        private const val TAG = "AntOcean"

        /**
         * 保护类型字段（静态）
         */
        private var userprotectType: ChoiceModelField? = null
    }

    /**
     * 海洋任务
     */
    private var dailyOceanTask: BooleanModelField? = null

    /**
     * 清理 | 开启
     */
    private var cleanOcean: BooleanModelField? = null

    /**
     * 清理 | 动作
     */
    private var cleanOceanType: ChoiceModelField? = null

    /**
     * 清理 | 好友列表
     */
    private var cleanOceanList: SelectModelField? = null

    /**
     * 神奇海洋 | 制作万能拼图
     */
    private var exchangeProp: BooleanModelField? = null

    /**
     * 神奇海洋 | 使用万能拼图
     */
    private var usePropByType: BooleanModelField? = null

    /**
     * 保护 | 开启
     */
    private var protectOcean: BooleanModelField? = null

    /**
     * 保护 | 海洋列表
     */
    private var protectOceanList: SelectAndCountModelField? = null

    private var PDL_task: BooleanModelField? = null

    private val oceanTaskTryCount = ConcurrentHashMap<String, AtomicInteger>()

    override fun getName(): String {
        return "海洋"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.FOREST
    }

    override fun getIcon(): String {
        return "AntOcean.png"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField("dailyOceanTask", "海洋任务", false).also { dailyOceanTask = it }
        )
        modelFields.addField(
            BooleanModelField("cleanOcean", "清理 | 开启", false).also { cleanOcean = it }
        )
        modelFields.addField(
            ChoiceModelField(
                "cleanOceanType",
                "清理 | 动作",
                CleanOceanType.DONT_CLEAN,
                CleanOceanType.nickNames
            ).also { cleanOceanType = it }
        )
        modelFields.addField(
            SelectModelField(
                "cleanOceanList",
                "清理 | 好友列表",
                LinkedHashSet(),
                AlipayUser::getListAsMapperEntity
            ).also { cleanOceanList = it }
        )
        modelFields.addField(
            BooleanModelField("exchangeProp", "神奇海洋 | 制作万能拼图", false).also { exchangeProp = it }
        )
        modelFields.addField(
            BooleanModelField("usePropByType", "神奇海洋 | 使用万能拼图", false).also { usePropByType = it }
        )
        modelFields.addField(
            ChoiceModelField(
                "userprotectType",
                "保护 | 类型",
                ProtectType.DONT_PROTECT,
                ProtectType.nickNames
            ).also { userprotectType = it }
        )
        modelFields.addField(
            SelectAndCountModelField(
                "protectOceanList",
                "保护 | 海洋列表",
                LinkedHashMap(),
                AlipayBeach::getListAsMapperEntity
            ).also { protectOceanList = it }
        )
        modelFields.addField(
            BooleanModelField("PDL_task", "潘多拉任务", false).also { PDL_task = it }
        )
        return modelFields
    }

    override fun check(): Boolean? {
        return when {
            TaskCommon.IS_ENERGY_TIME -> {
                Log.record(
                    TAG,
                    "⏸ 当前为只收能量时间【" + BaseModel.energyTime.value + "】，停止执行" + getName() + "任务！"
                )
                false
            }
            TaskCommon.IS_MODULE_SLEEP_TIME -> {
                Log.record(
                    TAG,
                    "💤 模块休眠时间【" + BaseModel.modelSleepTime.value + "】停止执行" + getName() + "任务！"
                )
                false
            }
            else -> true
        }
    }

    override suspend fun runSuspend() {
        try {
            Log.record(TAG, "执行开始-" + getName())

            if (!queryOceanStatus()) {
                return
            }
            queryHomePage()

            if (dailyOceanTask?.value == true) {
                receiveTaskAward() // 日常任务
            }

            if (userprotectType?.value != ProtectType.DONT_PROTECT) {
                protectOcean() // 保护
            }

            // 制作万能碎片
            if (exchangeProp?.value == true) {
                exchangeProp()
            }

            // 使用万能拼图
            if (usePropByType?.value == true) {
                usePropByType()
            }

            if (PDL_task?.value == true) {
                doOceanPDLTask() // 潘多拉任务领取
            }
        } catch (e: CancellationException) {
            Log.runtime(TAG, "AntOcean 协程被取消")
            throw e
        } catch (t: Throwable) {
            Log.runtime(TAG, "start.run err:")
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    /**
     * 初始化沙滩任务。
     * 通过调用 AntOceanRpc 接口查询养成列表，
     * 并将符合条件的任务加入 BeachMap。
     */
    fun initBeach() {
        try {
            val response = AntOceanRpcCall.queryCultivationList()
            val jsonResponse = JsonUtil.parseJSONObjectOrNull(response) ?: run {
                IdMapManager.getInstance(BeachMap::class.java).load()
                return
            }
            if (ResChecker.checkRes(TAG, jsonResponse)) {
                val cultivationList = jsonResponse.optJSONArray("cultivationItemVOList")
                if (cultivationList != null) {
                    for (i in 0 until cultivationList.length()) {
                        val item = cultivationList.getJSONObject(i)
                        val templateSubType = item.getString("templateSubType")
                        // 检查 applyAction 是否为 AVAILABLE
                        val actionStr = item.getString("applyAction")
                        val action = ApplyAction.fromString(actionStr)
                        if (action == ApplyAction.AVAILABLE) {
                            val templateCode = item.getString("templateCode") // 业务id
                            val cultivationName = item.getString("cultivationName")
                            val energy = item.getInt("energy")
                            when (userprotectType?.value) {
                                ProtectType.PROTECT_ALL -> {
                                    IdMapManager.getInstance(BeachMap::class.java)
                                        .add(templateCode, "$cultivationName(${energy}g)")
                                }
                                ProtectType.PROTECT_BEACH -> {
                                    if (templateSubType != "BEACH") {
                                        IdMapManager.getInstance(BeachMap::class.java)
                                            .add(templateCode, "$cultivationName(${energy}g)")
                                    }
                                }
                                else -> {
                                    // DONT_PROTECT 或其他，不做处理
                                }
                            }
                        }
                    }
                    Log.runtime(TAG, "初始化沙滩数据成功。")
                }
                // 将所有筛选结果保存到 BeachMap
                IdMapManager.getInstance(BeachMap::class.java).save()
            } else {
                Log.runtime(jsonResponse.optString("resultDesc", "未知错误"))
            }
        } catch (e: JSONException) {
            Log.printStackTrace(TAG, "JSON 解析错误：", e)
            IdMapManager.getInstance(BeachMap::class.java).load() // 若出现异常则加载保存的 BeachMap 备份
        } catch (e: Exception) {
            Log.printStackTrace(TAG, "初始化沙滩任务时出错", e)
            IdMapManager.getInstance(BeachMap::class.java).load() // 加载保存的 BeachMap 备份
        }
    }

    private suspend fun queryOceanStatus(): Boolean {
        return try {
            val jo = JsonUtil.parseJSONObjectOrNull(AntOceanRpcCall.queryOceanStatus()) ?: return false
            if (ResChecker.checkRes(TAG, jo)) {
                if (!jo.getBoolean("opened")) {
                    enableField.setObjectValue(false)
                    Log.record("请先开启神奇海洋，并完成引导教程")
                    false
                } else {
                    initBeach()
                    true
                }
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryOceanStatus err:")
            Log.printStackTrace(TAG, t)
            false
        }
    }

    private suspend fun queryHomePage() {
        try {
            val joHomePage = JsonUtil.parseJSONObjectOrNull(AntOceanRpcCall.queryHomePage()) ?: return
            if (ResChecker.checkRes(TAG, joHomePage)) {
                if (joHomePage.has("bubbleVOList")) {
                    collectEnergy(joHomePage.getJSONArray("bubbleVOList"))
                }
                val userInfoVO = joHomePage.getJSONObject("userInfoVO")
                val rubbishNumber = userInfoVO.optInt("rubbishNumber", 0)
                val userId = userInfoVO.getString("userId")
                cleanOcean(userId, rubbishNumber)
                val ipVO = userInfoVO.optJSONObject("ipVO")
                if (ipVO != null) {
                    val surprisePieceNum = ipVO.optInt("surprisePieceNum", 0)
                    if (surprisePieceNum > 0) {
                        ipOpenSurprise()
                    }
                }
                queryMiscInfo()
                queryReplicaHome()
                queryUserRanking() // 清理
                querySeaAreaDetailList()
            } else {
                Log.runtime(TAG, joHomePage.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryHomePage err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun queryMiscInfo() {
        try {
            val s = AntOceanRpcCall.queryMiscInfo()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val miscHandlerVOMap = jo.getJSONObject("miscHandlerVOMap")
                val homeTipsRefresh = miscHandlerVOMap.getJSONObject("HOME_TIPS_REFRESH")
                if (homeTipsRefresh.optBoolean("fishCanBeCombined") || homeTipsRefresh.optBoolean("canBeRepaired")) {
                    querySeaAreaDetailList()
                }
                switchOceanChapter()
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryMiscInfo err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun collectEnergy(bubbleVOList: JSONArray) {
        try {
            for (i in 0 until bubbleVOList.length()) {
                val bubble = bubbleVOList.getJSONObject(i)
                if ("ocean" != bubble.getString("channel")) {
                    continue
                }
                if ("AVAILABLE" == bubble.getString("collectStatus")) {
                    val bubbleId = bubble.getLong("id")
                    val userId = bubble.getString("userId")
                    val s = AntForestRpcCall.collectEnergy("", userId, bubbleId)
                    val jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                    if (ResChecker.checkRes(TAG, jo)) {
                        val retBubbles = jo.optJSONArray("bubbles")
                        if (retBubbles != null) {
                            for (j in 0 until retBubbles.length()) {
                                val retBubble = retBubbles.optJSONObject(j)
                                if (retBubble != null) {
                                    val collectedEnergy = retBubble.getInt("collectedEnergy")
                                    Log.forest("神奇海洋🌊收取[${UserMap.getMaskName(userId)}]#${collectedEnergy}g")
                                    Toast.show("海洋能量🌊收取[${UserMap.getMaskName(userId)}]#${collectedEnergy}g")
                                }
                            }
                        }
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"))
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryHomePage err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun cleanOcean(userId: String, rubbishNumber: Int) {
        try {
            for (i in 0 until rubbishNumber) {
                val s = AntOceanRpcCall.cleanOcean(userId)
                val jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                if (ResChecker.checkRes(TAG, jo)) {
                    val cleanRewardVOS = jo.getJSONArray("cleanRewardVOS")
                    checkReward(cleanRewardVOS)
                    Log.forest("神奇海洋🌊[清理:${UserMap.getMaskName(userId)}海域]")
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"))
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "cleanOcean err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun ipOpenSurprise() {
        try {
            val s = AntOceanRpcCall.ipOpenSurprise()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val rewardVOS = jo.getJSONArray("surpriseRewardVOS")
                checkReward(rewardVOS)
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "ipOpenSurprise err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun combineFish(fishId: String) {
        try {
            val s = AntOceanRpcCall.combineFish(fishId)
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val fishDetailVO = jo.getJSONObject("fishDetailVO")
                val name = fishDetailVO.getString("name")
                Log.forest("神奇海洋🌊[$name]合成成功")
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "combineFish err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun checkReward(rewards: JSONArray) {
        try {
            for (i in 0 until rewards.length()) {
                val reward = rewards.getJSONObject(i)
                val name = reward.getString("name")
                val attachReward = reward.getJSONArray("attachRewardBOList")
                if (attachReward.length() > 0) {
                    Log.forest("神奇海洋🌊[获得:" + name + "碎片]")
                    var canCombine = true
                    for (j in 0 until attachReward.length()) {
                        val detail = attachReward.getJSONObject(j)
                        if (detail.optInt("count", 0) == 0) {
                            canCombine = false
                            break
                        }
                    }
                    if (canCombine && reward.optBoolean("unlock", false)) {
                        val fishId = reward.getString("id")
                        combineFish(fishId)
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "checkReward err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun collectReplicaAsset(canCollectAssetNum: Int) {
        try {
            for (i in 0 until canCollectAssetNum) {
                val s = AntOceanRpcCall.collectReplicaAsset()
                val jo = JsonUtil.parseJSONObjectOrNull(s) ?: continue
                if (ResChecker.checkRes(TAG, jo)) {
                    Log.forest("神奇海洋🌊[学习海洋科普知识]#潘多拉能量+1")
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"))
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "collectReplicaAsset err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun unLockReplicaPhase(replicaCode: String, replicaPhaseCode: String) {
        try {
            val s = AntOceanRpcCall.unLockReplicaPhase(replicaCode, replicaPhaseCode)
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val name = jo.getJSONObject("currentPhaseInfo").getJSONObject("extInfo").getString("name")
                Log.forest("神奇海洋🌊迎回[$name]")
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "unLockReplicaPhase err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun queryReplicaHome() {
        try {
            val s = AntOceanRpcCall.queryReplicaHome()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                if (jo.has("userReplicaAssetVO")) {
                    val userReplicaAssetVO = jo.getJSONObject("userReplicaAssetVO")
                    val canCollectAssetNum = userReplicaAssetVO.getInt("canCollectAssetNum")
                    collectReplicaAsset(canCollectAssetNum)
                }
                if (jo.has("userCurrentPhaseVO")) {
                    val userCurrentPhaseVO = jo.getJSONObject("userCurrentPhaseVO")
                    val phaseCode = userCurrentPhaseVO.getString("phaseCode")
                    val code = jo.getJSONObject("userReplicaInfoVO").getString("code")
                    if ("COMPLETED" == userCurrentPhaseVO.getString("phaseStatus")) {
                        unLockReplicaPhase(code, phaseCode)
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryReplicaHome err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun queryOceanPropList() {
        try {
            val s = AntOceanRpcCall.queryOceanPropList()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                AntOceanRpcCall.repairSeaArea()
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryOceanPropList err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun switchOceanChapter() {
        val s = AntOceanRpcCall.queryOceanChapterList()
        try {
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val currentChapterCode = jo.getString("currentChapterCode")
                val chapterVOs = jo.getJSONArray("userChapterDetailVOList")
                var isFinish = false
                var dstChapterCode = ""
                var dstChapterName = ""
                for (i in 0 until chapterVOs.length()) {
                    val chapterVO = chapterVOs.getJSONObject(i)
                    val repairedSeaAreaNum = chapterVO.getInt("repairedSeaAreaNum")
                    val seaAreaNum = chapterVO.getInt("seaAreaNum")
                    if (chapterVO.getString("chapterCode") == currentChapterCode) {
                        isFinish = repairedSeaAreaNum >= seaAreaNum
                    } else {
                        if (repairedSeaAreaNum >= seaAreaNum || !chapterVO.getBoolean("chapterOpen")) {
                            continue
                        }
                        dstChapterName = chapterVO.getString("chapterName")
                        dstChapterCode = chapterVO.getString("chapterCode")
                    }
                }
                if (isFinish && dstChapterCode.isNotEmpty()) {
                    val switchS = AntOceanRpcCall.switchOceanChapter(dstChapterCode)
                    jo = JsonUtil.parseJSONObjectOrNull(switchS) ?: return
                    if (ResChecker.checkRes(TAG, jo)) {
                        Log.forest("神奇海洋🌊切换到[$dstChapterName]系列")
                    } else {
                        Log.runtime(TAG, jo.getString("resultDesc"))
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryUserRanking err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun querySeaAreaDetailList() {
        try {
            val s = AntOceanRpcCall.querySeaAreaDetailList()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val seaAreaNum = jo.getInt("seaAreaNum")
                val fixSeaAreaNum = jo.getInt("fixSeaAreaNum")
                val currentSeaAreaIndex = jo.getInt("currentSeaAreaIndex")
                if (currentSeaAreaIndex < fixSeaAreaNum && seaAreaNum > fixSeaAreaNum) {
                    queryOceanPropList()
                }
                val seaAreaVOs = jo.getJSONArray("seaAreaVOs")
                for (i in 0 until seaAreaVOs.length()) {
                    val seaAreaVO = seaAreaVOs.getJSONObject(i)
                    val fishVOs = seaAreaVO.getJSONArray("fishVO")
                    for (j in 0 until fishVOs.length()) {
                        val fishVO = fishVOs.getJSONObject(j)
                        if (!fishVO.getBoolean("unlock") && "COMPLETED" == fishVO.getString("status")) {
                            val fishId = fishVO.getString("id")
                            combineFish(fishId)
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "querySeaAreaDetailList err:")
            Log.printStackTrace(TAG, t)
        }
    }

    @Suppress("ReturnCount")
    private suspend fun cleanFriendOcean(fillFlag: JSONObject) {
        if (!fillFlag.optBoolean("canClean")) {
            return
        }
        try {
            val userId = fillFlag.getString("userId")
            var isOceanClean = cleanOceanList?.value?.contains(userId) == true
            if (cleanOceanType?.value == CleanOceanType.DONT_CLEAN) {
                isOceanClean = !isOceanClean
            }
            if (!isOceanClean) {
                return
            }
            var s = AntOceanRpcCall.queryFriendPage(userId)
            var jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                s = AntOceanRpcCall.cleanFriendOcean(userId)
                jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
                Log.forest("神奇海洋🌊[帮助:${UserMap.getMaskName(userId)}清理海域]")
                if (ResChecker.checkRes(TAG, jo)) {
                    val cleanRewardVOS = jo.getJSONArray("cleanRewardVOS")
                    checkReward(cleanRewardVOS)
                } else {
                    Log.runtime(TAG, jo.getString("resultDesc"))
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryMiscInfo err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun queryUserRanking() {
        try {
            val s = AntOceanRpcCall.queryUserRanking()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val fillFlagVOList = jo.getJSONArray("fillFlagVOList")
                for (i in 0 until fillFlagVOList.length()) {
                    val fillFlag = fillFlagVOList.getJSONObject(i)
                    if (cleanOcean?.value == true) {
                        cleanFriendOcean(fillFlag)
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryMiscInfo err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun receiveTaskAward() {
        try {
            val presetBad = LinkedHashSet(listOf("DEMO", "DEMO1"))
            val typeRef = object : TypeReference<MutableSet<String>>() {}
            var badTaskSet = DataStore.getOrCreate("badOceanTaskSet", typeRef)
            if (badTaskSet.isEmpty()) {
                badTaskSet.addAll(presetBad)
                DataStore.put("badOceanTaskSet", badTaskSet)
            }
            while (true) {
                var done = false
                val s = AntOceanRpcCall.queryTaskList()
                val jo = JsonUtil.parseJSONObjectOrNull(s) ?: break
                if (!ResChecker.checkRes(TAG, jo)) {
                    Log.record(TAG, "查询任务列表失败：" + jo.getString("resultDesc"))
                }
                val jaTaskList = jo.getJSONArray("antOceanTaskVOList")
                for (i in 0 until jaTaskList.length()) {
                    val task = jaTaskList.getJSONObject(i)
                    val bizInfo = JSONObject(task.getString("bizInfo"))
                    val taskTitle = bizInfo.optString("taskTitle")
                    val awardCount = bizInfo.optString("awardCount", "0")
                    val sceneCode = task.getString("sceneCode")
                    val taskType = task.getString("taskType")
                    val taskStatus = task.getString("taskStatus")
                    // 在处理任何任务前，先检查黑名单
                    if (badTaskSet.contains(taskTitle)) {
                        Log.record(TAG, "海洋任务🌊[$taskTitle]已在黑名单中，跳过处理")
                        continue
                    }

                    if (TaskStatus.FINISHED.name == taskStatus) {
                        val awardResponse = AntOceanRpcCall.receiveTaskAward(sceneCode, taskType)
                        val joAward = JsonUtil.parseJSONObjectOrNull(awardResponse) ?: continue
                        if (ResChecker.checkRes(TAG, joAward)) {
                            Log.forest("海洋奖励🌊[" + taskTitle + "]# " + awardCount + "拼图")
                            done = true
                        } else {
                            Log.error(TAG, "海洋奖励🌊领取失败：$joAward")
                        }
                    } else if (TaskStatus.TODO.name == taskStatus) {
                        if (taskTitle.contains("答题")) {
                            answerQuestion()
                        } else {
                            val bizKey = "${sceneCode}_$taskType"
                            val count = oceanTaskTryCount.computeIfAbsent(bizKey) { AtomicInteger(0) }
                                .incrementAndGet()

                            val finishResponse = AntOceanRpcCall.finishTask(sceneCode, taskType)
                            val joFinishTask = JsonUtil.parseJSONObjectOrNull(finishResponse) ?: continue

                            // 检查特定错误码：不支持RPC完成的任务，直接加入黑名单
                            val errorCode = joFinishTask.optString("code", "")
                            val desc = joFinishTask.optString("desc", "")
                            if (errorCode == "400000040" || desc.contains("不支持RPC完成")) {
                                Log.error(TAG, "海洋任务🌊[$taskTitle]不支持RPC完成，已加入黑名单")
                                badTaskSet.add(taskTitle)
                                DataStore.put("badOceanTaskSet", badTaskSet)
                                continue
                            }

                            if (count > 1) {
                                badTaskSet.add(taskType)
                                DataStore.put("badOceanTaskSet", badTaskSet)
                            } else {
                                if (ResChecker.checkRes(TAG, joFinishTask)) {
                                    Log.forest("海洋任务🌊完成[$taskTitle]")
                                    done = true
                                } else {
                                    Log.error(TAG, "海洋任务🌊完成失败：$joFinishTask")
                                }
                            }
                        }

                        GlobalThreadPools.sleepCompat(500)
                    }
                }
                if (!done) break
            }
        } catch (e: JSONException) {
            Log.runtime(TAG, "JSON解析错误: " + (e.message ?: ""))
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            Log.runtime(TAG, "receiveTaskAward err:")
            Log.printStackTrace(TAG, t)
        }
    }

    // 海洋答题任务
    private suspend fun answerQuestion() {
        try {
            val questionResponse = AntOceanRpcCall.getQuestion()
            val questionJson = JsonUtil.parseJSONObjectOrNull(questionResponse) ?: return
            if (questionJson.getBoolean("answered")) {
                Log.runtime(TAG, "问题已经被回答过，跳过答题流程")
                return
            }
            if (questionJson.getInt("resultCode") == 200) {
                val questionId = questionJson.getString("questionId")
                val options = questionJson.getJSONArray("options")
                val answer = options.getString(0)
                val submitResponse = AntOceanRpcCall.submitAnswer(answer, questionId)
                val submitJson = JsonUtil.parseJSONObjectOrNull(submitResponse) ?: return
                if (submitJson.getInt("resultCode") == 200) {
                    Log.forest(TAG, "🌊海洋答题成功")
                } else {
                    Log.error(TAG, "海洋答题失败：$submitJson")
                }
            } else {
                Log.error(TAG, "海洋获取问题失败：$questionJson")
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "海洋答题错误", t)
        }
    }

    /**
     * 潘多拉海洋任务领取
     */
    private suspend fun doOceanPDLTask() {
        try {
            Log.runtime(TAG, "执行潘多拉海域任务")
            val homeResponse = AntOceanRpcCall.PDLqueryReplicaHome()
            val homeJson = JsonUtil.parseJSONObjectOrNull(homeResponse) ?: return
            if (ResChecker.checkRes(TAG, homeJson)) {
                val taskListResponse = AntOceanRpcCall.PDLqueryTaskList()
                val taskListJson = JsonUtil.parseJSONObjectOrNull(taskListResponse) ?: return
                val antOceanTaskVOList = taskListJson.getJSONArray("antOceanTaskVOList")
                for (i in 0 until antOceanTaskVOList.length()) {
                    val task = antOceanTaskVOList.getJSONObject(i)
                    val taskStatus = task.getString("taskStatus")
                    if ("FINISHED" == taskStatus) {
                        val bizInfoString = task.getString("bizInfo")
                        val bizInfo = JSONObject(bizInfoString)
                        val taskTitle = bizInfo.getString("taskTitle")
                        val awardCount = bizInfo.getInt("awardCount")
                        val taskType = task.getString("taskType")
                        val receiveTaskResponse = AntOceanRpcCall.PDLreceiveTaskAward(taskType)
                        val receiveTaskJson = JsonUtil.parseJSONObjectOrNull(receiveTaskResponse) ?: continue
                        val code = receiveTaskJson.getInt("code")
                        if (code == 100000000) {
                            Log.forest("海洋奖励🌊[领取:$taskTitle]获得潘多拉能量x$awardCount")
                        } else {
                            if (receiveTaskJson.has("message")) {
                                Log.record(TAG, "领取任务奖励失败: ${receiveTaskJson.getString("message")}")
                            } else {
                                Log.record(TAG, "领取任务奖励失败，未返回错误信息")
                            }
                        }
                    }
                }
            } else {
                Log.record(TAG, "PDLqueryReplicaHome调用失败: ${homeJson.optString("message")}")
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "doOceanPDLTask err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun protectOcean() {
        try {
            val s = AntOceanRpcCall.queryCultivationList()
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return
            if (ResChecker.checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("cultivationItemVOList")
                for (i in 0 until ja.length()) {
                    val item = ja.getJSONObject(i)
                    val templateSubType = item.getString("templateSubType")
                    val applyAction = item.getString("applyAction")
                    val cultivationName = item.getString("cultivationName")
                    val templateCode = item.getString("templateCode")
                    val projectConfig = item.getJSONObject("projectConfigVO")
                    val projectCode = projectConfig.getString("code")
                    val map = protectOceanList?.value ?: continue
                    for (entry in map.entries) {
                        if (entry.key == templateCode) {
                            val count = entry.value
                            if (count != null && count > 0) {
                                oceanExchangeTree(templateCode, projectCode, cultivationName, count)
                            }
                            break
                        }
                    }
                }
            } else {
                Log.runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "protectBeach err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun oceanExchangeTree(
        cultivationCode: String,
        projectCode: String,
        itemName: String,
        count: Int
    ) {
        try {
            var appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count)
            if (appliedTimes < 0) return

            for (applyCount in 1..count) {
                val s = AntOceanRpcCall.oceanExchangeTree(cultivationCode, projectCode)
                val jo = JsonUtil.parseJSONObjectOrNull(s) ?: break
                if (ResChecker.checkRes(TAG, jo)) {
                    val awardInfos = jo.getJSONArray("rewardItemVOs")
                    val award = StringBuilder()
                    for (i in 0 until awardInfos.length()) {
                        val awardItem = awardInfos.getJSONObject(i)
                        award.append(awardItem.getString("name")).append("*").append(awardItem.getInt("num"))
                    }
                    val str = "保护海洋生态🏖️[$itemName]#第${appliedTimes}次-获得奖励$award"
                    Log.forest(str)
                    GlobalThreadPools.sleepCompat(300)
                } else {
                    Log.error("保护海洋生态🏖️[$itemName]#发生未知错误，停止申请")
                    break
                }
                appliedTimes = queryCultivationDetail(cultivationCode, projectCode, count)
                if (appliedTimes < 0) {
                    break
                } else {
                    GlobalThreadPools.sleepCompat(300)
                }
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, "海洋保护错误:", t)
        }
    }

    private suspend fun queryCultivationDetail(
        cultivationCode: String,
        projectCode: String,
        count: Int
    ): Int {
        var appliedTimes = -1
        try {
            val s = AntOceanRpcCall.queryCultivationDetail(cultivationCode, projectCode)
            val jo = JsonUtil.parseJSONObjectOrNull(s) ?: return appliedTimes
            if (ResChecker.checkRes(TAG, jo)) {
                val userInfo = jo.getJSONObject("userInfoVO")
                val currentEnergy = userInfo.getInt("currentEnergy")
                val cultivationDetailVO = jo.getJSONObject("cultivationDetailVO")
                val applyAction = cultivationDetailVO.getString("applyAction")
                val certNum = cultivationDetailVO.getInt("certNum")
                if ("AVAILABLE" == applyAction) {
                    if (currentEnergy >= cultivationDetailVO.getInt("energy")) {
                        if (certNum < count) {
                            appliedTimes = certNum + 1
                        }
                    } else {
                        Log.forest("保护海洋🏖️[${cultivationDetailVO.getString("cultivationName")}]#能量不足停止申请")
                    }
                } else {
                    Log.forest("保护海洋🏖️[${cultivationDetailVO.getString("cultivationName")}]#似乎没有了")
                }
            } else {
                Log.record(jo.getString("resultDesc"))
                Log.runtime(s)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "queryCultivationDetail err:")
            Log.printStackTrace(TAG, t)
        }
        return appliedTimes
    }

    // 制作万能碎片
    private suspend fun exchangeProp() {
        try {
            var shouldContinue = true
            while (shouldContinue) {
                // 获取道具兑换列表的JSON数据
                val propListJson = AntOceanRpcCall.exchangePropList()
                val propListObj = JsonUtil.parseJSONObjectOrNull(propListJson) ?: return
                // 检查是否成功获取道具列表
                if (ResChecker.checkRes(TAG, propListObj)) {
                    // 获取道具重复数量
                    val duplicatePieceNum = propListObj.getInt("duplicatePieceNum")
                    // 如果道具重复数量小于10，直接返回并停止循环
                    if (duplicatePieceNum < 10) {
                        return
                    }
                    // 如果道具重复数量大于等于10，则执行道具兑换操作
                    val exchangeResultJson = AntOceanRpcCall.exchangeProp()
                    val exchangeResultObj = JsonUtil.parseJSONObjectOrNull(exchangeResultJson) ?: return
                    // 获取兑换后的碎片数量和兑换数量
                    val exchangedPieceNum = exchangeResultObj.getString("duplicatePieceNum")
                    val exchangeNum = exchangeResultObj.getString("exchangeNum")
                    // 检查道具兑换操作是否成功
                    if (ResChecker.checkRes(TAG, exchangeResultObj)) {
                        // 输出日志信息
                        Log.forest("神奇海洋🏖️[万能拼图]制作${exchangeNum}张,剩余${exchangedPieceNum}张碎片")
                        // 制作完成后休眠1秒钟
                        GlobalThreadPools.sleepCompat(1000)
                    }
                } else {
                    // 如果未成功获取道具列表，停止循环
                    shouldContinue = false
                }
            }
        } catch (t: Throwable) {
            // 捕获并记录异常
            Log.runtime(TAG, "exchangeProp error:")
            Log.printStackTrace(TAG, t)
        }
    }

    // 使用万能拼图
    private suspend fun usePropByType() {
        try {
            // 获取道具使用类型列表的JSON数据
            val propListJson = AntOceanRpcCall.usePropByTypeList()
            val propListObj = JsonUtil.parseJSONObjectOrNull(propListJson) ?: return
            if (ResChecker.checkRes(TAG, propListObj)) {
                // 获取道具类型列表中的holdsNum值
                val oceanPropVOByTypeList =
                    propListObj.getJSONArray("oceanPropVOByTypeList") // 获取数组中的数据
                // 遍历每个道具类型信息
                for (i in 0 until oceanPropVOByTypeList.length()) {
                    val propInfo = oceanPropVOByTypeList.getJSONObject(i)
                    var holdsNum = propInfo.getInt("holdsNum")
                    // 只要holdsNum大于0，就继续执行循环操作
                    var pageNum = 0
                    th@ while (holdsNum > 0) {
                        // 查询鱼列表的JSON数据
                        pageNum++
                        val fishListJson = AntOceanRpcCall.queryFishList(pageNum)
                        val fishListObj = JsonUtil.parseJSONObjectOrNull(fishListJson) ?: break
                        // 检查是否成功获取到鱼列表并且 hasMore 为 true
                        if (!ResChecker.checkRes(TAG, fishListObj)) {
                            // 如果没有成功获取到鱼列表或者 hasMore 为 false，则停止后续操作
                            break
                        }
                        // 获取鱼列表中的fishVOS数组
                        val fishVOS = fishListObj.optJSONArray("fishVOS")
                        if (fishVOS == null) {
                            break
                        }
                        // 遍历fishVOS数组，寻找pieces中num值为0的鱼的order和id
                        for (j in 0 until fishVOS.length()) {
                            val fish = fishVOS.getJSONObject(j)
                            val pieces = fish.optJSONArray("pieces")
                            if (pieces == null) {
                                continue
                            }
                            val order = fish.getInt("order")
                            val name = fish.getString("name")
                            val idSet = HashSet<Int>()
                            for (k in 0 until pieces.length()) {
                                val piece = pieces.getJSONObject(k)
                                if (piece.optInt("num") == 0) {
                                    idSet.add(Integer.parseInt(piece.getString("id")))
                                    holdsNum--
                                    if (holdsNum <= 0) {
                                        break
                                    }
                                }
                            }
                            if (idSet.isNotEmpty()) {
                                val usePropResult = AntOceanRpcCall.usePropByType(order, idSet) ?: continue
                                val usePropResultObj = JsonUtil.parseJSONObjectOrNull(usePropResult) ?: continue
                                if (ResChecker.checkRes(TAG, usePropResultObj)) {
                                    val userCount = idSet.size
                                    Log.forest("神奇海洋🏖️[万能拼图]使用${userCount}张，获得[$name]剩余${holdsNum}张")
                                    GlobalThreadPools.sleepCompat(1000)
                                    if (holdsNum <= 0) {
                                        break@th
                                    }
                                }
                            }
                        }
                        if (!fishListObj.optBoolean("hasMore")) {
                            break
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "usePropByType error:")
            Log.printStackTrace(TAG, t)
        }
    }
}

