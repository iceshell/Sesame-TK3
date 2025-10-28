# 🔍 rc100调试版本测试指南

## 📦 版本信息

**APK文件名**: `sesame-tk-v0.3.0-rc100-debug.apk`  
**版本号**: v0.3.0-rc100  
**编译时间**: 2025-10-28 21:13  
**Git提交数**: 100  

**APK位置**:
```
D:\Sesame-TK-n\app\build\outputs\apk\debug\sesame-tk-v0.3.0-rc100-debug.apk
```

---

## 🎯 本次修改内容

### WebSettingsActivity.onCreate() 增强

在`WebSettingsActivity.onCreate()`的**每个关键步骤**都添加了详细日志：

```java
try {
    Log.runtime(TAG, "onCreate: 开始初始化");
    super.onCreate(savedInstanceState);
    Log.runtime(TAG, "onCreate: super.onCreate完成");
    
    // Intent解析
    Log.runtime(TAG, "onCreate: 准备获取Intent");
    // ... Intent处理
    Log.runtime(TAG, "onCreate: Intent解析完成, userId=XXX, userName=XXX");
    
    // Model初始化
    Log.runtime(TAG, "onCreate: 准备初始化Model");
    Model.initAllModel();
    Log.runtime(TAG, "onCreate: Model初始化完成");
    
    // UserMap设置
    Log.runtime(TAG, "onCreate: 准备设置UserMap");
    UserMap.setCurrentUserId(userId);
    Log.runtime(TAG, "onCreate: UserMap.setCurrentUserId完成");
    
    // ... 更多步骤，每步都有日志
    
    Log.runtime(TAG, "onCreate: ✅ WebSettingsActivity初始化完成！");
} catch (Exception e) {
    Log.error(TAG, "onCreate发生异常: " + e.getMessage());
    Log.printStackTrace(TAG, e);
    ToastUtil.showToast(this, "初始化失败: " + e.getMessage());
    finish();
}
```

---

## 🚀 安装步骤

### 1️⃣ 卸载旧版本
```
设置 → 应用 → 芝麻粒 → 卸载
```

### 2️⃣ 清空日志目录
```powershell
Remove-Item "D:\Sesame-TK-n\log\*" -Recurse -Force
```

### 3️⃣ 安装新版本
```
D:\Sesame-TK-n\app\build\outputs\apk\debug\sesame-tk-v0.3.0-rc100-debug.apk
```

### 4️⃣ 重启LSPosed模块
```
LSPosed管理器 → 芝麻粒 → 重启模块
```

---

## 🧪 测试步骤

### 复现问题

1. 打开芝麻粒应用
2. 点击"设置"按钮
3. 选择任意支付宝账号
4. **观察是否闪退**

### 收集日志

**立即查看**: `D:\Sesame-TK-n\log\runtime.log`

---

## 📊 预期日志输出

### 场景A: 完整初始化成功

```log
[WebSettingsActivity]: onCreate: 开始初始化
[WebSettingsActivity]: onCreate: super.onCreate完成
[WebSettingsActivity]: onCreate: 准备获取Intent
[WebSettingsActivity]: onCreate: Intent解析完成, userId=2088xxx, userName=XXX
[WebSettingsActivity]: onCreate: 准备初始化Model
[WebSettingsActivity]: onCreate: Model初始化完成
[WebSettingsActivity]: onCreate: 准备设置UserMap
[WebSettingsActivity]: onCreate: UserMap.setCurrentUserId完成
[WebSettingsActivity]: onCreate: UserMap.load完成
[WebSettingsActivity]: onCreate: CooperateMap加载完成
[WebSettingsActivity]: onCreate: VitalityRewardsMap加载完成
[WebSettingsActivity]: onCreate: MemberBenefitsMap加载完成
[WebSettingsActivity]: onCreate: ParadiseCoinBenefitIdMap加载完成
[WebSettingsActivity]: onCreate: ReserveaMap加载完成
[WebSettingsActivity]: onCreate: BeachMap加载完成
[WebSettingsActivity]: onCreate: Config加载完成
[WebSettingsActivity]: onCreate: LanguageUtil设置完成
[WebSettingsActivity]: onCreate: setContentView完成
[WebSettingsActivity]: onCreate: 准备设置返回键处理
[WebSettingsActivity]: onCreate: 返回键处理设置完成
[WebSettingsActivity]: onCreate: 准备初始化导出逻辑
[WebSettingsActivity]: onCreate: 导出逻辑初始化完成
[WebSettingsActivity]: onCreate: 准备初始化导入逻辑
[WebSettingsActivity]: onCreate: 导入逻辑初始化完成
[WebSettingsActivity]: onCreate: 标题设置完成
[WebSettingsActivity]: onCreate: 准备初始化WebView
[WebSettingsActivity]: onCreate: WebView findViewById完成
[WebSettingsActivity]: onCreate: WebSettings配置完成
[WebSettingsActivity]: onCreate: 准备设置水印
[WebSettingsActivity]: onCreate: 水印设置完成
[WebSettingsActivity]: onCreate: ✅ WebSettingsActivity初始化完成！
```

**结论**: 初始化完全成功，如果还闪退，问题在其他地方

---

### 场景B: 在某一步崩溃

```log
[WebSettingsActivity]: onCreate: 开始初始化
[WebSettingsActivity]: onCreate: super.onCreate完成
[WebSettingsActivity]: onCreate: 准备获取Intent
[WebSettingsActivity]: onCreate: Intent解析完成, userId=2088xxx, userName=XXX
[WebSettingsActivity]: onCreate: 准备初始化Model
# 没有"Model初始化完成"
```

**结论**: 崩溃发生在`Model.initAllModel()`

---

### 场景C: 捕获到异常

```log
[WebSettingsActivity]: onCreate: 开始初始化
[WebSettingsActivity]: onCreate: super.onCreate完成
[WebSettingsActivity]: onCreate: 准备初始化Model
[WebSettingsActivity]: onCreate发生异常: XXX
java.lang.XXXException: ...
    at fansirsqi.xposed.sesame.model.Model.initAllModel(Model.java:XX)
    at fansirsqi.xposed.sesame.ui.WebSettingsActivity.onCreate(WebSettingsActivity.java:114)
    ...
```

**结论**: 完美！捕获到具体异常和堆栈

---

## 🔍 关键检查点

### 检查点1: 版本确认

在`runtime.log`中搜索：
```
onCreate: 开始初始化
```

- ✅ **找到**: 使用的是rc100版本
- ❌ **没找到**: 仍是旧版本，需要重新安装

### 检查点2: 崩溃位置

查看日志中**最后一条**`onCreate:`日志：

| 最后一条日志 | 崩溃位置 |
|-------------|---------|
| `准备初始化Model` | Model.initAllModel() |
| `准备设置UserMap` | UserMap.setCurrentUserId() |
| `准备加载Config` | Config.load() |
| `setContentView完成` | 后续代码 |
| `✅ 初始化完成` | onCreate()之外 |

### 检查点3: 异常信息

搜索关键字：
- `onCreate发生异常`
- `Exception`
- `Error`

---

## 📝 反馈格式

请按以下格式反馈：

```markdown
### 测试结果 - rc100

**1. 版本确认**
- APK文件名: sesame-tk-v0.3.0-rc100-debug.apk
- 安装时间: 2025-10-28 XX:XX
- 日志中有"onCreate: 开始初始化": 是/否

**2. 是否仍然闪退**
- [ ] 是，仍然闪退
- [ ] 否，已经正常

**3. runtime.log关键日志**
```log
# 粘贴所有包含"WebSettingsActivity"的日志
# 特别是"onCreate:"开头的日志
```

**4. 最后一条onCreate日志**
最后一条是: `onCreate: XXXXX`

**5. 有无异常信息**
```log
# 如果有"onCreate发生异常"，粘贴完整堆栈
```

**6. 其他观察**
- 闪退时机: 点击账号后[立即/延迟X秒]闪退
- 有无Toast提示: [有/无]，内容: XXX
- 能否看到设置界面: [能/不能]
```

---

## 🎯 根据日志的下一步行动

### 如果崩溃在Model.initAllModel()

**说明**: Model类初始化失败

**需要检查**:
1. Model类的构造函数
2. modelClazzList是否正确
3. 是否有类加载失败

### 如果崩溃在UserMap相关

**说明**: 用户映射设置失败

**需要检查**:
1. UserMap的静态方法
2. userId是否为null导致问题

### 如果崩溃在setContentView

**说明**: 布局文件加载失败

**需要检查**:
1. activity_web_settings.xml是否存在
2. 资源文件是否正确

### 如果看到"✅ 初始化完成"

**说明**: onCreate()成功，但之后崩溃

**需要检查**:
1. onResume()方法
2. WebView加载HTML
3. JavaScript接口

---

## 🔧 临时解决方案

如果WebSettingsActivity确实有问题，可以切换到Kotlin版本的SettingActivity：

### 修改默认UI

编辑配置文件：
```
/storage/emulated/0/Android/media/com.eg.android.AlipayGphone/sesame-TK/config/app_config.json
```

修改为：
```json
{
  "uiOption": "new"
}
```

或在SettingActivity中点击菜单选择"切换UI"。

---

**状态**: ⏳ 等待测试反馈  
**版本**: rc100  
**下次更新**: 根据日志分析结果决定
