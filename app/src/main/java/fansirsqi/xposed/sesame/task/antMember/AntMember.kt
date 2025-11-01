package fansirsqi.xposed.sesame.task.antMember

import fansirsqi.xposed.sesame.data.Status
import fansirsqi.xposed.sesame.entity.MemberBenefit
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.GlobalThreadPools
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.ResChecker
import fansirsqi.xposed.sesame.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class AntMember : ModelTask() {

    companion object {
        private const val TAG = "AntMember"

        /**
         * 不能完成的任务黑名单（根据title关键词匹配）
         */
        private val TASK_BLACKLIST = arrayOf(
            "每日施肥领水果",           // 需要淘宝操作
            "坚持种水果",              // 需要淘宝操作  
            "坚持去玩休闲小游戏",       // 需要游戏操作
            "去AQapp提问",            // 需要下载APP
            "去AQ提问",               // 需要下载APP
            "坚持看直播领福利",        // 需要淘宝直播
            "去淘金币逛一逛",          // 需要淘宝操作
            "坚持攒保障金",
            "芝麻租赁下单得芝麻粒",
            "浏览租赁商家小程序"        // 需要小程序操作
        )

        /**
         * 检查任务是否在黑名单中
         * @param taskTitle 任务标题
         * @return true表示在黑名单中，应该跳过
         */
        private fun isTaskInBlacklist(taskTitle: String?): Boolean {
            if (taskTitle == null) return false
            for (blacklistItem in TASK_BLACKLIST) {
                if (taskTitle.contains(blacklistItem)) {
                    return true
                }
            }
            return false
        }

        /**
         * 会员积分收取
         * @param page 第几页
         * @param pageSize 每页数据条数
         */
        private suspend fun queryPointCert(page: Int, pageSize: Int) {
            try {
                var s = AntMemberRpcCall.queryPointCert(page, pageSize)
                GlobalThreadPools.sleepCompat(500)
                var jo = JSONObject(s)
                if (ResChecker.checkRes("$TAG 查询会员积分证书失败:", jo)) {
                    val hasNextPage = jo.getBoolean("hasNextPage")
                    val jaCertList = jo.getJSONArray("certList")
                    for (i in 0 until jaCertList.length()) {
                        val cert = jaCertList.getJSONObject(i)
                        val bizTitle = cert.getString("bizTitle")
                        val id = cert.getString("id")
                        val pointAmount = cert.getInt("pointAmount")
                        s = AntMemberRpcCall.receivePointByUser(id)
                        jo = JSONObject(s)
                        if (ResChecker.checkRes("$TAG 会员积分领取失败:", jo)) {
                            Log.other("会员积分🎖️[领取$bizTitle]#${pointAmount}积分")
                        } else {
                            Log.record(jo.getString("resultDesc"))
                            Log.runtime(s)
                        }
                    }
                    if (hasNextPage) {
                        queryPointCert(page + 1, pageSize)
                    }
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(s)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "queryPointCert err:")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * 检查是否满足运行芝麻信用任务的条件
         * @return bool
         */
        private suspend fun checkSesameCanRun(): Boolean {
            return try {
                val s = AntMemberRpcCall.queryHome()
                val jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.other(TAG, "芝麻信用💳[首页响应失败]#${jo.optString("errorMsg")}")
                    Log.error("$TAG.checkSesameCanRun.queryHome", "芝麻信用💳[首页响应失败]#$s")
                    false
                } else {
                    val entrance = jo.getJSONObject("entrance")
                    if (!entrance.optBoolean("openApp")) {
                        Log.other("芝麻信用💳[未开通芝麻信用]")
                        false
                    } else {
                        true
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace("$TAG.checkSesameCanRun", t)
                false
            }
        }

        /**
         * 芝麻信用-领取并完成任务（带结果统计）
         * @param taskList 任务列表
         * @return int数组 [完成数量, 跳过数量]
         * @throws JSONException JSON解析异常，上抛处理
         */
        private suspend fun joinAndFinishSesameTaskWithResult(taskList: JSONArray): IntArray {
            var completedCount = 0
            var skippedCount = 0

            for (i in 0 until taskList.length()) {
                val task = taskList.getJSONObject(i)
                val taskTitle = if (task.has("title")) task.getString("title") else "未知任务"

                // 打印任务状态信息用于调试
                val finishFlag = task.optBoolean("finishFlag", false)
                val actionText = task.optString("actionText", "")

                // 检查任务是否已完成
                if (finishFlag || "已完成" == actionText) {
                    Log.record(TAG, "芝麻信用💳[跳过已完成任务]#$taskTitle")
                    skippedCount++
                    continue
                }

                // 检查黑名单
                if (isTaskInBlacklist(taskTitle)) {
                    Log.record(TAG, "芝麻信用💳[跳过黑名单任务]#$taskTitle")
                    skippedCount++
                    continue
                }

                // 添加检查，确保templateId存在
                if (!task.has("templateId")) {
                    Log.record(TAG, "芝麻信用💳[跳过缺少templateId任务]#$taskTitle")
                    skippedCount++
                    continue
                }

                val taskTemplateId = task.getString("templateId")
                val needCompleteNum = if (task.has("needCompleteNum")) task.getInt("needCompleteNum") else 1
                val completedNum = task.optInt("completedNum", 0)

                if (task.has("actionUrl") && task.getString("actionUrl").contains("jumpAction")) {
                    // 跳转APP任务 依赖跳转的APP发送请求鉴别任务完成 仅靠hook支付宝无法完成
                    Log.record(TAG, "芝麻信用💳[跳过跳转APP任务]#$taskTitle")
                    skippedCount++
                    continue
                }

                var taskCompleted = false
                val recordId: String
                if (!task.has("todayFinish")) {
                    // 领取任务
                    var s = AntMemberRpcCall.joinSesameTask(taskTemplateId)
                    GlobalThreadPools.sleepCompat(200)
                    val responseObj = JSONObject(s)
                    if (!responseObj.optBoolean("success")) {
                        Log.other(TAG, "芝麻信用💳[领取任务" + taskTitle + "失败]#" + s)
                        skippedCount++
                        continue
                    }
                    recordId = responseObj.getJSONObject("data").getString("recordId")
                } else {
                    if (!task.has("recordId")) {
                        Log.other(TAG, "芝麻信用💳[任务" + taskTitle + "未获取到recordId]#" + task.toString())
                        skippedCount++
                        continue
                    }
                    recordId = task.getString("recordId")
                }

                // 完成任务
                for (j in completedNum until needCompleteNum) {
                    val s = AntMemberRpcCall.finishSesameTask(recordId)
                    GlobalThreadPools.sleepCompat(200)
                    val responseObj = JSONObject(s)
                    if (responseObj.optBoolean("success")) {
                        Log.record(TAG, "芝麻信用💳[完成任务" + taskTitle + "]#(" + (j + 1) + "/" + needCompleteNum + "天)")
                        taskCompleted = true
                    } else {
                        Log.other(TAG, "芝麻信用💳[完成任务" + taskTitle + "失败]#" + s)
                        break
                    }
                }

                if (taskCompleted) {
                    completedCount++
                } else {
                    skippedCount++
                }
            }

            return intArrayOf(completedCount, skippedCount)
        }

        /**
         * 商家开门打卡签到
         */
        private suspend fun kmdkSignIn() {
            try {
                val s = AntMemberRpcCall.queryActivity()
                val jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    if ("SIGN_IN_ENABLE" == jo.getString("signInStatus")) {
                        val activityNo = jo.getString("activityNo")
                        val joSignIn = JSONObject(AntMemberRpcCall.signIn(activityNo))
                        if (joSignIn.optBoolean("success")) {
                            Log.other("商家服务🏬[开门打卡签到成功]")
                        } else {
                            Log.record(joSignIn.getString("errorMsg"))
                            Log.runtime(joSignIn.toString())
                        }
                    }
                } else {
                    Log.record(TAG, "queryActivity $s")
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "kmdkSignIn err:")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * 商家开门打卡报名
         */
        private suspend fun kmdkSignUp() {
            try {
                for (i in 0 until 5) {
                    val jo = JSONObject(AntMemberRpcCall.queryActivity())
                    if (jo.optBoolean("success")) {
                        val activityNo = jo.getString("activityNo")
                        if (!TimeUtil.getFormatDate().replace("-", "").equals(activityNo.split("_")[2])) {
                            break
                        }
                        if ("SIGN_UP" == jo.getString("signUpStatus")) {
                            break
                        }
                        if ("UN_SIGN_UP" == jo.getString("signUpStatus")) {
                            val activityPeriodName = jo.getString("activityPeriodName")
                            val joSignUp = JSONObject(AntMemberRpcCall.signUp(activityNo))
                            if (joSignUp.optBoolean("success")) {
                                Log.other("商家服务🏬[" + activityPeriodName + "开门打卡报名]")
                                return
                            } else {
                                Log.record(joSignUp.getString("errorMsg"))
                                Log.runtime(joSignUp.toString())
                            }
                        }
                    } else {
                        Log.record(TAG, "queryActivity")
                        Log.runtime(jo.toString())
                    }
                    GlobalThreadPools.sleepCompat(500)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "kmdkSignUp err:")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * 商家积分签到
         */
        private suspend fun doMerchantSign() {
            try {
                var s = AntMemberRpcCall.merchantSign()
                var jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.runtime(TAG, "doMerchantSign err:$s")
                    return
                }
                jo = jo.getJSONObject("data")
                val signResult = jo.getString("signInResult")
                val reward = jo.getString("todayReward")
                if ("SUCCESS" == signResult) {
                    Log.other("商家服务🏬[每日签到]#获得积分$reward")
                } else {
                    Log.record(s)
                    Log.runtime(s)
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "kmdkSignIn err:")
                Log.printStackTrace(TAG, t)
            }
        }

        /**
         * 商家积分任务
         */
        private suspend fun doMerchantMoreTask() {
            val s = AntMemberRpcCall.taskListQuery()
            try {
                var doubleCheck = false
                val jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    val taskList = jo.getJSONObject("data").getJSONArray("taskList")
                    for (i in 0 until taskList.length()) {
                        val task = taskList.getJSONObject(i)
                        if (!task.has("status")) {
                            continue
                        }
                        val title = task.getString("title")
                        val reward = task.getString("reward")
                        val taskStatus = task.getString("status")
                        if ("NEED_RECEIVE" == taskStatus) {
                            if (task.has("pointBallId")) {
                                val receiveJo = JSONObject(AntMemberRpcCall.ballReceive(task.getString("pointBallId")))
                                if (receiveJo.optBoolean("success")) {
                                    Log.other("商家服务🏬[$title]#领取积分$reward")
                                }
                            }
                        } else if ("PROCESSING" == taskStatus || "UNRECEIVED" == taskStatus) {
                            if (task.has("extendLog")) {
                                val bizExtMap = task.getJSONObject("extendLog").getJSONObject("bizExtMap")
                                val finishJo = JSONObject(AntMemberRpcCall.taskFinish(bizExtMap.getString("bizId")))
                                if (finishJo.optBoolean("success")) {
                                    Log.other("商家服务🏬[$title]#领取积分$reward")
                                }
                                doubleCheck = true
                            } else {
                                val taskCode = task.getString("taskCode")
                                when (taskCode) {
                                    "SYH_CPC_DYNAMIC" -> {
                                        // 逛一逛商品橱窗
                                        taskReceive(taskCode, "SYH_CPC_DYNAMIC_VIEWED", title)
                                    }
                                    "JFLLRW_TASK" -> {
                                        // 逛一逛得缴费红包
                                        taskReceive(taskCode, "JFLL_VIEWED", title)
                                    }
                                    "ZFBHYLLRW_TASK" -> {
                                        // 逛一逛支付宝会员
                                        taskReceive(taskCode, "ZFBHYLL_VIEWED", title)
                                    }
                                    "QQKLLRW_TASK" -> {
                                        // 逛一逛支付宝亲情卡
                                        taskReceive(taskCode, "QQKLL_VIEWED", title)
                                    }
                                    "SSLLRW_TASK" -> {
                                        // 逛逛领优惠得红包
                                        taskReceive(taskCode, "SSLL_VIEWED", title)
                                    }
                                    "ELMGYLLRW2_TASK" -> {
                                        // 去饿了么果园0元领水果
                                        taskReceive(taskCode, "ELMGYLL_VIEWED", title)
                                    }
                                    "ZMXYLLRW_TASK" -> {
                                        // 去逛逛芝麻攒粒攻略
                                        taskReceive(taskCode, "ZMXYLL_VIEWED", title)
                                    }
                                    "GXYKPDDYH_TASK" -> {
                                        // 逛信用卡频道得优惠
                                        taskReceive(taskCode, "xykhkzd_VIEWED", title)
                                    }
                                    "HHKLLRW_TASK" -> {
                                        // 49999元花呗红包集卡抽
                                        taskReceive(taskCode, "HHKLLX_VIEWED", title)
                                    }
                                    "TBNCLLRW_TASK" -> {
                                        // 去淘宝芭芭农场领水果百货
                                        taskReceive(taskCode, "TBNCLLRW_TASK_VIEWED", title)
                                    }
                                }
                            }
                        }
                    }
                    if (doubleCheck) {
                        doMerchantMoreTask()
                    }
                } else {
                    Log.runtime(TAG, "taskListQuery err: $s")
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "taskListQuery err:")
                Log.printStackTrace(TAG, t)
            } finally {
                try {
                    GlobalThreadPools.sleepCompat(1000)
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
        }

        /**
         * 完成商家积分任务
         * @param taskCode 任务代码
         * @param actionCode 行为代码
         * @param title 标题
         */
        private suspend fun taskReceive(taskCode: String, actionCode: String, title: String) {
            try {
                var s = AntMemberRpcCall.taskReceive(taskCode)
                var jo = JSONObject(s)
                if (jo.optBoolean("success")) {
                    GlobalThreadPools.sleepCompat(500)
                    jo = JSONObject(AntMemberRpcCall.actioncode(actionCode))
                    if (jo.optBoolean("success")) {
                        GlobalThreadPools.sleepCompat(16000)
                        jo = JSONObject(AntMemberRpcCall.produce(actionCode))
                        if (jo.optBoolean("success")) {
                            Log.other("商家服务🏬[完成任务$title]")
                        }
                    }
                } else {
                    Log.record(TAG, "taskReceive $s")
                }
            } catch (t: Throwable) {
                Log.runtime(TAG, "taskReceive err:")
                Log.printStackTrace(TAG, t)
            }
        }
    }

    private var memberSign: BooleanModelField? = null
    private var memberTask: BooleanModelField? = null
    private var memberPointExchangeBenefit: BooleanModelField? = null
    private var memberPointExchangeBenefitList: SelectModelField? = null
    private var collectSesame: BooleanModelField? = null
    private var collectSesameWithOneClick: BooleanModelField? = null
    private var sesameTask: BooleanModelField? = null
    private var collectInsuredGold: BooleanModelField? = null
    private var enableGoldTicket: BooleanModelField? = null
    private var enableGameCenter: BooleanModelField? = null
    private var merchantSign: BooleanModelField? = null
    private var merchantKmdk: BooleanModelField? = null
    private var merchantMoreTask: BooleanModelField? = null
    private var beanSignIn: BooleanModelField? = null
    private var beanExchangeBubbleBoost: BooleanModelField? = null

    override fun getName(): String {
        return "会员"
    }

    override fun getGroup(): ModelGroup {
        return ModelGroup.MEMBER
    }

    override fun getIcon(): String {
        return "AntMember.png"
    }

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField("memberSign", "会员签到", false).also { memberSign = it }
        )
        modelFields.addField(
            BooleanModelField("memberTask", "会员任务", false).also { memberTask = it }
        )
        modelFields.addField(
            BooleanModelField("memberPointExchangeBenefit", "会员积分 | 兑换权益", false)
                .also { memberPointExchangeBenefit = it }
        )
        modelFields.addField(
            SelectModelField(
                "memberPointExchangeBenefitList",
                "会员积分 | 权益列表",
                LinkedHashSet(),
                MemberBenefit.getList()
            ).also { memberPointExchangeBenefitList = it }
        )
        modelFields.addField(
            BooleanModelField("sesameTask", "芝麻信用|芝麻粒信用任务", false).also { sesameTask = it }
        )
        modelFields.addField(
            BooleanModelField("collectSesame", "芝麻信用|芝麻粒领取", false).also { collectSesame = it }
        )
        modelFields.addField(
            BooleanModelField("collectSesameWithOneClick", "芝麻信用|芝麻粒领取使用一键收取", false)
                .also { collectSesameWithOneClick = it }
        )
        modelFields.addField(
            BooleanModelField("collectInsuredGold", "蚂蚁保|保障金领取", false).also { collectInsuredGold = it }
        )
        modelFields.addField(
            BooleanModelField("enableGoldTicket", "黄金票签到", false).also { enableGoldTicket = it }
        )
        modelFields.addField(
            BooleanModelField("enableGameCenter", "游戏中心签到", false).also { enableGameCenter = it }
        )
        modelFields.addField(
            BooleanModelField("merchantSign", "商家服务|签到", false).also { merchantSign = it }
        )
        modelFields.addField(
            BooleanModelField("merchantKmdk", "商家服务|开门打卡", false).also { merchantKmdk = it }
        )
        modelFields.addField(
            BooleanModelField("merchantMoreTask", "商家服务|积分任务", false).also { merchantMoreTask = it }
        )
        modelFields.addField(
            BooleanModelField("beanSignIn", "安心豆签到", false).also { beanSignIn = it }
        )
        modelFields.addField(
            BooleanModelField("beanExchangeBubbleBoost", "安心豆兑换时光加速器", false)
                .also { beanExchangeBubbleBoost = it }
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
            if (memberSign?.value == true) {
                doMemberSign()
            }
            if (memberTask?.value == true) {
                doAllMemberAvailableTask()
            }
            if (memberPointExchangeBenefit?.value == true) {
                memberPointExchangeBenefit()
            }
            if ((sesameTask?.value == true || collectSesame?.value == true) && checkSesameCanRun()) {
                if (sesameTask?.value == true) {
                    doAllAvailableSesameTask()
                }
                if (collectSesame?.value == true) {
                    collectSesame(collectSesameWithOneClick?.value ?: false)
                }
            }
            if (collectInsuredGold?.value == true) {
                collectInsuredGold()
            }
            if (enableGoldTicket?.value == true) {
                goldTicket()
            }
            if (enableGameCenter?.value == true) {
                enableGameCenter()
            }
            if (beanSignIn?.value == true) {
                beanSignIn()
            }
            if (beanExchangeBubbleBoost?.value == true) {
                beanExchangeBubbleBoost()
            }
            if (merchantSign?.value == true || merchantKmdk?.value == true || merchantMoreTask?.value == true) {
                val jo = JSONObject(AntMemberRpcCall.transcodeCheck())
                if (!jo.optBoolean("success")) {
                    return
                }
                val data = jo.getJSONObject("data")
                if (!data.optBoolean("isOpened")) {
                    Log.record(TAG, "商家服务👪未开通")
                    return
                }
                if (merchantKmdk?.value == true) {
                    if (TimeUtil.isNowAfterTimeStr("0600") && TimeUtil.isNowBeforeTimeStr("1200")) {
                        kmdkSignIn()
                    }
                    kmdkSignUp()
                }
                if (merchantSign?.value == true) {
                    doMerchantSign()
                }
                if (merchantMoreTask?.value == true) {
                    doMerchantMoreTask()
                }
            }
        } catch (e: CancellationException) {
            Log.runtime(TAG, "AntMember 协程被取消")
            throw e
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        } finally {
            Log.record(TAG, "执行结束-" + getName())
        }
    }

    /**
     * 会员积分0元兑，权益道具兑换
     */
    private suspend fun memberPointExchangeBenefit() {
        try {
            val userId = UserMap.currentUid ?: return
            val memberInfo = JSONObject(AntMemberRpcCall.queryMemberInfo())
            if (!ResChecker.checkRes(TAG, memberInfo)) {
                return
            }
            val pointBalance = memberInfo.getString("pointBalance")
            val jo = JSONObject(AntMemberRpcCall.queryShandieEntityList(userId, pointBalance))
            if (!ResChecker.checkRes(TAG, jo)) {
                return
            }
            if (!jo.has("benefits")) {
                Log.record(TAG, "会员积分[未找到可兑换权益]")
                return
            }
            val benefits = jo.getJSONArray("benefits")
            for (i in 0 until benefits.length()) {
                val benefitInfo = benefits.getJSONObject(i)
                val pricePresentation = benefitInfo.getJSONObject("pricePresentation")
                val name = benefitInfo.getString("name")
                val benefitId = benefitInfo.getString("benefitId")
                IdMapManager.getInstance(MemberBenefitsMap::class.java).add(benefitId, name)
                if (!Status.canMemberPointExchangeBenefitToday(benefitId)
                    || !(memberPointExchangeBenefitList?.value?.contains(benefitId) == true)
                ) {
                    continue
                }
                val itemId = benefitInfo.getString("itemId")
                if (exchangeBenefit(benefitId, itemId)) {
                    val point = pricePresentation.getString("point")
                    Log.other("会员积分🎐兑换[" + name + "]#花费[" + point + "积分]")
                } else {
                    Log.other("会员积分🎐兑换[$name]失败！")
                }
            }
            IdMapManager.getInstance(MemberBenefitsMap::class.java).save(userId)
        } catch (e: JSONException) {
            Log.record(TAG, "JSON解析错误: " + (e.message ?: ""))
            Log.printStackTrace(TAG, e)
        } catch (t: Throwable) {
            Log.runtime(TAG, "memberPointExchangeBenefit err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun exchangeBenefit(benefitId: String, itemId: String): Boolean {
        return try {
            val jo = JSONObject(AntMemberRpcCall.exchangeBenefit(benefitId, itemId))
            if (ResChecker.checkRes(TAG + "会员权益兑换失败:", jo)) {
                Status.memberPointExchangeBenefitToday(benefitId)
                true
            } else {
                false
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "exchangeBenefit err:")
            Log.printStackTrace(TAG, t)
            false
        }
    }

    /**
     * 会员签到
     */
    private suspend fun doMemberSign() {
        try {
            val currentUid = UserMap.currentUid
            if (currentUid != null && Status.canMemberSignInToday(currentUid)) {
                var s = AntMemberRpcCall.queryMemberSigninCalendar()
                GlobalThreadPools.sleepCompat(500)
                val jo = JSONObject(s)
                if (ResChecker.checkRes(TAG + "会员签到失败:", jo)) {
                    Log.other(
                        "会员签到📅[" + jo.getString("signinPoint") + "积分]#已签到" + jo.getString("signinSumDay") + "天"
                    )
                    Status.memberSignInToday(currentUid)
                } else {
                    Log.record(jo.getString("resultDesc"))
                    Log.runtime(s)
                }
            }
            queryPointCert(1, 8)
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 会员任务-逛一逛
     * 单次执行 1
     */
    private suspend fun doAllMemberAvailableTask() {
        try {
            var str = AntMemberRpcCall.queryAllStatusTaskList()
            GlobalThreadPools.sleepCompat(500)
            val jsonObject = JSONObject(str)
            if (!ResChecker.checkRes(TAG, jsonObject)) {
                Log.error(TAG + ".doAllMemberAvailableTask", "会员任务响应失败: " + jsonObject.getString("resultDesc"))
                return
            }
            if (!jsonObject.has("availableTaskList")) {
                return
            }
            val taskList = jsonObject.getJSONArray("availableTaskList")
            for (j in 0 until taskList.length()) {
                val task = taskList.getJSONObject(j)
                processTask(task)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "doAllMemberAvailableTask err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 芝麻信用任务 - 重构版本
     */
    private suspend fun doAllAvailableSesameTask() {
        try {
            var s = AntMemberRpcCall.queryAvailableSesameTask()
            GlobalThreadPools.sleepCompat(500)
            var jo = JSONObject(s)
            if (jo.has("resData")) {
                jo = jo.getJSONObject("resData")
            }
            if (!jo.optBoolean("success")) {
                Log.other(TAG, "芝麻信用💳[查询任务响应失败]#" + jo.getString("resultCode"))
                Log.error("$TAG.doAllAvailableSesameTask.queryAvailableSesameTask", "芝麻信用💳[查询任务响应失败]#$s")
                return
            }

            val taskObj = jo.getJSONObject("data")
            var totalTasks = 0
            var completedTasks = 0
            var skippedTasks = 0

            // 处理日常任务
            if (taskObj.has("dailyTaskListVO")) {
                val dailyTaskListVO = taskObj.getJSONObject("dailyTaskListVO")

                if (dailyTaskListVO.has("waitCompleteTaskVOS")) {
                    val waitCompleteTaskVOS = dailyTaskListVO.getJSONArray("waitCompleteTaskVOS")
                    totalTasks += waitCompleteTaskVOS.length()
                    Log.record(TAG, "芝麻信用💳[待完成任务]#开始处理(" + waitCompleteTaskVOS.length() + "个)")
                    val results = joinAndFinishSesameTaskWithResult(waitCompleteTaskVOS)
                    completedTasks += results[0]
                    skippedTasks += results[1]
                }

                if (dailyTaskListVO.has("waitJoinTaskVOS")) {
                    val waitJoinTaskVOS = dailyTaskListVO.getJSONArray("waitJoinTaskVOS")
                    totalTasks += waitJoinTaskVOS.length()
                    Log.record(TAG, "芝麻信用💳[待加入任务]#开始处理(" + waitJoinTaskVOS.length() + "个)")
                    val results = joinAndFinishSesameTaskWithResult(waitJoinTaskVOS)
                    completedTasks += results[0]
                    skippedTasks += results[1]
                }
            }

            // 处理toCompleteVOS任务
            if (taskObj.has("toCompleteVOS")) {
                val toCompleteVOS = taskObj.getJSONArray("toCompleteVOS")
                totalTasks += toCompleteVOS.length()
                Log.record(TAG, "芝麻信用💳[toCompleteVOS任务]#开始处理(" + toCompleteVOS.length() + "个)")
                val results = joinAndFinishSesameTaskWithResult(toCompleteVOS)
                completedTasks += results[0]
                skippedTasks += results[1]
            }

            // 统计结果并决定是否关闭开关
            Log.record(
                TAG,
                "芝麻信用💳[任务处理完成]#总任务:" + totalTasks + "个, 完成:" + completedTasks + "个, 跳过:" + skippedTasks + "个"
            )

            // 如果所有任务都已完成或跳过（没有剩余可完成任务），关闭开关
            if (totalTasks > 0 && (completedTasks + skippedTasks) >= totalTasks) {
                sesameTask?.setObjectValue(false)
                Log.record(TAG, "芝麻信用💳[已全部完成任务，临时关闭]")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.doAllAvailableSesameTask", t)
        }
    }

    /**
     * 芝麻粒收取
     * @param withOneClick 启用一键收取
     */
    private suspend fun collectSesame(withOneClick: Boolean) {
        try {
            var jo = JSONObject(AntMemberRpcCall.queryCreditFeedback())
            GlobalThreadPools.sleepCompat(500)
            if (!jo.optBoolean("success")) {
                Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"))
                Log.error("$TAG.collectSesame.queryCreditFeedback", "芝麻信用💳[查询未领取芝麻粒响应失败]#$jo")
                return
            }
            val availableCollectList = jo.getJSONArray("creditFeedbackVOS")
            if (withOneClick) {
                GlobalThreadPools.sleepCompat(2000)
                jo = JSONObject(AntMemberRpcCall.collectAllCreditFeedback())
                GlobalThreadPools.sleepCompat(2000)
                if (!jo.optBoolean("success")) {
                    Log.other(TAG, "芝麻信用💳[一键收取芝麻粒响应失败]#$jo")
                    Log.error(
                        "$TAG.collectSesame.collectAllCreditFeedback",
                        "芝麻信用💳[一键收取芝麻粒响应失败]#$jo"
                    )
                    return
                }
            }
            for (i in 0 until availableCollectList.length()) {
                val item = availableCollectList.getJSONObject(i)
                if ("UNCLAIMED" != item.getString("status")) {
                    continue
                }
                val title = item.getString("title")
                val creditFeedbackId = item.getString("creditFeedbackId")
                val potentialSize = item.getString("potentialSize")
                if (!withOneClick) {
                    jo = JSONObject(AntMemberRpcCall.collectCreditFeedback(creditFeedbackId))
                    GlobalThreadPools.sleepCompat(2000)
                    if (!jo.optBoolean("success")) {
                        Log.other(TAG, "芝麻信用💳[查询未领取芝麻粒响应失败]#" + jo.getString("resultView"))
                        Log.error("$TAG.collectSesame.collectCreditFeedback", "芝麻信用💳[收取芝麻粒响应失败]#$jo")
                        continue
                    }
                }
                Log.other("芝麻信用💳[" + title + "]#" + potentialSize + "粒" + (if (withOneClick) "(一键收取)" else ""))
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.collectSesame", t)
        }
    }

    /**
     * 保障金领取
     */
    private suspend fun collectInsuredGold() {
        try {
            var s = AntMemberRpcCall.queryAvailableCollectInsuredGold()
            GlobalThreadPools.sleepCompat(200)
            var jo = JSONObject(s)
            if (!jo.optBoolean("success")) {
                Log.other("$TAG.collectInsuredGold.queryInsuredHome", "保障金🏥[响应失败]#$s")
                return
            }
            jo = jo.getJSONObject("data")
            val signInBall = jo.getJSONObject("signInDTO")
            val otherBallList = jo.getJSONArray("eventToWaitDTOList")
            if (1 == signInBall.getInt("sendFlowStatus") && 1 == signInBall.getInt("sendType")) {
                s = AntMemberRpcCall.collectInsuredGold(signInBall)
                GlobalThreadPools.sleepCompat(2000)
                jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.other("$TAG.collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#$s")
                    return
                }
                val gainGold = jo.getJSONObject("data").getString("gainSumInsuredYuan")
                Log.other("保障金🏥[领取保证金]#+" + gainGold + "元")
            }
            for (i in 0 until otherBallList.length()) {
                val anotherBall = otherBallList.getJSONObject(i)
                s = AntMemberRpcCall.collectInsuredGold(anotherBall)
                GlobalThreadPools.sleepCompat(2000)
                jo = JSONObject(s)
                if (!jo.optBoolean("success")) {
                    Log.other("$TAG.collectInsuredGold.collectInsuredGold", "保障金🏥[响应失败]#$s")
                    return
                }
                val gainGold = jo.getJSONObject("data").getJSONObject("gainSumInsuredDTO")
                    .getString("gainSumInsuredYuan")
                Log.other("保障金🏥[领取保证金]+" + gainGold + "元")
            }
        } catch (t: Throwable) {
            Log.printStackTrace("$TAG.collectInsuredGold", t)
        }
    }

    /**
     * 执行会员任务 类型1
     * @param task 单个任务对象
     */
    private suspend fun processTask(task: JSONObject) {
        try {
            val taskConfigInfo = task.getJSONObject("taskConfigInfo")
            val name = taskConfigInfo.getString("name")
            val id = taskConfigInfo.getLong("id")
            val awardParamPoint = taskConfigInfo.getJSONObject("awardParam").getString("awardParamPoint")
            val targetBusiness = taskConfigInfo.getJSONArray("targetBusiness").getString(0)
            val targetBusinessArray = targetBusiness.split("#")
            if (targetBusinessArray.size < 3) {
                Log.runtime(TAG, "processTask target param err:" + targetBusinessArray.joinToString(separator = ","))
                return
            }
            val bizType = targetBusinessArray[0]
            val bizSubType = targetBusinessArray[1]
            val bizParam = targetBusinessArray[2]
            GlobalThreadPools.sleepCompat(16000)
            val str = AntMemberRpcCall.executeTask(bizParam, bizSubType, bizType, id)
            val jo = JSONObject(str)
            if (!ResChecker.checkRes(TAG + "执行会员任务失败:", jo)) {
                Log.runtime(TAG, "执行任务失败:" + jo.optString("resultDesc"))
                return
            }
            if (checkMemberTaskFinished(id)) {
                Log.other("会员任务🎖️[$name]#获得积分$awardParamPoint")
            }
        } catch (e: JSONException) {
            Log.runtime(TAG, "processTask JSONException: " + (e.message ?: ""))
        }
    }

    /**
     * 查询指定会员任务是否完成
     * @param taskId 任务id
     */
    private suspend fun checkMemberTaskFinished(taskId: Long): Boolean {
        return try {
            val str = AntMemberRpcCall.queryAllStatusTaskList()
            GlobalThreadPools.sleepCompat(500)
            val jsonObject = JSONObject(str)
            if (!ResChecker.checkRes(TAG + "查询会员任务状态失败:", jsonObject)) {
                Log.error(TAG + ".checkMemberTaskFinished", "会员任务响应失败: " + jsonObject.getString("resultDesc"))
            }
            if (!jsonObject.has("availableTaskList")) {
                return true
            }
            val taskList = jsonObject.getJSONArray("availableTaskList")
            for (i in 0 until taskList.length()) {
                val taskConfigInfo = taskList.getJSONObject(i).getJSONObject("taskConfigInfo")
                val id = taskConfigInfo.getLong("id")
                if (taskId == id) {
                    return false
                }
            }
            true
        } catch (e: JSONException) {
            false
        }
    }

    fun kbMember() {
        try {
            if (!Status.canKbSignInToday()) {
                return
            }
            val s = AntMemberRpcCall.rpcCall_signIn()
            val jo = JSONObject(s)
            if (jo.optBoolean("success", false)) {
                val data = jo.getJSONObject("data")
                Log.other("口碑签到📅[第${data.getString("dayNo")}天]#获得${data.getString("value")}积分")
                Status.KbSignInToday()
            } else if (s.contains("\"HAS_SIGN_IN\"")) {
                Status.KbSignInToday()
            } else {
                Log.runtime(TAG, jo.getString("errorMessage"))
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "signIn err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun goldTicket() {
        try {
            // 签到
            goldBillCollect("\"campId\":\"CP1417744\",\"directModeDisableCollect\":true,\"from\":\"antfarm\",")
            // 收取其他
            goldBillCollect("")
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }

    /** 收取黄金票 */
    private suspend fun goldBillCollect(signInfo: String) {
        try {
            val str = AntMemberRpcCall.goldBillCollect(signInfo)
            val jsonObject = JSONObject(str)
            if (!jsonObject.optBoolean("success")) {
                Log.runtime("$TAG.goldBillCollect.goldBillCollect", jsonObject.optString("resultDesc"))
                return
            }
            val object_ = jsonObject.getJSONObject("result")
            val jsonArray = object_.getJSONArray("collectedList")
            val length = jsonArray.length()
            if (length == 0) {
                return
            }
            for (i in 0 until length) {
                Log.other("黄金票🙈[${jsonArray.getString(i)}]")
            }
            Log.other("黄金票🏦本次总共获得[${JsonUtil.getValueByPath(object_, "collectedCamp.amount")}]")
        } catch (th: Throwable) {
            Log.runtime(TAG, "signIn err:")
            Log.printStackTrace(TAG, th)
        }
    }

    private suspend fun enableGameCenter() {
        try {
            try {
                var str = AntMemberRpcCall.querySignInBall()
                var jsonObject = JSONObject(str)
                if (!jsonObject.optBoolean("success")) {
                    Log.runtime("$TAG.signIn.querySignInBall", jsonObject.optString("resultDesc"))
                    return
                }
                str = JsonUtil.getValueByPath(jsonObject, "data.signInBallModule.signInStatus")
                if (true.toString() == str) {
                    return
                }
                str = AntMemberRpcCall.continueSignIn()
                GlobalThreadPools.sleepCompat(300)
                jsonObject = JSONObject(str)
                if (!jsonObject.optBoolean("success")) {
                    Log.runtime("$TAG.signIn.continueSignIn", jsonObject.optString("resultDesc"))
                    return
                }
                Log.other("游戏中心🎮签到成功")
            } catch (th: Throwable) {
                Log.runtime(TAG, "signIn err:")
                Log.printStackTrace(TAG, th)
            }
            try {
                var str = AntMemberRpcCall.queryPointBallList()
                var jsonObject = JSONObject(str)
                if (!jsonObject.optBoolean("success")) {
                    Log.runtime("$TAG.batchReceive.queryPointBallList", jsonObject.optString("resultDesc"))
                    return
                }
                val jsonArray = JsonUtil.getValueByPathObject(jsonObject, "data.pointBallList") as? JSONArray
                if (jsonArray == null || jsonArray.length() == 0) {
                    return
                }
                str = AntMemberRpcCall.batchReceivePointBall()
                GlobalThreadPools.sleepCompat(300)
                jsonObject = JSONObject(str)
                if (jsonObject.optBoolean("success")) {
                    Log.other("游戏中心🎮全部领取成功[${JsonUtil.getValueByPath(jsonObject, "data.totalAmount")}]乐豆")
                } else {
                    Log.runtime("$TAG.batchReceive.batchReceivePointBall", jsonObject.optString("resultDesc"))
                }
            } catch (th: Throwable) {
                Log.runtime(TAG, "batchReceive err:")
                Log.printStackTrace(TAG, th)
            }
        } catch (t: Throwable) {
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun beanSignIn() {
        try {
            try {
                val signInProcessStr = AntMemberRpcCall.querySignInProcess("AP16242232", "INS_BLUE_BEAN_SIGN")

                var jo = JSONObject(signInProcessStr)
                if (!jo.optBoolean("success")) {
                    Log.runtime(jo.toString())
                    return
                }

                if (jo.getJSONObject("result").getBoolean("canPush")) {
                    val signInTriggerStr = AntMemberRpcCall.signInTrigger("AP16242232", "INS_BLUE_BEAN_SIGN")

                    jo = JSONObject(signInTriggerStr)
                    if (jo.optBoolean("success")) {
                        val prizeName = jo.getJSONObject("result").getJSONArray("prizeSendOrderDTOList")
                            .getJSONObject(0).getString("prizeName")
                        Log.record(TAG, "安心豆🫘[$prizeName]")
                    } else {
                        Log.runtime(jo.toString())
                    }
                }
            } catch (e: NullPointerException) {
                Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化")
                Log.printStackTrace(TAG, e)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "beanSignIn err:")
            Log.printStackTrace(TAG, t)
        }
    }

    private suspend fun beanExchangeBubbleBoost() {
        try {
            // 检查RPC调用是否可用
            try {
                val accountInfo = AntMemberRpcCall.queryUserAccountInfo("INS_BLUE_BEAN")

                var jo = JSONObject(accountInfo)
                if (!jo.optBoolean("success")) {
                    Log.runtime(jo.toString())
                    return
                }

                val userCurrentPoint = jo.getJSONObject("result").getInt("userCurrentPoint")

                // 检查beanExchangeDetail调用
                var exchangeDetailStr = AntMemberRpcCall.beanExchangeDetail("IT20230214000700069722")

                jo = JSONObject(exchangeDetailStr)
                if (!jo.optBoolean("success")) {
                    Log.runtime(jo.toString())
                    return
                }

                jo = jo.getJSONObject("result").getJSONObject("rspContext").getJSONObject("params")
                    .getJSONObject("exchangeDetail")
                val itemId = jo.getString("itemId")
                val itemName = jo.getString("itemName")
                jo = jo.getJSONObject("itemExchangeConsultDTO")
                val realConsumePointAmount = jo.getInt("realConsumePointAmount")

                if (!jo.getBoolean("canExchange") || realConsumePointAmount > userCurrentPoint) {
                    return
                }

                val exchangeResult = AntMemberRpcCall.beanExchange(itemId, realConsumePointAmount)

                jo = JSONObject(exchangeResult)
                if (jo.optBoolean("success")) {
                    Log.record(TAG, "安心豆🫘[兑换:$itemName]")
                } else {
                    Log.runtime(jo.toString())
                }
            } catch (e: NullPointerException) {
                Log.error(TAG, "安心豆🫘[RPC桥接失败]#可能是RpcBridge未初始化")
                Log.printStackTrace(TAG, e)
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "beanExchangeBubbleBoost err:")
            Log.printStackTrace(TAG, t)
        }
    }
}

