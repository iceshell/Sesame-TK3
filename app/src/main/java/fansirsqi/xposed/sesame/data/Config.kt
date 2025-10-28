package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException
import com.fasterxml.jackson.databind.node.ObjectNode
import fansirsqi.xposed.sesame.entity.UserEntity
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelFields
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.task.TaskCommon
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.maps.UserMap
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * 配置类，负责加载、保存、管理应用的配置数据。
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Config private constructor() {

    /** 是否初始化标志 */
    @JsonIgnore
    @Volatile
    var isInit: Boolean = false
        private set

    /** 存储模型字段的映射 */
    val modelFieldsMap: MutableMap<String, ModelFields> = ConcurrentHashMap()

    /**
     * 设置新的模型字段配置
     *
     * @param newModels 新的模型字段映射
     */
    fun setModelFieldsMap(newModels: Map<String, ModelFields>?) {
        modelFieldsMap.clear()
        val modelConfigMap = Model.getModelConfigMap()
        val models = newModels ?: emptyMap()

        // 遍历所有模型配置，合并字段配置
        for ((modelCode, modelConfig) in modelConfigMap.entries) {
            val newModelFields = ModelFields()
            val configModelFields = modelConfig.fields
            val modelFields = models[modelCode]

            if (modelFields != null) {
                // 如果已有模型字段，则按值覆盖配置
                for (configModelField in configModelFields.values) {
                    val modelField = modelFields[configModelField.code]
                    try {
                        if (modelField != null) {
                            val value = modelField.value
                            if (value != null) {
                                configModelField.setObjectValue(value)
                            }
                        }
                    } catch (e: Exception) {
                        Log.printStackTrace(e)
                    }
                    newModelFields.addField(configModelField)
                }
            } else {
                // 如果没有找到对应的模型字段，则直接添加配置字段
                for (configModelField in configModelFields.values) {
                    newModelFields.addField(configModelField)
                }
            }
            modelFieldsMap[modelCode] = newModelFields
        }
    }

    /**
     * 检查是否存在指定的模型字段
     *
     * @param modelCode 模型代码
     * @return 是否存在该模型字段
     */
    fun hasModelFields(modelCode: String): Boolean {
        return modelFieldsMap.containsKey(modelCode)
    }

    /**
     * 检查指定模型字段是否存在
     *
     * @param modelCode 模型代码
     * @param fieldCode 字段代码
     * @return 是否存在该字段
     */
    fun hasModelField(modelCode: String, fieldCode: String): Boolean {
        val modelFields = modelFieldsMap[modelCode] ?: return false
        return modelFields.containsKey(fieldCode)
    }

    companion object {
        private const val TAG = "Config"

        /** 单例实例 */
        @JvmField
        val INSTANCE = Config()

        /**
         * 判断配置文件是否已修改
         *
         * @param userId 用户 ID
         * @return 是否已修改
         */
        @JvmStatic
        fun isModify(userId: String?): Boolean {
            val configV2File = if (userId.isNullOrEmpty()) {
                Files.getDefaultConfigV2File()
            } else {
                Files.getConfigV2File(userId)
            }

            if (!configV2File.exists()) {
                return true
            }

            val json = Files.readFromFile(configV2File) ?: return true
            val formatted = JsonUtil.formatJson(INSTANCE)
            return formatted == null || formatted != json
        }

        /**
         * 保存配置文件
         *
         * @param userId 用户 ID
         * @param force  是否强制保存
         * @return 保存是否成功
         */
        @JvmStatic
        @Synchronized
        fun save(userId: String?, force: Boolean): Boolean {
            if (!force && !isModify(userId)) {
                return true
            }

            val json = try {
                JsonUtil.formatJson(INSTANCE)
                    ?: throw IllegalStateException("配置格式化失败，返回的 JSON 为空")
            } catch (e: Exception) {
                Log.printStackTrace(TAG, e)
                Log.runtime(TAG, "保存用户配置失败，格式化 JSON 时出错")
                return false
            }

            return try {
                val actualUserId = userId?.takeIf { it.isNotEmpty() } ?: "默认"
                val success = if (userId.isNullOrEmpty()) {
                    Files.setDefaultConfigV2File(json)
                } else {
                    Files.setConfigV2File(userId, json)
                }

                if (!success) {
                    throw IOException("配置文件保存失败")
                }

                val userName = if (actualUserId == "默认") {
                    "默认用户"
                } else {
                    UserMap.get(actualUserId)?.showName ?: "默认"
                }

                Log.runtime(TAG, "保存 [$userName] 配置")
                true
            } catch (e: Exception) {
                Log.printStackTrace(TAG, e)
                Log.runtime(TAG, "保存用户配置失败")
                false
            }
        }

        /**
         * 检查配置是否已加载
         *
         * @return 是否已加载
         */
        @JvmStatic
        fun isLoaded(): Boolean = INSTANCE.isInit

        /**
         * 加载配置文件
         *
         * @param userId 用户 ID
         * @return 配置实例
         */
        @JvmStatic
        @Synchronized
        fun load(userId: String?): Config {
            Log.record(TAG, "开始加载配置")
            var configV2File: File? = null

            try {
                val userName: String
                if (userId.isNullOrEmpty()) {
                    configV2File = Files.getDefaultConfigV2File()
                    userName = "默认"
                    if (!configV2File.exists()) {
                        Log.record(TAG, "默认配置文件不存在，初始化新配置")
                        unload()
                        toSaveStr()?.let { Files.write2File(it, configV2File) }
                    }
                } else {
                    configV2File = Files.getConfigV2File(userId)
                    val userEntity = UserMap.get(userId)
                    userName = userEntity?.showName ?: userId
                }

                Log.record(TAG, "加载配置: $userName")
                val configV2FileExists = configV2File.exists()
                val defaultConfigV2FileExists = Files.getDefaultConfigV2File().exists()

                when {
                    configV2FileExists -> {
                        val json = Files.readFromFile(configV2File)
                        Log.runtime(TAG, "读取配置文件成功: ${configV2File.path}")
                        try {
                            JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json, Config::class.java)
                        } catch (e: UnrecognizedPropertyException) {
                            Log.error(TAG, "配置文件中存在无法识别的字段: '${e.propertyName}'，将尝试移除并重新加载。")
                            try {
                                // 移除无法识别的字段并重新解析
                                val mapper = JsonUtil.copyMapper()
                                val rootNode = mapper.readTree(json)
                                (rootNode as ObjectNode).remove(e.propertyName)
                                val cleanedJson = mapper.writeValueAsString(rootNode)
                                mapper.readerForUpdating(INSTANCE).readValue(cleanedJson, Config::class.java)
                                Log.error(TAG, "成功移除问题字段并加载配置。")
                                // 保存修复后的配置
                                toSaveStr()?.let { Files.write2File(it, configV2File) }
                                Log.error(TAG, "已保存修复后的配置文件。")
                            } catch (innerEx: Exception) {
                                Log.printStackTrace(TAG, "移除问题字段后，加载配置仍然失败。", innerEx)
                                throw innerEx // 抛出内部异常，触发重置逻辑
                            }
                        }
                        Log.record(TAG, "格式化配置成功:$configV2File")
                        val formatted = toSaveStr()
                        if (formatted != null && formatted != json) {
                            Log.record(TAG, "格式化配置: $userName")
                            Files.write2File(formatted, configV2File)
                        }
                    }
                    defaultConfigV2FileExists -> {
                        val json = Files.readFromFile(Files.getDefaultConfigV2File())
                        JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue(json, Config::class.java)
                        Log.record(TAG, "复制新配置: $userName")
                        Files.write2File(json, configV2File)
                    }
                    else -> {
                        unload()
                        Log.record(TAG, "初始新配置: $userName")
                        toSaveStr()?.let { Files.write2File(it, configV2File) }
                    }
                }
            } catch (t: Throwable) {
                Log.error(TAG, "重置配置失败$t")
                Log.error(TAG, "重置配置")
                try {
                    unload()
                    configV2File?.let { file ->
                        toSaveStr()?.let { json -> Files.write2File(json, file) }
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(TAG, "重置配置失败", e)
                }
            }

            INSTANCE.isInit = true
            TaskCommon.update()
            return INSTANCE
        }

        /**
         * 卸载当前配置
         */
        @JvmStatic
        @Synchronized
        fun unload() {
            for (modelFields in INSTANCE.modelFieldsMap.values) {
                for (modelField in modelFields.values) {
                    modelField?.reset()
                }
            }
        }

        /**
         * 转换为保存字符串
         *
         * @return JSON字符串
         */
        @JvmStatic
        fun toSaveStr(): String? = JsonUtil.formatJson(INSTANCE)
    }
}
