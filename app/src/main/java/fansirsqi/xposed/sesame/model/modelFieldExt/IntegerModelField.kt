package fansirsqi.xposed.sesame.model.modelFieldExt

import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.ui.StringDialog
import fansirsqi.xposed.sesame.util.Log

/**
 * Integer 类型字段类，继承自 ModelField<Int>
 * 该类用于表示具有最小值和最大值限制的整数字段。
 */
open class IntegerModelField : ModelField<Int> {
    
    /** 最小值限制 */
    val minLimit: Int?
    
    /** 最大值限制 */
    val maxLimit: Int?

    /**
     * 构造函数：创建一个没有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     */
    constructor(code: String, name: String, value: Int) : super(code, name, value) {
        this.minLimit = null
        this.maxLimit = null
    }

    /**
     * 构造函数：创建一个具有最小值和最大值限制的 Integer 类型字段
     *
     * @param code 字段代码
     * @param name 字段名称
     * @param value 字段初始值
     * @param minLimit 最小值限制
     * @param maxLimit 最大值限制
     */
    constructor(code: String, name: String, value: Int, minLimit: Int?, maxLimit: Int?) : super(code, name, value) {
        this.minLimit = minLimit
        this.maxLimit = maxLimit
    }

    /**
     * 获取字段类型
     *
     * @return 返回字段类型的字符串表示 "INTEGER"
     */
    override fun getType(): String = "INTEGER"

    /**
     * 获取字段的配置值（将当前的值转换为字符串）
     *
     * @return 返回字段的字符串形式的配置值
     */
    override fun getConfigValue(): String? = value?.toString()

    /**
     * 设置字段的配置值（根据配置值设置新的值，并且在有最小/最大值限制的情况下进行限制）
     *
     * @param configValue 字段的配置值
     */
    override fun setConfigValue(configValue: String?) {
        var newValue: Int = if (configValue.isNullOrBlank()) {
            defaultValue ?: 0
        } else {
            try {
                configValue.toInt()
            } catch (e: Exception) {
                Log.printStackTrace(e)
                defaultValue ?: 0
            }
        }

        // 根据最小值限制调整新值
        minLimit?.let { newValue = maxOf(it, newValue) }
        
        // 根据最大值限制调整新值
        maxLimit?.let { newValue = minOf(it, newValue) }

        // 设置字段值
        this.value = newValue
    }

    /**
     * 获取视图（返回一个 Button，点击后弹出编辑框）
     *
     * @param context 上下文
     * @return 按钮视图
     */
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
                StringDialog.showEditDialog(v.context, (v as Button).text, this@IntegerModelField)
            }
        }
    }

    /**
     * MultiplyIntegerModelField 类，继承自 IntegerModelField，处理带乘数的整数类型字段
     * 该类在设置值时会乘以指定的倍数。
     */
    class MultiplyIntegerModelField(
        code: String,
        name: String,
        value: Int,
        minLimit: Int?,
        maxLimit: Int?,
        /** 乘数，用于计算最终值 */
        val multiple: Int
    ) : IntegerModelField(code, name, value * multiple, minLimit, maxLimit) {

        /**
         * 获取字段类型
         *
         * @return 返回字段类型的字符串表示 "MULTIPLY_INTEGER"
         */
        override fun getType(): String = "MULTIPLY_INTEGER"

        /**
         * 设置字段的配置值（乘数影响最终值）
         *
         * @param configValue 字段的配置值
         */
        override fun setConfigValue(configValue: String?) {
            if (configValue.isNullOrBlank()) {
                reset()
                return
            }
            
            super.setConfigValue(configValue)
            try {
                // 根据乘数调整值
                value = (value ?: 0) * multiple
                return
            } catch (e: Exception) {
                Log.printStackTrace(e)
            }
            reset()
        }

        /**
         * 获取字段的配置值（返回值除以乘数）
         *
         * @return 配置值（字段值除以乘数）
         */
        override fun getConfigValue(): String? = value?.let { (it / multiple).toString() }
    }
}
