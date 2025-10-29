package fansirsqi.xposed.sesame.ui

import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import fansirsqi.xposed.sesame.R
import fansirsqi.xposed.sesame.data.Config
import fansirsqi.xposed.sesame.data.UIConfig
import fansirsqi.xposed.sesame.entity.AlipayUser
import fansirsqi.xposed.sesame.model.Model
import fansirsqi.xposed.sesame.model.ModelConfig
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc
import fansirsqi.xposed.sesame.newui.WatermarkView
import fansirsqi.xposed.sesame.task.ModelTask
import fansirsqi.xposed.sesame.ui.widget.ContentPagerAdapter
import fansirsqi.xposed.sesame.ui.widget.ListDialog
import fansirsqi.xposed.sesame.ui.widget.TabAdapter
import fansirsqi.xposed.sesame.ui.widget.TabAdapter.OnTabClickListener
import fansirsqi.xposed.sesame.util.Files
import fansirsqi.xposed.sesame.util.LanguageUtil
import fansirsqi.xposed.sesame.util.Log
import fansirsqi.xposed.sesame.util.PortUtil
import fansirsqi.xposed.sesame.util.StringUtil
import fansirsqi.xposed.sesame.util.ToastUtil
import fansirsqi.xposed.sesame.util.maps.BeachMap
import fansirsqi.xposed.sesame.util.maps.CooperateMap
import fansirsqi.xposed.sesame.util.maps.IdMapManager
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap
import fansirsqi.xposed.sesame.util.maps.ReserveaMap
import fansirsqi.xposed.sesame.util.maps.UserMap
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap
import java.io.File

class SettingActivity : BaseActivity() {
    private var exportLauncher: ActivityResultLauncher<Intent>? = null
    private var importLauncher: ActivityResultLauncher<Intent>? = null
    private var userId: String? = null // 用户 ID
    private var userName: String? = null // 用户名

    protected override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 初始化用户信息
        this.userId = null
        this.userName = null
        val intent = getIntent()
        if (intent != null) {
            this.userId = intent.getStringExtra("userId")
            this.userName = intent.getStringExtra("userName")
        }

        // 初始化各种配置数据
        Model.initAllModel()
        UserMap.setCurrentUserId(this.userId)
        UserMap.load(this.userId)
        IdMapManager.getInstance(CooperateMap::class.java).load(this.userId)
        IdMapManager.getInstance(VitalityRewardsMap::class.java).load(this.userId)
        IdMapManager.getInstance(MemberBenefitsMap::class.java).load(this.userId)
        IdMapManager.getInstance(ParadiseCoinBenefitIdMap::class.java).load(this.userId)
        IdMapManager.getInstance(ReserveaMap::class.java).load()
        IdMapManager.getInstance(BeachMap::class.java).load()
        Config.load(this.userId)
        // 设置语言和布局
        LanguageUtil.setLocale(this)
        setContentView(R.layout.activity_settings)
        // 处理返回键
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                this@SettingActivity.save()
                finish()
            }
        })
        // 初始化导出逻辑
        exportLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    PortUtil.handleExport(this@SettingActivity, uri, userId)
                }
            }
        }
        // 初始化导入逻辑
        importLauncher = registerForActivityResult(
            StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.data?.let { uri ->
                    PortUtil.handleImport(this@SettingActivity, uri, userId)
                }
            }
        }
        // 设置副标题
        if (this.userName != null) {
            baseSubtitle = getString(R.string.settings) + ": " + this.userName
        }
        initializeTabs()
        val watermarkView = WatermarkView.install(this)
        var tag = "用户: " + userName + "\n ID: " + userId
        if (userName == "默认" || userId == null) {
            tag = "用户: " + "未登录" + "\n ID: " + "*************"
        }
        watermarkView.watermarkText = tag
    }

    private fun initializeTabs() {
        try {
            val recyclerTabList = findViewById<RecyclerView>(R.id.recycler_tab_list)
            recyclerTabList.setLayoutManager(LinearLayoutManager(this))
            val modelConfigMap = Model.getModelConfigMap()
            val tabTitles: MutableList<String> = ArrayList<String>()
            for (config in modelConfigMap.values) {
                config.name?.let { tabTitles.add(it) }
            }
            val tabAdapter = TabAdapter(this, tabTitles, OnTabClickListener { position: Int ->
                val viewPager = findViewById<ViewPager2>(R.id.view_pager_content)
                viewPager.setCurrentItem(position, true)
            })
            recyclerTabList.setAdapter(tabAdapter)
            val viewPager = findViewById<ViewPager2>(R.id.view_pager_content)
            val contentAdapter =
                ContentPagerAdapter(getSupportFragmentManager(), lifecycle, modelConfigMap)
            viewPager.setAdapter(contentAdapter)
            viewPager.setUserInputEnabled(false) // 禁止用户手动滑动
            viewPager.registerOnPageChangeCallback(object : OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    recyclerTabList.smoothScrollToPosition(position)
                    tabAdapter.setSelectedPosition(position)
                }
            })
        } catch (t: Throwable) {
            Log.error(TAG, "初始化Tabs失败: " + t.message)
            Log.printStackTrace(TAG, t)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // 创建菜单选项
        menu.add(0, 1, 1, "导出配置")
        menu.add(0, 2, 2, "导入配置")
        menu.add(0, 3, 3, "删除配置")
        menu.add(0, 4, 4, "单向好友")
        menu.add(0, 5, 5, "切换WEBUI")
        menu.add(0, 6, 6, "保存")
        menu.add(0, 7, 7, "复制ID")
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // 处理菜单项点击事件
        when (item.getItemId()) {
            1 -> {
                val exportIntent = Intent(Intent.ACTION_CREATE_DOCUMENT)
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE)
                exportIntent.setType("*/*")
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + this.userName + "]-config_v2.json")
                exportLauncher?.launch(exportIntent)
            }

            2 -> {
                val importIntent = Intent(Intent.ACTION_GET_CONTENT)
                importIntent.addCategory(Intent.CATEGORY_OPENABLE)
                importIntent.setType("*/*")
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json")
                importLauncher?.launch(importIntent)
            }

            3 -> AlertDialog.Builder(this)
                .setTitle("警告")
                .setMessage("确认删除该配置？")
                .setPositiveButton(
                    R.string.ok,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int ->
                        val userConfigDirectoryFile: File?
                        userConfigDirectoryFile = if (this.userId.isNullOrEmpty()) {
                            Files.getDefaultConfigV2File()
                        } else {
                            Files.getUserConfigDir(this.userId ?: "")
                        }
                        if (Files.delFile(userConfigDirectoryFile)) {
                            ToastUtil.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show()
                        } else {
                            ToastUtil.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show()
                        }
                        finish()
                    })
                .setNegativeButton(
                    R.string.cancel,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, id: Int -> dialog?.dismiss() })
                .create()
                .show()

            4 -> ListDialog.show(
                this,
                "单向好友列表",
                AlipayUser.getList(AlipayUser.Filter { user -> user.friendStatus != 1 }),
                SelectModelFieldFunc.newMapInstance(),
                false,
                ListDialog.ListType.SHOW
            )

            5 -> {
                UIConfig.INSTANCE.uiOption = UIConfig.UI_OPTION_WEB
                if (UIConfig.save()) {
                    val intent = Intent(this, UIConfig.INSTANCE.targetActivityClass)
                    intent.putExtra("userId", this.userId)
                    intent.putExtra("userName", this.userName)
                    finish()
                    startActivity(intent)
                } else {
                    ToastUtil.makeText(this, "切换失败", Toast.LENGTH_SHORT).show()
                }
            }

            6 -> this.save()
            7 -> {
                //复制userId到剪切板
                val cm = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                val clipData = ClipData.newPlainText("userId", this.userId)
                cm.setPrimaryClip(clipData)
                ToastUtil.showToastWithDelay(this, "复制成功！", 100)
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun save() {
        try {
//            if (!ViewAppInfo.INSTANCE.getVeriftag()) {
//                ToastUtil.showToastWithDelay(this, "非内测用户！", 100);
//            }
            if (Config.isModify(this.userId) && Config.save(this.userId, false)) {
                ToastUtil.showToastWithDelay(this, "保存成功！", 100)
                if (!this.userId.isNullOrEmpty()) {
                    val intent = Intent("com.eg.android.AlipayGphone.sesame.restart")
                    intent.putExtra("userId", this.userId)
                    sendBroadcast(intent)
                }
            }
            if (!this.userId.isNullOrEmpty()) {
                UserMap.save(this.userId)
                IdMapManager.getInstance(CooperateMap::class.java).save(this.userId)
            }
        } catch (th: Throwable) {
            Log.printStackTrace(th)
        }
    }

    companion object {
        private val TAG: String = SettingActivity::class.java.getSimpleName()
    }
}
