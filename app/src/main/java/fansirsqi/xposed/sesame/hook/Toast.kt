package fansirsqi.xposed.sesame.hook

import android.content.Context
import android.os.Handler
import android.os.Looper
import fansirsqi.xposed.sesame.model.BaseModel
import fansirsqi.xposed.sesame.util.Log

/**
 * Toast显示工具类
 */
object Toast {
    private val TAG = Toast::class.java.simpleName

    /**
     * 显示Toast消息
     *
     * @param message 要显示的消息
     */
    @JvmStatic
    fun show(message: CharSequence) {
        show(message, false)
    }

    /**
     * 显示Toast消息
     *
     * @param message 要显示的消息
     * @param force 是否强制显示
     */
    @JvmStatic
    fun show(message: CharSequence, force: Boolean) {
        val context = ApplicationHookConstants.appContext
        if (context == null) {
            Log.runtime(TAG, "Context is null, cannot show toast")
            return
        }

        val shouldShow = force || (BaseModel.showToast?.value ?: false)
        if (shouldShow) {
            displayToast(context.applicationContext, message)
        }
    }

    /**
     * 显示Toast消息（确保在主线程中调用）
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private fun displayToast(context: Context, message: CharSequence) {
        try {
            val mainHandler = Handler(Looper.getMainLooper())
            if (Looper.myLooper() == Looper.getMainLooper()) {
                // 如果当前线程是主线程，直接显示
                createAndShowToast(context, message)
            } else {
                // 在非主线程，通过Handler切换到主线程
                mainHandler.post { createAndShowToast(context, message) }
            }
        } catch (t: Throwable) {
            Log.runtime(TAG, "displayToast err:")
            Log.printStackTrace(TAG, t)
        }
    }

    /**
     * 创建并显示Toast
     *
     * @param context 上下文
     * @param message 要显示的消息
     */
    private fun createAndShowToast(context: Context, message: CharSequence) {
        try {
            val toast = android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT)
            toast.setGravity(
                toast.gravity,
                toast.xOffset,
                BaseModel.toastOffsetY?.value ?: 0
            )
            toast.show()
        } catch (t: Throwable) {
            Log.runtime(TAG, "createAndShowToast err:")
            Log.printStackTrace(TAG, t)
        }
    }
}
