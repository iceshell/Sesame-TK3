package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import org.json.JSONException
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.entity.MapperEntity
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.ui.widget.ListDialog

/**
 * 多选字段
 * 数据结构说明：
 * - Set<String> 表示已选择的数据
 * - List<MapperEntity> 需要选择的数据
 */
class SelectModelField : ModelField<MutableSet<String>>, SelectModelFieldFunc {
    
    private val selectListFunc: SelectListFunc?
    private val expandValue: List<MapperEntity>?

    constructor(code: String, name: String, value: MutableSet<String>, expandValue: List<out MapperEntity>) 
        : super(code, name, value) {
        this.expandValue = expandValue
        this.selectListFunc = null
    }

    constructor(code: String, name: String, value: MutableSet<String>, selectListFunc: SelectListFunc) 
        : super(code, name, value) {
        this.selectListFunc = selectListFunc
        this.expandValue = null
    }

    constructor(code: String, name: String, value: MutableSet<String>, expandValue: List<out MapperEntity>, desc: String) 
        : super(code, name, value, desc) {
        this.expandValue = expandValue
        this.selectListFunc = null
    }

    constructor(code: String, name: String, value: MutableSet<String>, selectListFunc: SelectListFunc, desc: String) 
        : super(code, name, value, desc) {
        this.selectListFunc = selectListFunc
        this.expandValue = null
    }

    override fun getType(): String = "SELECT"

    @Throws(JSONException::class)
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
                try {
                    ListDialog.show(v.context, (v as Button).text, this@SelectModelField)
                } catch (e: JSONException) {
                    throw RuntimeException(e)
                }
            }
        }
    }

    override fun clear() {
        value.clear()
    }

    override fun get(id: String?): Int? = 0

    override fun add(id: String?, count: Int?) {
        id?.let { value.add(it) }
    }

    override fun remove(id: String?) {
        id?.let { value.remove(it) }
    }

    override fun contains(id: String?): Boolean? = id?.let { value.contains(it) }

    /**
     * 选择列表函数接口
     */
    fun interface SelectListFunc {
        @Throws(JSONException::class)
        fun getList(): List<MapperEntity>
    }
}
