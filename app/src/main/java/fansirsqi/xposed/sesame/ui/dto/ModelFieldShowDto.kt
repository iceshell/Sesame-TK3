package fansirsqi.xposed.sesame.ui.dto

import fansirsqi.xposed.sesame.model.ModelField
import java.io.Serializable

/**
 * 模型字段展示数据传输对象。
 * 用于封装模型字段的展示信息，包括字段代码、名称、类型、扩展键和配置值。
 *
 * @property code 字段代码
 * @property name 字段名称
 * @property type 字段类型
 * @property expandKey 扩展键，用于存储额外的信息
 * @property configValue 配置值，用于存储字段的配置信息
 * @property desc 字段描述
 */
data class ModelFieldShowDto(
    var code: String = "",
    var name: String = "",
    var type: String = "",
    var expandKey: Any? = null,
    var configValue: String = "",
    var desc: String = ""
) : Serializable {

    companion object {
        /**
         * 将ModelField对象转换为ModelFieldShowDto对象。
         * 这是一个静态工厂方法，用于创建ModelFieldShowDto实例。
         *
         * @param modelField ModelField对象
         * @return ModelFieldShowDto对象
         */
        @JvmStatic
        fun toShowDto(modelField: ModelField<*>): ModelFieldShowDto {
            return ModelFieldShowDto(
                code = modelField.code,
                name = modelField.name,
                type = modelField.type,
                expandKey = modelField.expandKey,
                configValue = modelField.configValue,
                desc = modelField.desc ?: ""  // 如果desc为null，使用空字符串
            )
        }
    }
}
