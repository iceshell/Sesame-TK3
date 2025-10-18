package fansirsqi.xposed.sesame.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.newutil.MMKVUtil
import fansirsqi.xposed.sesame.util.FansirsqiUtil.getFolderList
import fansirsqi.xposed.sesame.util.Files
import io.github.libxposed.service.XposedService
import java.util.UUID


@SuppressLint("StaticFieldLeak")
object ViewAppInfo {
    val TAG: String = ViewAppInfo::class.java.simpleName
    var context: Context? = null
    var appTitle: String = ""
    var appVersion: String = ""
    var appBuildTarget: String = ""
    var appBuildNumber: String = ""
    var verifyId: String = ""
    var veriftag: Boolean = false

    var verifuids: List<String> = listOf()

    private var _service: XposedService? = null
    val service get() = _service

    @SuppressLint("HardwareIds")
    val emojiList = listOf(
        "🍅", "🍓", "🥓", "🍂", "🍚", "🌰", "🟢", "🌴",
        "🥗", "🧀", "🥩", "🍍", "🌶️", "🍲", "🍆", "🥕",
        "✨", "🍑", "🍘", "🍀", "🥞", "🍈", "🥝", "🧅",
        "🌵", "🌾", "🥜", "🍇", "🌭", "🥑", "🥐", "🥖",
        "🍊", "🌽", "🍉", "🍖", "🍄", "🥚", "🥙", "🥦",
        "🍌", "🍱", "🍏", "🍎", "🌲", "🌿", "🍁", "🍒",
        "🥔", "🌯", "🌱", "🍐", "🍞", "🍳", "🍙", "🍋",
        "🍗", "🌮", "🍃", "🥘", "🥒", "🧄", "🍠", "🥥", "📦"
    )

    @Volatile
    internal var runType: RunType = RunType.DISABLE
        @Synchronized set

    @JvmStatic
    fun getRunType(): RunType = runType

    /**
     * 初始化 ViewAppInfo，设置应用的相关信息，如版本号、构建日期等
     *
     * @param context 上下文对象，用于获取应用的资源信息
     */
    @SuppressLint("HardwareIds")
    fun init(context: Context) {
        Log.d(TAG, "app data init")
        if (ViewAppInfo.context == null) {
            ViewAppInfo.context = context
            MMKVUtil.init()
            val kv = MMKVUtil.getMMKV("sesame-tk")
            verifyId = kv.decodeString("verify").takeIf { !it.isNullOrEmpty() }
                ?: UUID.randomUUID().toString().replace("-", "").also { kv.encode("verify", it) }
            verifuids = getFolderList(Files.CONFIG_DIR.absolutePath)
            appBuildNumber = BuildConfig.VERSION_CODE.toString()
            appTitle = context.getString(R.string.app_name)
            appBuildTarget = BuildConfig.BUILD_DATE + " " + BuildConfig.BUILD_TIME + " ⏰"
            try {
                appVersion = "${BuildConfig.VERSION_NAME} " + emojiList.random()
            } catch (e: Exception) {
                Log.e(TAG, "init: ", e)
            }
            runType = RunType.LOADED
        }
//        XposedServiceHelper.registerListener(object : XposedServiceHelper.OnServiceListener {
//            override fun onServiceBind(service: XposedService) {
//                XposedBridge.log("XposedScope onServiceBind: $service")
//                _service = service
//                XposedBridge.log("Framework: ${service.frameworkName} ${service.frameworkVersion}")
//                XposedBridge.log("API: ${service.apiVersion} FrameworkVersion${service.frameworkVersionCode}")
//                // 服务连接成功 → 模块已激活
//                runType = RunType.ACTIVE
//            }
//
//            override fun onServiceDied(service: XposedService) {
//                XposedBridge.log("Service died: ${service.frameworkName}")
//                if (_service == service) {
//                    _service = null
//                    // 服务断开，但模块仍处于加载状态（代码仍在运行）
//                    // 所以回退到 LOADED，而不是 DISABLE
//                    runType = RunType.LOADED
//                }
//            }
//        })
    }
}