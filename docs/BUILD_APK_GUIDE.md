# 构建APK完整指南

> **问题**: gradlew命令行构建可能遇到"没有主清单属性"错误  
> **解决方案**: 使用Android Studio构建（推荐）

---

## ⚠️ 当前情况

由于项目的 `gradle-wrapper.jar` 需要重新生成，命令行 `./gradlew` 暂时无法使用。

**推荐使用 Android Studio 进行首次构建**，AS会自动修复wrapper问题。

---

## 🚀 方法1: Android Studio 构建（推荐）⭐

### 步骤1: 打开项目

1. 启动 **Android Studio 2025.1.4.8**
2. 选择 `File` → `Open`
3. 选择目录: `D:\Sesame-TK-n`
4. 点击 `OK`

### 步骤2: 同步Gradle

AS会自动提示:
```
Gradle files have changed since last sync
[Sync Now] [File an Issue]
```

**点击 "Sync Now"** 并等待：
- 下载 Gradle 9.1.0 (~140MB，2-5分钟)
- 下载依赖包 (~300MB，3-8分钟)
- 配置项目结构

**成功标志**:
```
Gradle sync finished in 3m 45s
```

### 步骤3: 构建APK

#### 方式A: 使用Build菜单

1. 点击菜单栏 `Build`
2. 选择 `Build Bundle(s) / APK(s)`
3. 选择 `Build APK(s)`

AS会开始构建，底部显示进度：
```
Building 'app' ...
Executing tasks: [assembleDebug]
```

#### 方式B: 使用Build Variants

1. 点击左侧 `Build Variants` 标签
2. 选择构建变体:
   - `normalDebug` - Normal版本Debug
   - `compatibleDebug` - Compatible版本Debug
   - `normalRelease` - Normal版本Release
   - `compatibleRelease` - Compatible版本Release
3. 点击菜单 `Build` → `Make Project` 或按 `Ctrl+F9`

### 步骤4: 查看构建结果

构建成功后，AS会显示通知：

```
Build APK(s) completed successfully in 2m 15s
    Normal Debug APK: app-normal-debug.apk
    Compatible Debug APK: app-compatible-debug.apk
[locate] [analyze]
```

点击 **[locate]** 打开APK所在目录。

---

## 📁 APK输出位置

### Normal版本

```
D:\Sesame-TK-n\app\build\outputs\apk\normal\debug\
├── Sesame-TK-Normal-vX.X.X-debug.apk
└── output-metadata.json
```

### Compatible版本

```
D:\Sesame-TK-n\app\build\outputs\apk\compatible\debug\
├── Sesame-TK-Compatible-vX.X.X-debug.apk
└── output-metadata.json
```

---

## 🔧 方法2: 修复gradlew后命令行构建

### 步骤1: 在AS中同步一次

按照上面的步骤在Android Studio中同步Gradle，这会自动修复 `gradle-wrapper.jar`。

### 步骤2: 使用命令行构建

同步成功后，打开PowerShell：

```bash
cd D:\Sesame-TK-n

# 检查Gradle版本
./gradlew --version

# 清理构建
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# 或构建指定变体
./gradlew assembleNormalDebug
./gradlew assembleCompatibleDebug
```

### 步骤3: 查看构建输出

```bash
# 查看生成的APK
Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk"
```

---

## 📊 构建选项说明

### Debug vs Release

| 特性 | Debug | Release |
|------|-------|---------|
| **用途** | 开发测试 | 生产发布 |
| **签名** | Debug证书 | Release证书（需配置）|
| **优化** | 未优化 | ProGuard/R8优化 |
| **调试** | 可调试 | 不可调试 |
| **大小** | 较大 | 较小（压缩后）|
| **速度** | 构建快 | 构建慢 |

### Normal vs Compatible

| 特性 | Normal | Compatible |
|------|--------|------------|
| **目标JVM** | JVM 17 | JVM 11 |
| **兼容性** | 新设备 | 旧设备 |
| **功能** | 完整功能 | 完整功能 |
| **推荐** | Android 12+ | Android 7-11 |

---

## ⏱️ 构建时间预估

### 首次构建（冷启动）

```
下载Gradle: 2-5分钟 (阿里云镜像)
下载依赖: 3-8分钟
编译代码: 2-5分钟
生成APK: 30秒-2分钟
------------------------
总计: 7-20分钟
```

### 增量构建（已有缓存）

```
无变更: 10-30秒
少量变更: 30秒-2分钟
大量变更: 2-5分钟
```

---

## ✅ 构建成功验证

### 1. 检查文件存在

```powershell
# PowerShell
Test-Path "app\build\outputs\apk\normal\debug\*.apk"
Test-Path "app\build\outputs\apk\compatible\debug\*.apk"
```

### 2. 查看APK信息

```bash
# 查看APK大小
Get-Item "app\build\outputs\apk\*\debug\*.apk" | Select-Object Name, Length
```

预期大小: 30-50MB

### 3. 安装测试

```bash
# 通过ADB安装到设备
adb install -r "app\build\outputs\apk\normal\debug\Sesame-TK-*.apk"
```

---

## ❌ 常见错误处理

### 错误1: "没有主清单属性"

**完整错误**:
```
gradle-wrapper.jar中没有主清单属性
```

**原因**: gradle-wrapper.jar损坏或版本不匹配

**解决方案**: 
1. ✅ **在Android Studio中同步**（会自动修复）
2. 或手动重新生成:
   ```bash
   gradle wrapper --gradle-version 9.1.0
   ```

### 错误2: AGP版本找不到

**错误信息**:
```
Could not find com.android.tools.build:gradle:8.13.0
```

**解决方案**:

编辑 `gradle/libs.versions.toml`:
```toml
# 改为确认存在的版本
android-plugin = "8.7.3"
```

重新同步。

### 错误3: 依赖下载失败

**错误信息**:
```
Could not resolve all dependencies for configuration ':app:debugRuntimeClasspath'
```

**解决方案**: 配置国内Maven镜像

在项目根目录创建/编辑 `settings.gradle.kts`:
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        google()
        mavenCentral()
    }
}
```

### 错误4: 内存不足

**错误信息**:
```
Expiring Daemon because JVM heap space is exhausted
```

**解决方案**: 编辑 `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m
org.gradle.daemon=true
org.gradle.parallel=true
org.gradle.caching=true
```

### 错误5: Kotlin编译错误

**错误信息**:
```
e: Compilation failed: ...
```

**解决方案**: 查看具体错误，通常是：
- 语法错误
- 导入缺失
- API变更

根据具体错误修复代码。

---

## 🎯 构建后验证清单

- [ ] 1. APK文件已生成
- [ ] 2. APK大小合理（30-50MB）
- [ ] 3. 版本号正确
- [ ] 4. 能够成功安装
- [ ] 5. 应用能够启动
- [ ] 6. Xposed Hook功能正常
- [ ] 7. 核心功能可用

---

## 📦 APK分发

### Debug版本（测试用）

- 可直接分发给测试人员
- 无需额外配置
- 使用Debug签名

### Release版本（正式发布）

需要配置Release签名：

1. 创建密钥库:
   ```bash
   keytool -genkey -v -keystore sesame-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias sesame
   ```

2. 在 `app/build.gradle.kts` 配置签名:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("../sesame-release.jks")
               storePassword = "your_password"
               keyAlias = "sesame"
               keyPassword = "your_password"
           }
       }
       buildTypes {
           release {
               signingConfig = signingConfigs.getByName("release")
           }
       }
   }
   ```

3. 构建Release版本:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 💡 构建优化建议

### 1. 启用构建缓存

`gradle.properties`:
```properties
org.gradle.caching=true
android.enableBuildCache=true
```

### 2. 并行构建

```properties
org.gradle.parallel=true
org.gradle.workers.max=4
```

### 3. 配置Daemon

```properties
org.gradle.daemon=true
org.gradle.jvmargs=-Xmx4096m
```

### 4. 使用R8优化

`app/build.gradle.kts`:
```kotlin
buildTypes {
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        proguardFiles(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro"
        )
    }
}
```

---

## 📚 相关文档

- **构建状态**: `BUILD_STATUS.md`
- **快速步骤**: `QUICK_BUILD_STEPS.txt`
- **版本更新**: `GRADLE_UPDATE_LOG.txt`

---

## 🎉 总结

**推荐流程**:

1. ✅ 在Android Studio中打开项目
2. ✅ 点击"Sync Now"同步Gradle
3. ✅ 等待同步完成（首次7-20分钟）
4. ✅ 点击 `Build` → `Build APK(s)`
5. ✅ 等待构建完成（2-5分钟）
6. ✅ 点击[locate]查看APK文件
7. ✅ 安装到设备测试

**首次构建预计总时间**: 10-25分钟

---

**最后更新**: 2025-10-25 23:30  
**状态**: 等待在Android Studio中构建
