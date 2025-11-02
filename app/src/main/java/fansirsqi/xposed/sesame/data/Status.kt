package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.databind.JsonMappingException
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.task.antForest.AntForest
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.StringUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class Status {
    // ===========================forest
    var waterFriendLogList: MutableMap<String, Int> = HashMap()
    var cooperateWaterList: MutableSet<String> = HashSet() //合作浇水
    var reserveLogList: MutableMap<String, Int> = HashMap()
    var ancientTreeCityCodeList: MutableSet<String> = HashSet() //古树
    var protectBubbleList: MutableSet<String> = HashSet()
    var exchangeDoubleCard: Int = 0 // 活力值兑换双倍卡
    var exchangeTimes: Int = 0
    var exchangeTimesLongTime: Int = 0
    var doubleTimes: Int = 0
    var exchangeEnergyShield: Boolean = false //活力值兑换能量保护罩
    var exchangeCollectHistoryAnimal7Days: Boolean = false
    var exchangeCollectToFriendTimes7Days: Boolean = false
    var youthPrivilege: Boolean = true
    var studentTask: Boolean = true
    var vitalityStoreList: MutableMap<String, Int> = HashMap()
    
    // ===========================farm
    var answerQuestion: Boolean = false
    var feedFriendLogList: MutableMap<String, Int> = HashMap()
    var visitFriendLogList: MutableMap<String, Int> = HashMap()
    var dailyAnswerList: MutableSet<String> = HashSet()
    var donationEggList: MutableSet<String> = HashSet()
    var useAccelerateToolCount: Int = 0
    var canOrnament: Boolean = true
    var animalSleep: Boolean = false
    
    // =============================stall
    var stallHelpedCountLogList: MutableMap<String, Int> = HashMap()
    var spreadManureList: MutableSet<String> = HashSet()
    var stallP2PHelpedList: MutableSet<String> = HashSet()
    var canStallDonate: Boolean = true
    
    // ==========================sport
    var syncStepList: MutableSet<String> = HashSet()
    var exchangeList: MutableSet<String> = HashSet()
    var donateCharityCoin: Boolean = false
    
    // =======================other
    var memberSignInList: MutableSet<String> = HashSet()
    val flagList: MutableSet<String> = HashSet()
    var kbSignIn: Long = 0
    var saveTime: Long = 0L
    var antStallAssistFriend: MutableSet<String> = HashSet()
    var canPasteTicketTime: MutableSet<String> = HashSet()
    var greenFinancePointFriend: MutableSet<String> = HashSet()
    var greenFinancePrizesMap: MutableMap<String, Int> = HashMap()
    var antOrchardAssistFriend: MutableSet<String> = HashSet()
    var memberPointExchangeBenefitLogList: MutableSet<String> = HashSet()

    companion object {
        private val TAG = Status::class.java.simpleName
        
        @JvmStatic
        val INSTANCE = Status()

        @JvmStatic
        fun getCurrentDayTimestamp(): Long {
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            return calendar.timeInMillis
        }

        @JvmStatic
        fun getVitalityCount(skuId: String): Int {
            return INSTANCE.vitalityStoreList[skuId] ?: 0
        }

        @JvmStatic
        fun canVitalityExchangeToday(skuId: String, count: Int): Boolean {
            return !hasFlagToday("forest::VitalityExchangeLimit::$skuId") && getVitalityCount(skuId) < count
        }

        @JvmStatic
        fun vitalityExchangeToday(skuId: String) {
            val count = getVitalityCount(skuId) + 1
            INSTANCE.vitalityStoreList[skuId] = count
            save()
        }

        @JvmStatic
        fun canAnimalSleep(): Boolean = !INSTANCE.animalSleep

        @JvmStatic
        fun animalSleep() {
            if (!INSTANCE.animalSleep) {
                INSTANCE.animalSleep = true
                save()
            }
        }

        @JvmStatic
        fun canWaterFriendToday(id: String, newCount: Int): Boolean {
            val key = UserMap.currentUid + "-" + id
            val count = INSTANCE.waterFriendLogList[key]
            return count == null || count < newCount
        }

        @JvmStatic
        fun waterFriendToday(id: String, count: Int) {
            val key = UserMap.currentUid + "-" + id
            INSTANCE.waterFriendLogList[key] = count
            save()
        }

        @JvmStatic
        fun getReserveTimes(id: String): Int {
            return INSTANCE.reserveLogList[id] ?: 0
        }

        @JvmStatic
        fun canReserveToday(id: String, count: Int): Boolean {
            return getReserveTimes(id) < count
        }

        @JvmStatic
        fun reserveToday(id: String, newCount: Int) {
            val count = INSTANCE.reserveLogList[id] ?: 0
            INSTANCE.reserveLogList[id] = count + newCount
            save()
        }

        @JvmStatic
        fun canCooperateWaterToday(uid: String, coopId: String): Boolean {
            return !INSTANCE.cooperateWaterList.contains("${uid}_${coopId}")
        }

        @JvmStatic
        fun cooperateWaterToday(uid: String, coopId: String) {
            val v = "${uid}_${coopId}"
            if (!INSTANCE.cooperateWaterList.contains(v)) {
                INSTANCE.cooperateWaterList.add(v)
                save()
            }
        }

        @JvmStatic
        fun canAncientTreeToday(cityCode: String): Boolean {
            return !INSTANCE.ancientTreeCityCodeList.contains(cityCode)
        }

        @JvmStatic
        fun ancientTreeToday(cityCode: String) {
            if (!INSTANCE.ancientTreeCityCodeList.contains(cityCode)) {
                INSTANCE.ancientTreeCityCodeList.add(cityCode)
                save()
            }
        }

        @JvmStatic
        fun canAnswerQuestionToday(): Boolean = !INSTANCE.answerQuestion

        @JvmStatic
        fun answerQuestionToday() {
            if (!INSTANCE.answerQuestion) {
                INSTANCE.answerQuestion = true
                save()
            }
        }

        @JvmStatic
        fun canFeedFriendToday(id: String, newCount: Int): Boolean {
            val count = INSTANCE.feedFriendLogList[id]
            return count == null || count < newCount
        }

        @JvmStatic
        fun feedFriendToday(id: String) {
            val count = INSTANCE.feedFriendLogList[id] ?: 0
            INSTANCE.feedFriendLogList[id] = count + 1
            save()
        }

        @JvmStatic
        fun canVisitFriendToday(id: String, newCount: Int): Boolean {
            val key = UserMap.currentUid + "-" + id
            val count = INSTANCE.visitFriendLogList[key]
            return count == null || count < newCount
        }

        @JvmStatic
        fun visitFriendToday(id: String, newCount: Int) {
            val key = UserMap.currentUid + "-" + id
            INSTANCE.visitFriendLogList[key] = newCount
            save()
        }

        @JvmStatic
        fun canMemberSignInToday(uid: String): Boolean {
            return !INSTANCE.memberSignInList.contains(uid)
        }

        @JvmStatic
        fun memberSignInToday(uid: String) {
            if (!INSTANCE.memberSignInList.contains(uid)) {
                INSTANCE.memberSignInList.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canUseAccelerateTool(): Boolean {
            return INSTANCE.useAccelerateToolCount < 8
        }

        @JvmStatic
        fun useAccelerateTool() {
            INSTANCE.useAccelerateToolCount += 1
            save()
        }

        @JvmStatic
        fun canDonationEgg(uid: String): Boolean {
            return !INSTANCE.donationEggList.contains(uid)
        }

        @JvmStatic
        fun donationEgg(uid: String) {
            if (!INSTANCE.donationEggList.contains(uid)) {
                INSTANCE.donationEggList.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canSpreadManureToday(uid: String): Boolean {
            return !INSTANCE.spreadManureList.contains(uid)
        }

        @JvmStatic
        fun spreadManureToday(uid: String) {
            if (!INSTANCE.spreadManureList.contains(uid)) {
                INSTANCE.spreadManureList.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canAntStallAssistFriendToday(): Boolean {
            return !INSTANCE.antStallAssistFriend.contains(UserMap.currentUid)
        }

        @JvmStatic
        fun antStallAssistFriendToday() {
            val uid = UserMap.currentUid ?: return
            if (!INSTANCE.antStallAssistFriend.contains(uid)) {
                INSTANCE.antStallAssistFriend.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canAntOrchardAssistFriendToday(): Boolean {
            return !INSTANCE.antOrchardAssistFriend.contains(UserMap.currentUid ?: return false)
        }

        @JvmStatic
        fun antOrchardAssistFriendToday() {
            val uid = UserMap.currentUid ?: return
            if (!INSTANCE.antOrchardAssistFriend.contains(uid)) {
                INSTANCE.antOrchardAssistFriend.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canProtectBubbleToday(uid: String): Boolean {
            return !INSTANCE.protectBubbleList.contains(uid)
        }

        @JvmStatic
        fun protectBubbleToday(uid: String) {
            if (!INSTANCE.protectBubbleList.contains(uid)) {
                INSTANCE.protectBubbleList.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canPasteTicketTime(): Boolean {
            return !INSTANCE.canPasteTicketTime.contains(UserMap.currentUid)
        }

        @JvmStatic
        fun pasteTicketTime() {
            val uid = UserMap.currentUid ?: return
            if (INSTANCE.canPasteTicketTime.contains(uid)) {
                return
            }
            INSTANCE.canPasteTicketTime.add(uid)
            save()
        }

        @JvmStatic
        fun canDoubleToday(): Boolean {
            val task = Model.getModel(AntForest::class.java)
            if (task == null) {
                return false
            }
            return INSTANCE.doubleTimes < (task.getDoubleCountLimit()?.value ?: 0)
        }

        @JvmStatic
        fun DoubleToday() {
            INSTANCE.doubleTimes = INSTANCE.doubleTimes + 1
            save()
        }

        @JvmStatic
        fun canKbSignInToday(): Boolean {
            return INSTANCE.kbSignIn < getCurrentDayTimestamp()
        }

        @JvmStatic
        fun KbSignInToday() {
            val todayZero = getCurrentDayTimestamp()
            if (INSTANCE.kbSignIn != todayZero) {
                INSTANCE.kbSignIn = todayZero
                save()
            }
        }

        @JvmStatic
        fun setDadaDailySet(dailyAnswerList: MutableSet<String>) {
            INSTANCE.dailyAnswerList = dailyAnswerList
            save()
        }

        @JvmStatic
        fun canDonateCharityCoin(): Boolean = !INSTANCE.donateCharityCoin

        @JvmStatic
        fun donateCharityCoin() {
            if (!INSTANCE.donateCharityCoin) {
                INSTANCE.donateCharityCoin = true
                save()
            }
        }

        @JvmStatic
        fun canExchangeToday(uid: String): Boolean {
            return !INSTANCE.exchangeList.contains(uid)
        }

        @JvmStatic
        fun exchangeToday(uid: String) {
            if (!INSTANCE.exchangeList.contains(uid)) {
                INSTANCE.exchangeList.add(uid)
                save()
            }
        }

        @JvmStatic
        fun canGreenFinancePointFriend(): Boolean {
            return INSTANCE.greenFinancePointFriend.contains(UserMap.currentUid)
        }

        @JvmStatic
        fun greenFinancePointFriend() {
            if (canGreenFinancePointFriend()) {
                return
            }
            val uid = UserMap.currentUid ?: return
            INSTANCE.greenFinancePointFriend.add(uid)
            save()
        }

        @JvmStatic
        fun canGreenFinancePrizesMap(): Boolean {
            val week = TimeUtil.getWeekNumber(Date())
            val currentUid = UserMap.currentUid
            if (INSTANCE.greenFinancePrizesMap.containsKey(currentUid)) {
                val storedWeek = INSTANCE.greenFinancePrizesMap[currentUid]
                return storedWeek == null || storedWeek != week
            }
            return true
        }

        @JvmStatic
        fun greenFinancePrizesMap() {
            if (!canGreenFinancePrizesMap()) {
                return
            }
            val uid = UserMap.currentUid ?: return
            INSTANCE.greenFinancePrizesMap[uid] = TimeUtil.getWeekNumber(Date())
            save()
        }

        @JvmStatic
        @Synchronized
        fun load(currentUid: String): Status {
            if (currentUid.isEmpty()) {
                Log.runtime(TAG, "用户为空，状态加载失败")
                throw RuntimeException("用户为空，状态加载失败")
            }
            try {
                val statusFile = Files.getStatusFile(currentUid) ?: throw RuntimeException("无法获取状态文件")
                if (statusFile.exists()) {
                    Log.runtime(TAG, "加载 status.json")
                    val json = Files.readFromFile(statusFile)
                    if (json.trim().isNotEmpty()) {
                        JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue<Status>(json)
                        val formatted = JsonUtil.formatJson(INSTANCE)
                        if (formatted != null && formatted != json) {
                            Log.runtime(TAG, "重新格式化 status.json")
                            Files.write2File(formatted, statusFile)
                        }
                    } else {
                        Log.runtime(TAG, "配置文件为空，初始化默认配置")
                        initializeDefaultConfig(statusFile)
                    }
                } else {
                    Log.runtime(TAG, "配置文件不存在，初始化默认配置")
                    initializeDefaultConfig(statusFile)
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, t)
                Log.runtime(TAG, "状态文件格式有误，已重置")
                resetAndSaveConfig()
            }
            if (INSTANCE.saveTime == 0L) {
                INSTANCE.saveTime = System.currentTimeMillis()
            }
            return INSTANCE
        }

        private fun initializeDefaultConfig(statusFile: java.io.File) {
            try {
                JsonUtil.copyMapper().updateValue(INSTANCE, Status())
                Log.runtime(TAG, "初始化 status.json")
                Files.write2File(JsonUtil.formatJson(INSTANCE), statusFile)
            } catch (e: JsonMappingException) {
                Log.printStackTrace(TAG, e)
                throw RuntimeException("初始化配置失败", e)
            }
        }

        private fun resetAndSaveConfig() {
            try {
                JsonUtil.copyMapper().updateValue(INSTANCE, Status())
                val uid = UserMap.currentUid ?: throw RuntimeException("用户ID为空")
                val statusFile = Files.getStatusFile(uid) ?: throw RuntimeException("无法获取状态文件")
                Files.write2File(JsonUtil.formatJson(INSTANCE), statusFile)
            } catch (e: JsonMappingException) {
                Log.printStackTrace(TAG, e)
                throw RuntimeException("重置配置失败", e)
            }
        }

        @JvmStatic
        @Synchronized
        fun unload() {
            try {
                JsonUtil.copyMapper().updateValue(INSTANCE, Status())
            } catch (e: JsonMappingException) {
                Log.printStackTrace(TAG, e)
            }
        }

        @JvmStatic
        @Synchronized
        fun save() {
            save(Calendar.getInstance())
        }

        @JvmStatic
        @Synchronized
        fun save(nowCalendar: Calendar) {
            val currentUid = UserMap.currentUid
            if (currentUid.isNullOrEmpty()) {
                Log.record(TAG, "用户为空，状态保存失败")
                throw RuntimeException("用户为空，状态保存失败")
            }
            if (updateDay(nowCalendar)) {
                Log.runtime(TAG, "重置 statistics.json")
            } else {
                Log.runtime(TAG, "保存 status.json")
            }
            val lastSaveTime = INSTANCE.saveTime
            try {
                INSTANCE.saveTime = System.currentTimeMillis()
                val statusFile = Files.getStatusFile(currentUid) ?: throw RuntimeException("无法获取状态文件")
                Files.write2File(JsonUtil.formatJson(INSTANCE), statusFile)
            } catch (e: Exception) {
                INSTANCE.saveTime = lastSaveTime
                throw e
            }
        }

        @JvmStatic
        fun updateDay(nowCalendar: Calendar): Boolean {
            return if (TimeUtil.isLessThanSecondOfDays(INSTANCE.saveTime, nowCalendar.timeInMillis)) {
                unload()
                true
            } else {
                false
            }
        }

        @JvmStatic
        fun canOrnamentToday(): Boolean = INSTANCE.canOrnament

        @JvmStatic
        fun setOrnamentToday() {
            if (INSTANCE.canOrnament) {
                INSTANCE.canOrnament = false
                save()
            }
        }

        @JvmStatic
        fun canStallDonateToday(): Boolean = INSTANCE.canStallDonate

        @JvmStatic
        fun setStallDonateToday() {
            if (INSTANCE.canStallDonate) {
                INSTANCE.canStallDonate = false
                save()
            }
        }

        @JvmStatic
        fun hasFlagToday(flag: String): Boolean {
            return INSTANCE.flagList.contains(flag)
        }

        @JvmStatic
        fun setFlagToday(flag: String) {
            if (!hasFlagToday(flag)) {
                INSTANCE.flagList.add(flag)
                save()
            }
        }

        @JvmStatic
        fun canMemberPointExchangeBenefitToday(benefitId: String): Boolean {
            return !INSTANCE.memberPointExchangeBenefitLogList.contains(benefitId)
        }

        @JvmStatic
        fun memberPointExchangeBenefitToday(benefitId: String) {
            if (canMemberPointExchangeBenefitToday(benefitId)) {
                INSTANCE.memberPointExchangeBenefitLogList.add(benefitId)
                save()
            }
        }

        @JvmStatic
        fun canParadiseCoinExchangeBenefitToday(spuId: String): Boolean {
            return !hasFlagToday("farm::paradiseCoinExchangeLimit::$spuId")
        }
    }
}
