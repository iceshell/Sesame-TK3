package fansirsqi.xposed.sesame.model.modelFieldExt

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Switch
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.model.ModelField

/**
 * Boolean类型字段类
 * 该类用于表示布尔值字段，使用Switch控件进行展示
 */
class BooleanModelField(code: String, name: String, value: Boolean) : ModelField<Boolean>(code, name, value) {

    /**
     * 获取字段类型
     *
     * @return 字段类型字符串
     */
    override fun getType(): String = "BOOLEAN"

    /**
     * 创建并返回 Switch 视图
     *
     * @param context 上下文对象
     * @return 生成的 Switch 视图
     */
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    override fun getView(context: Context): View {
        return Switch(context).apply {
            text = name // 设置 Switch 的文本为字段名称
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ) // 设置布局参数
            minHeight = 150 // 设置最小高度
            maxHeight = 180 // 设置最大高度
            setPaddingRelative(40, 0, 40, 0) // 设置左右内边距
            isChecked = value ?: false // 根据字段值设置 Switch 的选中状态
            // 设置按钮和轨道样式
            setThumbResource(R.drawable.switch_thumb)
            setTrackResource(R.drawable.switch_track)
            // 设置点击监听器，更新字段值
            setOnClickListener { v ->
                setObjectValue((v as Switch).isChecked)
            }
        }
    }
}
