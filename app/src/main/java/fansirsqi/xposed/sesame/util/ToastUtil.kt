package fansirsqi.xposed.sesame.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import fansirsqi.xposed.sesame.model.BaseModel

/**
 * Toast工具类
 * 提供统一的Toast显示功能
 */
object ToastUtil {
    
    private const val TAG = "ToastUtil"
    private var appContext: Context? = null
    
    /**
     * 初始化全局Context。建议在Application类中调用。
     *
     * @param context 应用上下文
     */
    @JvmStatic
    fun init(context: Context?) {
        context?.let {
            appContext = it.applicationContext
        }
    }
    
    /**
     * 获取当前环境的Context
     *
     * @return Context
     */
    private fun getContext(): Context {
        return appContext 
            ?: throw IllegalStateException("ToastUtil is not initialized. Call ToastUtil.init(context) in Application.")
    }
    
    /**
     * 显示自定义Toast
     *
     * @param message 显示的消息
     */
    @JvmStatic
    fun showToast(message: String) {
        showToast(getContext(), message)
    }
    
    /**
     * 显示自定义Toast
     *
     * @param context 上下文
     * @param message 显示的消息
     */
    @JvmStatic
    fun showToast(context: Context, message: String) {
        Log.runtime(TAG, "showToast: $message")
        val toast = Toast.makeText(context, message, Toast.LENGTH_SHORT)
        toast.setGravity(toast.gravity, toast.xOffset, BaseModel.toastOffsetY.value ?: 0)
        toast.show()
    }
    
    /**
     * 创建自定义Toast
     *
     * @param message 显示的消息
     * @param duration 显示时长
     * @return Toast对象
     */
    @JvmStatic
    fun makeText(message: String, duration: Int): Toast {
        return makeText(getContext(), message, duration)
    }
    
    /**
     * 创建自定义Toast
     *
     * @param context 上下文
     * @param message 显示的消息
     * @param duration 显示时长
     * @return Toast对象
     */
    @JvmStatic
    fun makeText(context: Context, message: String, duration: Int): Toast {
        val toast = Toast.makeText(context, message, duration)
        toast.setGravity(toast.gravity, toast.xOffset, BaseModel.toastOffsetY.value ?: 0)
        return toast
    }
    
    /**
     * 延迟显示Toast
     *
     * @param context 上下文
     * @param message 显示的消息
     * @param delayMillis 延迟毫秒数
     */
    @JvmStatic
    fun showToastWithDelay(context: Context, message: String, delayMillis: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            makeText(context, message, Toast.LENGTH_SHORT).show()
        }, delayMillis.toLong())
    }
    
    /**
     * 延迟显示Toast
     *
     * @param message 显示的消息
     * @param delayMillis 延迟毫秒数
     */
    @JvmStatic
    fun showToastWithDelay(message: String, delayMillis: Int) {
        Handler(Looper.getMainLooper()).postDelayed({
            makeText(message, Toast.LENGTH_SHORT).show()
        }, delayMillis.toLong())
    }
}
