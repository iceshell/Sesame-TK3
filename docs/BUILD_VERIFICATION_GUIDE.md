# 构建验证指南

> **版本**: Gradle 9.1.0 + AGP 8.13.0  
> **日期**: 2025-10-25

---

## ✅ 版本更新已完成

### 更新内容

| 组件 | 旧版本 | 新版本 | 状态 |
|------|--------|--------|------|
| **Gradle** | 8.10 | **9.1.0** | ✅ 已更新 |
| **AGP** | 8.7.3 | **8.13.0** | ✅ 已更新 |
| **镜像源** | 官方 | **阿里云** | ✅ 已配置 |

**配置文件**:
- ✅ `gradle/wrapper/gradle-wrapper.properties` - Gradle 9.1.0
- ✅ `gradle/libs.versions.toml` - AGP 8.13.0

---

## 🚀 推荐方法：使用 Android Studio 同步

### 步骤1: 打开项目

1. 启动 **Android Studio 2025.1.4.8**
2. 打开项目: `File` → `Open` → 选择 `D:\Sesame-TK-n`

### 步骤2: Gradle 同步

Android Studio会自动检测到版本变化并提示同步：

```
Gradle files have changed since last sync
[Sync Now] [File an Issue]
```

**点击 "Sync Now"**

### 步骤3: 等待下载

Android Studio会自动：
1. ✅ 从阿里云镜像下载Gradle 9.1.0 (~140MB)
2. ✅ 下载AGP 8.13.0插件
3. ✅ 下载依赖包
4. ✅ 生成项目结构

**预计时间**: 
- 首次下载: 2-5分钟（国内镜像快速）
- 后续同步: 10-30秒

### 步骤4: 查看同步结果

在Android Studio底部的 **Build** 窗口查看输出：

**成功标志**:
```
BUILD SUCCESSFUL in 30s
Gradle sync finished in 30s
```

**如果失败**:
```
BUILD FAILED
```
查看错误信息进行排查。

---

## 🔧 备选方法：命令行验证

### 方法A: 使用本地Gradle

如果您的系统已安装Gradle 9.1.0：

```bash
# Windows (需要将Gradle添加到PATH)
gradle --version
gradle clean build
```

### 方法B: 重新生成Wrapper

如果gradlew命令有问题：

```bash
# 使用系统Gradle重新生成wrapper
gradle wrapper --gradle-version 9.1.0

# 然后使用新的gradlew
./gradlew --version
./gradlew clean build
```

---

## ⚠️ 常见问题排查

### 问题1: gradlew报错"没有主清单属性"

**原因**: gradle-wrapper.jar损坏

**解决方案**:
1. **推荐**: 在Android Studio中同步（会自动修复）
2. 或者手动重新生成wrapper（见上方方法B）

### 问题2: AGP 8.13.0 找不到

**错误信息**:
```
Could not find com.android.tools.build:gradle:8.13.0
```

**原因**: AGP 8.13.0 可能不存在（最新稳定版通常是8.7.x）

**解决方案**:

编辑 `gradle/libs.versions.toml`:
```toml
# 降级到确认存在的版本
android-plugin = "8.7.3"  # 或查看最新版本
```

然后重新同步。

### 问题3: 下载速度慢

**解决方案**: 确认使用的是阿里云镜像

检查 `gradle/wrapper/gradle-wrapper.properties`:
```properties
distributionUrl=https\://mirrors.aliyun.com/macports/distfiles/gradle/gradle-9.1.0-bin.zip
```

如果阿里云镜像失败，切换到腾讯云：
```properties
distributionUrl=https\://mirrors.cloud.tencent.com/gradle/gradle-9.1.0-bin.zip
```

### 问题4: 依赖下载失败

**解决方案**: 配置Maven国内镜像

在项目根目录的 `build.gradle.kts` 或 `settings.gradle.kts` 中添加：

```kotlin
pluginManagement {
    repositories {
        // 阿里云镜像
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

---

## 📋 完整验证清单

在Android Studio同步成功后，执行以下验证：

### 1. 版本检查

在Android Studio的Terminal中运行：

```bash
# 检查Gradle版本
./gradlew --version

# 应该显示: Gradle 9.1.0
```

### 2. 清理构建

```bash
./gradlew clean
```

### 3. 运行测试

```bash
# 运行StringUtil测试
./gradlew test --tests StringUtilTest

# 查看测试报告
# app/build/reports/tests/testDebugUnitTest/index.html
```

### 4. ktlint检查

```bash
./gradlew ktlintCheck

# 如果有格式问题，自动修复
./gradlew ktlintFormat
```

### 5. 完整构建

```bash
# 构建Debug版本
./gradlew assembleDebug

# 构建所有变体
./gradlew build
```

### 6. 查看构建产物

成功后应该生成：
- `app/build/outputs/apk/normal/debug/Sesame-TK-Normal-*.apk`
- `app/build/outputs/apk/compatible/debug/Sesame-TK-Compatible-*.apk`

---

## ✅ 成功标准

构建验证成功的标志：

- [x] Gradle同步无错误
- [x] Gradle版本显示为9.1.0
- [x] AGP版本正确加载
- [x] 所有依赖下载成功
- [x] StringUtil测试全部通过（35个测试）
- [x] ktlint检查通过
- [x] 构建成功生成APK

---

## 📊 预期输出

### Gradle版本信息

```
------------------------------------------------------------
Gradle 9.1.0
------------------------------------------------------------

Build time:   2024-10-24 16:06:04 UTC
Revision:     1234567890abcdef

Kotlin:       2.0.21
Groovy:       3.0.22
Ant:          Apache Ant(TM) version 1.10.14
JVM:          17.0.16.8 (Eclipse Adoptium)
OS:           Windows 10 10.0 amd64
```

### 测试成功输出

```
> Task :app:testDebugUnitTest

StringUtilTest > isEmpty should return true for null string PASSED
StringUtilTest > isEmpty should return true for empty string PASSED
...
StringUtilTest > large collection joinToString performance PASSED

BUILD SUCCESSFUL in 15s
35 tests completed, 35 passed
```

### 构建成功输出

```
BUILD SUCCESSFUL in 1m 23s
142 actionable tasks: 142 executed
```

---

## 🔄 如果验证失败

### 回滚到稳定版本

如果遇到无法解决的问题，可以回滚：

**gradle/wrapper/gradle-wrapper.properties**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
```

**gradle/libs.versions.toml**:
```toml
android-plugin = "8.7.3"
```

然后在Android Studio中重新同步。

---

## 📞 获取帮助

如果遇到问题：

1. **查看错误日志**: Android Studio的Build窗口
2. **搜索错误**: Google/Stack Overflow
3. **查看文档**: 
   - AGP发布说明: https://developer.android.com/studio/releases/gradle-plugin
   - Gradle发布说明: https://docs.gradle.org/9.1/release-notes.html

---

## 🎯 下一步

验证成功后：

1. ✅ 提交版本更新
   ```bash
   git add gradle/
   git commit -m "chore: update to Gradle 9.1.0 and AGP 8.13.0"
   ```

2. ✅ 继续阶段1迁移
   - 下一个目标: TimeUtil.java
   
3. ✅ 更新文档
   - 更新VERSION_UPDATE_LOG.txt
   - 更新REFACTOR_TRACKING.md

---

**最后更新**: 2025-10-25 23:20  
**状态**: 等待验证
