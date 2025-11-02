package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.JsonUtil

/**
 * 数据结构说明
 * Map<String, Integer> 表示已选择的数据与已经设置的数量映射关系
 * List<? extends IdAndName> 需要选择的数据
 */
class SelectAndCountModelField : ModelField<MutableMap<String?, Int?>>, SelectModelFieldFunc {
    
    private val selectListFunc: SelectListFunc?
    private val expandValueList: List<MapperEntity>?

    constructor(code: String, name: String, value: MutableMap<String?, Int?>, expandValue: List<MapperEntity>) : super(code, name, value) {
        this.expandValueList = expandValue
        this.selectListFunc = null
        valueType = value.javaClass
    }

    constructor(code: String, name: String, value: MutableMap<String?, Int?>, selectListFunc: SelectListFunc) : super(code, name, value) {
        this.selectListFunc = selectListFunc
        this.expandValueList = null
        valueType = value.javaClass
    }

    constructor(code: String, name: String, value: MutableMap<String?, Int?>, expandValue: List<MapperEntity>, desc: String) : super(code, name, value, desc) {
        this.expandValueList = expandValue
        this.selectListFunc = null
        valueType = value.javaClass
    }

    constructor(code: String, name: String, value: MutableMap<String?, Int?>, selectListFunc: SelectListFunc, desc: String) : super(code, name, value, desc) {
        this.selectListFunc = selectListFunc
        this.expandValueList = null
        valueType = value.javaClass
    }

    override fun getType(): String = "SELECT_AND_COUNT"

    override fun getExpandValue(): List<MapperEntity>? {
        return selectListFunc?.getList() ?: expandValueList
    }
    
    /**
     * 设置配置值
     * 直接解析Map类型，避免父类的类型推断错误
     */
    override fun setConfigValue(configValue: String?) {
        value = when {
            configValue.isNullOrBlank() -> defaultValue
            else -> {
                try {
                    JsonUtil.parseObject(configValue, object : TypeReference<LinkedHashMap<String?, Int?>>() {})
                } catch (e: Exception) {
                    defaultValue ?: LinkedHashMap()
                }
            }
        }
    }

    override fun getView(context: Context): View {
        val btn = Button(context).apply {
            text = name
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setTextColor(ContextCompat.getColor(context, R.color.selection_color))
            background = ContextCompat.getDrawable(context, R.drawable.dialog_list_button)
            gravity = Gravity.START or Gravity.CENTER_VERTICAL
            minHeight = 150
            maxHeight = 180
            setPaddingRelative(40, 0, 40, 0)
            isAllCaps = false
            setOnClickListener { v ->
                ListDialog.show(v.context, (v as Button).text, this@SelectAndCountModelField)
            }
        }
        return btn
    }

    override fun clear() {
        value?.clear()
    }

    override fun get(id: String?): Int? {
        return value?.get(id)
    }

    override fun add(id: String?, count: Int?) {
        if (id != null && count != null) {
            value?.set(id, count)
        }
    }

    override fun remove(id: String?) {
        value?.remove(id)
    }

    override fun contains(id: String?): Boolean? {
        return value?.containsKey(id)
    }

    fun interface SelectListFunc {
        fun getList(): List<MapperEntity>
    }
}
