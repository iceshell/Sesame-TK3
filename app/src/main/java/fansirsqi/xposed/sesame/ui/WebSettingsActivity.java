package fansirsqi.xposed.sesame.ui;

import static fansirsqi.xposed.sesame.data.UIConfig.UI_OPTION_NEW;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.fasterxml.jackson.core.type.TypeReference;

import org.json.JSONException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import fansirsqi.xposed.sesame.BuildConfig;
import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.data.Config;
import fansirsqi.xposed.sesame.data.UIConfig;
import fansirsqi.xposed.sesame.data.ViewAppInfo;
import fansirsqi.xposed.sesame.entity.AlipayUser;
import fansirsqi.xposed.sesame.model.Model;
import fansirsqi.xposed.sesame.model.ModelConfig;
import fansirsqi.xposed.sesame.model.ModelField;
import fansirsqi.xposed.sesame.model.ModelFields;
import fansirsqi.xposed.sesame.model.ModelGroup;
import fansirsqi.xposed.sesame.model.SelectModelFieldFunc;
import fansirsqi.xposed.sesame.newui.WatermarkView;
import fansirsqi.xposed.sesame.task.ModelTask;
import fansirsqi.xposed.sesame.ui.dto.ModelDto;
import fansirsqi.xposed.sesame.ui.dto.ModelFieldInfoDto;
import fansirsqi.xposed.sesame.ui.dto.ModelFieldShowDto;
import fansirsqi.xposed.sesame.ui.dto.ModelGroupDto;
import fansirsqi.xposed.sesame.ui.widget.ListDialog;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.JsonUtil;
import fansirsqi.xposed.sesame.util.LanguageUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ToastUtil;
import fansirsqi.xposed.sesame.util.maps.BeachMap;
import fansirsqi.xposed.sesame.util.maps.CooperateMap;
import fansirsqi.xposed.sesame.util.maps.IdMapManager;
import fansirsqi.xposed.sesame.util.maps.MemberBenefitsMap;
import fansirsqi.xposed.sesame.util.maps.ParadiseCoinBenefitIdMap;
import fansirsqi.xposed.sesame.util.maps.ReserveaMap;
import fansirsqi.xposed.sesame.util.maps.UserMap;
import fansirsqi.xposed.sesame.util.maps.VitalityRewardsMap;
import fansirsqi.xposed.sesame.util.PortUtil;
import fansirsqi.xposed.sesame.util.StringUtil;

public class WebSettingsActivity extends BaseActivity {
    private static final String TAG = "WebSettingsActivity";
    private static final Integer EXPORT_REQUEST_CODE = 1;
    private static final Integer IMPORT_REQUEST_CODE = 2;
    private ActivityResultLauncher<Intent> exportLauncher;
    private ActivityResultLauncher<Intent> importLauncher;
    private WebView webView;
    private Context context;
    private String userId = null;
    private String userName = null;
    private final List<ModelDto> tabList = new ArrayList<>();
    private final List<ModelGroupDto> groupList = new ArrayList<>();

    @Override
    public String getBaseSubtitle() {
        return getString(R.string.settings);
    }

    @SuppressLint({"MissingInflatedId", "SetJavaScriptEnabled"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            Log.runtime(TAG, "onCreate: 开始初始化");
            super.onCreate(savedInstanceState);
            Log.runtime(TAG, "onCreate: super.onCreate完成");
            
            context = this;
            userId = null;
            userName = null;
            
            Log.runtime(TAG, "onCreate: 准备获取Intent");
            Intent intent = getIntent();
            if (intent != null) {
                userId = intent.getStringExtra("userId");
                userName = intent.getStringExtra("userName");
                intent.getBooleanExtra("debug", BuildConfig.DEBUG);
                Log.runtime(TAG, "onCreate: Intent解析完成, userId=" + userId + ", userName=" + userName);
            }
            
            Log.runtime(TAG, "onCreate: 准备初始化Model");
            Model.initAllModel();
            Log.runtime(TAG, "onCreate: Model初始化完成");
            Log.runtime(TAG, "onCreate: 准备设置UserMap");
            UserMap.setCurrentUserId(userId);
            Log.runtime(TAG, "onCreate: UserMap.setCurrentUserId完成");
            
            UserMap.load(userId);
            Log.runtime(TAG, "onCreate: UserMap.load完成");
            
            CooperateMap.getInstance(CooperateMap.class).load(userId);
            Log.runtime(TAG, "onCreate: CooperateMap加载完成");
            
            IdMapManager.getInstance(VitalityRewardsMap.class).load(this.userId);
            Log.runtime(TAG, "onCreate: VitalityRewardsMap加载完成");
            
            IdMapManager.getInstance(MemberBenefitsMap.class).load(this.userId);
            Log.runtime(TAG, "onCreate: MemberBenefitsMap加载完成");
            
            IdMapManager.getInstance(ParadiseCoinBenefitIdMap.class).load(this.userId);
            Log.runtime(TAG, "onCreate: ParadiseCoinBenefitIdMap加载完成");
            
            IdMapManager.getInstance(ReserveaMap.class).load();
            Log.runtime(TAG, "onCreate: ReserveaMap加载完成");
            
            IdMapManager.getInstance(BeachMap.class).load();
            Log.runtime(TAG, "onCreate: BeachMap加载完成");
            
            Config.load(userId);
            Log.runtime(TAG, "onCreate: Config加载完成");
            
            LanguageUtil.setLocale(this);
            Log.runtime(TAG, "onCreate: LanguageUtil设置完成");
            
            setContentView(R.layout.activity_web_settings);
            Log.runtime(TAG, "onCreate: setContentView完成");
            //处理返回键
            Log.runtime(TAG, "onCreate: 准备设置返回键处理");
            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    Log.runtime(TAG, "WebSettingsActivity.handleOnBackPressed: go back");
                    webView.goBack();
                } else {
                    Log.runtime(TAG, "WebSettingsActivity.handleOnBackPressed: save");
                    save();
                    finish();
                }
            }
            });
            Log.runtime(TAG, "onCreate: 返回键处理设置完成");

            // 初始化导出逻辑
            Log.runtime(TAG, "onCreate: 准备初始化导出逻辑");
            exportLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        PortUtil.handleExport(this, result.getData().getData(), userId);
                    }
                }
            );
            Log.runtime(TAG, "onCreate: 导出逻辑初始化完成");
            
            // 初始化导入逻辑
            Log.runtime(TAG, "onCreate: 准备初始化导入逻辑");
            importLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        PortUtil.handleImport(this, result.getData().getData(), userId);
                    }
                }
            );
            Log.runtime(TAG, "onCreate: 导入逻辑初始化完成");
            
            if (userName != null) {
                setBaseSubtitle(getString(R.string.settings) + ": " + userName);
                Log.runtime(TAG, "onCreate: 标题设置完成");
            }
            
            context = this;
            Log.runtime(TAG, "onCreate: 准备初始化WebView");
            webView = findViewById(R.id.webView);
            Log.runtime(TAG, "onCreate: WebView findViewById完成");
        WebSettings settings = webView.getSettings();
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setLoadsImagesAutomatically(true);
            settings.setDefaultTextEncodingName(StandardCharsets.UTF_8.name());
            Log.runtime(TAG, "onCreate: WebSettings配置完成");
            
            Log.runtime(TAG, "onCreate: 准备加载WebView内容");
            webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                Log.runtime(TAG, "WebView: 页面加载完成 - " + url);
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                Log.error(TAG, "WebView加载错误: code=" + errorCode + ", desc=" + description + ", url=" + failingUrl);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 强制在当前 WebView 中加载 url
                Uri requestUrl = request.getUrl();
                String scheme = requestUrl.getScheme();
                assert scheme != null;
                if (
                        scheme.equalsIgnoreCase("http")
                                || scheme.equalsIgnoreCase("https")
                                || scheme.equalsIgnoreCase("ws")
                                || scheme.equalsIgnoreCase("wss")
                ) {
                    view.loadUrl(requestUrl.toString());
                    return true;
                }
                view.stopLoading();
                Toast.makeText(context, "Forbidden Scheme:\"" + scheme + "\"", Toast.LENGTH_SHORT).show();
                return false;
            }
            });
            Log.runtime(TAG, "onCreate: WebViewClient设置完成");
            
            // 添加WebChromeClient捕获JavaScript错误
            webView.setWebChromeClient(new android.webkit.WebChromeClient() {
                @Override
                public boolean onConsoleMessage(android.webkit.ConsoleMessage consoleMessage) {
                    Log.runtime(TAG, "WebView Console [" + consoleMessage.messageLevel() + "]: " + 
                        consoleMessage.message() + " -- From line " + 
                        consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
                    return true;
                }
            });
            Log.runtime(TAG, "onCreate: WebChromeClient设置完成");
            
            if (BuildConfig.DEBUG) {
                WebView.setWebContentsDebuggingEnabled(true);
//            webView.loadUrl("http://192.168.31.69:5500/app/src/main/assets/web/index.html");
                webView.loadUrl("file:///android_asset/web/index.html");
            } else {
                webView.loadUrl("file:///android_asset/web/index.html");
            }
            Log.runtime(TAG, "onCreate: WebView loadUrl完成");
            
            webView.addJavascriptInterface(new WebViewCallback(), "HOOK");
            Log.runtime(TAG, "onCreate: JavaScript接口注册完成");

            webView.requestFocus();
            Log.runtime(TAG, "onCreate: 准备填充tabList和groupList");
            Map<String, ModelConfig> modelConfigMap = ModelTask.getModelConfigMap();
            for (Map.Entry<String, ModelConfig> configEntry : modelConfigMap.entrySet()) {
                ModelConfig modelConfig = configEntry.getValue();
                // 修复：modelFields不能为null，使用空列表
                tabList.add(new ModelDto(configEntry.getKey(), modelConfig.getName(), modelConfig.getIcon(), modelConfig.getGroup().getCode(), new ArrayList<>()));
            }
            Log.runtime(TAG, "onCreate: tabList填充完成, size=" + tabList.size());
            
            for (ModelGroup modelGroup : ModelGroup.values()) {
                groupList.add(new ModelGroupDto(modelGroup.getCode(), modelGroup.getName(), modelGroup.getIcon()));
            }
            Log.runtime(TAG, "onCreate: groupList填充完成, size=" + groupList.size());
            Log.runtime(TAG, "onCreate: 准备设置水印");
            WatermarkView watermarkView = WatermarkView.Companion.install(this);
            String tag = "用户: " + userName + "\n ID: " + userId;
            if (userName != null && userName.equals("默认") || userId == null) {
                tag = "用户: " + "未登录" + "\n ID: " + "*************";
            }
            watermarkView.setWatermarkText(tag);
            Log.runtime(TAG, "onCreate: 水印设置完成");
            
            Log.runtime(TAG, "onCreate: ✅ WebSettingsActivity初始化完成！");
        } catch (Exception e) {
            Log.error(TAG, "onCreate发生异常: " + e.getMessage());
            Log.printStackTrace(TAG, e);
            ToastUtil.showToast(this, "初始化失败: " + e.getMessage());
            finish();
        }
    }


    public class WebAppInterface {
        @JavascriptInterface
        public void onBackPressed() {
            runOnUiThread(() -> {
                if (webView.canGoBack()) {
                    webView.goBack();
                } else {
                    Log.runtime(TAG, "WebAppInterface onBackPressed: save");
                    save();
                    WebSettingsActivity.this.finish();
                }
            });
        }

        @JavascriptInterface
        public void onExit() {
            runOnUiThread(WebSettingsActivity.this::finish);
        }
    }

    private class WebViewCallback {
        @JavascriptInterface
        public String getTabs() {
            String result = JsonUtil.formatJson(tabList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getTabs: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String getBuildInfo() {
            return BuildConfig.APPLICATION_ID + ":" + BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public String getGroup() {
            String result = JsonUtil.formatJson(groupList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getGroup: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String getModelByGroup(String groupCode) {
            Collection<ModelConfig> modelConfigCollection = ModelTask.getGroupModelConfig(ModelGroup.getByCode(groupCode)).values();
            List<ModelDto> modelDtoList = new ArrayList<>();
            for (ModelConfig modelConfig : modelConfigCollection) {
                List<ModelFieldShowDto> modelFields = new ArrayList<>();
                for (ModelField<?> modelField : modelConfig.getFields().values()) {
                    modelFields.add(ModelFieldShowDto.toShowDto(modelField));
                }
                modelDtoList.add(new ModelDto(modelConfig.getCode(), modelConfig.getName(), modelConfig.getIcon(), groupCode, modelFields));
            }
            String result = JsonUtil.formatJson(modelDtoList, false);
            if (BuildConfig.DEBUG) {
                Log.runtime(TAG, "WebSettingsActivity.getModelByGroup: " + result);
            }
            return result;
        }

        @JavascriptInterface
        public String setModelByGroup(String groupCode, String modelsValue) {
            List<ModelDto> modelDtoList = JsonUtil.parseObject(modelsValue, new TypeReference<List<ModelDto>>() {
            });
            Map<String, ModelConfig> modelConfigSet = ModelTask.getGroupModelConfig(ModelGroup.getByCode(groupCode));
            for (ModelDto modelDto : modelDtoList) {
                ModelConfig modelConfig = modelConfigSet.get(modelDto.getModelCode());
                if (modelConfig != null) {
                    List<ModelFieldShowDto> modelFields = modelDto.getModelFields();
                    if (modelFields != null) {
                        for (ModelFieldShowDto newModelField : modelFields) {
                            if (newModelField != null) {
                                ModelField<?> modelField = modelConfig.getModelField(newModelField.getCode());
                                if (modelField != null) {
                                    modelField.setConfigValue(newModelField.getConfigValue());
                                }
                            }
                        }
                    }
                }
            }
            return "SUCCESS";
        }

        @JavascriptInterface
        public String getModel(String modelCode) {
            try {
                Log.runtime(TAG, "getModel调用: modelCode=" + modelCode);
                ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
                if (modelConfig != null) {
                    ModelFields modelFields = modelConfig.getFields();
                    List<ModelFieldShowDto> list = new ArrayList<>();
                    for (ModelField<?> modelField : modelFields.values()) {
                        try {
                            list.add(ModelFieldShowDto.toShowDto(modelField));
                        } catch (Exception e) {
                            Log.error(TAG, "getModel转换字段失败: field=" + modelField.getCode() + ", error=" + e.getMessage());
                            Log.printStackTrace(TAG, e);
                        }
                    }
                    String result = JsonUtil.formatJson(list, false);
                    if (BuildConfig.DEBUG) {
                        Log.runtime(TAG, "WebSettingsActivity.getModel: " + result);
                    }
                    return result;
                }
                Log.error(TAG, "getModel: modelConfig为null, modelCode=" + modelCode);
                return "[]";
            } catch (Exception e) {
                Log.error(TAG, "getModel发生异常: modelCode=" + modelCode + ", error=" + e.getMessage());
                Log.printStackTrace(TAG, e);
                return "[]";
            }
        }

        @JavascriptInterface
        public String setModel(String modelCode, String fieldsValue) {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                try {
                    ModelFields modelFields = modelConfig.getFields();
                    // 同时获取Config中的字段（用于持久化）
                    ModelFields configFields = Config.INSTANCE.modelFieldsMap.get(modelCode);
                    
                    Map<String, ModelFieldShowDto> map = JsonUtil.parseObject(fieldsValue,
                            new TypeReference<Map<String, ModelFieldShowDto>>() {
                            });
                    if (map != null) {
                        for (Map.Entry<String, ModelFieldShowDto> entry : map.entrySet()) {
                            ModelFieldShowDto newModelField = entry.getValue();
                            if (newModelField != null) {
                                ModelField<?> modelField = modelFields.get(entry.getKey());
                                if (modelField != null) {
                                    String configValue = newModelField.getConfigValue();
                                    if (configValue == null || configValue.trim().isEmpty()) {
                                        continue;
                                    }
                                    modelField.setConfigValue(configValue);
                                    
                                    // 同步到Config.modelFieldsMap
                                    if (configFields != null) {
                                        ModelField<?> configField = configFields.get(entry.getKey());
                                        if (configField != null) {
                                            try {
                                                configField.setConfigValue(configValue);
                                            } catch (Exception ex) {
                                                Log.printStackTrace("WebSettingsActivity", "同步配置字段失败: " + entry.getKey(), ex);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        return "SUCCESS";
                    }
                } catch (Exception e) {
                    Log.printStackTrace("WebSettingsActivity", e);
                }
            }
            return "FAILED";
        }

        @JavascriptInterface
        public String getField(String modelCode, String fieldCode) throws JSONException {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                ModelField<?> modelField = modelConfig.getModelField(fieldCode);
                if (modelField != null) {
                    String result = JsonUtil.formatJson(ModelFieldInfoDto.toInfoDto(modelField), false);
                    if (BuildConfig.DEBUG) {
                        Log.runtime(TAG, "WebSettingsActivity.getField: " + result);
                    }
                    return result;
                }
            }
            return null;
        }

        @JavascriptInterface
        public String setField(String modelCode, String fieldCode, String fieldValue) {
            ModelConfig modelConfig = ModelTask.getModelConfigMap().get(modelCode);
            if (modelConfig != null) {
                try {
                    ModelField<?> modelField = modelConfig.getModelField(fieldCode);
                    if (modelField != null) {
                        modelField.setConfigValue(fieldValue);
                        return "SUCCESS";
                    }
                } catch (Exception e) {
                    Log.printStackTrace(e);
                }
            }
            return "FAILED";
        }

        @JavascriptInterface
        public void Log(String log) {
            Log.record(TAG, "设置：" + log);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 1, "导出配置");
        menu.add(0, 2, 2, "导入配置");
        menu.add(0, 3, 3, "删除配置");
        menu.add(0, 4, 4, "单向好友");
        menu.add(0, 5, 5, "切换UI");
        menu.add(0, 6, 6, "保存");
        menu.add(0, 7, 7, "复制ID");
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                Intent exportIntent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                exportIntent.addCategory(Intent.CATEGORY_OPENABLE);
                exportIntent.setType("*/*");
                exportIntent.putExtra(Intent.EXTRA_TITLE, "[" + userName + "]-config_v2.json");
//                startActivityForResult(exportIntent, EXPORT_REQUEST_CODE);
                exportLauncher.launch(exportIntent);
                break;
            case 2:
                Intent importIntent = new Intent(Intent.ACTION_GET_CONTENT);
                importIntent.addCategory(Intent.CATEGORY_OPENABLE);
                importIntent.setType("*/*");
                importIntent.putExtra(Intent.EXTRA_TITLE, "config_v2.json");
//                startActivityForResult(importIntent, IMPORT_REQUEST_CODE);
                importLauncher.launch(importIntent);
                break;
            case 3:
                new AlertDialog.Builder(context)
                        .setTitle("警告")
                        .setMessage("确认删除该配置？")
                        .setPositiveButton(R.string.ok, (dialog, id) -> {
                            File userConfigDirectoryFile;
                            if (userId == null || userId.isEmpty()) {
                                userConfigDirectoryFile = Files.getDefaultConfigV2File();
                            } else {
                                userConfigDirectoryFile = Files.getUserConfigDir(userId);
                            }
                            if (Files.delFile(userConfigDirectoryFile)) {
                                Toast.makeText(this, "配置删除成功", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "配置删除失败", Toast.LENGTH_SHORT).show();
                            }
                            finish();
                        })
                        .setNegativeButton(R.string.cancel, (dialog, id) -> dialog.dismiss())
                        .create()
                        .show();
                break;
            case 4:
                ListDialog.show(this, "单向好友列表", AlipayUser.getList(user -> user.getFriendStatus() != 1), SelectModelFieldFunc.newMapInstance(), false,
                        ListDialog.ListType.SHOW);
                break;
            case 5:
                UIConfig.INSTANCE.setUiOption(UI_OPTION_NEW);
                if (UIConfig.save()) {
                    Intent intent = new Intent(this, UIConfig.INSTANCE.getTargetActivityClass());
                    intent.putExtra("userId", userId);
                    intent.putExtra("userName", userName);
                    finish();
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "切换失败", Toast.LENGTH_SHORT).show();
                }
                break;
            case 6:
                // 在调用 save() 之前，先调用 JS 函数同步 WebView 中的数据到 Java 端
                Log.runtime(TAG, "WebSettingsActivity.onOptionsItemSelected: Calling handleData() in WebView");
                webView.evaluateJavascript("if(typeof handleData === 'function'){ handleData(); } else { console.error('handleData function not found'); }", null);
                // 使用 Handler 延迟执行 save()，给 JS 一点时间完成异步操作
                // 200 毫秒是一个经验值，如果仍然有问题可以适当增加
                new Handler(Looper.getMainLooper()).postDelayed(this::save, 200); // 延迟 200 毫秒
                break;
            case 7:
                //复制userId到剪切板
                android.content.ClipboardManager cm = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clipData = ClipData.newPlainText("userId", this.userId);
                cm.setPrimaryClip(clipData);
                ToastUtil.showToastWithDelay(this, "复制成功！", 100);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void save() {
        // 已移除授权检查提示，避免每次保存配置时弹窗
        if (Config.isModify(userId)) {
            if (Config.save(userId, false)) {
                Toast.makeText(context, "保存成功！", Toast.LENGTH_SHORT).show();
                if (userId != null && !userId.isEmpty()) {
                    try {
                        Intent intent = new Intent("com.eg.android.AlipayGphone.sesame.restart");
                        intent.putExtra("userId", userId);
                        intent.putExtra("configReload", true);  // 标记为配置重新加载，而不是强制重启
                        sendBroadcast(intent);
                    } catch (Throwable th) {
                        Log.printStackTrace(th);
                    }
                }
            } else {
                Toast.makeText(context, "保存失败！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "配置未修改，无需保存！", Toast.LENGTH_SHORT).show();
        }
        if (userId != null && !userId.isEmpty()) {
            UserMap.save(userId);
            CooperateMap.getInstance(CooperateMap.class).save(userId);
        }
    }
}
