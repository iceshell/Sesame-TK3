# Sesame-TK 开发指南

**版本**: v0.3.0-rc146+  
**更新时间**: 2024-11-02

---

## 🎯 开发环境配置

### 必需软件

| 软件 | 版本 | 安装路径 |
|------|------|----------|
| Windows 10+ | 10/11 | - |
| Android Studio | 2024.1.4.8+ | D:\Android\Android Studio |
| Android SDK | Latest | D:\Android\Sdk |
| JDK | 17.0.16+ | C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot |
| Gradle | 9.1.0 | (Wrapper) |
| Kotlin | 2.2.20 | (Plugin) |

### 项目配置

#### build.gradle.kts (项目级)
```kotlin
plugins {
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.20" apply false
}
```

#### app/build.gradle.kts
```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    kotlin("kapt")
}

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 26
        targetSdk = 34
        versionCode = 146
        versionName = "0.3.0-rc146"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Xposed
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")
    
    // Android
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // JSON
    implementation("com.google.code.gson:gson:2.11.0")
    
    // Lombok (Java only)
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")
}
```

---

## 🏗️ 项目结构详解

### 根目录
```
Sesame-TK-n/
├── app/                          # 应用主模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/fansirsqi/xposed/sesame/
│   │   │   ├── res/             # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   └── test/                # 单元测试
│   ├── build.gradle.kts         # 模块构建脚本
│   └── proguard-rules.pro       # 混淆规则
├── docs/                         # 文档目录
├── log/                          # 日志输出
├── build.gradle.kts             # 项目构建脚本
├── gradle.properties            # Gradle 配置
├── settings.gradle.kts          # 项目设置
└── README.md                    # 项目说明
```

### 源代码结构
```
fansirsqi.xposed.sesame/
├── data/                        # 数据模型
│   ├── ModelFields.kt          # 字段定义
│   ├── ModelGroup.kt           # 分组配置
│   └── task/                   # 任务数据模型
├── entity/                      # 实体类
│   ├── RpcEntity.kt            # RPC 实体
│   └── AlipayUser.kt           # 用户实体
├── hook/                        # Hook 相关
│   ├── ApplicationHook.kt      # 应用 Hook 入口
│   ├── RequestManager.kt       # 请求管理
│   └── rpc/                    # RPC 相关
│       ├── bridge/
│       │   └── RpcBridge.kt    # RPC 桥接
│       └── interval/
│           └── RpcIntervalLimit.kt  # 频率限制
├── task/                        # 任务模块
│   ├── ModelTask.kt            # 任务基类
│   ├── TaskCommon.kt           # 通用任务
│   ├── antForest/              # 蚂蚁森林
│   │   ├── AntForest.kt
│   │   └── AntForestRpcCall.kt
│   ├── antFarm/                # 蚂蚁庄园
│   │   ├── AntFarm.kt
│   │   ├── AntFarmFamily.kt
│   │   └── AntFarmRpcCall.kt
│   ├── antSports/              # 运动
│   │   ├── AntSports.kt
│   │   └── AntSportsRpcCall.kt
│   └── antDodo/                # 神奇物种
│       ├── AntDodo.kt
│       └── AntDodoRpcCall.kt
├── ui/                          # UI 界面
│   ├── MainActivity.kt         # 主界面
│   ├── StringDialog.kt         # 字符串对话框
│   ├── ChoiceDialog.kt         # 选择对话框
│   └── widget/                 # 自定义组件
│       └── ContentPagerAdapter.kt
└── util/                        # 工具类
    ├── Log.kt                  # 日志工具
    ├── Status.kt               # 状态管理
    ├── NetworkUtils.kt         # 网络工具
    ├── FileUtils.kt            # 文件工具
    ├── ResChecker.kt           # 响应检查
    └── CoroutineUtils.kt       # 协程工具
```

---

## 🔌 核心机制详解

### 1. Xposed Hook 机制

#### ApplicationHook.kt
```kotlin
class ApplicationHook : IXposedHookLoadPackage {
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (lpparam.packageName != "com.eg.android.AlipayGphone") return
        
        // Hook RPC 请求
        XposedHelpers.findAndHookMethod(
            "com.alipay.mobile.framework.service.common.RpcService",
            lpparam.classLoader,
            "rpc",
            String::class.java,
            String::class.java,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    // 拦截和处理 RPC 响应
                }
            }
        )
    }
}
```

### 2. RPC 调用流程

```
用户触发 -> Task -> RpcCall -> RequestManager -> RpcBridge
                                                       |
                                                       v
                                                  Xposed Hook
                                                       |
                                                       v
                                                  支付宝 RPC
                                                       |
                                                       v
                                              返回响应 <- 服务器
```

#### RPC 实体定义
```kotlin
data class RpcEntity(
    val method: String,              // RPC 方法名
    val data: String,                // 请求参数
    val appName: String? = null,     // 应用名称
    val methodName: String? = null,  // 方法名称
    val facadeName: String? = null   // Facade 名称
)
```

#### 请求示例
```kotlin
// 收集能量
val result = RequestManager.requestString(
    RpcEntity(
        method = "alipay.antforest.forest.h5.collectEnergy",
        data = "[{\"userId\":\"$userId\",\"bubbleId\":$bubbleId}]"
    )
)

// 解析响应
val jo = JSONObject(result)
if (jo.optBoolean("success")) {
    val energy = jo.getJSONObject("data").getInt("energy")
    Log.forest("收取能量: ${energy}g")
}
```

### 3. 任务调度机制

#### TaskCommon.kt
```kotlin
object TaskCommon {
    fun start() {
        // 定时任务
        Handler(Looper.getMainLooper()).postDelayed({
            runTasks()
        }, delayMillis)
    }
    
    private fun runTasks() {
        CoroutineScope(Dispatchers.Default).launch {
            // 按顺序执行任务
            if (ModelTask.antForest.value) {
                AntForest.start()
            }
            if (ModelTask.antFarm.value) {
                AntFarm.start()
            }
            // ... 其他任务
        }
    }
}
```

### 4. 状态管理

#### Status.kt
```kotlin
object Status {
    private val statusMap = ConcurrentHashMap<String, Long>()
    
    // 检查今日是否已执行
    fun hasFlagToday(key: String): Boolean {
        val timestamp = statusMap[key] ?: return false
        return isToday(timestamp)
    }
    
    // 设置今日标记
    fun setFlagToday(key: String) {
        statusMap[key] = System.currentTimeMillis()
    }
    
    // 在每日 0 点重置
    fun resetDaily() {
        statusMap.clear()
    }
}
```

---

## 🧪 调试技巧

### 1. 日志查看

#### Log.kt
```kotlin
object Log {
    fun record(tag: String, msg: String) {
        // 记录到 log/record.log
        writeToFile("record", "[$tag] $msg")
    }
    
    fun forest(msg: String) {
        // 蚂蚁森林专用日志
        writeToFile("forest", msg)
    }
    
    fun farm(msg: String) {
        // 蚂蚁庄园专用日志
        writeToFile("farm", msg)
    }
}
```

#### 查看日志
```bash
# 查看所有日志
adb shell cat /data/data/com.eg.android.AlipayGphone/files/sesame/log/record.log

# 实时监控
adb shell tail -f /data/data/com.eg.android.AlipayGphone/files/sesame/log/record.log

# 过滤特定标签
adb shell cat /data/data/com.eg.android.AlipayGphone/files/sesame/log/record.log | grep "AntForest"
```

### 2. 断点调试

由于 Xposed 模块运行在目标应用进程中，无法直接使用 Android Studio 调试器。

**替代方案**:
1. 使用详细的日志输出
2. 在关键位置添加 Toast 提示
3. 使用 Xposed 日志查看工具

### 3. 错误处理

```kotlin
try {
    // 可能出错的代码
    val result = RequestManager.requestString(rpcEntity)
    val jo = JSONObject(result)
} catch (e: JSONException) {
    Log.printStackTrace("TAG", "JSON解析失败", e)
} catch (e: Exception) {
    Log.printStackTrace("TAG", "执行失败", e)
}
```

---

## 🔐 代码混淆

### proguard-rules.pro

```proguard
# 保留 Xposed 接口
-keep class de.robv.android.xposed.** { *; }
-keep interface de.robv.android.xposed.** { *; }

# 保留 Hook 入口
-keep class fansirsqi.xposed.sesame.hook.ApplicationHook { *; }

# 保留 RPC 实体
-keep class fansirsqi.xposed.sesame.entity.** { *; }

# 保留所有继承 ModelTask 的类
-keep class * extends fansirsqi.xposed.sesame.task.ModelTask { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
```

---

## 📦 发布流程

### 1. 版本号管理

```kotlin
// app/build.gradle.kts
defaultConfig {
    versionCode = 146          // 每次发布递增
    versionName = "0.3.0-rc146"  // 语义化版本
}
```

### 2. 构建 Release

```bash
# 清理旧构建
./gradlew clean

# 构建 Release APK
./gradlew assembleRelease

# 输出位置
# app/build/outputs/apk/release/sesame-tk-v0.3.0-rc146-release.apk
```

### 3. 测试清单

- [ ] 基本功能测试
  - [ ] 蚂蚁森林收能量
  - [ ] 蚂蚁庄园喂鸡
  - [ ] 运动签到
- [ ] 异常处理测试
  - [ ] 网络断开恢复
  - [ ] 支付宝重启
  - [ ] 配置修改
- [ ] 性能测试
  - [ ] 内存占用
  - [ ] 电池消耗
  - [ ] 响应速度
- [ ] 兼容性测试
  - [ ] 不同支付宝版本
  - [ ] 不同 Android 版本
  - [ ] LSPosed/LSPatch

### 4. 发布步骤

1. 更新版本号
2. 更新 RELEASE_NOTES.md
3. 构建并测试
4. Git 提交和打标签
5. 发布到 GitHub Releases

```bash
git add .
git commit -m "release: v0.3.0-rc146"
git tag v0.3.0-rc146
git push origin main --tags
```

---

## 🐛 常见问题

### Q: Hook 不生效？
**A**: 
1. 确认 LSPosed 已启用模块
2. 检查是否勾选支付宝
3. 重启支付宝应用
4. 查看 LSPosed 日志

### Q: RPC 调用失败？
**A**:
1. 检查网络连接
2. 确认支付宝版本兼容
3. 查看 log/error.log
4. 可能触发风控，需等待

### Q: 编译失败？
**A**:
1. 清理构建缓存: `./gradlew clean`
2. 检查 JDK 版本: `java -version`
3. 更新 Gradle: `./gradlew wrapper --gradle-version=9.1.0`
4. 同步依赖: File -> Sync Project with Gradle Files

### Q: 内存泄漏？
**A**:
1. 使用 LeakCanary 检测
2. 检查协程是否正确取消
3. 避免在静态变量中持有 Context
4. 及时释放资源

---

## 📚 参考资料

### 官方文档
- [Xposed API](https://api.xposed.info/)
- [LSPosed Wiki](https://github.com/LSPosed/LSPosed/wiki)
- [Android 开发文档](https://developer.android.com/)
- [Kotlin 文档](https://kotlinlang.org/docs/)
- [支付宝开放平台](https://opendocs.alipay.com/)

### 相关项目
- [LSPosed](https://github.com/LSPosed/LSPosed) - Xposed 框架
- [LSPatch](https://github.com/LSPosed/LSPatch) - 免 Root Hook
- [EdXposed](https://github.com/ElderDrivers/EdXposed) - 早期 Xposed 实现

### 技术博客
- [Xposed 模块开发教程](https://www.jianshu.com/p/c9d78b5f1c7a)
- [Android Hook 技术详解](https://blog.csdn.net/wxyyxc1992/article/details/17320911)
- [Kotlin 协程最佳实践](https://kotlinlang.org/docs/coroutines-guide.html)

---

## 🤝 贡献者

感谢所有为项目做出贡献的开发者！

如需贡献代码，请参考项目 README.md 中的贡献指南。

---

**最后更新**: 2024-11-02  
**维护者**: Sesame-TK Team
