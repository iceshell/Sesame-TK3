package fansirsqi.xposed.sesame.data

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import fansirsqi.xposed.sesame.ui.SettingActivity
import fansirsqi.xposed.sesame.ui.WebSettingsActivity
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.Log
import java.io.File

/**
 * UI配置类
 * 管理应用的UI选项配置
 */
class UIConfig private constructor() {
    
    @JsonIgnore  // 排除掉不需要序列化的字段
    var isInit: Boolean = false
        private set
    
    @JsonProperty("uiOption")  // 直接序列化 uiOption 字段
    var uiOption: String = UI_OPTION_WEB  // 默认值为 "web"
    
    /**
     * 获取目标Activity类
     */
    @get:JsonIgnore
    val targetActivityClass: Class<*>
        get() = when (uiOption) {
            UI_OPTION_WEB -> WebSettingsActivity::class.java
            UI_OPTION_NEW -> SettingActivity::class.java
            else -> {
                Log.runtime(TAG, "未知的 UI 选项: $uiOption")
                WebSettingsActivity::class.java
            }
        }
    
    companion object {
        private val TAG = UIConfig::class.java.simpleName
        
        @JvmField
        val INSTANCE = UIConfig()
        
        const val UI_OPTION_WEB = "web"  // webUI
        const val UI_OPTION_NEW = "new"
        
        /**
         * 保存UI配置
         */
        @JvmStatic
        fun save(): Boolean {
            Log.record(TAG, "保存UI配置")
            return Files.setTargetFileofDir(
                JsonUtil.formatJson(INSTANCE),
                File(Files.CONFIG_DIR, "app_config.json")
            )
        }
        
        /**
         * 加载UI配置
         */
        @JvmStatic
        @Synchronized
        fun load(): UIConfig {
            val targetFile = Files.getTargetFileofDir(Files.CONFIG_DIR, "app_config.json")
            
            try {
                if (targetFile.exists()) {
                    val json = Files.readFromFile(targetFile)
                    if (json.trim().isNotEmpty()) {
                        JsonUtil.copyMapper().readerForUpdating(INSTANCE).readValue<Any>(json)
                        val formatted = JsonUtil.formatJson(INSTANCE)
                        if (formatted != json) {
                            Log.runtime(TAG, "格式化${TAG}配置")
                            Files.write2File(formatted, targetFile)
                        }
                    } else {
                        resetToDefault()
                    }
                } else {
                    resetToDefault()
                    Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile)
                }
            } catch (e: Exception) {
                Log.printStackTrace(TAG, e)
                Log.runtime(TAG, "重置${TAG}配置")
                resetToDefault()
                try {
                    Files.write2File(JsonUtil.formatJson(INSTANCE), targetFile)
                } catch (e2: Exception) {
                    Log.printStackTrace(TAG, e2)
                }
            }
            
            INSTANCE.isInit = true
            return INSTANCE
        }
        
        /**
         * 重置为默认配置
         */
        @Synchronized
        private fun resetToDefault() {
            Log.runtime(TAG, "重置UI配置")
            INSTANCE.uiOption = UI_OPTION_WEB  // 默认设置为 "web"
            INSTANCE.isInit = false
        }
    }
}
