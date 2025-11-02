package fansirsqi.xposed.sesame.task.antSports

import android.annotation.SuppressLint
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedHelpers
import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.data.Status.Companion.canDonateCharityCoin
import fansirsqi.xposed.sesame.data.Status.Companion.donateCharityCoin
import fansirsqi.xposed.sesame.data.Status.Companion.hasFlagToday
import fansirsqi.xposed.sesame.data.Status.Companion.setFlagToday
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.BaseModel.Companion.checkInterval
import fansirsqi.xposed.sesame.model.BaseModel.Companion.energyTime
import fansirsqi.xposed.sesame.model.BaseModel.Companion.modelSleepTime
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.newutil.DataStore.getOrCreate
import fansirsqi.xposed.sesame.newutil.DataStore.put
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.buyMember
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.collectBubble
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.completeExerciseTasks
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.donate
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.donateWalkHome
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.exchange
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.exchangeItem
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.go
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.openAndJoinFirst
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.openTreasureBox
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.pathMapJoin
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.pickBubbleTaskEnergy
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryAccount
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryBaseList
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryClubMember
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryCoinBubbleModule
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryCoinTaskPanel
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryItemDetail
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryMemberPriceRanking
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryMyHomePage
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryPath
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryProjectList
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryRoundList
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryUser
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.queryWalkStep
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.receiveCoinAsset
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.rewardReceive
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.stepQuery
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.tiyubizGo
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.trainMember
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.userTaskComplete
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.userTaskRightsReceive
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.walkDonateSignInfo
import fansirsqi.xposed.sesame.task.antSports.AntSportsRpcCall.walkGo
import fansirsqi.xposed.sesame.util.GlobalThreadPools.sleepCompat
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Log.error
import fansirsqi.xposed.sesame.util.Log.other
import fansirsqi.xposed.sesame.util.Log.printStackTrace
import fansirsqi.xposed.sesame.util.Log.record
import fansirsqi.xposed.sesame.util.Log.runtime
import fansirsqi.xposed.sesame.util.RandomUtil.nextInt
import fansirsqi.xposed.sesame.util.ResChecker.checkRes
import fansirsqi.xposed.sesame.util.TimeCounter
import fansirsqi.xposed.sesame.util.TimeUtil.getDateStr2
import fansirsqi.xposed.sesame.util.TimeUtil.getFormatDate
import fansirsqi.xposed.sesame.util.TimeUtil.getFormatTime
import fansirsqi.xposed.sesame.util.TimeUtil.isNowAfterOrCompareTimeStr
import fansirsqi.xposed.sesame.util.maps.UserMap.currentUid
import fansirsqi.xposed.sesame.util.maps.UserMap.getMaskName
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.Any
import kotlin.Array
import kotlin.Exception
import kotlin.Int
import kotlin.String
import kotlin.Throwable
import kotlin.Unit
import kotlin.also
import kotlin.arrayOf
import kotlin.math.min

class AntSports : ModelTask() {
    private var tmpStepCount = -1
    private var walk: BooleanModelField? = null
    private var walkPathTheme: ChoiceModelField? = null
    private var walkPathThemeId: String? = null
    private var walkCustomPath: BooleanModelField? = null
    private var walkCustomPathId: StringModelField? = null
    private var openTreasureBox: BooleanModelField? = null
    private var receiveCoinAsset: BooleanModelField? = null
    private var donateCharityCoin: BooleanModelField? = null
    private var donateCharityCoinType: ChoiceModelField? = null
    private var donateCharityCoinAmount: IntegerModelField? = null
    private var minExchangeCount: IntegerModelField? = null
    private var latestExchangeTime: IntegerModelField? = null
    private var syncStepCount: IntegerModelField? = null
    private var bicubic: BooleanModelField? = null
    private var battleForFriends: BooleanModelField? = null // 抢好友总开关
    private var battleForFriendType: ChoiceModelField? = null
    private var originBossIdList: SelectModelField? = null
    private var sportsTasks: BooleanModelField? = null

    // 训练好友相关变量
    private var trainFriend: BooleanModelField? = null
    private var zeroCoinLimit: IntegerModelField? = null

    // 记录训练好友获得0金币的次数
    private var zeroTrainCoinCount = 0

    // 运动任务黑名单
    private var sportsTaskBlacklist: StringModelField? = null


    public override fun getName(): String? {
        return "运动"
    }

    public override fun getGroup(): ModelGroup? {
        return ModelGroup.SPORTS
    }

    public override fun getIcon(): String? {
        return "AntSports.png"
    }


    public override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(BooleanModelField("walk", "行走路线 | 开启", false).also { walk = it })
        modelFields.addField(
            ChoiceModelField(
                "walkPathTheme",
                "行走路线 | 主题",
                WalkPathTheme.Companion.DA_MEI_ZHONG_GUO,
                WalkPathTheme.Companion.nickNames
            ).also { walkPathTheme = it })
        modelFields.addField(
            BooleanModelField(
                "walkCustomPath",
                "行走路线 | 开启自定义路线",
                false
            ).also { walkCustomPath = it })
        modelFields.addField(
            StringModelField(
                "walkCustomPathId",
                "行走路线 | 自定义路线代码(debug)",
                "p0002023122214520001"
            ).also { walkCustomPathId = it })
        modelFields.addField(
            BooleanModelField(
                "openTreasureBox",
                "开启宝箱",
                false
            ).also { openTreasureBox = it })
        modelFields.addField(
            BooleanModelField(
                "sportsTasks",
                "开启运动任务",
                false
            ).also { sportsTasks = it })
        modelFields.addField(
            StringModelField(
                "sportsTaskBlacklist",
                "运动任务黑名单 | 任务名称(用,分隔)",
                "开通包裹查询服务,添加支付宝小组件,领取价值1.7万元配置,支付宝积分可兑券"
            ).also { sportsTaskBlacklist = it })
        modelFields.addField(
            BooleanModelField(
                "receiveCoinAsset",
                "收能量🎈",
                false
            ).also { receiveCoinAsset = it })
        modelFields.addField(
            BooleanModelField(
                "donateCharityCoin",
                "捐能量🎈 | 开启",
                false
            ).also { donateCharityCoin = it })
        modelFields.addField(
            ChoiceModelField(
                "donateCharityCoinType",
                "捐能量🎈 | 方式",
                DonateCharityCoinType.Companion.ONE,
                DonateCharityCoinType.Companion.nickNames
            ).also { donateCharityCoinType = it })
        modelFields.addField(
            IntegerModelField(
                "donateCharityCoinAmount",
                "捐能量🎈 | 数量(每次)",
                100
            ).also { donateCharityCoinAmount = it })


        // 抢好友相关配置
        modelFields.addField(
            BooleanModelField(
                "battleForFriends",
                "抢好友 | 开启",
                false
            ).also { battleForFriends = it })
        modelFields.addField(
            ChoiceModelField(
                "battleForFriendType",
                "抢好友 | 动作",
                BattleForFriendType.Companion.ROB,
                BattleForFriendType.Companion.nickNames
            ).also { battleForFriendType = it })
        modelFields.addField(
            SelectModelField(
                "originBossIdList",
                "抢好友 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                originBossIdList = it
            })


        // 训练好友相关配置
        modelFields.addField(
            BooleanModelField(
                "trainFriend",
                "训练好友 | 开启",
                false
            ).also { trainFriend = it })
        modelFields.addField(
            IntegerModelField(
                "zeroCoinLimit",
                "训练好友 | 0金币上限次数当天关闭",
                5
            ).also { zeroCoinLimit = it })

        modelFields.addField(BooleanModelField("bicubic", "文体中心", false).also { bicubic = it })
        modelFields.addField(
            IntegerModelField(
                "minExchangeCount",
                "最小捐步步数",
                0
            ).also { minExchangeCount = it })
        modelFields.addField(
            IntegerModelField(
                "latestExchangeTime",
                "最晚捐步时间(24小时制)",
                22
            ).also { latestExchangeTime = it })
        modelFields.addField(
            IntegerModelField(
                "syncStepCount",
                "自定义同步步数",
                22000
            ).also { syncStepCount = it })
        // 本地变量，用于添加字段到模型
        val coinExchangeDoubleCard =
            BooleanModelField("coinExchangeDoubleCard", "能量🎈兑换限时能量双击卡", false)
        modelFields.addField(coinExchangeDoubleCard)
        return modelFields
    }

    public override fun boot(classLoader: ClassLoader?) {
        try {
            XposedHelpers.findAndHookMethod(
                "com.alibaba.health.pedometer.core.datasource.PedometerAgent", classLoader,
                "readDailyStep", object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val originStep = param.getResult() as Int
                        val step = tmpStepCount()
                        if (TaskCommon.IS_AFTER_8AM && originStep < step) { //早于8点或步数小于自定义步数hook
                            param.setResult(step)
                        }
                    }
                })
            runtime(TAG, "hook readDailyStep successfully")
        } catch (t: Throwable) {
            runtime(TAG, "hook readDailyStep err:")
            printStackTrace(TAG, t)
        }
    }

    override fun check(): Boolean? {
        if (TaskCommon.IS_ENERGY_TIME) {
            record(
                TAG,
                "⏸ 当前为只收能量时间【" + energyTime.value + "】，停止执行" + getName() + "任务！"
            )
            return java.lang.Boolean.FALSE
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            record(
                TAG,
                "💤 模块休眠时间【" + modelSleepTime.value + "】停止执行" + getName() + "任务！"
            )
            return java.lang.Boolean.FALSE
        } else {
            return java.lang.Boolean.TRUE
        }
    }

    override suspend fun runSuspend() {
        val tc = TimeCounter(TAG)
        record(TAG, "执行开始-" + getName())
        try {
            if (!hasFlagToday("sport::syncStep") && isNowAfterOrCompareTimeStr("0600")) {
                addChildTask(ChildModelTask("syncStep", Runnable {
                    val step = tmpStepCount()
                    try {
                        val classLoader = fansirsqi.xposed.sesame.hook.ApplicationHookConstants.classLoader
                        if (classLoader == null) {
                            error(TAG, "同步运动步数失败: ClassLoader为null")
                            return@Runnable
                        }
                        val syncResult = XposedHelpers.callMethod(
                            XposedHelpers.callStaticMethod(
                                classLoader.loadClass(
                                    "com.alibaba.health.pedometer.intergation.rpc.RpcManager"
                                ), "a"
                            ), "a", *arrayOf<Any?>(step, java.lang.Boolean.FALSE, "system")
                        ) as? Boolean
                        if (syncResult == true) {
                            other(TAG, "同步步数🏃🏻‍♂️[" + step + "步]")
                        } else {
                            error(TAG, "同步运动步数失败:" + step)
                        }
                        setFlagToday("sport::syncStep")
                    } catch (t: Throwable) {
                        printStackTrace(TAG, t)
                    }
                }))
                tc.countDebug("同步步数")
            }
            if (sportsTasks!!.value == true) {
                // 检查今天是否已完成所有任务
                val today = getDateStr2()
                val completedDate =
                    getOrCreate<String>(SPORTS_TASKS_COMPLETED_DATE, String::class.java)
                if (today == completedDate) {
                    record(TAG, "运动任务今日已完成，跳过执行")
                } else {
                    sportsTasks()
                    tc.countDebug("运动任务")
                }
            }

            val loader = fansirsqi.xposed.sesame.hook.ApplicationHookConstants.classLoader
            if (walk!!.value == true) {
                walkPathThemeIdOnConfig()
                walk()
                tc.countDebug("行走")
            }
            if (openTreasureBox!!.value == true && walk!!.value != true) {
                queryMyHomePage(loader)
                tc.countDebug("开启宝箱")
            }

            if (donateCharityCoin!!.value == true && canDonateCharityCoin()) {
                queryProjectList(loader)
                tc.countDebug("捐能量🎈")
            }

            if (minExchangeCount!!.value!! > 0 && Status.canExchangeToday(currentUid!!)) {
                queryWalkStep(loader)
                tc.countDebug("最小捐步步数")
            }

            if (bicubic!!.value == true) {
                userTaskGroupQuery("SPORTS_DAILY_SIGN_GROUP")
                userTaskGroupQuery("SPORTS_DAILY_GROUP")
                tc.countDebug("查询任务")
                userTaskRightsReceive()
                tc.countDebug("userTaskRightsReceive")
                pathFeatureQuery()
                tc.countDebug("pathFeatureQuery")
                participate()
                tc.countDebug("文体中心")
            }
            // 抢好友和训练好友是两个独立功能，需要分别检查开关
            // 抢好友功能
            if (battleForFriends!!.value == true) {
                buyMember()
                tc.countDebug("抢好友")
            }


            // 训练好友功能
            if (trainFriend!!.value == true) {
                // 检查今天是否已达到0金币上限
                val today = getDateStr2()
                val zeroCoinDate =
                    getOrCreate<String>(TRAIN_FRIEND_ZERO_COIN_DATE, String::class.java)
                if (today == zeroCoinDate) {
                    record(TAG, "训练好友今日已达0金币上限，跳过执行")
                } else {
                    queryClubHome()
                    queryTrainItem()
                    tc.countDebug("训练好友")
                }
            }
            if (receiveCoinAsset!!.value == true) {
                receiveCoinAsset()
                tc.countDebug("收能量🎈")
            }
            tc.stop()
        } catch (t: Throwable) {
            runtime(TAG, "start.run err:")
            printStackTrace(TAG, t)
        } finally {
            record(TAG, "执行结束-" + getName())
        }
    }

    private fun coinExchangeItem(itemId: String) {
        try {
            var jo = JSONObject(queryItemDetail(itemId))
            if (!checkRes(TAG, jo)) {
                return
            }
            jo = jo.getJSONObject("data")
            if ("OK" != jo.optString("exchangeBtnStatus")) {
                return
            }
            jo = jo.getJSONObject("itemBaseInfo")
            val itemTitle = jo.getString("itemTitle")
            val valueCoinCount = jo.getInt("valueCoinCount")
            jo = JSONObject(exchangeItem(itemId, valueCoinCount))
            if (!checkRes(TAG, jo)) {
                return
            }
            jo = jo.getJSONObject("data")
            if (jo.optBoolean("exgSuccess")) {
                other(TAG, "运动好礼🎐兑换[" + itemTitle + "]花费" + valueCoinCount + "能量🎈")
            }
        } catch (t: Throwable) {
            error(TAG, "trainMember err:")
            printStackTrace(TAG, t)
        }
    }

    fun tmpStepCount(): Int {
        if (tmpStepCount >= 0) {
            return tmpStepCount
        }
        tmpStepCount = syncStepCount!!.value!!
        if (tmpStepCount > 0) {
            tmpStepCount = nextInt(tmpStepCount, tmpStepCount + 2000)
            if (tmpStepCount > 100000) {
                tmpStepCount = 100000
            }
        }
        return tmpStepCount
    }

    // 运动
    private fun sportsTasks() {
        try {
            sportsCheck_in()
            // 运动任务查询
            val taskResult = queryCoinTaskPanel()
            if (taskResult.isEmpty()) {
                record(TAG, "运动任务查询失败: RPC返回为空")
                return
            }
            var jo = JSONObject(taskResult)
            //  Log.record(TAG,"运动任务响应："+jo);
            if (jo.optBoolean("success")) {
                val data = jo.getJSONObject("data")
                val taskList = data.getJSONArray("taskList")


                // 统计任务完成状态
                var totalTasks = 0
                var completedTasks = 0
                var availableTasks = 0 // 可执行的任务数

                for (i in 0..<taskList.length()) {
                    val taskDetail = taskList.getJSONObject(i)
                    val taskId = taskDetail.getString("taskId")
                    val taskName = taskDetail.getString("taskName")
                    val prizeAmount = taskDetail.getString("prizeAmount")
                    val taskStatus = taskDetail.getString("taskStatus")
                    val currentNum = taskDetail.getInt("currentNum")
                    // 要完成的次数
                    val limitConfigNum = taskDetail.getInt("limitConfigNum") - currentNum


                    // 统计总任务数（排除特殊任务类型）
                    val taskType = taskDetail.optString("taskType", "")
                    if (taskType != "SETTLEMENT") { // 排除步数和锻炼时长等自动完成的任务
                        totalTasks++


                        // 获取按钮文本和assetId
                        val buttonText = taskDetail.getString("buttonText")


                        // 检查任务是否在黑名单中
                        val blacklistStr = sportsTaskBlacklist!!.value
                        if (blacklistStr != null && !blacklistStr.trim { it <= ' ' }.isEmpty()) {
                            val blacklist =
                                blacklistStr.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                                    .toTypedArray()
                            var isBlacklisted = false
                            for (blackItem in blacklist) {
                                if (taskName.contains(blackItem.trim { it <= ' ' })) {
                                    isBlacklisted = true
                                    break
                                }
                            }
                            if (isBlacklisted) {
                                record(
                                    TAG,
                                    "做任务得能量🎈[任务已屏蔽：" + taskName + "（在黑名单中）]"
                                )
                                completedTasks++ // 将黑名单任务视为已完成
                                continue
                            }
                        }


                        // 跳过已完成的任务（检查状态和按钮文本）
                        if (buttonText == "任务已完成") {
                            record(
                                TAG,
                                "做任务得能量🎈[任务已完成：" + taskName + "，状态：" + taskStatus + "，按钮：" + buttonText + "]"
                            )
                            completedTasks++
                            continue
                        }

                        // 判断并领取奖励
                        if (buttonText == "领取奖励") {
                            val assetId = taskDetail.getString("assetId")
                            val result = pickBubbleTaskEnergy(assetId)
                            try {
                                val resultData = JSONObject(result)
                                if (resultData.optBoolean("success", false)) {
                                    val changeAmount = resultData.optString("changeAmount", "0")
                                    record(
                                        TAG, "做任务得能量🎈[领取成功：" + taskName +
                                                "，获得：" + changeAmount + "能量🎈]"
                                    )
                                    completedTasks++
                                } else {
                                    val errorMsg = resultData.optString("errorMsg", "未知错误")
                                    val errorCode = resultData.optString("errorCode", "")
                                    record(
                                        TAG, "做任务得能量🎈[领取失败：" + taskName +
                                                "，错误：" + errorCode + " - " + errorMsg + "]"
                                    )
                                    // 如果是不可重试的错误，标记为已完成避免重复尝试
                                    if (!resultData.optBoolean("retryable", true) ||
                                        "CAMP_TRIGGER_ERROR" == errorCode
                                    ) {
                                        completedTasks++
                                        record(
                                            TAG,
                                            "做任务得能量🎈[任务已标记完成，避免重复尝试：" + taskName + "]"
                                        )
                                    }
                                }
                                continue
                            } catch (e: Exception) {
                                record(
                                    TAG,
                                    "做任务得能量🎈[响应解析异常：" + taskName + "，错误：" + e.message + "]"
                                )
                            }
                        }


                        // 跳过不需要完成的任务状态
                        if (taskStatus != "WAIT_RECEIVE" && taskStatus != "WAIT_COMPLETE") {
                            record(
                                TAG,
                                "做任务得能量🎈[跳过任务：" + taskName + "，状态：" + taskStatus + "]"
                            )
                            continue
                        }


                        // 检查是否需要执行任务
                        if (limitConfigNum <= 0) {
                            record(
                                TAG,
                                "做任务得能量🎈[任务无需执行：" + taskName + "，已完成" + currentNum + "/" + taskDetail.getInt(
                                    "limitConfigNum"
                                ) + "]"
                            )
                            completedTasks++
                            continue
                        }
                        // 这是一个可执行的任务
                        availableTasks++
                        record(
                            TAG,
                            "做任务得能量🎈[开始执行任务：" + taskName + "，需完成" + limitConfigNum + "次]"
                        )
                        for (i1 in 0..<limitConfigNum) {
                            jo = JSONObject(completeExerciseTasks(taskId))
                            if (jo.optBoolean("success")) {
                                record(
                                    TAG,
                                    "做任务得能量🎈[完成任务：" + taskName + "，得" + prizeAmount + "💰]#(" + (i1 + 1) + "/" + limitConfigNum + ")"
                                )
                                receiveCoinAsset()
                            } else {
                                record(
                                    TAG,
                                    "做任务得能量🎈[任务执行失败：" + taskName + "]#(" + (i1 + 1) + "/" + limitConfigNum + ")"
                                )
                                break // 失败时跳出循环
                            }
                            if (limitConfigNum > 1 && i1 < limitConfigNum - 1) {
                                sleepCompat(10000)
                            }
                        }
                        // 任务执行完成后，增加完成计数
                        completedTasks++
                    }
                }
                // 检查是否所有可执行任务都已完成
                record(
                    TAG,
                    "运动任务完成情况：" + completedTasks + "/" + totalTasks + "，可执行任务：" + availableTasks
                )
                // 如果所有可执行的任务都已完成（没有可执行的任务了），记录当天日期，今日不再执行
                if (totalTasks > 0 && completedTasks >= totalTasks && availableTasks == 0) {
                    val today = getDateStr2()
                    put(SPORTS_TASKS_COMPLETED_DATE, today)
                    record(TAG, "✅ 所有运动任务已完成，今日不再执行，明日自动恢复")
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    private fun sportsCheck_in() {
        try {
            val result = AntSportsRpcCall.sportsCheck_in()
            if (result.isEmpty()) {
                record(TAG, "运动签到失败: RPC返回为空")
                return
            }
            val jo = JSONObject(result)
            if (jo.optBoolean("success")) {
                val data = jo.getJSONObject("data")
                if (!data.getBoolean("signed")) {
                    val subscribeConfig: JSONObject?
                    if (data.has("subscribeConfig")) {
                        subscribeConfig = data.getJSONObject("subscribeConfig")
                        record(
                            TAG,
                            "做任务得能量🎈能量🎈[完成任务：签到" + subscribeConfig.getString("subscribeExpireDays") + "天，" + data.getString(
                                "toast"
                            ) + "💰]"
                        )
                    }
                } else {
                    record(TAG, "运动签到今日已签到")
                }
            } else {
                record(jo.toString())
            }
        } catch (e: Exception) {
            record(TAG, "sportsCheck_in err")
            Log.printStackTrace(e)
        }
    }

    private fun receiveCoinAsset() {
        try {
            val s = queryCoinBubbleModule()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val data = jo.getJSONObject("data")
                if (!data.has("receiveCoinBubbleList")) return
                val ja = data.getJSONArray("receiveCoinBubbleList")
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    val assetId = jo.getString("assetId")
                    val coinAmount = jo.getInt("coinAmount")
                    jo = JSONObject(receiveCoinAsset(assetId, coinAmount))
                    if (jo.optBoolean("success")) {
                        other(TAG, "收集金币💰[" + coinAmount + "个]")
                    } else {
                        record(TAG, "首页收集金币" + " " + jo)
                    }
                }
            } else {
                runtime(TAG, s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "receiveCoinAsset err:")
            printStackTrace(TAG, t)
        }
    }

    /*
     * 新版行走路线 -- begin
     */
    private fun walk() {
        try {
            val user = JSONObject(queryUser())
            if (!user.optBoolean("success")) {
                return
            }
            val data = user.optJSONObject("data")
            if (data == null) {
                record(TAG, "行走路线🚶🏻‍♂️未获取到用户数据")
                return
            }
            val joinedPathId = data.optString("joinedPathId")
            if (joinedPathId.isNullOrEmpty()) {
                record(TAG, "行走路线🚶🏻‍♂️用户尚未加入任何路线")
                return
            }
            val path = queryPath(joinedPathId) // 调用本地方法
            if (path == null || !path.has("userPathStep")) {
                record(TAG, "行走路线🚶🏻‍♂️查询路径失败")
                return
            }
            val userPathStep = path.getJSONObject("userPathStep")
            if ("COMPLETED" == userPathStep.getString("pathCompleteStatus")) {
                record(TAG, "行走路线🚶🏻‍♂️路线[" + userPathStep.getString("pathName") + "]已完成")
                val pathId = queryJoinPath(walkPathThemeId)
                joinPath(pathId)
                return
            }
            val minGoStepCount = path.getJSONObject("path").getInt("minGoStepCount")
            val pathStepCount = path.getJSONObject("path").getInt("pathStepCount")
            val forwardStepCount = userPathStep.getInt("forwardStepCount")
            val remainStepCount = userPathStep.getInt("remainStepCount")
            val needStepCount = pathStepCount - forwardStepCount
            if (remainStepCount >= minGoStepCount) {
                val useStepCount = min(remainStepCount, needStepCount)
                walkGo(
                    userPathStep.getString("pathId"),
                    useStepCount,
                    userPathStep.getString("pathName")
                )
            }
        } catch (t: Throwable) {
            runtime(TAG, "walk err:")
            printStackTrace(TAG, t)
        }
    }

    private fun walkGo(pathId: String, useStepCount: Int, pathName: String?) {
        try {
            val date = Date()
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
            val jo = JSONObject(AntSportsRpcCall.walkGo("202312191135", sdf.format(date), pathId, useStepCount))
            if (jo.optBoolean("success")) {
                record(TAG, "行走路线🚶🏻‍♂️路线[" + pathName + "]#前进了" + useStepCount + "步")
                queryPath(pathId) // 调用本地方法
            }
        } catch (t: Throwable) {
            runtime(TAG, "walkGo err:")
            printStackTrace(TAG, t)
        }
    }

    private fun queryWorldMap(themeId: String): JSONObject? {
        var theme: JSONObject? = null
        try {
            val jo = JSONObject(AntSportsRpcCall.queryWorldMap(themeId))
            if (jo.optBoolean("success")) {
                theme = jo.getJSONObject("data")
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryWorldMap err:")
            printStackTrace(TAG, t)
        }
        return theme
    }

    private fun queryCityPath(cityId: String): JSONObject? {
        var city: JSONObject? = null
        try {
            val jo = JSONObject(AntSportsRpcCall.queryCityPath(cityId))
            if (jo.optBoolean("success")) {
                city = jo.getJSONObject("data")
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryCityPath err:")
            printStackTrace(TAG, t)
        }
        return city
    }

    private fun queryPath(pathId: String): JSONObject {
        var path: JSONObject? = null
        try {
            val date = Date()
            @SuppressLint("SimpleDateFormat") val sdf = SimpleDateFormat("yyyy-MM-dd")
            val jo = JSONObject(AntSportsRpcCall.queryPath("202312191135", sdf.format(date), pathId))
            if (jo.optBoolean("success")) {
                path = jo.getJSONObject("data")
                val ja = jo.getJSONObject("data").getJSONArray("treasureBoxList")
                for (i in 0..<ja.length()) {
                    val treasureBox = ja.getJSONObject(i)
                    receiveEvent(treasureBox.getString("boxNo"))
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryPath err:")
            printStackTrace(TAG, t)
        }
        return path ?: JSONObject()
    }

    private fun receiveEvent(eventBillNo: String) {
        try {
            var jo = JSONObject(AntSportsRpcCall.receiveEvent(eventBillNo))
            if (!jo.optBoolean("success")) {
                return
            }
            val ja = jo.getJSONObject("data").getJSONArray("rewards")
            for (i in 0..<ja.length()) {
                jo = ja.getJSONObject(i)
                record(
                    TAG,
                    "行走路线🎁开启宝箱[" + jo.getString("rewardName") + "]*" + jo.getInt("count")
                )
            }
        } catch (t: Throwable) {
            runtime(TAG, "receiveEvent err:")
            printStackTrace(TAG, t)
        }
    }

    private fun queryJoinPath(themeId: String?): String? {
        if (walkCustomPath!!.value == true) {
            return walkCustomPathId!!.value
        }
        var pathId: String? = null
        try {
            val theme: JSONObject? = queryWorldMap(walkPathThemeId!!)
            if (theme == null) {
                return pathId
            }
            val cityList = theme.getJSONArray("cityList")
            for (i in 0..<cityList.length()) {
                val cityId = cityList.getJSONObject(i).getString("cityId")
                val city: JSONObject? = queryCityPath(cityId)
                if (city == null) {
                    continue
                }
                val cityPathList = city.getJSONArray("cityPathList")
                for (j in 0..<cityPathList.length()) {
                    val cityPath = cityPathList.getJSONObject(j)
                    pathId = cityPath.getString("pathId")
                    if ("COMPLETED" != cityPath.getString("pathCompleteStatus")) {
                        return pathId
                    }
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryJoinPath err:")
            printStackTrace(TAG, t)
        }
        return pathId
    }

    private fun joinPath(pathId: String?) {
        var pathId = pathId
        if (pathId == null) {
            // 龙年祈福线
            pathId = "p0002023122214520001"
        }
        try {
            val jo = JSONObject(AntSportsRpcCall.joinPath(pathId))
            if (jo.optBoolean("success")) {
                val path = queryPath(pathId) // 调用本地方法
                if (path?.has("path") == true) {
                    record(
                        TAG,
                        "行走路线🚶🏻‍♂️路线[" + path.getJSONObject("path").getString("name") + "]已加入"
                    )
                } else {
                    record(TAG, "行走路线🚶🏻‍♂️路线已加入")
                }
            } else {
                record(TAG, "行走路线🚶🏻‍♂️路线[" + pathId + "]有误，无法加入！")
            }
        } catch (t: Throwable) {
            runtime(TAG, "joinPath err:")
            printStackTrace(TAG, t)
        }
    }

    private fun walkPathThemeIdOnConfig() {
        if (walkPathTheme!!.value == WalkPathTheme.Companion.DA_MEI_ZHONG_GUO) {
            walkPathThemeId = "M202308082226"
        }
        if (walkPathTheme!!.value == WalkPathTheme.Companion.GONG_YI_YI_XIAO_BU) {
            walkPathThemeId = "M202401042147"
        }
        if (walkPathTheme!!.value == WalkPathTheme.Companion.DENG_DING_ZHI_MA_SHAN) {
            walkPathThemeId = "V202405271625"
        }
        if (walkPathTheme!!.value == WalkPathTheme.Companion.WEI_C_DA_TIAO_ZHAN) {
            walkPathThemeId = "202404221422"
        }
        if (walkPathTheme!!.value == WalkPathTheme.Companion.LONG_NIAN_QI_FU) {
            walkPathThemeId = "WF202312050200"
        }
    }

    /*
    * 新版行走路线 -- end
    */
    private fun queryMyHomePage(loader: ClassLoader?) {
        try {
            var s = queryMyHomePage()
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                s = jo.getString("pathJoinStatus")
                if ("GOING" == s) {
                    if (jo.has("pathCompleteStatus")) {
                        if ("COMPLETED" == jo.getString("pathCompleteStatus")) {
                            jo = JSONObject(AntSportsRpcCall.queryBaseList())
                            if (checkRes(TAG, jo)) {
                                val allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList")
                                val otherAllPathBaseInfoList =
                                    jo.getJSONArray("otherAllPathBaseInfoList")
                                        .getJSONObject(0)
                                        .getJSONArray("allPathBaseInfoList")
                                join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, "")
                            } else {
                                runtime(TAG, jo.getString("resultDesc"))
                            }
                        }
                    } else {
                        val rankCacheKey = jo.getString("rankCacheKey")
                        val ja = jo.getJSONArray("treasureBoxModelList")
                        for (i in 0..<ja.length()) {
                            parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey)
                        }
                        val joPathRender = jo.getJSONObject("pathRenderModel")
                        val title = joPathRender.getString("title")
                        val minGoStepCount = joPathRender.getInt("minGoStepCount")
                        jo = jo.getJSONObject("dailyStepModel")
                        val consumeQuantity = jo.getInt("consumeQuantity")
                        val produceQuantity = jo.getInt("produceQuantity")
                        val day = jo.getString("day")
                        val canMoveStepCount = produceQuantity - consumeQuantity
                        if (canMoveStepCount >= minGoStepCount) {
                            go(loader, day, rankCacheKey, canMoveStepCount, title)
                        }
                    }
                } else if ("NOT_JOIN" == s) {
                    val firstJoinPathTitle = jo.getString("firstJoinPathTitle")
                    val allPathBaseInfoList = jo.getJSONArray("allPathBaseInfoList")
                    val otherAllPathBaseInfoList =
                        jo.getJSONArray("otherAllPathBaseInfoList").getJSONObject(0)
                            .getJSONArray("allPathBaseInfoList")
                    join(loader, allPathBaseInfoList, otherAllPathBaseInfoList, firstJoinPathTitle)
                }
            } else {
                runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryMyHomePage err:")
            printStackTrace(TAG, t)
        }
    }

    private fun join(
        loader: ClassLoader?, allPathBaseInfoList: JSONArray, otherAllPathBaseInfoList: JSONArray,
        firstJoinPathTitle: String?
    ) {
        try {
            var index = -1
            var title: String? = null
            var pathId: String? = null
            var jo: JSONObject
            for (i in allPathBaseInfoList.length() - 1 downTo 0) {
                jo = allPathBaseInfoList.getJSONObject(i)
                if (jo.getBoolean("unlocked")) {
                    title = jo.getString("title")
                    pathId = jo.getString("pathId")
                    index = i
                    break
                }
            }
            if (index < 0 || index == allPathBaseInfoList.length() - 1) {
                for (j in otherAllPathBaseInfoList.length() - 1 downTo 0) {
                    jo = otherAllPathBaseInfoList.getJSONObject(j)
                    if (jo.getBoolean("unlocked")) {
                        if (j != otherAllPathBaseInfoList.length() - 1 || index != allPathBaseInfoList.length() - 1) {
                            title = jo.getString("title")
                            pathId = jo.getString("pathId")
                            index = j
                        }
                        break
                    }
                }
            }
            if (index >= 0) {
                val s: String?
                if (title == firstJoinPathTitle) {
                    s = openAndJoinFirst()
                } else {
                    s = AntSportsRpcCall.join(pathId!!)
                }
                jo = JSONObject(s)
                if (checkRes(TAG, jo)) {
                    other(TAG, "加入线路🚶🏻‍♂️[" + title + "]")
                    queryMyHomePage(loader)
                } else {
                    runtime(TAG, jo.getString("resultDesc"))
                }
            } else {
                record(TAG, "好像没有可走的线路了！")
            }
        } catch (t: Throwable) {
            runtime(TAG, "join err:")
            printStackTrace(TAG, t)
        }
    }

    private fun go(
        loader: ClassLoader?,
        day: String,
        rankCacheKey: String,
        stepCount: Int,
        title: String?
    ) {
        try {
            val s = AntSportsRpcCall.go(day, rankCacheKey, stepCount)
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                other(TAG, "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("goStepCount") + "步")
                val completed = "COMPLETED" == jo.getString("completeStatus")
                val ja = jo.getJSONArray("allTreasureBoxModelList")
                for (i in 0..<ja.length()) {
                    parseTreasureBoxModel(loader, ja.getJSONObject(i), rankCacheKey)
                }
                if (completed) {
                    other(TAG, "完成线路🚶🏻‍♂️[" + title + "]")
                    queryMyHomePage(loader)
                }
            } else {
                runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "go err:")
            printStackTrace(TAG, t)
        }
    }

    private fun parseTreasureBoxModel(loader: ClassLoader?, jo: JSONObject, rankCacheKey: String) {
        try {
            val canOpenTime = jo.getString("canOpenTime")
            val issueTime = jo.getString("issueTime")
            val boxNo = jo.getString("boxNo")
            val userId = jo.getString("userId")
            if (canOpenTime == issueTime) {
                openTreasureBox(loader, boxNo, userId)
            } else {
                val cot = canOpenTime.toLong()
                val now = rankCacheKey.toLong()
                val delay = cot - now
                if (delay <= 0) {
                    openTreasureBox(loader, boxNo, userId)
                    return
                }
                if (delay < checkInterval.value!!) {
                    val taskId = "BX|" + boxNo
                    if (hasChildTask(taskId)) {
                        return
                    }
                    record(TAG, "还有 " + delay + "ms 开运动宝箱")
                    addChildTask(ChildModelTask(taskId, "BX", Runnable {
                        record(TAG, "蹲点开箱开始")
                        val startTime = System.currentTimeMillis()
                        while (System.currentTimeMillis() - startTime < 5000) {
                            if (openTreasureBox(loader, boxNo, userId) > 0) {
                                break
                            }
                            sleepCompat(200)
                        }
                    }, System.currentTimeMillis() + delay))
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "parseTreasureBoxModel err:")
            printStackTrace(TAG, t)
        }
    }

    private fun openTreasureBox(loader: ClassLoader?, boxNo: String, userId: String): Int {
        try {
            val s = AntSportsRpcCall.openTreasureBox(boxNo, userId)
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val ja = jo.getJSONArray("treasureBoxAwards")
                var num = 0
                for (i in 0..<ja.length()) {
                    jo = ja.getJSONObject(i)
                    num += jo.getInt("num")
                    other(TAG, "运动宝箱🎁[" + num + jo.getString("name") + "]")
                }
                return num
            } else if ("TREASUREBOX_NOT_EXIST" == jo.getString("resultCode")) {
                record(jo.getString("resultDesc"))
                return 1
            } else {
                record(jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "openTreasureBox err:")
            printStackTrace(TAG, t)
        }
        return 0
    }

    private fun queryProjectList(loader: ClassLoader?) {
        try {
            var jo = JSONObject(AntSportsRpcCall.queryProjectList(0))
            if (checkRes(TAG, jo)) {
                var charityCoinCount = jo.getInt("charityCoinCount")
                if (charityCoinCount < donateCharityCoinAmount!!.value!!) {
                    return
                }
                val ja = jo.getJSONObject("projectPage").getJSONArray("data")
                var i = 0
                while (i < ja.length() && charityCoinCount >= donateCharityCoinAmount!!.value!!) {
                    jo = ja.getJSONObject(i).getJSONObject("basicModel")
                    if ("DONATE_COMPLETED" == jo.getString("footballFieldStatus")) {
                        break
                    }
                    donate(
                        loader,
                        donateCharityCoinAmount!!.value!!,
                        jo.getString("projectId"),
                        jo.getString("title")
                    )
                    donateCharityCoin()
                    charityCoinCount -= donateCharityCoinAmount!!.value!!
                    if (donateCharityCoinType!!.value == DonateCharityCoinType.Companion.ONE) {
                        break
                    }
                    i++
                }
            } else {
                record(TAG)
                runtime(jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryProjectList err:")
            printStackTrace(TAG, t)
        }
    }

    private fun donate(
        loader: ClassLoader?,
        donateCharityCoin: Int,
        projectId: String,
        title: String?
    ) {
        try {
            val s = AntSportsRpcCall.donate(donateCharityCoin, projectId)
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                other(TAG, "捐赠活动❤️[" + title + "][" + donateCharityCoin + "能量🎈]")
            } else {
                runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "donate err:")
            printStackTrace(TAG, t)
        }
    }

    private fun queryWalkStep(loader: ClassLoader?) {
        try {
            var s = AntSportsRpcCall.queryWalkStep()
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                jo = jo.getJSONObject("dailyStepModel")
                val produceQuantity = jo.getInt("produceQuantity")
                val hour = getFormatTime().split(":".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()[0].toInt()

                if (produceQuantity >= minExchangeCount!!.value!! || hour >= latestExchangeTime!!.value!!) {
                    walkDonateSignInfo(produceQuantity)
                    s = donateWalkHome(produceQuantity)
                    jo = JSONObject(s)
                    if (!jo.getBoolean("isSuccess")) return
                    val walkDonateHomeModel = jo.getJSONObject("walkDonateHomeModel")
                    val walkUserInfoModel = walkDonateHomeModel.getJSONObject("walkUserInfoModel")
                    if (!walkUserInfoModel.has("exchangeFlag")) {
                        Status.exchangeToday(currentUid!!)
                        return
                    }
                    val donateToken = walkDonateHomeModel.getString("donateToken")
                    val walkCharityActivityModel =
                        walkDonateHomeModel.getJSONObject("walkCharityActivityModel")
                    val activityId = walkCharityActivityModel.getString("activityId")
                    s = exchange(activityId, produceQuantity, donateToken)
                    jo = JSONObject(s)
                    if (jo.getBoolean("isSuccess")) {
                        val donateExchangeResultModel =
                            jo.getJSONObject("donateExchangeResultModel")
                        val userCount = donateExchangeResultModel.getInt("userCount")
                        val amount = donateExchangeResultModel.getJSONObject("userAmount")
                            .getDouble("amount")
                        other(TAG, "捐出活动❤️[" + userCount + "步]#兑换" + amount + "元公益金")
                        Status.exchangeToday(currentUid!!)
                    } else if (s.contains("已捐步")) {
                        Status.exchangeToday(currentUid!!)
                    } else {
                        runtime(TAG, jo.getString("resultDesc"))
                    }
                }
            } else {
                runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryWalkStep err:")
            printStackTrace(TAG, t)
        }
    }

    /* 文体中心 */ // SPORTS_DAILY_SIGN_GROUP SPORTS_DAILY_GROUP
    private fun userTaskGroupQuery(groupId: String) {
        try {
            val s = AntSportsRpcCall.userTaskGroupQuery(groupId)
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group")
                val userTaskList = jo.getJSONArray("userTaskList")
                for (i in 0..<userTaskList.length()) {
                    jo = userTaskList.getJSONObject(i)
                    if ("TODO" != jo.getString("status")) continue
                    val taskInfo = jo.getJSONObject("taskInfo")
                    val bizType = taskInfo.getString("bizType")
                    val taskId = taskInfo.getString("taskId")
                    jo = JSONObject(userTaskComplete(bizType, taskId))
                    if (jo.optBoolean("success")) {
                        val taskName = taskInfo.optString("taskName", taskId)
                        other(TAG, "完成任务🧾[" + taskName + "]")
                    } else {
                        record(TAG, "文体每日任务" + " " + jo)
                    }
                }
            } else {
                record(TAG, "文体每日任务" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "userTaskGroupQuery err:")
            printStackTrace(TAG, t)
        }
    }

    private fun participate() {
        try {
            val s = queryAccount()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val balance = jo.getDouble("balance")
                if (balance < 100) return
                jo = JSONObject(queryRoundList())
                if (jo.optBoolean("success")) {
                    val dataList = jo.getJSONArray("dataList")
                    for (i in 0..<dataList.length()) {
                        jo = dataList.getJSONObject(i)
                        if ("P" != jo.getString("status")) continue
                        if (jo.has("userRecord")) continue
                        val instanceList = jo.getJSONArray("instanceList")
                        var pointOptions = 0
                        val roundId = jo.getString("id")
                        var InstanceId: String? = null
                        var ResultId: String? = null
                        for (j in instanceList.length() - 1 downTo 0) {
                            jo = instanceList.getJSONObject(j)
                            if (jo.getInt("pointOptions") < pointOptions) continue
                            pointOptions = jo.getInt("pointOptions")
                            InstanceId = jo.getString("id")
                            ResultId = jo.getString("instanceResultId")
                        }
                        jo = JSONObject(
                            AntSportsRpcCall.participate(
                                pointOptions,
                                InstanceId!!,
                                ResultId!!,
                                roundId
                            )
                        )
                        if (jo.optBoolean("success")) {
                            jo = jo.getJSONObject("data")
                            val roundDescription = jo.getString("roundDescription")
                            val targetStepCount = jo.getInt("targetStepCount")
                            other(TAG, "走路挑战🚶🏻‍♂️[" + roundDescription + "]#" + targetStepCount)
                        } else {
                            record(TAG, "走路挑战赛" + " " + jo)
                        }
                    }
                } else {
                    record(TAG, "queryRoundList" + " " + jo)
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "participate err:")
            printStackTrace(TAG, t)
        }
    }

    private fun userTaskRightsReceive() {
        try {
            val s = AntSportsRpcCall.userTaskGroupQuery("SPORTS_DAILY_GROUP")
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("group")
                val userTaskList = jo.getJSONArray("userTaskList")
                for (i in 0..<userTaskList.length()) {
                    jo = userTaskList.getJSONObject(i)
                    if ("COMPLETED" != jo.getString("status")) continue
                    val userTaskId = jo.getString("userTaskId")
                    val taskInfo = jo.getJSONObject("taskInfo")
                    val taskId = taskInfo.getString("taskId")
                    jo = JSONObject(userTaskRightsReceive(taskId, userTaskId))
                    if (jo.optBoolean("success")) {
                        val taskName = taskInfo.optString("taskName", taskId)
                        val rightsRuleList = taskInfo.getJSONArray("rightsRuleList")
                        val award = StringBuilder()
                        for (j in 0..<rightsRuleList.length()) {
                            jo = rightsRuleList.getJSONObject(j)
                            award.append(jo.getString("rightsName")).append("*")
                                .append(jo.getInt("baseAwardCount"))
                        }
                        other(TAG, "领取奖励🎖️[" + taskName + "]#" + award)
                    } else {
                        record(TAG, "文体中心领取奖励")
                        runtime(jo.toString())
                    }
                }
            } else {
                record(TAG, "文体中心领取奖励")
                runtime(s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "userTaskRightsReceive err:")
            printStackTrace(TAG, t)
        }
    }

    private fun pathFeatureQuery() {
        try {
            val s = AntSportsRpcCall.pathFeatureQuery()
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val path = jo.getJSONObject("path")
                val pathId = path.getString("pathId")
                val title = path.getString("title")
                val minGoStepCount = path.getInt("minGoStepCount")
                if (jo.has("userPath")) {
                    val userPath = jo.getJSONObject("userPath")
                    val userPathRecordStatus = userPath.getString("userPathRecordStatus")
                    if ("COMPLETED" == userPathRecordStatus) {
                        pathMapHomepage(pathId)
                        pathMapJoin(title, pathId)
                    } else if ("GOING" == userPathRecordStatus) {
                        pathMapHomepage(pathId)
                        val countDate = getFormatDate()
                        jo = JSONObject(stepQuery(countDate, pathId))
                        if (jo.optBoolean("success")) {
                            val canGoStepCount = jo.getInt("canGoStepCount")
                            if (canGoStepCount >= minGoStepCount) {
                                val userPathRecordId = userPath.getString("userPathRecordId")
                                tiyubizGo(
                                    countDate,
                                    title,
                                    canGoStepCount,
                                    pathId,
                                    userPathRecordId
                                )
                            }
                        }
                    }
                } else {
                    pathMapJoin(title, pathId)
                }
            } else {
                runtime(TAG, jo.getString("resultDesc"))
            }
        } catch (t: Throwable) {
            runtime(TAG, "pathFeatureQuery err:")
            printStackTrace(TAG, t)
        }
    }

    private fun pathMapHomepage(pathId: String) {
        try {
            val s = AntSportsRpcCall.pathMapHomepage(pathId)
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                if (!jo.has("userPathGoRewardList")) return
                val userPathGoRewardList = jo.getJSONArray("userPathGoRewardList")
                for (i in 0..<userPathGoRewardList.length()) {
                    jo = userPathGoRewardList.getJSONObject(i)
                    if ("UNRECEIVED" != jo.getString("status")) continue
                    val userPathRewardId = jo.getString("userPathRewardId")
                    jo = JSONObject(rewardReceive(pathId, userPathRewardId))
                    if (jo.optBoolean("success")) {
                        jo = jo.getJSONObject("userPathRewardDetail")
                        val rightsRuleList = jo.getJSONArray("userPathRewardRightsList")
                        val award = StringBuilder()
                        for (j in 0..<rightsRuleList.length()) {
                            jo = rightsRuleList.getJSONObject(j).getJSONObject("rightsContent")
                            award.append(jo.getString("name")).append("*")
                                .append(jo.getInt("count"))
                        }
                        other(TAG, "文体宝箱🎁[" + award + "]")
                    } else {
                        record(TAG, "文体中心开宝箱")
                        runtime(jo.toString())
                    }
                }
            } else {
                record(TAG, "文体中心开宝箱")
                runtime(s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "pathMapHomepage err:")
            printStackTrace(TAG, t)
        }
    }

    private fun pathMapJoin(title: String?, pathId: String) {
        try {
            val jo = JSONObject(AntSportsRpcCall.pathMapJoin(pathId))
            if (jo.optBoolean("success")) {
                other(TAG, "加入线路🚶🏻‍♂️[" + title + "]")
                pathFeatureQuery()
            } else {
                runtime(TAG, jo.toString())
            }
        } catch (t: Throwable) {
            runtime(TAG, "pathMapJoin err:")
            printStackTrace(TAG, t)
        }
    }

    private fun tiyubizGo(
        countDate: String, title: String?, goStepCount: Int, pathId: String,
        userPathRecordId: String
    ) {
        try {
            val s = AntSportsRpcCall.tiyubizGo(countDate, goStepCount, pathId, userPathRecordId)
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                jo = jo.getJSONObject("userPath")
                other(
                    TAG,
                    "行走线路🚶🏻‍♂️[" + title + "]#前进了" + jo.getInt("userPathRecordForwardStepCount") + "步"
                )
                pathMapHomepage(pathId)
                val completed = "COMPLETED" == jo.getString("userPathRecordStatus")
                if (completed) {
                    other(TAG, "完成线路🚶🏻‍♂️[" + title + "]")
                    // 🔴 修复无限递归Bug：移除递归调用，让下一轮任务执行时处理新路径
                    // pathFeatureQuery() // 这会导致StackOverflowError
                }
            } else {
                runtime(TAG, s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "tiyubizGo err:")
            printStackTrace(TAG, t)
        }
    }

    /* 抢好友大战 */
    private fun queryClubHome() {
        try {
            // 检查是否已达到0金币上限（实时检查）
            val maxCount: Int = zeroCoinLimit!!.value!!
            if (zeroTrainCoinCount >= maxCount) {
                val today = getDateStr2()
                put(TRAIN_FRIEND_ZERO_COIN_DATE, today)
                record(TAG, "✅ 训练好友获得0金币已达" + maxCount + "次上限，今日不再执行")
                return
            }
            // 发送 RPC 请求获取 club home 数据
            val clubHomeData = JSONObject(AntSportsRpcCall.queryClubHome())
            // 处理 mainRoom 中的 bubbleList
            processBubbleList(clubHomeData.optJSONObject("mainRoom"))
            // 处理 roomList 中的每个房间的 bubbleList
            val roomList = clubHomeData.optJSONArray("roomList")
            if (roomList != null) {
                for (i in 0..<roomList.length()) {
                    val room = roomList.optJSONObject(i)
                    processBubbleList(room)
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryClubHome err:")
            printStackTrace(TAG, t)
        }
    }

    // 训练好友-收金币
    private fun processBubbleList(`object`: JSONObject?) {
        if (`object` != null && `object`.has("bubbleList")) {
            try {
                val bubbleList = `object`.getJSONArray("bubbleList")
                for (j in 0..<bubbleList.length()) {
                    val bubble = bubbleList.getJSONObject(j)
                    // 获取 bubbleId
                    val bubbleId = bubble.optString("bubbleId")
                    // 调用 collectBubble 方法
                    collectBubble(bubbleId)
                    // 输出日志信息
                    val fullCoin = bubble.optInt("fullCoin")
                    other(TAG, "训练好友💰️[获得:" + fullCoin + "金币]")


                    // 记录0金币情况
                    if (fullCoin == 0) {
                        zeroTrainCoinCount++
                        // 获取用户设置的0金币上限次数
                        val maxCount: Int = zeroCoinLimit!!.value!!
                        // 如果0金币次数达到设置的上限，记录今天日期，今日不再执行
                        if (zeroTrainCoinCount >= maxCount) {
                            val today = getDateStr2()
                            put(TRAIN_FRIEND_ZERO_COIN_DATE, today)
                            record(
                                TAG,
                                "✅ 训练好友获得0金币已超过" + maxCount + "次，今日不再执行，明日自动恢复"
                            )
                            return  // 立即退出处理
                        } else {
                            // 显示当前计数情况
                            record(TAG, "训练好友0金币次数: " + zeroTrainCoinCount + "/" + maxCount)
                        }
                    }


                    // 添加 1 秒的等待时间
                    sleepCompat(1000)
                }
            } catch (t: Throwable) {
                runtime(TAG, "processBubbleList err:")
                printStackTrace(TAG, t)
            }
        }
    }

    // 训练好友-训练操作
    private fun queryTrainItem() {
        try {
            // 发送 RPC 请求获取 club home 数据
            val clubHomeData = JSONObject(AntSportsRpcCall.queryClubHome())
            // 检查是否存在 roomList
            if (clubHomeData.has("roomList")) {
                val roomList = clubHomeData.getJSONArray("roomList")
                // 遍历 roomList
                for (i in 0..<roomList.length()) {
                    val room = roomList.getJSONObject(i)
                    // 获取 memberList
                    val memberList = room.getJSONArray("memberList")
                    // 遍历 memberList
                    for (j in 0..<memberList.length()) {
                        val member = memberList.getJSONObject(j)
                        // 提取 memberId 和 originBossId
                        val memberId = member.getString("memberId")
                        val originBossId = member.getString("originBossId")
                        // 获取用户名称
                        val userName = getMaskName(originBossId)
                        // 发送 RPC 请求获取 train item 数据
                        val responseData = AntSportsRpcCall.queryTrainItem()
                        // 解析 JSON 数据
                        val responseJson = JSONObject(responseData)
                        // 检查请求是否成功
                        val success = responseJson.optBoolean("success")
                        if (!success) {
                            return
                        }
                        // 获取 trainItemList
                        val trainItemList = responseJson.getJSONArray("trainItemList")
                        // 遍历 trainItemList
                        for (k in 0..<trainItemList.length()) {
                            val trainItem = trainItemList.getJSONObject(k)
                            // 提取训练项目的相关信息
                            val itemType = trainItem.getString("itemType")
                            // 如果找到了 itemType 为 "barbell" 的训练项目，则调用 trainMember 方法并传递 itemType、memberId 和 originBossId 值
                            if ("barbell" == itemType) {
                                // 调用 trainMember 方法并传递 itemType、memberId 和 originBossId 值
                                val trainMemberResponse =
                                    trainMember(itemType, memberId, originBossId)
                                // 解析 trainMember 响应数据
                                val trainMemberResponseJson = JSONObject(trainMemberResponse)
                                // 检查 trainMember 响应是否成功
                                val trainMemberSuccess =
                                    trainMemberResponseJson.optBoolean("success")
                                if (!trainMemberSuccess) {
                                    runtime(TAG, "trainMember request failed")
                                    continue  // 如果 trainMember 请求失败，继续处理下一个训练项目
                                }
                                // 获取训练项目的名称
                                val trainItemName = trainItem.getString("name")
                                // 将用户名称和训练项目的名称添加到日志输出
                                other(TAG, "训练好友🥋[训练:" + userName + " " + trainItemName + "]")
                            }
                        }
                    }
                    // 添加 1 秒的间隔
                    sleepCompat(1000)
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "queryTrainItem err:")
            printStackTrace(TAG, t)
        }
    }

    // 抢好友大战-抢购好友
    private fun buyMember() {
        try {
            // 发送 RPC 请求获取 club home 数据
            val clubHomeResponse = AntSportsRpcCall.queryClubHome()
            sleepCompat(500)
            val clubHomeJson = JSONObject(clubHomeResponse)
            // 判断 clubAuth 字段是否为 "ENABLE"
            if (clubHomeJson.optString("clubAuth") != "ENABLE") {
                // 如果 clubAuth 不是 "ENABLE"，停止执行
                record(TAG, "抢好友大战🧑‍🤝‍🧑未授权开启")
                return
            }
            // 获取 coinBalance 的值
            val assetsInfo = clubHomeJson.getJSONObject("assetsInfo")
            val coinBalance = assetsInfo.getInt("coinBalance")
            val roomList = clubHomeJson.getJSONArray("roomList")
            // 遍历 roomList
            for (i in 0..<roomList.length()) {
                val room = roomList.getJSONObject(i)
                val memberList = room.optJSONArray("memberList")
                // 检查 memberList 是否为空
                if (memberList == null || memberList.length() == 0) {
                    // 获取 roomId 的值
                    val roomId = room.getString("roomId")
                    // 调用 queryMemberPriceRanking 方法并传递 coinBalance 的值
                    val memberPriceResult = queryMemberPriceRanking(coinBalance.toString())
                    sleepCompat(500)
                    val memberPriceJson = JSONObject(memberPriceResult)
                    // 检查是否存在 rank 字段
                    if (memberPriceJson.has("rank") && memberPriceJson.getJSONObject("rank")
                            .has("data")
                    ) {
                        val dataArray = memberPriceJson.getJSONObject("rank").getJSONArray("data")
                        // 遍历 data 数组
                        for (j in 0..<dataArray.length()) {
                            val dataObj = dataArray.getJSONObject(j)
                            val originBossId = dataObj.getString("originBossId")
                            // 检查 originBossId 是否在 originBossIdList 中
                            var isBattleForFriend =
                                originBossIdList!!.value!!.contains(originBossId)
                            if (battleForFriendType!!.value == BattleForFriendType.Companion.DONT_ROB) {
                                isBattleForFriend = !isBattleForFriend
                            }
                            if (isBattleForFriend) {
                                // 在这里调用 queryClubMember 方法并传递 memberId 和 originBossId 的值
                                val clubMemberResult =
                                    queryClubMember(dataObj.getString("memberId"), originBossId)
                                sleepCompat(500)
                                // 解析 queryClubMember 返回的 JSON 数据
                                val clubMemberJson = JSONObject(clubMemberResult)
                                if (clubMemberJson.has("member")) {
                                    val memberObj = clubMemberJson.getJSONObject("member")
                                    // 获取当前成员的信息
                                    val currentBossId = memberObj.getString("currentBossId")
                                    val memberId = memberObj.getString("memberId")
                                    val priceInfo = memberObj.getString("priceInfo")
                                    // 调用 buyMember 方法
                                    val buyMemberResult = buyMember(
                                        currentBossId,
                                        memberId,
                                        originBossId,
                                        priceInfo,
                                        roomId
                                    )
                                    sleepCompat(500)
                                    // 处理 buyMember 的返回结果
                                    val buyMemberResponse = JSONObject(buyMemberResult)
                                    if (checkRes(TAG, buyMemberResponse)) {
                                        val userName = getMaskName(originBossId)
                                        other(TAG, "抢购好友🥋[成功:将 " + userName + " 抢回来]")
                                        // 抢好友成功后，如果训练好友功能开启，则执行训练
                                        if (trainFriend!!.value == true) {
                                            queryTrainItem()
                                        }
                                    } else if ("CLUB_AMOUNT_NOT_ENOUGH" == buyMemberResponse.getString(
                                            "resultCode"
                                        )
                                    ) {
                                        record(TAG, "[能量🎈不足，无法完成抢购好友！]")
                                    } else if ("CLUB_MEMBER_TRADE_PROTECT" == buyMemberResponse.getString(
                                            "resultCode"
                                        )
                                    ) {
                                        record(TAG, "[暂时无法抢购好友，给Ta一段独处的时间吧！]")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "buyMember err:")
            printStackTrace(TAG, t)
        }
    }

    interface WalkPathTheme {
        companion object {
            const val DA_MEI_ZHONG_GUO: Int = 0
            const val GONG_YI_YI_XIAO_BU: Int = 1
            const val DENG_DING_ZHI_MA_SHAN: Int = 2
            const val WEI_C_DA_TIAO_ZHAN: Int = 3
            const val LONG_NIAN_QI_FU: Int = 4
            val nickNames: Array<String?> =
                arrayOf<String?>("大美中国", "公益一小步", "登顶芝麻山", "维C大挑战", "龙年祈福")
        }
    }

    interface DonateCharityCoinType {
        companion object {
            const val ONE: Int = 0
            const val ALL: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("捐赠一个项目", "捐赠所有项目")
        }
    }

    interface BattleForFriendType {
        companion object {
            const val ROB: Int = 0
            const val DONT_ROB: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中抢", "选中不抢")
        }
    }


    companion object {
        private val TAG: String = AntSports::class.java.getSimpleName()
        private const val SPORTS_TASKS_COMPLETED_DATE = "SPORTS_TASKS_COMPLETED_DATE" // 运动任务完成日期缓存键
        private const val TRAIN_FRIEND_ZERO_COIN_DATE =
            "TRAIN_FRIEND_ZERO_COIN_DATE" // 训练好友0金币达上限日期缓存键
    }
}
