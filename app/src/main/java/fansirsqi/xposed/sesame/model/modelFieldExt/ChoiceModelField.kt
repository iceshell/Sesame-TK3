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
import fansirsqi.xposed.sesame.ui.ChoiceDialog

/**
 * 选择型字段，用于在多个选项中选择一个
 */
class ChoiceModelField : ModelField<Int> {
    
    private var choiceArray: Array<out String?>? = null

    constructor(code: String, name: String, value: Int) : super(code, name, value)

    constructor(code: String, name: String, value: Int, choiceArray: Array<out String?>) : super(code, name, value) {
        this.choiceArray = choiceArray
    }

    constructor(code: String, name: String, value: Int, desc: String) : super(code, name, value, desc)

    constructor(code: String, name: String, value: Int, choiceArray: Array<out String?>, desc: String) 
        : super(code, name, value, desc) {
        this.choiceArray = choiceArray
    }

    override fun getType(): String = "CHOICE"

    override fun getExpandKey(): Array<out String?>? = choiceArray

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
                ChoiceDialog.show(v.context, (v as Button).text, this@ChoiceModelField)
            }
        }
    }
}
