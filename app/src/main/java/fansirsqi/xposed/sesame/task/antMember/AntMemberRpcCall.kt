package fansirsqi.xposed.sesame.task.antMember

import fansirsqi.xposed.sesame.hook.RequestManager
import fansirsqi.xposed.sesame.util.RandomUtil
import fansirsqi.xposed.sesame.util.TimeUtil
import org.json.JSONException
import org.json.JSONObject

object AntMemberRpcCall {
    
    private fun getUniqueId(): String {
        return System.currentTimeMillis().toString() + RandomUtil.nextLong()
    }

    /* ant member point */
    @JvmStatic
    fun queryPointCert(page: Int, pageSize: Int): String {
        val args1 = """[{"page":$page,"pageSize":$pageSize}]"""
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.queryPointCert", args1)
    }

    @JvmStatic
    fun receivePointByUser(certId: String): String {
        val args1 = """[{"certId":$certId}]"""
        return RequestManager.requestString("alipay.antmember.biz.rpc.member.h5.receivePointByUser", args1)
    }

    @JvmStatic
    @Throws(JSONException::class)
    fun receiveAllPointByUser(): String {
        val args = JSONObject().apply {
            put("bizSource", "myTab")
            val passMap = JSONObject().apply {
                put("innerSource", "")
                put("passInfo", JSONObject().apply {
                    put("tc", "EXPIRING_POINT")
                })
                put("source", "myTab")
                put("unid", "")
            }
            put("sourcePassMap", passMap)
        }
        val params = "[$args]"
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.pointcert.h5.receiveAllPointByUser", params)
    }

    @JvmStatic
    fun queryMemberSigninCalendar(): String {
        return RequestManager.requestString(
            "com.alipay.amic.biz.rpc.signin.h5.queryMemberSigninCalendar",
            """[{"autoSignIn":true,"invitorUserId":"","sceneCode":"QUERY"}]"""
        )
    }

    /* 商家开门打卡任务 */
    @JvmStatic
    fun signIn(activityNo: String): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signIn",
            """[{"activityNo":"$activityNo"}]"""
        )
    }

    @JvmStatic
    fun signUp(activityNo: String): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.signUp",
            """[{"activityNo":"$activityNo"}]"""
        )
    }

    /* 商家服务 */
    @JvmStatic
    fun transcodeCheck(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchbusiness.sign.transcode.check",
            "[{}]"
        )
    }

    @JvmStatic
    fun merchantSign(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.sqyj.homepage.signin.v1",
            "[{}]"
        )
    }

    @JvmStatic
    fun zcjSignInQuery(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.zcj.view.invoke",
            """[{"compId":"ZCJ_SIGN_IN_QUERY"}]"""
        )
    }

    @JvmStatic
    fun zcjSignInExecute(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.zcj.view.invoke",
            """[{"compId":"ZCJ_SIGN_IN_EXECUTE"}]"""
        )
    }

    @JvmStatic
    fun taskListQuery(): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.more.query",
            """[{"paramMap":{"platform":"Android"},"taskItemCode":""}]"""
        )
    }

    @JvmStatic
    fun queryActivity(): String {
        return RequestManager.requestString(
            "alipay.merchant.kmdk.query.activity",
            """[{"scene":"activityCenter"}]"""
        )
    }

    /* 商家服务任务 */
    @JvmStatic
    fun taskFinish(bizId: String): String {
        return RequestManager.requestString(
            "com.alipay.adtask.biz.mobilegw.service.task.finish",
            """[{"bizId":"$bizId"}]"""
        )
    }

    @JvmStatic
    fun taskReceive(taskCode: String): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.sqyj.task.receive",
            """[{"compId":"ZTS_TASK_RECEIVE","extInfo":{"taskCode":"$taskCode"}}]"""
        )
    }

    @JvmStatic
    fun actioncode(actionCode: String): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.task.query.by.actioncode",
            """[{"actionCode":"$actionCode"}]"""
        )
    }

    @JvmStatic
    fun produce(actionCode: String): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.biz.task.action.produce",
            """[{"actionCode":"$actionCode"}]"""
        )
    }

    @JvmStatic
    fun ballReceive(ballIds: String): String {
        return RequestManager.requestString(
            "alipay.mrchservbase.mrchpoint.ball.receive",
            """[{"ballIds":["$ballIds"],"channel":"MRCH_SELF","outBizNo":"${getUniqueId()}"}]"""
        )
    }

    /* 会员任务 */
    @JvmStatic
    fun signPageTaskList(): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.signPageTaskList",
            """[{"sourceBusiness":"antmember","spaceCode":"ant_member_xlight_task"}]"""
        )
    }

    @JvmStatic
    fun applyTask(darwinName: String, taskConfigId: Long): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.applyTask",
            """[{"darwinExpParams":{"darwinName":"$darwinName"},"sourcePassMap":{"innerSource":"","source":"myTab","unid":""},"taskConfigId":$taskConfigId}]"""
        )
    }

    @JvmStatic
    fun executeTask(bizParam: String, bizSubType: String, bizType: String, taskConfigId: Long): String {
        val bizOutNo = TimeUtil.getFormatDate().replace("-", "")
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.executeTask",
            """[{"bizOutNo":"$bizOutNo","bizParam":"$bizParam","bizSubType":"$bizSubType","bizType":"$bizType","sourcePassMap":{"innerSource":"","source":"myTab","unid":""},"syncProcess":true,"taskConfigId":"$taskConfigId"}]"""
        )
    }

    @JvmStatic
    fun queryAllStatusTaskList(): String {
        return RequestManager.requestString(
            "alipay.antmember.biz.rpc.membertask.h5.queryAllStatusTaskList",
            """[{"sourceBusiness":"signInAd","sourcePassMap":{"innerSource":"","source":"myTab","unid":""}}]"""
        )
    }

    @JvmStatic
    fun rpcCall_signIn(): String {
        val args1 = """[{"sceneCode":"KOUBEI_INTEGRAL","source":"ALIPAY_TAB","version":"2.0"}]"""
        return RequestManager.requestString("alipay.kbmemberprod.action.signIn", args1)
    }

    /**
     * 黄金票收取
     *
     * @param str signInfo
     * @return 结果
     */
    @JvmStatic
    fun goldBillCollect(str: String): String {
        return RequestManager.requestString(
            "com.alipay.wealthgoldtwa.goldbill.v2.index.collect",
            """[{$str"trigger":"Y"}]"""
        )
    }

    /**
     * 游戏中心签到查询
     */
    @JvmStatic
    fun querySignInBall(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.querySignInBall",
            """[{"source":"ch_appcenter__chsub_9patch"}]"""
        )
    }

    /**
     * 游戏中心签到
     */
    @JvmStatic
    fun continueSignIn(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.continueSignIn",
            """[{"sceneId":"GAME_CENTER","signType":"NORMAL_SIGN","source":"ch_appcenter__chsub_9patch"}]"""
        )
    }

    /**
     * 游戏中心查询待领取乐豆列表
     */
    @JvmStatic
    fun queryPointBallList(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.queryPointBallList",
            """[{"source":"ch_appcenter__chsub_9patch"}]"""
        )
    }

    /**
     * 游戏中心全部领取
     */
    @JvmStatic
    fun batchReceivePointBall(): String {
        return RequestManager.requestString(
            "com.alipay.gamecenteruprod.biz.rpc.v3.batchReceivePointBall",
            "[{}]"
        )
    }

    /**
     * 芝麻信用首页
     */
    @JvmStatic
    fun queryHome(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.api.HomeV7RpcManager.queryHome",
            """[{"invokeSource":"zmHome","miniZmGrayInside":"","version":"week"}]"""
        )
    }

    /**
     * 获取芝麻信用任务列表
     */
    @JvmStatic
    fun queryAvailableSesameTask(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.queryListV3",
            "[{}]"
        )
    }

    /**
     * 芝麻信用领取任务
     */
    @JvmStatic
    fun joinSesameTask(taskTemplateId: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.joinActivity",
            """[{"chInfo":"seasameList","joinFromOuter":false,"templateId":"$taskTemplateId"}]"""
        )
    }

    /**
     * 芝麻信用获取任务回调
     */
    @JvmStatic
    fun feedBackSesameTask(taskTemplateId: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.creditaccumulate.CreditAccumulateStrategyRpcManager.taskFeedback",
            """[{"actionType":"TO_COMPLETE","templateId":"$taskTemplateId"}]""",
            "zmmemberop", "taskFeedback", "CreditAccumulateStrategyRpcManager"
        )
    }

    /**
     * 芝麻信用完成任务
     */
    @JvmStatic
    fun finishSesameTask(recordId: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.pushActivity",
            """[{"recordId":"$recordId"}]"""
        )
    }

    /**
     * 查询可收取的芝麻粒
     */
    @JvmStatic
    fun queryCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.queryCreditFeedback",
            """[{"queryPotential":false,"size":20,"status":"UNCLAIMED"}]"""
        )
    }

    /**
     * 一键收取芝麻粒
     */
    @JvmStatic
    fun collectAllCreditFeedback(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
            """[{"collectAll":true,"status":"UNCLAIMED"}]"""
        )
    }

    /**
     * 收取芝麻粒
     *
     * @param creditFeedbackId creditFeedbackId
     */
    @JvmStatic
    fun collectCreditFeedback(creditFeedbackId: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmcustprod.biz.rpc.home.creditaccumulate.api.CreditAccumulateRpcManager.collectCreditFeedback",
            """[{"collectAll":false,"creditFeedbackId":"$creditFeedbackId","status":"UNCLAIMED"}]"""
        )
    }

    /**
     * 获取保障金信息
     */
    @JvmStatic
    fun queryInsuredHome(): String {
        return RequestManager.requestString(
            "com.alipay.insplatformbff.insgift.accountService.queryAccountForPlat",
            """[{"includePolicy":true,"specialChannel":"wealth_entry"}]"""
        )
    }

    /**
     * 获取所有可领取的保障金
     */
    @JvmStatic
    fun queryAvailableCollectInsuredGold(): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
            """[{"entrance":"wealth_entry","eventToWaitParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"helpChildParamDTO":{"giftProdCode":"GIFT_HEALTH_GOLD_CHILD","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"priorityChannelParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"signInParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]}}]""",
            "insgiftbff", "queryMultiSceneWaitToGainList", "insgiftMain"
        )
    }

    /**
     * 领取保障金
     */
    @JvmStatic
    fun collectInsuredGold(goldBallObj: JSONObject): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
            goldBallObj.toString(), "insgiftbff", "gainMyAndFamilySumInsured", "insgiftMain"
        )
    }

    /**
     * 查询生活记录
     *
     * @return 结果
     */
    @JvmStatic
    fun promiseQueryHome(): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.queryHome",
            null
        )
    }

    /**
     * 查询生活记录明细
     *
     * @param recordId recordId
     * @return 结果
     */
    @JvmStatic
    fun promiseQueryDetail(recordId: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.queryDetail",
            """[{"recordId":"$recordId"}]"""
        )
    }

    /**
     * 生活记录加入新纪录
     *
     * @param data data
     * @return 结果
     */
    @JvmStatic
    fun promiseJoin(data: String): String {
        return RequestManager.requestString(
            "com.antgroup.zmxy.zmmemberop.biz.rpc.promise.PromiseRpcManager.join",
            "[$data]"
        )
    }

    /**
     * 查询待领取的保障金
     *
     * @return 结果
     */
    @JvmStatic
    fun queryMultiSceneWaitToGainList(): String {
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.queryMultiSceneWaitToGainList",
            """[{"entrance":"jkj_zhima_dairy66","eventToWaitParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"helpChildParamDTO":{"giftProdCode":"GIFT_HEALTH_GOLD_CHILD","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"priorityChannelParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]},"signInParamDTO":{"giftProdCode":"GIFT_UNIVERSAL_COVERAGE","rightNoList":["UNIVERSAL_ACCIDENT","UNIVERSAL_HOSPITAL","UNIVERSAL_OUTPATIENT","UNIVERSAL_SERIOUSNESS","UNIVERSAL_WEALTH","UNIVERSAL_TRANS","UNIVERSAL_FRAUD_LIABILITY"]}}]"""
        )
    }

    /**
     * 领取保障金
     *
     * @param jsonObject jsonObject
     * @return 结果
     */
    @JvmStatic
    @Throws(JSONException::class)
    fun gainMyAndFamilySumInsured(jsonObject: JSONObject): String {
        jsonObject.apply {
            put("disabled", false)
            put("entrance", "jkj_zhima_dairy66")
        }
        return RequestManager.requestString(
            "com.alipay.insgiftbff.insgiftMain.gainMyAndFamilySumInsured",
            "[$jsonObject]"
        )
    }

    // 安心豆
    @JvmStatic
    fun querySignInProcess(appletId: String, scene: String): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.querySignInProcess",
            """[{"appletId":"$appletId","scene":"$scene"}]"""
        )
    }

    @JvmStatic
    fun signInTrigger(appletId: String, scene: String): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.bean.signInTrigger",
            """[{"appletId":"$appletId","scene":"$scene"}]"""
        )
    }

    @JvmStatic
    fun beanExchangeDetail(itemId: String): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            """[{"extParams":{"itemId":"$itemId"},"planCode":"bluebean_onestop","planOperateCode":"exchangeDetail"}]"""
        )
    }

    @JvmStatic
    fun beanExchange(itemId: String, pointAmount: Int): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.onestop.planTrigger",
            """[{"extParams":{"itemId":"$itemId","pointAmount":"$pointAmount"},"planCode":"bluebean_onestop","planOperateCode":"exchange"}]"""
        )
    }

    @JvmStatic
    fun queryUserAccountInfo(pointProdCode: String): String {
        return RequestManager.requestString(
            "com.alipay.insmarketingbff.point.queryUserAccountInfo",
            """[{"channel":"HiChat","pointProdCode":"$pointProdCode","pointUnitType":"COUNT"}]"""
        )
    }

    /**
     * 查询会员信息
     */
    @JvmStatic
    fun queryMemberInfo(): String {
        val data = """[{"needExpirePoint":true,"needGrade":true,"needPoint":true,"queryScene":"POINT_EXCHANGE_SCENE","source":"POINT_EXCHANGE_SCENE","sourcePassMap":{"innerSource":"","source":"","unid":""}}]"""
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.member.h5.queryMemberInfo", data)
    }

    /**
     * 查询0元兑公益道具列表
     *
     * @param userId       userId
     * @param pointBalance 当前可用会员积分
     */
    @JvmStatic
    fun queryShandieEntityList(userId: String, pointBalance: String): String {
        val uniqueId = "${System.currentTimeMillis()}${userId}94000SR202501061144200394000SR2025010611458003"
        val data = """[{"blackIds":[],"deliveryIdList":["94000SR2025010611442003","94000SR2025010611458003"],"filterCityCode":false,"filterPointNoEnough":false,"filterStockNoEnough":false,"pageNum":1,"pageSize":18,"point":$pointBalance,"previewCopyDbId":"","queryType":"DELIVERY_ID_LIST","source":"member_day","sourcePassMap":{"innerSource":"","source":"0yuandui","unid":""},"topIds":[],"uniqueId":"$uniqueId"}]"""
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.config.h5.queryShandieEntityList", data)
    }

    /**
     * 会员积分兑换道具
     *
     * @param benefitId benefitId
     * @param itemId    itemId
     * @return 结果
     */
    @JvmStatic
    fun exchangeBenefit(benefitId: String, itemId: String): String {
        val requestId = "requestId${System.currentTimeMillis()}"
        val alipayClientVersion = fansirsqi.xposed.sesame.hook.ApplicationHookConstants.alipayVersion.versionString
        val data = """[{"benefitId":"$benefitId","cityCode":"","exchangeType":"POINT_PAY","itemId":"$itemId","miniAppId":"","orderSource":"","requestId":"$requestId","requestSourceInfo":"","sourcePassMap":{"alipayClientVersion":"$alipayClientVersion","innerSource":"","mobileOsType":"Android","source":"","unid":""},"userOutAccount":""}]"""
        return RequestManager.requestString("com.alipay.alipaymember.biz.rpc.exchange.h5.exchangeBenefit", data)
    }
}
