package fansirsqi.xposed.sesame.util

//import android.R
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import fansirsqi.xposed.sesame.data.RuntimeInfo
import fansirsqi.xposed.sesame.hook.Toast
import fansirsqi.xposed.sesame.model.BaseModel
import kotlin.concurrent.Volatile

@SuppressLint("StaticFieldLeak")
object Notify {
    private val TAG: String = Notify::class.java.getSimpleName()

    @SuppressLint("StaticFieldLeak")
    var context: Context? = null
    private const val NOTIFICATION_ID = 99
    private const val ERROR_NOTIFICATION_ID = 98
    private const val CHANNEL_ID = "fansirsqi.xposed.sesame.ANTFOREST_NOTIFY_CHANNEL"
    private var mNotifyManager: NotificationManager? = null

    @SuppressLint("StaticFieldLeak")
    private var builder: NotificationCompat.Builder? = null

    @Volatile
    private var isNotificationStarted = false

    private var lastUpdateTime: Long = 0
    private var nextExecTimeCache: Long = 0
    private var titleText: String? = ""
    private var contentText = ""


    private fun checkPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.error(TAG, "Missing POST_NOTIFICATIONS permission to send new notification$context")
                Toast.show("请在设置中开启支付宝通知权限")
                return false
            }
        }
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            Log.error(TAG, "Notifications are disabled for this app.$context")
            Toast.show("请在设置中开启支付宝通知权限")
            return false
        }
        return true
    }

    @JvmStatic
    fun start(context: Context) {
        ErrorHandler.safelyRun(TAG, "通知启动失败") {
            if (!checkPermission(context)) return@safelyRun
            
            Notify.context = context
            stop()
            titleText = "🚀 启动中"
            contentText = "🔔 暂无消息"
            lastUpdateTime = System.currentTimeMillis()
            
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@safelyRun
            
            val it = Intent(Intent.ACTION_VIEW)
            it.setData("alipays://platformapi/startapp?appId=".toUri())
            val pi = PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            
            // ✅ minSdk 26+: 直接使用通知渠道
            val notificationChannel = NotificationChannel(CHANNEL_ID, "🔔 芝麻粒能量提醒", NotificationManager.IMPORTANCE_LOW).apply {
                enableLights(false)
                enableVibration(false)
                setShowBadge(false)
            }
            manager.createNotificationChannel(notificationChannel)
            
            val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(context.resources, android.R.drawable.sym_def_app_icon))
                .setContentTitle(titleText)
                .setContentText(contentText)
                .setSubText("芝麻粒")
                .setAutoCancel(false)
                .setContentIntent(pi)
            
            if (BaseModel.enableOnGoing.value == true) {
                notificationBuilder.setOngoing(true)
            }
            
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notificationBuilder.build())
            
            mNotifyManager = manager
            builder = notificationBuilder
            isNotificationStarted = true
        }
    }

    /**
     * 停止通知。 移除通知并停止前台服务。
     */
    @JvmStatic
    fun stop() {
        ErrorHandler.safelyRun(TAG, "通知停止失败") {
            val ctx = context ?: return@safelyRun
            
            // ✅ minSdk 26+: 直接使用stopForeground
            if (ctx is Service) {
                ctx.stopForeground(Service.STOP_FOREGROUND_REMOVE)
            }
            
            NotificationManagerCompat.from(ctx).cancel(NOTIFICATION_ID)
            mNotifyManager = null
            builder = null
            isNotificationStarted = false
        }
    }

    /**
     * 更新通知文本。 更新通知的标题和内容文本，并发送通知。
     *
     * @param status 要更新的状态文本。
     */
    @JvmStatic
    fun updateStatusText(status: String?) {
        var status = status
        if (!isNotificationStarted || context == null || builder == null || mNotifyManager == null) return
        try {
            val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)
            if (forestPauseTime > System.currentTimeMillis()) {
                status = "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行"
            }
            titleText = status
            sendText(true)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 更新下一次执行时间的文本。
     *
     * @param nextExecTime 下一次执行的时间。
     */
    @JvmStatic
    fun updateNextExecText(nextExecTime: Long) {
        if (!isNotificationStarted || context == null || builder == null || mNotifyManager == null) return
        try {
            if (nextExecTime != -1L) {
                nextExecTimeCache = nextExecTime
            }
            titleText = if (nextExecTimeCache > 0) "⏰ 下次执行 " + TimeUtil.getTimeStr(nextExecTimeCache) else ""
            sendText(false)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }

    /**
     * 更新上一次执行的文本。
     *
     * @param content 上一次执行的内容。
     */
    @JvmStatic
    fun updateLastExecText(content: String?) {
        if (!isNotificationStarted || context == null || builder == null || mNotifyManager == null) return
        try {
            contentText = "📌 上次执行 " + TimeUtil.getTimeStr(System.currentTimeMillis()) + "\n🌾 " + content
            sendText(false)
        } catch (e: Exception) {
            Log.printStackTrace(e)
        }
    }


    /**
     * 设置状态文本为执行中。
     */
    @JvmStatic
    fun setStatusTextExec() {
        if (!isNotificationStarted) return
        ErrorHandler.safelyRun(TAG, "设置状态失败") {
            val forestPauseTime = RuntimeInfo.getInstance().getLong(RuntimeInfo.RuntimeInfoKey.ForestPauseTime)

            if (forestPauseTime > System.currentTimeMillis()) {
                titleText = "❌ 触发异常，等待至" + TimeUtil.getCommonDate(forestPauseTime) + "恢复运行"
            }
            titleText = "⚙️ 芝麻粒正在施工中..."
            builder?.setContentTitle(titleText)
            sendText(true)
        }
    }

    /**
     * 设置状态文本为已禁用
     */
    @JvmStatic
    fun setStatusTextDisabled() {
        if (!isNotificationStarted) return
        ErrorHandler.safelyRun(TAG, "设置禁用状态失败") {
            val notificationBuilder = builder ?: return@safelyRun
            notificationBuilder.setContentTitle("🚫 芝麻粒已禁用")
            if (!contentText.isNullOrEmpty()) {
                notificationBuilder.setContentText(contentText)
            }
            notificationBuilder.setProgress(0, 0, false)
            sendText(true)
        }
    }

    @JvmStatic
    fun setStatusTextExec(content: String?) {
        updateStatusText("🔥 $content 运行中...")
    }

    /**
     * 发送文本更新。 更新通知的内容文本，并重新发送通知。
     *
     * @param force 是否强制刷新
     */
    private fun sendText(force: Boolean) {
        if (!isNotificationStarted) return
        ErrorHandler.safelyRun(TAG, "发送通知失败") {
            if (!force && System.currentTimeMillis() - lastUpdateTime < 500) {
                return@safelyRun
            }
            lastUpdateTime = System.currentTimeMillis()
            
            val notificationBuilder = builder ?: return@safelyRun
            val manager = mNotifyManager ?: return@safelyRun
            
            notificationBuilder.setContentTitle(titleText)
            if (!contentText.isNullOrEmpty()) {
                notificationBuilder.setContentText(contentText)
            }
            manager.notify(NOTIFICATION_ID, notificationBuilder.build())
        }
    }

    @SuppressLint("StaticFieldLeak")
    @JvmStatic
    fun sendErrorNotification(title: String?, content: String?) {
        ErrorHandler.safelyRun(TAG, "发送错误通知失败") {
            val ctx = context ?: run {
                Log.error(TAG, "Context is null in sendErrorNotification, cannot proceed.")
                return@safelyRun
            }
            
            if (!Notify.checkPermission(ctx) || !isNotificationStarted) return@safelyRun
            
            val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return@safelyRun
            
            // ✅ minSdk 26+: 直接使用通知渠道
            val notificationChannel = NotificationChannel(CHANNEL_ID, "‼️ 芝麻粒异常通知", NotificationManager.IMPORTANCE_LOW)
            manager.createNotificationChannel(notificationChannel)
            
            val errorBuilder = NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setCategory(NotificationCompat.CATEGORY_ERROR)
                .setSmallIcon(android.R.drawable.sym_def_app_icon)
                .setLargeIcon(BitmapFactory.decodeResource(ctx.resources, android.R.drawable.sym_def_app_icon))
                .setContentTitle(title)
                .setContentText(content)
                .setSubText("芝麻粒")
                .setAutoCancel(true)
            
            if (ctx is Service) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    NotificationManagerCompat.from(ctx).notify(ERROR_NOTIFICATION_ID, errorBuilder.build())
                } else {
                    ctx.startForeground(ERROR_NOTIFICATION_ID, errorBuilder.build())
                }
            } else {
                NotificationManagerCompat.from(ctx).notify(ERROR_NOTIFICATION_ID, errorBuilder.build())
            }
            
            mNotifyManager = manager
        }
    }
}
