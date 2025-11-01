# 🔍 rc104调试版本 - WebView日志捕获

## 📦 版本信息

**版本**: v0.3.0-rc104  
**APK**: `sesame-tk-v0.3.0-rc104-debug.apk`  
**编译时间**: 2025-10-28 21:46

---

## 🎯 本次修改

### 新增WebView日志捕获

```java
// 1. 页面加载完成回调
@Override
public void onPageFinished(WebView view, String url) {
    Log.runtime(TAG, "WebView: 页面加载完成 - " + url);
}

// 2. 资源加载错误回调
@Override
public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
    Log.error(TAG, "WebView加载错误: code=" + errorCode + ", desc=" + description + ", url=" + failingUrl);
}

// 3. JavaScript Console输出捕获
webView.setWebChromeClient(new WebChromeClient() {
    @Override
    public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
        Log.runtime(TAG, "WebView Console [" + consoleMessage.messageLevel() + "]: " + 
            consoleMessage.message() + " -- From line " + 
            consoleMessage.lineNumber() + " of " + consoleMessage.sourceId());
        return true;
    }
});
```

---

## 🚀 测试步骤

1. **卸载rc103**
2. **安装rc104**: `sesame-tk-v0.3.0-rc104-debug.apk`
3. **重启LSPosed模块**
4. **清空日志**: `Remove-Item "D:\Sesame-TK-n\log\*" -Recurse -Force`
5. **测试**: 打开芝麻粒 → 点击设置 → 选择账号
6. **查看日志**: `D:\Sesame-TK-n\log\runtime.log`

---

## 📊 预期日志

### 正常情况

```log
[WebSettingsActivity]: onCreate: WebView loadUrl完成
[WebSettingsActivity]: WebView: 页面加载完成 - file:///android_asset/web/index.html
[WebSettingsActivity]: WebView Console [LOG]: Vue app mounted
[WebSettingsActivity]: WebView Console [LOG]: Tabs loaded: 14
```

### 如果有错误

```log
[WebSettingsActivity]: WebView加载错误: code=-2, desc=net::ERR_FILE_NOT_FOUND, url=file:///android_asset/web/css/vant.css
[WebSettingsActivity]: WebView Console [ERROR]: Uncaught ReferenceError: Vue is not defined -- From line 278 of file:///android_asset/web/index.html
```

---

## 🔍 关键检查点

### 1. 页面是否加载完成

搜索日志：`页面加载完成`

- ✅ 找到 → 页面加载成功
- ❌ 没找到 → 页面加载失败

### 2. 是否有资源加载错误

搜索日志：`WebView加载错误`

- ✅ 找到 → 某些资源文件缺失（CSS/JS）
- ❌ 没找到 → 资源加载正常

### 3. 是否有JavaScript错误

搜索日志：`WebView Console`

查看是否有`ERROR`级别的消息

---

## 📝 反馈格式

```markdown
### 测试反馈 - rc104

**1. 页面是否空白**: 是/否

**2. runtime.log关键日志**

**页面加载**:
```log
# 搜索"页面加载完成"
```

**资源错误**:
```log
# 搜索"WebView加载错误"
```

**Console输出**:
```log
# 搜索"WebView Console"
```

**3. 观察**
- 页面状态: 空白/正常显示
- 是否有Toast提示: 是/否
```

---

**请安装rc104版本测试并反馈日志！**
