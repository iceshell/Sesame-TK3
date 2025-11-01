package fansirsqi.xposed.sesame.task.AnswerAI

import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.modelFieldExt.ChoiceModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.StringModelField
import fansirsqi.xposed.sesame.model.modelFieldExt.TextModelField
import fansirsqi.xposed.sesame.util.Log

class AnswerAI : Model() {

    object AIType {
        const val TONGYI = 0
        const val GEMINI = 1
        const val DEEPSEEK = 2
        const val CUSTOM = 3

        val nickNames = arrayOf(
            "通义千问",
            "Gemini",
            "DeepSeek",
            "自定义"
        )
    }

    private val getTongyiAIToken = TextModelField.UrlTextModelField(
        "getTongyiAIToken",
        "通义千问 | 获取令牌",
        "https://help.aliyun.com/zh/dashscope/developer-reference/acquisition-and-configuration-of-api-key"
    )
    private val tongYiToken = StringModelField("tongYiToken", "qwen-turbo | 设置令牌", "")
    private val getGeminiAIToken = TextModelField.UrlTextModelField(
        "getGeminiAIToken",
        "Gemini | 获取令牌",
        "https://aistudio.google.com/app/apikey"
    )
    private val GeminiToken = StringModelField("GeminiAIToken", "gemini-1.5-flash | 设置令牌", "")
    private val getDeepSeekToken = TextModelField.UrlTextModelField(
        "getDeepSeekToken",
        "DeepSeek | 获取令牌",
        "https://platform.deepseek.com/usage"
    )
    private val DeepSeekToken = StringModelField("DeepSeekToken", "DeepSeek-R1 | 设置令牌", "")
    private val getCustomServiceToken = TextModelField.ReadOnlyTextModelField(
        "getCustomServiceToken",
        "粉丝福利😍",
        "下面这个不用动可以白嫖到3月10号让我们感谢讯飞大善人🙏"
    )
    private val CustomServiceToken = StringModelField(
        "CustomServiceToken",
        "自定义服务 | 设置令牌",
        "sk-pQF9jek0CTTh3boKDcA9DdD7340a4e929eD00a13F681Cd8e"
    )
    private val CustomServiceUrl = StringModelField(
        "CustomServiceBaseUrl",
        "自定义服务 | 设置BaseUrl",
        "https://maas-api.cn-huabei-1.xf-yun.com/v1"
    )
    private val CustomServiceModel = StringModelField(
        "CustomServiceModel",
        "自定义服务 | 设置模型",
        "xdeepseekr1"
    )

    override fun getName(): String = "AI答题"

    override fun getGroup(): ModelGroup = ModelGroup.OTHER

    override fun getIcon(): String = "AnswerAI.svg"

    override fun getFields(): ModelFields {
        val modelFields = ModelFields()
        modelFields.addField(aiType)
        modelFields.addField(getTongyiAIToken)
        modelFields.addField(tongYiToken)
        modelFields.addField(getGeminiAIToken)
        modelFields.addField(GeminiToken)
        modelFields.addField(getDeepSeekToken)
        modelFields.addField(DeepSeekToken)
        modelFields.addField(getCustomServiceToken)
        modelFields.addField(CustomServiceToken)
        modelFields.addField(CustomServiceUrl)
        modelFields.addField(CustomServiceModel)
        return modelFields
    }

    override fun boot(classLoader: ClassLoader?) {
        try {
            enable = enableField?.value ?: false
            val selectedType = aiType.value ?: AIType.TONGYI
            Log.runtime(String.format("初始化AI服务：已选择[%s]", AIType.nickNames[selectedType]))
            initializeAIService(selectedType)
        } catch (e: Exception) {
            Log.error(TAG, "初始化AI服务失败: ${e.message}")
            Log.printStackTrace(TAG, e)
        }
    }

    private fun initializeAIService(selectedType: Int) {
        answerAIInterface?.release()

        answerAIInterface = when (selectedType) {
            AIType.TONGYI -> TongyiAI(tongYiToken.value)
            AIType.GEMINI -> GeminiAI(GeminiToken.value)
            AIType.DEEPSEEK -> DeepSeek(DeepSeekToken.value)
            AIType.CUSTOM -> {
                val service = CustomService(CustomServiceToken.value, CustomServiceUrl.value)
                service.setModelName(CustomServiceModel.value ?: "")
                Log.runtime(
                    String.format(
                        "已配置自定义服务：URL=[%s], Model=[%s]",
                        CustomServiceUrl.value,
                        CustomServiceModel.value
                    )
                )
                service
            }
            else -> AnswerAIInterface.getInstance()
        }
    }

    private fun selectlogger(flag: String, msg: String) {
        when (flag) {
            "farm" -> Log.farm(msg)
            "forest" -> Log.forest(msg)
            else -> Log.other(msg)
        }
    }

    companion object {
        private val TAG = AnswerAI::class.java.simpleName
        private const val QUESTION_LOG_FORMAT = "题目📒 [%s] | 选项: %s"
        private const val AI_ANSWER_LOG_FORMAT = "AI回答🧠 [%s] | AI类型: [%s] | 模型名称: [%s]"
        private const val NORMAL_ANSWER_LOG_FORMAT = "普通回答🤖 [%s]"
        private const val ERROR_AI_ANSWER = "AI回答异常：无法获取有效答案，请检查AI服务配置是否正确"

        private var enable = false
        private var answerAIInterface: AnswerAIInterface? = AnswerAIInterface.getInstance()
        private val aiType = ChoiceModelField("useGeminiAI", "AI类型", AIType.TONGYI, AIType.nickNames)

        @JvmStatic
        fun getAnswer(text: String?, answerList: List<String>?, flag: String): String {
            if (text == null || answerList == null) {
                when (flag) {
                    "farm" -> Log.farm("问题或答案列表为空")
                    "forest" -> Log.forest("问题或答案列表为空")
                    else -> Log.other("问题或答案列表为空")
                }
                return ""
            }
            var answerStr = ""
            try {
                val msg = String.format(QUESTION_LOG_FORMAT, text, answerList)
                when (flag) {
                    "farm" -> Log.farm(msg)
                    "forest" -> Log.forest(msg)
                    else -> Log.other(msg)
                }
                
                if (enable && answerAIInterface != null) {
                    val answer = answerAIInterface?.getAnswer(text, answerList)
                    if (answer != null && answer >= 0 && answer < answerList.size) {
                        answerStr = answerList[answer]
                        val logMsg = String.format(
                            AI_ANSWER_LOG_FORMAT,
                            answerStr,
                            AIType.nickNames[aiType.value ?: AIType.TONGYI],
                            answerAIInterface?.getModelName() ?: ""
                        )
                        when (flag) {
                            "farm" -> Log.farm(logMsg)
                            "forest" -> Log.forest(logMsg)
                            else -> Log.other(logMsg)
                        }
                    } else {
                        Log.error(ERROR_AI_ANSWER)
                    }
                } else if (answerList.isNotEmpty()) {
                    answerStr = answerList[0]
                    val logMsg = String.format(NORMAL_ANSWER_LOG_FORMAT, answerStr)
                    when (flag) {
                        "farm" -> Log.farm(logMsg)
                        "forest" -> Log.forest(logMsg)
                        else -> Log.other(logMsg)
                    }
                }
            } catch (t: Throwable) {
                Log.printStackTrace(TAG, "AI获取答案异常:", t)
            }
            return answerStr
        }
    }
}
