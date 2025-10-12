package fansirsqi.xposed.sesame.ui;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.view.OnApplyWindowInsetsListener;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.core.content.ContextCompat;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;

import java.io.File;

import fansirsqi.xposed.sesame.R;
import fansirsqi.xposed.sesame.newui.WatermarkView;
import fansirsqi.xposed.sesame.util.Files;
import fansirsqi.xposed.sesame.util.LanguageUtil;
import fansirsqi.xposed.sesame.util.Log;
import fansirsqi.xposed.sesame.util.ToastUtil;

public class HtmlViewerActivity extends BaseActivity {
    private static final String TAG = HtmlViewerActivity.class.getSimpleName();
    MyWebView mWebView;
    ProgressBar progressBar;
    private Uri uri;
    private Boolean canClear;
    WebSettings settings = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LanguageUtil.setLocale(this);
        setContentView(R.layout.activity_html_viewer);
        WatermarkView.Companion.install(this);
        // 初始化 WebView 和进度条
        mWebView = findViewById(R.id.mwv_webview);
        progressBar = findViewById(R.id.pgb_webview);

        setupWebView();
        settings = mWebView.getSettings();

        // 安全设置 WebView
        try {
            if (mWebView != null) {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                    try {
                        WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
                    } catch (Exception e) {
                        Log.error(TAG, "设置夜间模式失败: " + e.getMessage());
                        Log.printStackTrace(TAG, e);
                    }
                }

                settings.setJavaScriptEnabled(false);
                settings.setDomStorageEnabled(false);
                progressBar.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.selection_color)));
                mWebView.setBackgroundColor(ContextCompat.getColor(this, R.color.background));
            }
        } catch (Exception e) {
            Log.error(TAG, "WebView初始化异常: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }

        View contentView = findViewById(android.R.id.content);

        ViewCompat.setOnApplyWindowInsetsListener(contentView, new OnApplyWindowInsetsListener() {
            @NonNull
            @Override
            public WindowInsetsCompat onApplyWindowInsets(@NonNull View v, @NonNull WindowInsetsCompat insets) {
                int systemBarsBottom = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;

                mWebView.setPadding(
                        mWebView.getPaddingLeft(),
                        mWebView.getPaddingTop(),
                        mWebView.getPaddingRight(),
                        systemBarsBottom
                );

                return insets;
            }
        });
    }

    /**
     * 设置 WebView 的 WebChromeClient 和进度变化监听
     */
    private void setupWebView() {
        mWebView.setWebChromeClient(
                new WebChromeClient() {
                    @SuppressLint("WrongConstant")
                    @Override
                    public void onProgressChanged(WebView view, int progress) {
                        progressBar.setProgress(progress);
                        if (progress < 100) {
                            setBaseSubtitle("Loading...");
                            progressBar.setVisibility(View.VISIBLE);
                        } else {
                            setBaseSubtitle(mWebView.getTitle());
                            progressBar.setVisibility(View.GONE);
                        }
                    }
                });
    }

    private static String toJsString(String s) {
        if (s == null) return "''";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        sb.append('\'');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\'': sb.append("\\'"); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\f': sb.append("\\f"); break;
                case '\b': sb.append("\\b"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int)c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('\'');
        return sb.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static String readAllTextSafe(String path) {
        try {
            java.nio.charset.Charset cs = java.nio.charset.StandardCharsets.UTF_8;
            byte[] data = java.nio.file.Files.readAllBytes(new File(path).toPath());
            return new String(data, cs);
        } catch (Throwable t) {
            return "";
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onResume() {
        super.onResume();
        // 安全设置WebView
        try {
            Intent intent = getIntent();// 获取传递过来的 Intent
            if (intent != null) {
                if (mWebView != null) {
                    settings.setSupportZoom(true); // 支持缩放
                    settings.setBuiltInZoomControls(true); // 启用内置缩放机制
                    settings.setDisplayZoomControls(false); // 不显示缩放控件
                    settings.setUseWideViewPort(true);// 启用触摸缩放
                    settings.setLoadWithOverviewMode(true);//概览模式加载
                    settings.setTextZoom(85);
                    // 可选夜间模式设置
                    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                        try {
                            WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true);
                        } catch (Exception e) {
                            Log.error(TAG, "设置夜间模式失败: " + e.getMessage());
                            Log.printStackTrace(TAG, e);
                        }
                    }
                }
                configureWebViewSettings(intent, settings);
                uri = intent.getData();
                if (uri != null) {
//                    mWebView.loadUrl(uri.toString());
/// 日志实时显示 begin
                    settings.setJavaScriptEnabled(true);
                    settings.setDomStorageEnabled(true); // 可选
                    mWebView.loadUrl("file:///android_asset/log_viewer.html");
                    mWebView.setWebChromeClient(new WebChromeClient() {
                        @RequiresApi(api = Build.VERSION_CODES.O)
                        @Override
                        public void onProgressChanged(WebView view, int progress) {
                            progressBar.setProgress(progress);
                            if (progress < 100) {
                                setBaseSubtitle("Loading...");
                                progressBar.setVisibility(View.VISIBLE);
                            } else {
                                setBaseSubtitle(mWebView.getTitle());
                                progressBar.setVisibility(View.GONE);

                                // ★★ 页面已就绪：在后台线程中读取文件并灌入 ★★
                                if (uri != null && "file".equalsIgnoreCase(uri.getScheme())) {
                                    String path = uri.getPath();
                                    if (path != null && path.endsWith(".log")) {
                                        // 在后台线程中执行文件读取
                                        new Thread(() -> {
                                            try {
                                                String all = readAllTextSafe(path); // 后台读取文件
                                                String jsArg = toJsString(all);     // 转换为 JS 字符串
                                                
                                                // 切回主线程执行 WebView 操作
                                                runOnUiThread(() -> {
                                                    try {
                                                        if (mWebView != null) {
                                                            mWebView.evaluateJavascript("setFullText(" + jsArg + ")", null);
                                                            
                                                            // 启动增量监听
                                                            ((MyWebView) mWebView).startWatchingIncremental(path);
                                                        }
                                                    } catch (Exception e) {
                                                        Log.error(TAG, "WebView 操作失败: " + e.getMessage());
                                                        Log.printStackTrace(TAG, e);
                                                    }
                                                });
                                            } catch (Exception e) {
                                                Log.error(TAG, "后台读取文件失败: " + e.getMessage());
                                                Log.printStackTrace(TAG, e);
                                                
                                                // 即使文件读取失败，也要启动监听
                                                runOnUiThread(() -> {
                                                    try {
                                                        if (mWebView != null) {
                                                            ((MyWebView) mWebView).startWatchingIncremental(path);
                                                        }
                                                    } catch (Exception ex) {
                                                        Log.error(TAG, "启动文件监听失败: " + ex.getMessage());
                                                    }
                                                });
                                            }
                                        }).start();
                                    }
                                }
                            }
                        }
                    });
/// 日志实时显示 end
                }
                canClear = intent.getBooleanExtra("canClear", false);
            }
        } catch (Exception e) {
            Log.error(TAG, "WebView设置异常: " + e.getMessage());
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 配置 WebView 的设置项
     *
     * @param intent   传递的 Intent
     * @param settings WebView 的设置
     */
    private void configureWebViewSettings(Intent intent, WebSettings settings) {
        if (intent.getBooleanExtra("nextLine", true)) {
            settings.setTextZoom(85);
            settings.setUseWideViewPort(false);
        } else {
            settings.setTextZoom(85);
            settings.setUseWideViewPort(true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 创建菜单选项
        menu.add(0, 1, 1, getString(R.string.export_file));
        if (canClear) {
            menu.add(0, 2, 2, getString(R.string.clear_file));
        }
        menu.add(0, 3, 3, getString(R.string.open_with_other_browser));
        menu.add(0, 4, 4, getString(R.string.copy_the_url));
        menu.add(0, 5, 5, getString(R.string.scroll_to_top));
        menu.add(0, 6, 6, getString(R.string.scroll_to_bottom));
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                // 导出文件
                exportFile();
                break;
            case 2:
                // 清空文件
                clearFile();
                break;
            case 3:
                // 使用其他浏览器打开
                openWithBrowser();
                break;
            case 4:
                // 复制 URL 到剪贴板
                copyUrlToClipboard();
                break;
            case 5:
                // 滚动到顶部
                mWebView.scrollTo(0, 0);
                break;
            case 6:
                // 滚动到底部
                mWebView.scrollToBottom();
                break;
        }
        return true;
    }

    /**
     * 导出当前文件
     */
    private void exportFile() {
        try {
            if (uri != null) {
                String path = uri.getPath();
                Log.runtime(TAG, "URI path: " + path);
                if (path != null) {
                    File exportFile = Files.exportFile(new File(path),true);
                    if (exportFile != null && exportFile.exists()) {
                        ToastUtil.showToast(getString(R.string.file_exported) + exportFile.getPath());
                    } else {
                        Log.runtime(TAG, "导出失败，exportFile 对象为 null 或不存在！");
                    }
                } else {
                    Log.runtime(TAG, "路径为 null！");
                }
            } else {
                Log.runtime(TAG, "URI 为 null！");
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 清空当前文件
     */
    private void clearFile() {
        try {
            if (uri != null) {
                String path = uri.getPath();
                if (path != null) {
                    File file = new File(path);
                    if (Files.clearFile(file)) {
                        ToastUtil.makeText(this, "文件已清空", Toast.LENGTH_SHORT).show();
                        mWebView.reload();
                    }
                }
            }
        } catch (Exception e) {
            Log.printStackTrace(TAG, e);
        }
    }

    /**
     * 使用其他浏览器打开当前 URL
     */
    private void openWithBrowser() {
        if (uri != null) {
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            } else if ("file".equalsIgnoreCase(scheme)) {
                ToastUtil.makeText(this, "该文件不支持用浏览器打开", Toast.LENGTH_SHORT).show();
            } else {
                ToastUtil.makeText(this, "不支持用浏览器打开", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 复制当前 WebView 的 URL 到剪贴板
     */
    private void copyUrlToClipboard() {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(null, mWebView.getUrl()));
            ToastUtil.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWebView instanceof MyWebView) {
            ((MyWebView) mWebView).stopWatchingIncremental();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mWebView instanceof MyWebView) {
            ((MyWebView) mWebView).stopWatchingIncremental();
        }
    }

    @Override
    protected void onDestroy() {
        // 先停止文件监听，再做 WebView 清理，最后再 super
        if (mWebView instanceof MyWebView) {
            ((MyWebView) mWebView).stopWatchingIncremental();
        }
        if (mWebView != null) {
            try {
                mWebView.loadUrl("about:blank");
                mWebView.stopLoading();
                mWebView.setWebChromeClient(null);
                mWebView.setWebViewClient(null);
                mWebView.destroy();
            } catch (Throwable ignore) {}
        }
        super.onDestroy();
    }


}