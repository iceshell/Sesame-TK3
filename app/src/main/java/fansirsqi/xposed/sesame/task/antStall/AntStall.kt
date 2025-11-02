package fansirsqi.xposed.sesame.task.antStall

import android.util.Base64
import fansirsqi.xposed.sesame.data.Status.Companion.antStallAssistFriendToday
import fansirsqi.xposed.sesame.data.Status.Companion.canAntStallAssistFriendToday
import fansirsqi.xposed.sesame.data.Status.Companion.canPasteTicketTime
import fansirsqi.xposed.sesame.data.Status.Companion.canStallDonateToday
import fansirsqi.xposed.sesame.data.Status.Companion.hasFlagToday
import fansirsqi.xposed.sesame.data.Status.Companion.pasteTicketTime
import fansirsqi.xposed.sesame.data.Status.Companion.setFlagToday
import fansirsqi.xposed.sesame.data.Status.Companion.setStallDonateToday
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.BaseModel.Companion.energyTime
import fansirsqi.xposed.sesame.model.BaseModel.Companion.modelSleepTime
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.BooleanModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.IntegerModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.SelectModelField
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.task.antStall.ReadingDada.Companion.answerQuestion
import fansirsqi.xposed.sesame.util.GlobalThreadPools.sleepCompat
import fansirsqi.xposed.sesame.util.JsonUtil.getValueByPathObject
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.Log.farm
import fansirsqi.xposed.sesame.util.Log.printStackTrace
import fansirsqi.xposed.sesame.util.Log.record
import fansirsqi.xposed.sesame.util.Log.runtime
import fansirsqi.xposed.sesame.util.RandomUtil.getRandomInt
import fansirsqi.xposed.sesame.util.ResChecker.checkRes
import fansirsqi.xposed.sesame.util.TimeCounter
import fansirsqi.xposed.sesame.util.TimeUtil.getCommonDate
import fansirsqi.xposed.sesame.util.maps.UserMap.currentUid
import fansirsqi.xposed.sesame.util.maps.UserMap.getMaskName
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject
import java.util.Collections
import java.util.LinkedList
import java.util.Objects
import java.util.Queue

/**
 * @author Constanline
 * @since 2023/08/22
 */
class AntStall : ModelTask() {
    private class Seat(var userId: String, var hot: Int)

    public override fun getName(): String? {
        return "新村"
    }

    public override fun getGroup(): ModelGroup? {
        return ModelGroup.STALL
    }

    public override fun getIcon(): String? {
        return "AntStall.png"
    }

    private var stallAutoOpen: BooleanModelField? = null
    private var stallOpenType: ChoiceModelField? = null
    private var stallOpenList: SelectModelField? = null
    private var stallAutoClose: BooleanModelField? = null
    private var stallAutoTicket: BooleanModelField? = null
    private var stallTicketType: ChoiceModelField? = null
    private var stallTicketList: SelectModelField? = null
    private var stallAutoTask: BooleanModelField? = null
    private var stallReceiveAward: BooleanModelField? = null
    private var stallWhiteList: SelectModelField? = null
    private var stallBlackList: SelectModelField? = null
    private var stallAllowOpenReject: BooleanModelField? = null
    private var stallAllowOpenTime: IntegerModelField? = null
    private var stallSelfOpenTime: IntegerModelField? = null
    private var stallDonate: BooleanModelField? = null
    private var stallInviteRegister: BooleanModelField? = null
    private var stallThrowManure: BooleanModelField? = null
    private var stallThrowManureType: ChoiceModelField? = null
    private var stallThrowManureList: SelectModelField? = null
    private var stallInviteShop: BooleanModelField? = null
    private var stallInviteShopType: ChoiceModelField? = null
    private var stallInviteShopList: SelectModelField? = null
    private var roadmap: BooleanModelField? = null

    /**
     * 邀请好友开通新村列表
     */
    private var stallInviteRegisterList: SelectModelField? = null

    /**
     * 助力好友列表
     */
    private var assistFriendList: SelectModelField? = null
    public override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(
            BooleanModelField(
                "stallAutoOpen",
                "摆摊 | 开启",
                false
            ).also { stallAutoOpen = it })
        modelFields.addField(
            ChoiceModelField(
                "stallOpenType",
                "摆摊 | 动作",
                StallOpenType.Companion.OPEN,
                StallOpenType.Companion.nickNames
            ).also { stallOpenType = it })
        modelFields.addField(
            SelectModelField(
                "stallOpenList",
                "摆摊 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallOpenList = it
            })
        modelFields.addField(
            BooleanModelField(
                "stallAutoClose",
                "收摊 | 开启",
                false
            ).also { stallAutoClose = it })
        modelFields.addField(
            IntegerModelField(
                "stallSelfOpenTime",
                "收摊 | 摆摊时长(分钟)",
                120
            ).also { stallSelfOpenTime = it })
        modelFields.addField(
            BooleanModelField(
                "stallAutoTicket",
                "贴罚单 | 开启",
                false
            ).also { stallAutoTicket = it })
        modelFields.addField(
            ChoiceModelField(
                "stallTicketType",
                "贴罚单 | 动作",
                StallTicketType.Companion.DONT_TICKET,
                StallTicketType.Companion.nickNames
            ).also { stallTicketType = it })
        modelFields.addField(
            SelectModelField(
                "stallTicketList",
                "贴罚单 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallTicketList = it
            })
        modelFields.addField(
            BooleanModelField(
                "stallThrowManure",
                "丢肥料 | 开启",
                false
            ).also { stallThrowManure = it })
        modelFields.addField(
            ChoiceModelField(
                "stallThrowManureType",
                "丢肥料 | 动作",
                StallThrowManureType.Companion.DONT_THROW,
                StallThrowManureType.Companion.nickNames
            ).also { stallThrowManureType = it })
        modelFields.addField(
            SelectModelField(
                "stallThrowManureList",
                "丢肥料 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallThrowManureList = it
            })
        modelFields.addField(
            BooleanModelField(
                "stallInviteShop",
                "邀请摆摊 | 开启",
                false
            ).also { stallInviteShop = it })
        modelFields.addField(
            ChoiceModelField(
                "stallInviteShopType",
                "邀请摆摊 | 动作",
                StallInviteShopType.Companion.INVITE,
                StallInviteShopType.Companion.nickNames
            ).also { stallInviteShopType = it })
        modelFields.addField(
            SelectModelField(
                "stallInviteShopList",
                "邀请摆摊 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallInviteShopList = it
            })
        modelFields.addField(
            BooleanModelField(
                "stallAllowOpenReject",
                "请走小摊 | 开启",
                false
            ).also { stallAllowOpenReject = it })
        modelFields.addField(
            IntegerModelField(
                "stallAllowOpenTime",
                "请走小摊 | 允许摆摊时长(分钟)",
                121
            ).also { stallAllowOpenTime = it })
        modelFields.addField(
            SelectModelField(
                "stallWhiteList",
                "请走小摊 | 白名单(超时也不赶)",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallWhiteList = it
            })
        modelFields.addField(
            SelectModelField(
                "stallBlackList",
                "请走小摊 | 黑名单(不超时也赶)",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallBlackList = it
            })
        modelFields.addField(
            BooleanModelField(
                "stallAutoTask",
                "自动任务",
                false
            ).also { stallAutoTask = it })
        modelFields.addField(
            BooleanModelField(
                "stallReceiveAward",
                "自动领奖",
                false
            ).also { stallReceiveAward = it })
        modelFields.addField(
            BooleanModelField(
                "stallDonate",
                "自动捐赠",
                false
            ).also { stallDonate = it })
        modelFields.addField(BooleanModelField("roadmap", "自动进入下一村", false).also {
            roadmap = it
        })
        modelFields.addField(
            BooleanModelField(
                "stallInviteRegister",
                "邀请 | 邀请好友开通新村",
                false
            ).also { stallInviteRegister = it })
        modelFields.addField(
            SelectModelField(
                "stallInviteRegisterList",
                "邀请 | 好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                stallInviteRegisterList = it
            })
        modelFields.addField(
            SelectModelField(
                "assistFriendList",
                "助力好友列表",
                LinkedHashSet<String?>(),
                SelectModelField.SelectListFunc { AlipayUser.Companion.getListAsMapperEntity() }).also {
                assistFriendList = it
            })
        return modelFields
    }

    public override fun check(): Boolean? {
        if (TaskCommon.IS_ENERGY_TIME) {
            record(
                TAG,
                "⏸ 当前为只收能量时间【" + energyTime.value + "】，停止执行" + getName() + "任务！"
            )
            return false
        } else if (TaskCommon.IS_MODULE_SLEEP_TIME) {
            record(
                TAG,
                "💤 模块休眠时间【" + modelSleepTime.value + "】停止执行" + getName() + "任务！"
            )
            return false
        } else {
            return true
        }
    }

    override suspend fun runSuspend() {
        try {
            val tc = TimeCounter(TAG)
            record(TAG, "执行开始-" + getName())
            val s = AntStallRpcCall.home()
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                if (!jo.getBoolean("hasRegister") || jo.getBoolean("hasQuit")) {
                    farm("蚂蚁新村⛪请先开启蚂蚁新村")
                    return
                }
                val astReceivableCoinVO = jo.getJSONObject("astReceivableCoinVO")
                if (astReceivableCoinVO.optBoolean("hasCoin")) {
                    settleReceivable()
                    tc.countDebug("收金币")
                }
                if (stallThrowManure!!.value == true) {
                    throwManure()
                    tc.countDebug("丢肥料")
                }
                val seatsMap = jo.getJSONObject("seatsMap")
                settle(seatsMap)
                tc.countDebug("收取金币")
                collectManure()
                tc.countDebug("收肥料")
                sendBack(seatsMap)
                tc.countDebug("请走")
                if (stallAutoClose!!.value == true) {
                    closeShop()
                    tc.countDebug("收摊")
                }
                if (stallAutoOpen!!.value == true) {
                    openShop()
                    tc.countDebug("摆摊")
                }
                if (stallAutoTask!!.value == true) {
                    taskList()
                    tc.countDebug("自动任务第一次")
                    sleepCompat(500)
                    taskList()
                    tc.countDebug("自动任务第二次")
                }
                assistFriend()
                tc.countDebug("新村助力")
                if (stallDonate!!.value == true && canStallDonateToday()) {
                    donate()
                    tc.countDebug("自动捐赠")
                }
                if (roadmap!!.value == true) {
                    roadmap()
                    tc.countDebug("自动进入下一村")
                }
                if (stallAutoTicket!!.value == true) {
                    pasteTicket()
                    tc.countDebug("贴罚单")
                }
            } else {
                record(TAG, "home err:" + " " + s)
            }
        } catch (e: CancellationException) {
            runtime(TAG, "AntStall 协程被取消")
            throw e
        } catch (t: Throwable) {
            runtime(TAG, "home err:")
            printStackTrace(TAG, t)
        } finally {
            record(TAG, "执行结束-" + getName())
        }
    }

    private fun sendBack(
        billNo: String,
        seatId: String,
        shopId: String,
        shopUserId: String,
        sentUserId: MutableSet<String?>
    ) {
        var s = AntStallRpcCall.shopSendBackPre(billNo, seatId, shopId, shopUserId)
        try {
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val astPreviewShopSettleVO = jo.getJSONObject("astPreviewShopSettleVO")
                val income = astPreviewShopSettleVO.getJSONObject("income")
                val amount = income.getDouble("amount").toInt()
                s = AntStallRpcCall.shopSendBack(seatId)
                jo = JSONObject(s)
                if (checkRes(TAG, jo)) {
                    farm(
                        ("蚂蚁新村⛪请走[" + getMaskName(shopUserId) + "]的小摊"
                                + (if (amount > 0) "获得金币" + amount else ""))
                    )
                } else {
                    record(TAG, "sendBack err:" + " " + s)
                }
                if (stallInviteShop!!.value == true) {
                    inviteOpen(seatId, sentUserId)
                }
            } else {
                record(TAG, "sendBackPre err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "sendBack err:")
            printStackTrace(TAG, t)
        }
    }

    private fun inviteOpen(seatId: String, sentUserId: MutableSet<String?>) {
        var s = AntStallRpcCall.rankInviteOpen()
        try {
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val friendRankList = jo.getJSONArray("friendRankList")
                for (i in 0..<friendRankList.length()) {
                    val friend = friendRankList.getJSONObject(i)
                    val friendUserId = friend.getString("userId")
                    var isInviteShop = stallInviteShopList!!.value!!.contains(friendUserId)
                    if (stallInviteShopType!!.value == StallInviteShopType.Companion.DONT_INVITE) {
                        isInviteShop = !isInviteShop
                    }
                    if (!isInviteShop) {
                        continue
                    }
                    if (sentUserId.contains(friendUserId)) {
                        continue
                    }
                    if (friend.getBoolean("canInviteOpenShop")) {
                        s = AntStallRpcCall.oneKeyInviteOpenShop(friendUserId, seatId)
                        if (s.isNullOrEmpty()) {
                            record(TAG, "邀请[" + getMaskName(friendUserId) + "]开店返回空，跳过")
                            continue
                        }
                        jo = JSONObject(s)
                        if (checkRes(TAG, jo)) {
                            farm("蚂蚁新村⛪邀请[" + getMaskName(friendUserId) + "]开店成功")
                            sentUserId.add(friendUserId)
                            return
                        } else {
                            record(
                                TAG,
                                "邀请[" + getMaskName(friendUserId) + "]开店失败: " + jo.optString("errorMessage")
                            )
                        }
                    }
                }
            } else {
                record(TAG, "inviteOpen err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "inviteOpen err:")
            printStackTrace(TAG, t)
        }
    }

    private fun sendBack(seatsMap: JSONObject) {
        try {
            val sentUserId: MutableSet<String?> = LinkedHashSet<String?>()
            for (i in 1..2) {
                val seat = seatsMap.getJSONObject("GUEST_0" + i)
                if ("BUSY" == seat.getString("status")) {
                    val rentLastUser = seat.optString("rentLastUser")
                    if (rentLastUser != null && !rentLastUser.isEmpty()) {
                        sentUserId.add(rentLastUser)
                    }
                }
            }
            for (i in 1..2) {
                val seat = seatsMap.getJSONObject("GUEST_0" + i)
                val seatId = seat.getString("seatId")
                if ("FREE" == seat.getString("status")) {
                    if (stallInviteShop!!.value == true) {
                        record(TAG, "摊位[" + i + "]空闲，尝试邀请好友...")
                        inviteOpen(seatId, sentUserId)
                    }
                    continue
                }
                // 请走小摊 未开启直接跳过
                if (stallAllowOpenReject!!.value != true) {
                    continue
                }
                val rentLastUser = seat.optString("rentLastUser")
                if (rentLastUser == null || rentLastUser.isEmpty()) {
                    continue
                }
                // 白名单直接跳过
                if (stallWhiteList!!.value!!.contains(rentLastUser)) {
                    record(TAG, "好友[" + getMaskName(rentLastUser) + "]在白名单中，跳过请走。")
                    continue
                }
                val rentLastBill = seat.getString("rentLastBill")
                val rentLastShop = seat.getString("rentLastShop")
                // 黑名单直接赶走
                if (stallBlackList!!.value!!.contains(rentLastUser)) {
                    record(TAG, "好友[" + getMaskName(rentLastUser) + "]在黑名单中，立即请走。")
                    sendBack(rentLastBill, seatId, rentLastShop, rentLastUser, sentUserId)
                    continue
                }
                val bizStartTime = seat.getLong("bizStartTime")
                val endTime = bizStartTime + stallAllowOpenTime!!.value!! * 60 * 1000
                if (System.currentTimeMillis() > endTime) {
                    record(TAG, "好友[" + getMaskName(rentLastUser) + "]摆摊超时，立即请走。")
                    sendBack(rentLastBill, seatId, rentLastShop, rentLastUser, sentUserId)
                } else {
                    val taskId = "SB|" + seatId
                    if (!hasChildTask(taskId)) {
                        addChildTask(ChildModelTask(taskId, "SB", Runnable {
                            if (stallAllowOpenReject!!.value == true) {
                                sendBack(
                                    rentLastBill,
                                    seatId,
                                    rentLastShop,
                                    rentLastUser,
                                    sentUserId
                                )
                            }
                        }, endTime))
                        record(TAG, "添加蹲点请走⛪在[" + getCommonDate(endTime) + "]执行")
                    }
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "sendBack err:")
            printStackTrace(TAG, t)
        }
    }

    private fun settle(seatsMap: JSONObject) {
        try {
            val seat = seatsMap.getJSONObject("MASTER")
            if (seat.has("coinsMap")) {
                val coinsMap = seat.getJSONObject("coinsMap")
                val master = coinsMap.getJSONObject("MASTER")
                val assetId = master.getString("assetId")
                val settleCoin = (master.getJSONObject("money").getDouble("amount")).toInt()
                val fullShow = master.getBoolean("fullShow")
                if (fullShow || settleCoin > 100) {
                    val s = AntStallRpcCall.settle(assetId, settleCoin)
                    val jo = JSONObject(s)
                    if (checkRes(TAG, jo)) {
                        farm("蚂蚁新村⛪[收取金币]#" + settleCoin)
                    } else {
                        record(TAG, "settle err:" + " " + s)
                    }
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "settle err:")
            printStackTrace(TAG, t)
        }
    }

    private fun closeShop() {
        val s = AntStallRpcCall.shopList()
        try {
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val astUserShopList = jo.getJSONArray("astUserShopList")
                if (astUserShopList.length() == 0) {
                    record(TAG, "没有正在摆摊的小摊可收。")
                    return
                }
                record(TAG, "检查 " + astUserShopList.length() + " 个小摊的收摊时间...")
                for (i in 0..<astUserShopList.length()) {
                    val shop = astUserShopList.getJSONObject(i)
                    if ("OPEN" == shop.getString("status")) {
                        val rentLastEnv = shop.getJSONObject("rentLastEnv")
                        val gmtLastRent = rentLastEnv.getLong("gmtLastRent")
                        val shopTime = gmtLastRent + stallSelfOpenTime!!.value!! * 60 * 1000
                        val shopId = shop.getString("shopId")
                        val rentLastBill = shop.getString("rentLastBill")
                        val rentLastUser = shop.getString("rentLastUser")
                        if (System.currentTimeMillis() > shopTime) {
                            record(TAG, "小摊[" + shopId + "]摆摊时间已到，执行收摊。")
                            this.shopClose(shopId, rentLastBill, rentLastUser)
                        } else {
                            val taskId = "SH|" + shopId
                            if (!hasChildTask(taskId)) {
                                addChildTask(ChildModelTask(taskId, "SH", Runnable {
                                    if (stallAutoClose!!.value == true) {
                                        this.shopClose(shopId, rentLastBill, rentLastUser)
                                    }
                                    sleepCompat(300L)
                                    if (stallAutoOpen!!.value == true) {
                                        openShop()
                                    }
                                }, shopTime))
                                record(TAG, "添加蹲点收摊⛪在[" + getCommonDate(shopTime) + "]执行")
                            } /*else {
                                addChildTask(new ChildModelTask(taskId, "SH", () -> {
                                    if (stallAutoClose.getValue()) {
                                        shopClose(shopId, rentLastBill, rentLastUser);
                                    }
                                }, shopTime));
                            }*/
                        }
                    }
                }
            } else {
                record(TAG, "closeShop err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "closeShop err:")
            printStackTrace(TAG, t)
        }
    }

    private fun openShop() {
        val s = AntStallRpcCall.shopList()
        try {
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val astUserShopList = jo.getJSONArray("astUserShopList")
                val shopIds: Queue<String?> = LinkedList<String?>()
                for (i in 0..<astUserShopList.length()) {
                    val astUserShop = astUserShopList.getJSONObject(i)
                    if ("FREE" == astUserShop.getString("status")) {
                        shopIds.add(astUserShop.getString("shopId"))
                    }
                }
                if (shopIds.isEmpty()) {
                    record(TAG, "没有空闲的小摊可用于摆摊。")
                    return
                }
                record(TAG, "找到 " + shopIds.size + " 个空闲小摊，开始寻找好友村庄...")
                this.rankCoinDonate(shopIds)
            } else {
                record(TAG, "openShop err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "openShop err:")
            printStackTrace(TAG, t)
        }
    }

    private fun rankCoinDonate(shopIds: Queue<String?>) {
        val s = AntStallRpcCall.rankCoinDonate()
        try {
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val friendRankList = jo.getJSONArray("friendRankList")
                val seats: MutableList<Seat> = ArrayList<Seat>()
                for (i in 0..<friendRankList.length()) {
                    val friendRank = friendRankList.getJSONObject(i)
                    if (friendRank.getBoolean("canOpenShop")) {
                        val userId = friendRank.getString("userId")
                        var isStallOpen = stallOpenList!!.value!!.contains(userId)
                        if (stallOpenType!!.value == StallOpenType.Companion.CLOSE) {
                            isStallOpen = !isStallOpen
                        }
                        if (!isStallOpen) {
                            continue
                        }
                        val hot = friendRank.getInt("hot")
                        seats.add(Seat(userId, hot))
                    }
                }
                friendHomeOpen(seats, shopIds)
            } else {
                record(TAG, "rankCoinDonate err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "rankCoinDonate err:")
            printStackTrace(TAG, t)
        }
    }

    private fun openShop(seatId: String, userId: String, shopId: String) {
        val s = AntStallRpcCall.shopOpen(seatId, userId, shopId)
        try {
            val jo = JSONObject(s)
            if ("SUCCESS" == jo.optString("resultCode")) {
                farm("蚂蚁新村⛪在[" + getMaskName(userId) + "]家摆摊")
            }
        } catch (t: Throwable) {
            runtime(TAG, "openShop err:")
            printStackTrace(TAG, t)
        }
    }

    private fun friendHomeOpen(seats: MutableList<Seat>, shopIds: Queue<String?>) {
        Collections.sort<Seat?>(seats, Comparator { e1: Seat?, e2: Seat? -> e2!!.hot - e1!!.hot })
        val currentUid = currentUid
        for (seat in seats) {
            val shopId = shopIds.poll()
            if (shopId == null) {
                return
            }
            val userId = seat.userId
            try {
                val s = AntStallRpcCall.friendHome(userId)
                val jo = JSONObject(s)
                if ("SUCCESS" == jo.optString("resultCode")) {
                    val seatsMap = jo.getJSONObject("seatsMap")
                    // 修复B_OPEN_SHOP_LIMIT错误：在尝试摆摊前，先检查自己是否已经在这个好友的村庄里占用了摊位。
                    // 如果已经存在一个摊位，则跳过此好友，避免在同一好友家重复摆摊导致接口报错。
                    val guest1 = seatsMap.getJSONObject("GUEST_01")
                    val rentUser1 = guest1.optString("rentLastUser")
                    var guest2 = seatsMap.getJSONObject("GUEST_02")
                    val rentUser2 = guest2.optString("rentLastUser")
                    if (currentUid == rentUser1 || currentUid == rentUser2) {
                        record(TAG, "已在[" + getMaskName(userId) + "]家摆摊，跳过")
                        continue
                    }
                    if (guest1.getBoolean("canOpenShop")) {
                        openShop(guest1.getString("seatId"), userId, shopId)
                    } else {
                        guest2 = seatsMap.getJSONObject("GUEST_02")
                        if (guest2.getBoolean("canOpenShop")) {
                            openShop(guest2.getString("seatId"), userId, shopId)
                        }
                    }
                } else {
                    record(TAG, "新村摆摊失败: " + s)
                    return
                }
            } catch (t: Throwable) {
                printStackTrace(TAG, t)
            }
        }
    }

    private fun shopClose(shopId: String, billNo: String, userId: String?) {
        var s = AntStallRpcCall.preShopClose(shopId, billNo)
        try {
            var jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val income = jo.getJSONObject("astPreviewShopSettleVO").getJSONObject("income")
                s = AntStallRpcCall.shopClose(shopId)
                jo = JSONObject(s)
                if (checkRes(TAG, jo)) {
                    farm(
                        "蚂蚁新村⛪收取在[" + getMaskName(userId) + "]的摊位获得" + income.getString(
                            "amount"
                        )
                    )
                } else {
                    record(TAG, "shopClose err:" + " " + s)
                }
            } else {
                record(TAG, "shopClose  err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "shopClose  err:")
            printStackTrace(TAG, t)
        }
    }

    private fun taskList() {
        try {
            var s = AntStallRpcCall.taskList()
            var jo = JSONObject(s)
            if (!checkRes(TAG, jo)) {
                record(TAG, "taskList err:" + " " + s)
                return
            }
            val signListModel = jo.getJSONObject("signListModel")
            if (!signListModel.getBoolean("currentKeySigned")) {
                record(TAG, "开始执行每日签到...")
                signToday()
            }
            val taskModels = jo.getJSONArray("taskModels")
            record(TAG, "开始检查 " + taskModels.length() + " 个新村任务...")
            for (i in 0..<taskModels.length()) {
                try {
                    val task = taskModels.getJSONObject(i)
                    val taskStatus = task.getString("taskStatus")
                    val taskType = task.getString("taskType")
                    if ("FINISHED" == taskStatus) {
                        record(TAG, "任务[" + taskType + "]已完成，尝试领取奖励...")
                        receiveTaskAward(taskType)
                        continue
                    }
                    if ("TODO" != taskStatus) {
                        continue
                    }
                    val bizInfo = JSONObject(task.getString("bizInfo"))
                    val title = bizInfo.optString("title", taskType)
                    if ("VISIT_AUTO_FINISH" == bizInfo.getString("actionType")
                        || taskTypeList.contains(taskType)
                    ) {
                        if (!this.finishTask(taskType)) {
                            continue
                        }
                        farm("蚂蚁新村👣任务[" + title + "]完成")
                        sleepCompat(200L)
                        continue
                    }
                    when (taskType) {
                        "ANTSTALL_NORMAL_DAILY_QA" -> if (answerQuestion(bizInfo)) {
                            receiveTaskAward(taskType)
                        }

                        "ANTSTALL_NORMAL_INVITE_REGISTER" -> if (inviteRegister()) {
                            sleepCompat(200L)
                            continue
                        }

                        "ANTSTALL_P2P_DAILY_SHARER" -> {}
                        "ANTSTALL_TASK_taojinbihuanduan" -> {}
                        "ANTSTALL_XLIGHT_VARIABLE_AWARD" -> {
                            //【木兰市集】逛精选好物
                            s = AntStallRpcCall.xlightPlugin()
                            if (s.isNullOrEmpty()) {
                                runtime(TAG, "taskList.xlightPlugin 返回空响应，跳过")
                                continue
                            }
                            jo = JSONObject(s)
                            if (!jo.has("playingResult")) {
                                runtime(
                                    TAG,
                                    "taskList.xlightPlugin err:" + jo.optString("resultDesc")
                                )
                                continue
                            }
                            jo = jo.getJSONObject("playingResult")
                            val pid = jo.getString("playingBizId")
                            val jsonArray = getValueByPathObject(
                                jo,
                                "eventRewardDetail.eventRewardInfoList"
                            ) as JSONArray?
                            if (jsonArray == null || jsonArray.length() == 0) {
                                continue
                            }
                            //                            Log.record("延时5S 木兰市集");
//                            GlobalThreadPools.sleepCompat(5000);
                            var j = 0
                            while (j < jsonArray.length()) {
                                try {
                                    val jsonObject = jsonArray.getJSONObject(j)
                                    s = AntStallRpcCall.finish(pid, jsonObject)
                                    record("延时5S 木兰市集")
                                    sleepCompat(5000)
                                    jo = JSONObject(s)
                                    if (!jo.optBoolean("success")) {
                                        runtime(
                                            TAG,
                                            "taskList.finish err:" + jo.optString("resultDesc")
                                        )
                                    }
                                } catch (t: Throwable) {
                                    runtime(TAG, "taskList for err:")
                                    printStackTrace(TAG, t)
                                }
                                j++
                            }
                        }
                    }
                    sleepCompat(200L)
                } catch (t: Throwable) {
                    runtime(TAG, "taskList for err:")
                    printStackTrace(TAG, t)
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "taskList err:")
            printStackTrace(TAG, t)
        }
    }

    private fun signToday() {
        val s = AntStallRpcCall.signToday()
        try {
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                farm("蚂蚁新村⛪[签到成功]")
            } else {
                record(TAG, "signToday err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "signToday err:")
            printStackTrace(TAG, t)
        }
    }

    private fun receiveTaskAward(taskType: String) {
        if (!stallReceiveAward!!.value!!) {
            return
        }
        val s = AntStallRpcCall.receiveTaskAward(taskType)
        try {
            val jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                farm("蚂蚁新村⛪[领取奖励]")
            } else {
                record(TAG, "receiveTaskAward err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "receiveTaskAward err:")
            printStackTrace(TAG, t)
        }
    }

    private fun finishTask(taskType: String): Boolean {
        val s = AntStallRpcCall.finishTask(taskType + "_" + System.currentTimeMillis(), taskType)
        try {
            val jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                return true
            } else {
                record(TAG, "finishTask err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "finishTask err:")
            printStackTrace(TAG, t)
        }
        return false
    }

    private fun inviteRegister(): Boolean {
        if (!stallInviteRegister!!.value!!) {
            return false
        }
        try {
            val s = AntStallRpcCall.rankInviteRegister()
            var jo = JSONObject(s)
            if (!checkRes(TAG, jo)) {
                record(TAG, "rankInviteRegister err:" + " " + s)
                return false
            }
            val friendRankList = jo.optJSONArray("friendRankList")
            if (friendRankList == null || friendRankList.length() <= 0) {
                return false
            }
            for (i in 0..<friendRankList.length()) {
                val friend = friendRankList.getJSONObject(i)
                if (!friend.optBoolean("canInviteRegister", false)
                    || "UNREGISTER" != friend.getString("userStatus")
                ) {
                    continue
                }
                /* 名单筛选 */
                val userId = friend.getString("userId")
                if (!stallInviteRegisterList!!.value!!.contains(userId)) {
                    continue
                }
                jo = JSONObject(AntStallRpcCall.friendInviteRegister(userId))
                if (checkRes(TAG, jo)) {
                    farm("蚂蚁新村⛪邀请好友[" + getMaskName(userId) + "]#开通新村")
                    return true
                } else {
                    record(TAG, "friendInviteRegister err:" + " " + jo)
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "InviteRegister err:")
            printStackTrace(TAG, t)
        }
        return false
    }

    private fun shareP2P(): String? {
        try {
            val s = AntStallRpcCall.shareP2P()
            val jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val shareId = jo.getString("shareId")
                record(TAG, "蚂蚁新村⛪[分享助力]")
                return shareId
            } else {
                record(TAG, "shareP2P err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "shareP2P err:")
            printStackTrace(TAG, t)
        }
        return null
    }

    /**
     * 助力好友
     */
    private fun assistFriend() {
        try {
            if (!canAntStallAssistFriendToday()) {
                record(TAG, "今日新村助力次数已用完。")
                return
            }
            val friendSet = assistFriendList!!.value
            if (friendSet!!.isEmpty()) {
                record(TAG, "未设置新村助力好友列表。")
                return
            }
            record(TAG, "开始为 " + friendSet.size + " 位好友进行新村助力...")
            for (uid in friendSet) {
                val shareId = Base64.encodeToString(
                    (uid + "-" + getRandomInt(5) + "ANUTSALTML_2PA_SHARE").toByteArray(),
                    Base64.NO_WRAP
                )
                val str = AntStallRpcCall.achieveBeShareP2P(shareId)
                val jsonObject = JSONObject(str)
                val name = getMaskName(uid)
                if (!jsonObject.optBoolean("success")) {
                    val code = jsonObject.getString("code")
                    if ("600000028" == code) {
                        record(TAG, "新村助力🐮被助力次数上限[" + name + "]")
                        continue
                    }
                    if ("600000027" == code) {
                        record(TAG, "新村助力💪今日助力他人次数上限")
                        antStallAssistFriendToday()
                        return
                    }
                    //600000010 人传人邀请关系不存在
                    //600000015 人传人完成邀请，菲方用户
                    //600000031 人传人完成邀请过于频繁
                    //600000029 人传人分享一对一接受邀请达到限制
                    record(TAG, "新村助力😔失败[" + name + "]" + jsonObject.optString("desc"))
                    continue
                }
                farm("新村助力🎉成功[" + name + "]")
                sleepCompat(5000)
            }
            //暂时一天只做一次
            antStallAssistFriendToday()
        } catch (t: Throwable) {
            runtime(TAG, "assistFriend err:")
            printStackTrace(TAG, t)
        }
    }

    // 捐赠项目
    private fun donate() {
        try {
            // 调用远程接口获取项目列表信息
            var response = AntStallRpcCall.projectList()
            // 将返回的 JSON 字符串转换为 JSONObject 对象
            val jsonResponse = JSONObject(response)
            // 检查返回结果是否成功
            if ("SUCCESS" == jsonResponse.optString("resultCode", "")) {
                // 获取 astUserInfoVO 对象
                val userInfo = jsonResponse.optJSONObject("astUserInfoVO")
                if (userInfo != null) {
                    // 获取当前余额的金额
                    val currentCoinAmount = userInfo.optJSONObject("currentCoin")
                        ?.optDouble("amount", 0.0) ?: 0.0
                    // 检查当前余额是否大于15000
                    if (currentCoinAmount < 15000) {
                        // 当 currentCoinAmount 小于 15000 时，直接返回，不执行后续操作
                        return
                    }
                }
                // 获取项目列表中的 astProjectVOS 数组
                val projects = jsonResponse.optJSONArray("astProjectVOS")
                // 遍历项目列表
                if (projects != null) {
                    for (i in 0..<projects.length()) {
                        // 获取每个项目的 JSONObject
                        val project = projects.optJSONObject(i)
                        if (project != null && "ONLINE" == project.optString("status", "")) {
                            // 获取项目的 projectId
                            val projectId = project.optString("projectId", "")
                            // 调用远程接口获取项目详情
                            response = AntStallRpcCall.projectDetail(projectId)
                            // 将返回的 JSON 字符串转换为 JSONObject 对象
                            val projectDetail = JSONObject(response)
                            // 检查返回结果是否成功
                            if ("SUCCESS" == projectDetail.optString("resultCode", "")) {
                                // 调用远程接口进行捐赠操作
                                response = AntStallRpcCall.projectDonate(projectId)
                                // 将返回的 JSON 字符串转换为 JSONObject 对象
                                val donateResponse = JSONObject(response)
                                // 获取捐赠操作返回的 astProjectVO 对象
                                val astProjectVO = donateResponse.optJSONObject("astProjectVO")
                                if (astProjectVO != null) {
                                    // 获取 astProjectVO 对象中的 title 字段值
                                    val title = astProjectVO.optString("title", "未知项目")
                                    // 检查捐赠操作返回结果是否成功
                                    if ("SUCCESS" == donateResponse.optString("resultCode", "")) {
                                        farm("蚂蚁新村⛪[捐赠:" + title + "]")
                                        setStallDonateToday()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            runtime(TAG, "donate err:")
            printStackTrace(TAG, t)
        }
    }

    // 进入下一村
    private fun roadmap() {
        try {
            val s = AntStallRpcCall.roadmap()
            val jo = JSONObject(s)
            if (!checkRes(TAG, jo)) {
                return
            }
            val roadList = jo.getJSONArray("roadList")
            var hasNewVillage = false
            for (i in 0..<roadList.length()) {
                val road = roadList.getJSONObject(i)
                // 检查 status 字段是否为 "NEW"
                if ("NEW" != road.getString("status")) {
                    continue
                }
                hasNewVillage = true
                val villageName = road.getString("villageName")


                // 检查今日是否已进入过这个村庄
                val flagKey = "stall::roadmap::" + villageName
                if (hasFlagToday(flagKey)) {
                    record(TAG, "今日已进入[" + villageName + "]，跳过重复打印。")
                    continue
                }

                farm("蚂蚁新村⛪[进入:" + villageName + "]成功")


                // 标记今日已进入该村庄，避免重复打印
                setFlagToday(flagKey)
                break // 进入一个新村后退出循环
            }
            if (!hasNewVillage) {
                record(TAG, "所有村庄都已解锁，无需进入下一村。")
            }
        } catch (t: Throwable) {
            runtime(TAG, "roadmap err:")
            printStackTrace(TAG, t)
        }
    }

    private fun collectManure() {
        var s = AntStallRpcCall.queryManureInfo()
        try {
            var jo = JSONObject(s)
            if (jo.optBoolean("success")) {
                val astManureInfoVO = jo.getJSONObject("astManureInfoVO")
                if (astManureInfoVO.optBoolean("hasManure")) {
                    val manure = astManureInfoVO.getInt("manure")
                    s = AntStallRpcCall.collectManure()
                    jo = JSONObject(s)
                    if (checkRes(TAG, jo)) {
                        farm("蚂蚁新村⛪获得肥料" + manure + "g")
                    }
                } else {
                    record(TAG, "没有可收取的肥料。")
                }
            } else {
                record(TAG, "collectManure err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "collectManure err:")
            printStackTrace(TAG, t)
        }
    }

    private fun throwManure(dynamicList: JSONArray) {
        try {
            val s = AntStallRpcCall.throwManure(dynamicList)
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                farm("蚂蚁新村⛪扔肥料")
            }
        } catch (th: Throwable) {
            runtime(TAG, "throwManure err:")
            printStackTrace(TAG, th)
        } finally {
            try {
                sleepCompat(1000)
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
        }
    }

    private fun throwManure() {
        try {
            val s = AntStallRpcCall.dynamicLoss()
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                val astLossDynamicVOS = jo.getJSONArray("astLossDynamicVOS")
                var dynamicList = JSONArray()
                for (i in 0..<astLossDynamicVOS.length()) {
                    val lossDynamic = astLossDynamicVOS.getJSONObject(i)
                    if (lossDynamic.has("specialEmojiVO")) {
                        continue
                    }
                    val objectId = lossDynamic.getString("objectId")
                    var isThrowManure = stallThrowManureList!!.value!!.contains(objectId)
                    if (stallThrowManureType!!.value == StallThrowManureType.Companion.DONT_THROW) {
                        isThrowManure = !isThrowManure
                    }
                    if (!isThrowManure) {
                        continue
                    }
                    val dynamic = JSONObject()
                    dynamic.put("bizId", lossDynamic.getString("bizId"))
                    dynamic.put("bizType", lossDynamic.getString("bizType"))
                    dynamicList.put(dynamic)
                    if (dynamicList.length() == 5) {
                        throwManure(dynamicList)
                        dynamicList = JSONArray()
                    }
                }
                if (dynamicList.length() > 0) {
                    throwManure(dynamicList)
                }
            } else {
                record(TAG, "throwManure err:" + " " + s)
            }
        } catch (t: Throwable) {
            runtime(TAG, "throwManure err:")
            printStackTrace(TAG, t)
        }
    }

    private fun settleReceivable() {
        val s = AntStallRpcCall.settleReceivable()
        try {
            val jo = JSONObject(s)
            if (checkRes(TAG, jo)) {
                farm("蚂蚁新村⛪收取应收金币")
            }
        } catch (th: Throwable) {
            runtime(TAG, "settleReceivable err:")
            printStackTrace(TAG, th)
        }
    }

    /**
     * 贴罚单
     */
    private fun pasteTicket() {
        try {
            if (!canPasteTicketTime()) {
                record(TAG, "未到贴罚单时间或今日已贴完。")
                return
            }
            record(TAG, "开始巡逻，寻找可贴罚单的好友...")
            while (true) {
                try {
                    var str = AntStallRpcCall.nextTicketFriend()
                    var jsonObject = JSONObject(str)
                    if (!jsonObject.optBoolean("success")) {
                        runtime(
                            TAG,
                            "pasteTicket.nextTicketFriend err:" + jsonObject.optString("resultDesc")
                        )
                        return
                    }
                    if (jsonObject.getInt("canPasteTicketCount") == 0) {
                        record(TAG, "蚂蚁新村👍[今日罚单已贴完]")
                        pasteTicketTime()
                        return
                    }
                    val friendId = jsonObject.optString("friendUserId")
                    if (friendId.isEmpty()) {
                        record(TAG, "没有更多可贴罚单的好友了。")
                        return
                    }
                    var isStallTicket = stallTicketList!!.value!!.contains(friendId)
                    if (stallTicketType!!.value == StallTicketType.Companion.DONT_TICKET) {
                        isStallTicket = !isStallTicket
                    }
                    if (!isStallTicket) {
                        continue
                    }
                    str = AntStallRpcCall.friendHome(friendId)
                    jsonObject = JSONObject(str)
                    if (!jsonObject.optBoolean("success")) {
                        runtime(
                            TAG,
                            "pasteTicket.friendHome err:" + jsonObject.optString("resultDesc")
                        )
                        return
                    }
                    val `object` = jsonObject.getJSONObject("seatsMap")
                    // 使用 keys() 方法获取所有键
                    val keys = `object`.keys()
                    // 遍历所有键
                    while (keys.hasNext()) {
                        try {
                            val key = keys.next()
                            // 获取键对应的值
                            val propertyValue = `object`.get(key)
                            if (propertyValue !is JSONObject) {
                                continue
                            }
                            //如signInDTO、priorityChannelDTO
                            if (propertyValue.length() == 0) {
                                continue
                            }
                            if (propertyValue.getBoolean("canOpenShop") || ("BUSY" != propertyValue.getString(
                                    "status"
                                )) || !propertyValue.getBoolean("overTicketProtection")
                            ) {
                                continue
                            }
                            val rentLastUser = propertyValue.getString("rentLastUser")
                            str = AntStallRpcCall.ticket(
                                propertyValue.getString("rentLastBill"),
                                propertyValue.getString("seatId"),
                                propertyValue.getString("rentLastShop"),
                                rentLastUser,
                                propertyValue.getString("userId")
                            )
                            val ticketResponse = JSONObject(str)
                            if (!ticketResponse.optBoolean("success")) {
                                runtime(
                                    TAG,
                                    "pasteTicket.ticket err:" + ticketResponse.optString("resultDesc")
                                )
                                return
                            }
                            farm("蚂蚁新村🚫在[" + getMaskName(friendId) + "]贴罚单")
                        } finally {
                            try {
                                sleepCompat(1000)
                            } catch (e: Exception) {
                                Log.printStackTrace(e)
                            }
                        }
                    }
                } finally {
                    try {
                        sleepCompat(1500)
                    } catch (e: Exception) {
                        Log.printStackTrace(e)
                    }
                }
            }
        } catch (th: Throwable) {
            runtime(TAG, "pasteTicket err:")
            printStackTrace(TAG, th)
        }
    }

    interface StallOpenType {
        companion object {
            const val OPEN: Int = 0
            const val CLOSE: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中摆摊", "选中不摆摊")
        }
    }

    interface StallTicketType {
        companion object {
            const val TICKET: Int = 0
            const val DONT_TICKET: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中贴罚单", "选中不贴罚单")
        }
    }

    interface StallThrowManureType {
        companion object {
            const val THROW: Int = 0
            const val DONT_THROW: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中丢肥料", "选中不丢肥料")
        }
    }

    interface StallInviteShopType {
        companion object {
            const val INVITE: Int = 0
            const val DONT_INVITE: Int = 1
            val nickNames: Array<String?> = arrayOf<String?>("选中邀请", "选中不邀请")
        }
    }

    companion object {
        private const val TAG: String = "AntStall"
        private val taskTypeList: MutableList<String> = mutableListOf(
            // 开启收新村收益提醒
            "ANTSTALL_NORMAL_OPEN_NOTICE",
            // 添加首页
            "tianjiashouye",
            // 【木兰市集】逛精选好物
            // "ANTSTALL_XLIGHT_VARIABLE_AWARD",
            // 去饿了么果园逛一逛
            "ANTSTALL_ELEME_VISIT",
            // 去点淘赚元宝提现
            "ANTSTALL_TASK_diantao202311",
            "ANTSTALL_TASK_nongchangleyuan"
        )
    }
}
