package fansirsqi.xposed.sesame.task.antStall

import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.task.AnswerAI.AnswerAI
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import org.json.JSONObject

/**
 * 阅读达达任务
 *
 * @author Constanline
 * @since 2023/08/22
 */
class ReadingDada {

    fun getGroup(): ModelGroup = ModelGroup.STALL

    companion object {
        private const val TAG = "ReadingDada"

        /**
         * 回答问题
         *
         * @param bizInfo 业务信息JSON对象
         * @return 是否成功回答
         */
        @JvmStatic
        fun answerQuestion(bizInfo: JSONObject): Boolean {
            try {
                // 获取活动ID
                var taskJumpUrl = bizInfo.optString("taskJumpUrl")
                if (taskJumpUrl.isNullOrEmpty()) {
                    taskJumpUrl = bizInfo.getString("targetUrl")
                }

                val activityId = taskJumpUrl.split("activityId%3D")[1].split("%26")[0]

                // 获取outBizId（可选）
                val outBizId = if (taskJumpUrl.contains("outBizId%3D")) {
                    taskJumpUrl.split("outBizId%3D")[1].split("%26")[0]
                } else {
                    ""
                }

                // 获取问题
                var response = ReadingDadaRpcCall.getQuestion(activityId)
                var json = JSONObject(response)

                if (json.getString("resultCode") == "200") {
                    val options = json.getJSONArray("options")
                    val question = json.getString("title")

                    // 获取AI答案
                    var answer = AnswerAI.getAnswer(
                        question,
                        JsonUtil.jsonArrayToList(options),
                        "other"
                    )

                    // 如果AI没有答案，使用第一个选项
                    if (answer.isNullOrEmpty()) {
                        answer = options.getString(0)
                    }

                    // 提交答案
                    response = ReadingDadaRpcCall.submitAnswer(
                        activityId,
                        outBizId,
                        json.getString("questionId"),
                        answer
                    )
                    json = JSONObject(response)

                    return if (json.getString("resultCode") == "200") {
                        Log.record(TAG, "答题完成")
                        true
                    } else {
                        Log.record(TAG, "答题失败")
                        false
                    }
                } else {
                    Log.record(TAG, "获取问题失败")
                }
            } catch (e: Throwable) {
                Log.runtime(TAG, "answerQuestion err:")
                Log.printStackTrace(TAG, e)
            }
            return false
        }
    }
}
