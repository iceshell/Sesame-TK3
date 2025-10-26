package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog

/**
 * 单选字段，从列表中选择一个选项
 */
class SelectOneModelField : ModelField<String>, SelectModelFieldFunc {
    
    private val selectListFunc: SelectListFunc?
    private val expandValue: List<MapperEntity>?

    constructor(code: String, name: String, value: String, expandValue: List<out MapperEntity>) 
        : super(code, name, value) {
        this.expandValue = expandValue
        this.selectListFunc = null
    }

    constructor(code: String, name: String, value: String, selectListFunc: SelectListFunc) 
        : super(code, name, value) {
        this.selectListFunc = selectListFunc
        this.expandValue = null
    }

    override fun getType(): String = "SELECT_ONE"

    override fun getExpandValue(): Any? = selectListFunc?.getList() ?: expandValue

    override fun getView(context: Context): View {
        return Button(context).apply {
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
                ListDialog.show(v.context, (v as Button).text, this@SelectOneModelField, ListDialog.ListType.RADIO)
            }
        }
    }

    override fun clear() {
        value = defaultValue
    }

    override fun get(id: String?): Int? = 0

    override fun add(id: String?, count: Int?) {
        id?.let { value = it }
    }

    override fun remove(id: String?) {
        if (value == id) {
            value = defaultValue
        }
    }

    override fun contains(id: String?): Boolean? = value == id

    /**
     * 选择列表函数接口
     */
    fun interface SelectListFunc {
        fun getList(): List<MapperEntity>
    }
}
