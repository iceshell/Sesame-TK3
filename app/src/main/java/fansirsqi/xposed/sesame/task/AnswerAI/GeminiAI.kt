package fansirsqi.xposed.sesame.task.AnswerAI

import fansirsqi.xposed.sesame.util.JsonUtil.getValueByPath
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import fansirsqi.xposed.sesame.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

/**
 * GeminiAI帮助类，用于与Gemini接口交互以获取AI回答
 * 支持单条文本问题及带有候选答案列表的问题请求
 */
class GeminiAI(token: String?) : AnswerAIInterface {
    
    private val token: String = if (!token.isNullOrEmpty()) token else ""
    private var modelNameInternal: String = "gemini-1.5-flash"
    
    override fun getModelName(): String = modelNameInternal
    override fun setModelName(modelName: String) {
        this.modelNameInternal = modelName
    }

    private fun removeControlCharacters(text: String): String {
        return text.replace(Regex("\\p{Cntrl}&&[^\n\t]"), "")
    }

    private fun buildRequestBody(text: String): String {
        val cleanText = removeControlCharacters(text)
        return "{" +
                "\"contents\":[{" +
                "\"parts\":[{" +
                "\"text\":\"${PREFIX}${cleanText}\"" +
                "}]" +
                "}]" +
                "}"
    }

    private fun buildRequestUrl(): String {
        return "$BASE_URL/v1beta/models/$modelNameInternal:generateContent?key=$token"
    }

    override fun getAnswerStr(text: String, model: String): String {
        setModelName(model)
        return getAnswerStr(text)
    }

    override fun getAnswerStr(text: String): String {
        var result = ""
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .writeTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .readTimeout(TIME_OUT_SECONDS.toLong(), TimeUnit.SECONDS)
                .build()

            val content = buildRequestBody(text)
            val mediaType = CONTENT_TYPE.toMediaType()
            val body = content.toRequestBody(mediaType)
            val url = buildRequestUrl()
            val request = Request.Builder()
                .url(url)
                .method("POST", body)
                .addHeader("Content-Type", CONTENT_TYPE)
                .build()

            client.newCall(request).execute().use { response ->
                if (response.body == null) {
                    return result
                }
                val json = response.body!!.string()
                if (!response.isSuccessful) {
                    Log.other("Gemini请求失败")
                    Log.runtime(TAG, "Gemini接口异常：$json")
                    return result
                }
                val jsonObject = JSONObject(json)
                result = getValueByPath(jsonObject, JSON_PATH)
            }
        } catch (e: IOException) {
            Log.printStackTrace(TAG, e)
        } catch (e: org.json.JSONException) {
            Log.printStackTrace(TAG, e)
        }
        return result
    }

    override fun getAnswer(title: String, answerList: List<String>): Int {
        try {
            val answerStr = StringBuilder()
            for (i in answerList.indices) {
                answerStr.append(i + 1).append(".[")
                    .append(answerList[i]).append("]\n")
            }

            val question = "问题：$title\n\n" +
                    "答案列表：\n\n$answerStr\n\n" +
                    "请只返回答案列表中的序号"

            val answerResult = getAnswerStr(question)

            if (answerResult.isNotEmpty()) {
                try {
                    val index = answerResult.trim().toInt() - 1
                    if (index in answerList.indices) {
                        return index
                    }
                } catch (e: NumberFormatException) {
                    Log.other("AI🧠回答，非序号格式：$answerResult")
                }

                for (i in answerList.indices) {
                    if (answerResult.contains(answerList[i])) {
                        return i
                    }
                }
            }
        } catch (e: Exception) {
            Log.printStackTrace(TAG, e)
        }
        return -1
    }

    companion object {
        private val TAG = GeminiAI::class.java.simpleName
        private const val BASE_URL = "https://api.genai.gd.edu.kg/google"
        private const val CONTENT_TYPE = "application/json"
        private const val JSON_PATH = "candidates.[0].content.parts.[0].text"
        private const val PREFIX = "只回答答案 "
        private const val TIME_OUT_SECONDS = 180
    }
}
