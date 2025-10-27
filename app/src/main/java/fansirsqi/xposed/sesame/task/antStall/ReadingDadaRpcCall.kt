package fansirsqi.xposed.sesame.task.antStall

import fansirsqi.xposed.sesame.hook.RequestManager

/**
 * 阅读达达RPC调用
 *
 * @author Constanline
 * @since 2023/08/22
 */
object ReadingDadaRpcCall {

    private const val VERSION = "1"

    /**
     * 提交答案
     *
     * @param activityId 活动ID
     * @param outBizId 外部业务ID（可选）
     * @param questionId 问题ID
     * @param answer 答案
     * @return RPC响应字符串
     */
    @JvmStatic
    fun submitAnswer(activityId: String, outBizId: String?, questionId: String, answer: String): String {
        val outBizIdParam = if (outBizId.isNullOrEmpty()) "" else "\"outBizId\":\"$outBizId\","
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.submitAnswer",
            "[{\"activityId\":\"$activityId\",\"answer\":\"$answer\",\"dadaVersion\":\"1.3.0\"," +
                    "$outBizIdParam\"questionId\":\"$questionId\",\"version\":$VERSION}]"
        )
    }

    /**
     * 获取问题
     *
     * @param activityId 活动ID
     * @return RPC响应字符串
     */
    @JvmStatic
    fun getQuestion(activityId: String): String {
        return RequestManager.requestString(
            "com.alipay.reading.game.dada.openDailyAnswer.getQuestion",
            "[{\"activityId\":\"$activityId\",\"dadaVersion\":\"1.3.0\",\"version\":$VERSION}]"
        )
    }
}
