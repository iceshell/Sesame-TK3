package fansirsqi.xposed.sesame.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.fasterxml.jackson.core.type.TypeReference
import fansirsqi.xposed.sesame.BuildConfig
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.UIConfig
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.ModelField
import fansirsqi.xposed.sesame.model.ModelGroup
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.newui.WatermarkView
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.ui.dto.ModelDto
import fansirsqi.xposed.sesame.ui.dto.ModelFieldInfoDto
import fansirsqi.xposed.sesame.ui.dto.ModelFieldShowDto
import fansirsqi.xposed.sesame.ui.dto.ModelGroupDto
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.JsonUtil
import fansirsqi.xposed.sesame.util.LanguageUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.PortUtil
import fansirsqi.xposed.sesame.util.ToastUtil
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.ReserveaMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap
import java.nio.charset.StandardCharsets

class WebSettingsActivity : BaseActivity() {
    
    private lateinit var exportLauncher: ActivityResultLauncher<Intent>
    private lateinit var importLauncher: ActivityResultLauncher<Intent>
    private lateinit var webView: WebView
    private lateinit var context: Context
    private var userId: String? = null
    private var userName: String? = null
    private val tabList = ArrayList<ModelDto>()
    private val groupList = ArrayList<ModelGroupDto>()


    @SuppressLint("MissingInflatedId", "SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            Log.runtime(TAG, "onCreate: 开始初始化")
            super.onCreate(savedInstanceState)
            Log.runtime(TAG, "onCreate: super.onCreate完成")

            context = this
            userId = null
            userName = null

            Log.runtime(TAG, "onCreate: 准备获取Intent")
            intent?.let {
                userId = it.getStringExtra("userId")
                userName = it.getStringExtra("userName")
                it.getBooleanExtra("debug", BuildConfig.DEBUG)
                Log.runtime(TAG, "onCreate: Intent解析完成, userId=$userId, userName=$userName")
            }

            LanguageUtil.setLocale(this)
            Log.runtime(TAG, "onCreate: LanguageUtil设置完成")

            setContentView(R.layout.activity_web_settings)
            Log.runtime(TAG, "onCreate: setContentView完成")

            // 处理返回键（必须在onCreate中注册）
            Log.runtime(TAG, "onCreate: 准备设置返回键处理")
            onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (webView.canGoBack()) {
                        Log.runtime(TAG, "WebSettingsActivity.handleOnBackPressed: go back")
                        webView.goBack()
                    } else {
                        Log.runtime(TAG, "WebSettingsActivity.handleOnBackPressed: save")
                        save()
                        finish()
                    }
                }
            })
            Log.runtime(TAG, "onCreate: 返回键处理设置完成")

            // 初始化导出逻辑（必须在onCreate中注册）
            Log.runtime(TAG, "onCreate: 准备初始化导出逻辑")
            exportLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    PortUtil.handleExport(this, result.data?.data, userId)
                }
            }
            Log.runtime(TAG, "onCreate: 导出逻辑初始化完成")

            // 初始化导入逻辑（必须在onCreate中注册）
            Log.runtime(TAG, "onCreate: 准备初始化导入逻辑")
            importLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK && result.data != null) {
                    PortUtil.handleImport(this, result.data?.data, userId)
                }
            }
            Log.runtime(TAG, "onCreate: 导入逻辑初始化完成")

            userName?.let {
                baseSubtitle = "${getString(R.string.settings)}: $it"
                Log.runtime(TAG, "onCreate: 标题设置完成")
            }

            // 显示加载进度
            val progressBar = findViewById<ProgressBar>(R.id.progress_bar)
            val webViewTemp = findViewById<WebView>(R.id.webView)
            progressBar?.visibility = View.VISIBLE
            webViewTemp?.visibility = View.GONE
            Log.runtime(TAG, "onCreate: 开始异步加载配置数据")

            // 在后台线程加载配置数据
            Thread {
                try {
                    Log.runtime(TAG, "后台线程: 准备初始化Model")
                    Model.initAllModel()
                    Log.runtime(TAG, "后台线程: Model初始化完成")

                    UserMap.setCurrentUserId(userId)
                    Log.runtime(TAG, "后台线程: UserMap.setCurrentUserId完成")

                    UserMap.load(userId)
                    Log.runtime(TAG, "后台线程: UserMap.load完成")

                    IdMapManager.getInstance(CooperateMap::class.java).load(userId)
                    Log.runtime(TAG, "后台线程: CooperateMap加载完成")

                    IdMapManager.getInstance(VitalityRewardsMap::class.java).load(userId)
                    Log.runtime(TAG, "后台线程: VitalityRewardsMap加载完成")

                    IdMapManager.getInstance(MemberBenefitsMap::class.java).load(userId)
                    Log.runtime(TAG, "后台线程: MemberBenefitsMap加载完成")

                    IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).load(userId)
                    Log.runtime(TAG, "后台线程: ParadiseCoinBenefitIdMap加载完成")

                    IdMapManager.getInstance(ReserveaMap::class.java).load()
                    Log.runtime(TAG, "后台线程: ReserveaMap加载完成")

                    IdMapManager.getInstance(BeachMap::class.java).load()
                    Log.runtime(TAG, "后台线程: BeachMap加载完成")

                    Config.load(userId)
                    Log.runtime(TAG, "后台线程: Config加载完成")

                    // 回到主线程初始化UI
                    runOnUiThread {
                        try {
                            Log.runtime(TAG, "主线程: 开始初始化UI")
                            progressBar?.visibility = View.GONE
                            webViewTemp?.visibility = View.VISIBLE
                            initializeWebView()
                            Log.runtime(TAG, "主线程: UI初始化完成")
                        } catch (e: Exception) {
                            Log.error(TAG, "主线程: UI初始化失败")
                            Log.printStackTrace(TAG, e)
                            Toast.makeText(context, "UI初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
                            finish()
                        }
                    }
                } catch (e: Exception) {
                    Log.error(TAG, "后台线程: 配置加载失败")
                    Log.printStackTrace(TAG, e)
                    runOnUiThread {
                        progressBar?.visibility = View.GONE
                        Toast.makeText(context, "加载配置失败: ${e.message}", Toast.LENGTH_LONG).show()
                        finish()
                    }
                }
            }.start()

            Log.runtime(TAG, "onCreate: ✅ 异步加载已启动")
        } catch (e: Exception) {
            Log.error(TAG, "onCreate: 发生异常")
            Log.printStackTrace(TAG, e)
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun initializeWebView() {
        try {
            Log.runtime(TAG, "initializeWebView: 开始")
            Log.runtime(TAG, "initializeWebView: 准备初始化WebView")
            webView = findViewById(R.id.webView)
            Log.runtime(TAG, "onCreate: WebView findViewById完成")
            
            webView.settings.apply {
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                javaScriptEnabled = true
                domStorageEnabled = true
                useWideViewPort = true
                loadWithOverviewMode = true
                setSupportZoom(true)
                builtInZoomControls = true
                displayZoomControls = false
                cacheMode = WebSettings.LOAD_DEFAULT
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
                loadsImagesAutomatically = true
                defaultTextEncodingName = StandardCharsets.UTF_8.name()
            }
            Log.runtime(TAG, "onCreate: WebSettings配置完成")

            Log.runtime(TAG, "onCreate: 准备加载WebView内容")
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.runtime(TAG, "WebView: 页面加载完成 - $url")
                }

                @Deprecated("Deprecated in Java")
                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.error(TAG, "WebView加载错误: code=$errorCode, desc=$description, url=$failingUrl")
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    // 强制在当前 WebView 中加载 url
                    val requestUrl = request?.url
                    val scheme = requestUrl?.scheme
                    return when {
                        scheme.equals("http", ignoreCase = true) ||
                        scheme.equals("https", ignoreCase = true) ||
                        scheme.equals("ws", ignoreCase = true) ||
                        scheme.equals("wss", ignoreCase = true) -> {
                            view?.loadUrl(requestUrl.toString())
                            true
                        }
                        else -> {
                            view?.stopLoading()
                            Toast.makeText(context, "Forbidden Scheme:\"$scheme\"", Toast.LENGTH_SHORT).show()
                            false
                        }
                    }
                }
            }
            Log.runtime(TAG, "onCreate: WebViewClient设置完成")

            // 添加WebChromeClient捕获JavaScript错误
            webView.webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                    consoleMessage?.let {
                        Log.runtime(TAG, "WebView Console [${it.messageLevel()}]: ${it.message()} -- From line ${it.lineNumber()} of ${it.sourceId()}")
                    }
                    return true
                }
            }
            Log.runtime(TAG, "onCreate: WebChromeClient设置完成")

            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true)
                webView.loadUrl("file:///android_asset/web/index.html")
            } else {
                webView.loadUrl("file:///android_asset/web/index.html")
            }
            Log.runtime(TAG, "onCreate: WebView loadUrl完成")

            webView.addJavascriptInterface(WebViewCallback(), "HOOK")
            Log.runtime(TAG, "onCreate: JavaScript接口注册完成")

            webView.requestFocus()
            Log.runtime(TAG, "onCreate: 准备填充tabList和groupList")
            
            val modelConfigMap = Model.getModelConfigMap()
            for ((key, modelConfig) in modelConfigMap.entries) {
                tabList.add(ModelDto(key, modelConfig.name, modelConfig.icon, modelConfig.group?.code ?: "", ArrayList()))
            }
            Log.runtime(TAG, "onCreate: tabList填充完成, size=${tabList.size}")

            for (modelGroup in ModelGroup.values()) {
                groupList.add(ModelGroupDto(modelGroup.code, modelGroup.name, modelGroup.icon))
            }
            Log.runtime(TAG, "onCreate: groupList填充完成, size=${groupList.size}")
            
            Log.runtime(TAG, "onCreate: 准备设置水印")
            val watermarkView = WatermarkView.install(this)
            val tag = when {
                userName == "默认" || userId == null -> "用户: 未登录\n ID: *************"
                else -> "用户: $userName\n ID: $userId"
            }
            watermarkView.watermarkText = tag
            Log.runtime(TAG, "initializeWebView: 水印设置完成")

            Log.runtime(TAG, "initializeWebView: ✅ WebSettingsActivity初始化完成！")
        } catch (e: Exception) {
            Log.error(TAG, "initializeWebView发生异常: ${e.message}")
            Log.printStackTrace(TAG, e)
            throw e
        }
    }

    inner class WebViewCallback {
        @JavascriptInterface
        fun getTabs(): String {
            val result = JsonUtil.formatJson(tabList, false)
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getTabs: $result")
            }
            return result
        }

        @JavascriptInterface
        fun getBuildInfo(): String {
            return "${BuildConfig.APPLICATION_ID}:${BuildConfig.VERSION_NAME}"
        }

        @JavascriptInterface
        fun getGroup(): String {
            val result = JsonUtil.formatJson(groupList, false)
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getGroup: $result")
            }
            return result
        }

        @JavascriptInterface
        fun getModelByGroup(groupCode: String): String {
            val modelGroup = ModelGroup.getByCode(groupCode) ?: return "[]"
            val modelConfigCollection = Model.getGroupModelConfig(modelGroup).values
            val modelDtoList = ArrayList<ModelDto>()
            for (modelConfig in modelConfigCollection) {
                val modelFields = ArrayList<ModelFieldShowDto>()
                for (modelField in modelConfig.fields.values) {
                    modelFields.add(ModelFieldShowDto.toShowDto(modelField))
                }
                modelDtoList.add(ModelDto(modelConfig.code, modelConfig.name, modelConfig.icon, groupCode, modelFields))
            }
            val result = JsonUtil.formatJson(modelDtoList, false)
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getModelByGroup: $result")
            }
            return result
        }

        @JavascriptInterface
        fun setModelByGroup(groupCode: String, modelsValue: String): String {
            val modelDtoList = JsonUtil.parseObject(modelsValue, object : TypeReference<List<ModelDto>>() {})
            val modelGroup = ModelGroup.getByCode(groupCode) ?: return "FAILED"
            val modelConfigSet = Model.getGroupModelConfig(modelGroup)
            for (modelDto in modelDtoList) {
                val modelConfig = modelConfigSet[modelDto.modelCode]
                if (modelConfig != null) {
                    val modelFields = modelDto.modelFields
                    if (modelFields != null) {
                        for (newModelField in modelFields) {
                            if (newModelField != null) {
                                val modelField = modelConfig.getModelField(newModelField.code)
                                if (modelField != null) {
                                    modelField.setConfigValue(newModelField.configValue)
                                }
                            }
                        }
                    }
                }
            }
            return "SUCCESS"
        }

        @JavascriptInterface
        fun getModel(modelCode: String): String {
            return try {
                Log.runtime(TAG, "getModel调用: modelCode=$modelCode")
                val modelConfig = Model.getModelConfigMap()[modelCode]
                if (modelConfig != null) {
                    val modelFields = modelConfig.fields
                    val list = ArrayList<ModelFieldShowDto>()
                    for (modelField in modelFields.values) {
                        try {
                            list.add(ModelFieldShowDto.toShowDto(modelField))
                        } catch (e: Exception) {
                            Log.error(TAG, "getModel转换字段失败: field=${modelField.code}, error=${e.message}")
                            Log.printStackTrace(TAG, e)
                        }
                    }
                    val result = JsonUtil.formatJson(list, false)
                    if (BuildConfig.DEBUG) {
                        Log.runtime(TAG, "WebSettingsActivity.getModel: $result")
                    }
                    result
                } else {
                    Log.error(TAG, "getModel: modelConfig为null, modelCode=$modelCode")
                    "[]"
                }
            } catch (e: Exception) {
                Log.error(TAG, "getModel发生异常: modelCode=$modelCode, error=${e.message}")
                Log.printStackTrace(TAG, e)
                "[]"
            }
        }

        @JavascriptInterface
        fun setModel(modelCode: String, fieldsValue: String): String {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                try {
                    val modelFields = modelConfig.fields
                    val map = JsonUtil.parseObject(fieldsValue, object : TypeReference<Map<String, ModelFieldShowDto>>() {})
                    if (map != null) {
                        for ((key, newModelField) in map) {
                            if (newModelField != null) {
                                val modelField = modelFields[key]
                                if (modelField != null) {
                                    val configValue = newModelField.configValue
                                    if (configValue == null || configValue.trim().isEmpty()) {
                                        continue
                                    }
                                    try {
                                        Log.runtime(TAG, "setModel: 设置字段 $modelCode.$key = $configValue, 字段类型=${modelField.javaClass.simpleName}")
                                        modelField.setConfigValue(configValue)
                                    } catch (e: ClassCastException) {
                                        Log.error(TAG, "setModel: 字段类型转换失败 $modelCode.$key, 字段类=${modelField.javaClass.name}, valueType=${modelField.getType()}")
                                        Log.printStackTrace(TAG, e)
                                    }
                                }
                            }
                        }
                        return "SUCCESS"
                    }
                } catch (e: Exception) {
                    Log.printStackTrace("WebSettingsActivity", e)
                }
            }
            return "FAILED"
        }

        @JavascriptInterface
        fun getField(modelCode: String, fieldCode: String): String? {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                val modelField = modelConfig.getModelField(fieldCode)
                if (modelField != null) {
                    val result = JsonUtil.formatJson(ModelFieldInfoDto.toInfoDto(modelField), false)
                    if (BuildConfig.DEBUG) {
                        Log.runtime(TAG, "WebSettingsActivity.getField: $result")
                    }
                    return result
                }
            }
            return null
        }

        @JavascriptInterface
        fun setField(modelCode: String, fieldCode: String, fieldValue: String): String {
            val modelConfig = Model.getModelConfigMap()[modelCode]
            if (modelConfig != null) {
                try {
                    val modelField = modelConfig.getModelField(fieldCode)
                    if (modelField != null) {
                        modelField.setConfigValue(fieldValue)
                        return "SUCCESS"
                    }
                } catch (e: Exception) {
                    Log.printStackTrace(e)
                }
            }
            return "FAILED"
        }

        @JavascriptInterface
        fun Log(log: String) {
            Log.record(TAG, "设置：$log")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 1, "导出配置")
        menu.add(0, 2, 2, "导入配置")
        menu.add(0, 3, 3, "删除配置")
        menu.add(0, 4, 4, "单向好友")
        menu.add(0, 5, 5, "切换UI")
        menu.add(0, 6, 6, "保存")
        menu.add(0, 7, 7, "复制ID")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, "[$userName]-config_v2.json")
                }
                exportLauncher.launch(exportIntent)
            }
            2 -> {
                val importIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                    putExtra(Intent.EXTRA_TITLE, "config_v2.json")
                }
                importLauncher.launch(importIntent)
            }
            3 -> {
                AlertDialog.Builder(context)
                    .setTitle("警告")
                    .setMessage("确认删除该配置？")
                    .setPositiveButton(R.string.ok) { _, _ ->
                        val currentUserId = userId
                        val userConfigDirectoryFile = if (currentUserId.isNullOrEmpty()) {
                            Files.getDefaultConfigV2File()
                        } else {
                            Files.getUserConfigDir(currentUserId)
                        }
                        if (Files.delFile(userConfigDirectoryFile)) {
                            Toast.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
                    .create()
                    .show()
            }
            4 -> {
                ListDialog.show(
                    this, "单向好友列表",
                    AlipayUser.getList { user -> user.friendStatus != 1 },
                    SelectModelFieldFunc.newMapInstance(), false,
                    ListDialog.ListType.SHOW
                )
            }
            5 -> {
                UIConfig.INSTANCE.uiOption = UIConfig.UI_OPTION_NEW
                if (UIConfig.save()) {
                    val intent = Intent(this, UIConfig.INSTANCE.targetActivityClass).apply {
                        putExtra("userId", userId)
                        putExtra("userName", userName)
                    }
                    finish()
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "切换失败", Toast.LENGTH_SHORT).show()
                }
            }
            6 -> {
                // 在调用 save() 之前，先调用 JS 函数同步 WebView 中的数据到 Java 端
                Log.runtime(TAG, "WebSettingsActivity.onOptionsItemSelected: Calling handleData() in WebView")
                webView.evaluateJavascript("if(typeof handleData === 'function'){ handleData(); } else { console.error('handleData function not found'); }", null)
                // 使用 Handler 延迟执行 save()，给 JS 一点时间完成异步操作
                Handler(Looper.getMainLooper()).postDelayed({ save() }, 200) // 延迟 200 毫秒
            }
            7 -> {
                // 复制userId到剪切板
                val cm = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipData = ClipData.newPlainText("userId", userId)
                cm.setPrimaryClip(clipData)
                ToastUtil.showToastWithDelay(this, "复制成功！", 100)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save() {
        // 已移除授权检查提示，避免每次保存配置时弹窗
        // 强制保存配置，避免isModify检测失败导致不保存
        if (Config.save(userId, true)) {
            Toast.makeText(context, "保存成功！", Toast.LENGTH_SHORT).show()
            if (!userId.isNullOrEmpty()) {
                try {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart").apply {
                        putExtra("userId", userId)
                        putExtra("configReload", true) // 标记为配置重新加载，而不是强制重启
                    }
                    sendBroadcast(intent)
                } catch (th: Throwable) {
                    Log.printStackTrace(th)
                }
            }
        } else {
            Toast.makeText(context, "保存失败！", Toast.LENGTH_SHORT).show()
        }
        val currentUserId = userId
        if (!currentUserId.isNullOrEmpty()) {
            UserMap.save(currentUserId)
            IdMapManager.getInstance(CooperateMap::class.java).save(currentUserId)
        }
    }

    companion object {
        private const val TAG = "WebSettingsActivity"
        private const val EXPORT_REQUEST_CODE = 1
        private const val IMPORT_REQUEST_CODE = 2
    }
}
