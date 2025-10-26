# 版本更新分析报告

> **分析日期**: 2025-10-25  
> **目的**: 根据本地环境评估项目依赖版本并给出更新建议

---

## 📊 本地环境信息

| 工具 | 版本 | 安装路径 |
|------|------|----------|
| **Android Studio** | 2025.1.4.8 (最新稳定版) | D:\Android\Android Studio |
| **Gradle** | v9.1.0 (最新稳定版) | 系统全局 |
| **Kotlin** | v2.2.20 (最新稳定版) | AS内置 |
| **AGP** | 8.13.0 | - |
| **JDK** | 17.0.16.8 (Eclipse Adoptium) | C:\Program Files\Eclipse Adoptium\jdk-17.0.16.8-hotspot |
| **Android SDK** | - | D:\Android\Sdk |

---

## 🔍 项目当前版本

### 核心构建工具

| 工具 | 当前版本 | 本地版本 | 状态 | 建议 |
|------|----------|----------|------|------|
| **Android Gradle Plugin** | 8.9.3 | 8.13.0 | ⚠️ 需更新 | 更新到 8.13.0 |
| **Kotlin Plugin** | 2.2.0-Beta2 | 2.2.20 | ⚠️ Beta版 | 更新到 2.2.20 稳定版 |
| **Kotlin Stdlib** | 2.1.20 | 2.2.20 | ⚠️ 需更新 | 更新到 2.2.20 |
| **Gradle Wrapper** | (待检查) | 9.1.0 | - | 建议使用 8.10 或 9.1.0 |
| **Kotlin Compose Plugin** | 2.0.0 | 2.2.20 | ⚠️ 需更新 | 更新到 2.2.20 |

### Android SDK版本

| 配置 | 当前值 | 建议值 | 说明 |
|------|--------|--------|------|
| compileSdk | 36 | 36 | ✅ 最新 (Android 16 Preview) |
| targetSdk | 36 | 36 | ✅ 最新 |
| minSdk | 24 | 24 | ✅ 合适 (Android 7.0+) |

### 关键依赖

| 依赖 | 当前版本 | 最新版本 | 状态 | 建议 |
|------|----------|----------|------|------|
| **Kotlin Coroutines** | 1.10.2 | 1.10.2 | ✅ 最新 | 保持 |
| **Kotlinx Serialization** | 1.9.0 | 1.9.0 | ✅ 最新 | 保持 |
| **Jetpack Compose** | BOM 2025.05.00 | - | ✅ 新版 | 保持 |
| **Activity Compose** | 1.10.1 | 1.10.1 | ✅ 最新 | 保持 |
| **Lifecycle** | 2.9.4 | 2.9.4 | ✅ 最新 | 保持 |
| **Core KTX** | 1.16.0 | 1.16.0 | ✅ 最新 | 保持 |
| **Material 3** | Compose BOM | - | ✅ | 保持 |
| **Jackson** | 2.19.2 | 2.19.2 | ✅ 最新 | 保持 |
| **OkHttp** | 5.0.0-alpha.14 | - | ⚠️ Alpha | 考虑稳定版 |
| **MMKV** | 2.2.3 | 2.2.3 | ✅ 最新 | 保持 |
| **DexKit** | 2.0.4 | 2.0.4 | ✅ 最新 | 保持 |
| **Lombok** | 1.18.38 | 1.18.38 | ✅ 最新 | ⚠️ 计划移除 |

### Xposed/LSPosed 相关

| 依赖 | 当前版本 | 最新版本 | 说明 |
|------|----------|----------|------|
| **libxposed-api** | 100 | 100 | ✅ 最新 |
| **libxposed-service** | 100-1.0.0 | 100-1.0.0 | ✅ 最新 |
| **Shizuku** | 13.1.5 | 13.1.5 | ✅ 最新 |

参考：
- LSPosed: https://github.com/LSPosed/LSPosed
- LSPatch: https://github.com/LSPosed/LSPatch

---

## ⚠️ 需要更新的版本

### 高优先级 (推荐立即更新)

#### 1. Kotlin 版本 ⭐⭐⭐⭐⭐

**问题**: 当前使用 Beta 版本

```toml
# 当前 (libs.versions.toml)
kotlin-plugin = "2.2.0-Beta2"
kotlin-stdlib = "2.1.20"

# 建议更新为
kotlin-plugin = "2.2.20"      # 稳定版
kotlin-stdlib = "2.2.20"      # 保持一致
```

**理由**:
- 本地环境是 2.2.20 稳定版
- Beta 版本可能有不稳定问题
- 应该统一版本号

**影响**: 低，向后兼容

---

#### 2. Android Gradle Plugin ⭐⭐⭐⭐

```toml
# 当前
android-plugin = "8.9.3"

# 建议更新为
android-plugin = "8.13.0"     # 或 8.7.3 (最新稳定)
```

**理由**:
- 本地 AGP 是 8.13.0
- 8.9.3 版本号可能不存在（最新稳定版是 8.7.x）
- 建议使用 8.7.3 或更高稳定版

**影响**: 低，但需要测试

---

#### 3. Kotlin Compose Plugin ⭐⭐⭐⭐

```toml
# 当前
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.0.0" }

# 建议更新为
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.2.20" }
```

**理由**: 应与 Kotlin 版本保持一致

---

### 中优先级 (可选更新)

#### 4. Gradle Wrapper

需要检查 `gradle/wrapper/gradle-wrapper.properties`：

```properties
# 建议版本
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
# 或使用最新版
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1-bin.zip
```

**兼容性**:
- Gradle 8.10: 兼容 AGP 8.7+
- Gradle 9.1.0: 需要 AGP 9.0+ (可能不兼容)

**建议**: 先使用 Gradle 8.10

---

### 低优先级 (观望)

#### 5. OkHttp

```toml
okhttp = "5.0.0-alpha.14"
```

**建议**: 
- Alpha 版本不稳定
- 如果当前运行正常，可以保持
- 或降级到 4.12.0 稳定版

---

## 📝 更新步骤建议

### 步骤1: 更新 Kotlin (最重要)

修改 `gradle/libs.versions.toml`:

```toml
[versions]
kotlin-plugin = "2.2.20"          # 从 2.2.0-Beta2 更新
kotlin-stdlib = "2.2.20"          # 从 2.1.20 更新
```

修改 `gradle/libs.versions.toml` 的 plugins 部分:

```toml
[plugins]
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.2.20" }  # 从 2.0.0 更新
```

---

### 步骤2: 更新 AGP

修改 `gradle/libs.versions.toml`:

```toml
[versions]
android-plugin = "8.7.3"          # 或 8.13.0 (但需验证存在性)
```

**注意**: AGP 8.13.0 版本号可能不正确，建议先查证：
- 查看 AGP 版本历史: https://developer.android.com/studio/releases/gradle-plugin
- 当前最新稳定版通常是 8.7.x 系列

---

### 步骤3: 更新 Gradle Wrapper (可选)

修改 `gradle/wrapper/gradle-wrapper.properties`:

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.10-bin.zip
```

或运行命令:

```bash
./gradlew wrapper --gradle-version 8.10
```

---

### 步骤4: 验证构建

```bash
# 清理构建
./gradlew clean

# 重新构建
./gradlew build

# 运行测试
./gradlew test
```

---

## ⚡ 推荐的完整更新配置

### gradle/libs.versions.toml

```toml
[versions]
activity-compose = "1.10.1"
android-plugin = "8.7.3"              # ✅ 更新
constraintlayout = "2.1.4"
hiddenapibypass = "6.1"
kotlin-plugin = "2.2.20"              # ✅ 更新 (从 Beta 到稳定版)
kotlin-stdlib = "2.2.20"              # ✅ 更新
kotlinxCoroutinesCore = "1.10.2"
kotlinxSerializationJson = "1.9.0"
lifecycleLivedataKtx = "2.9.4"
mmkv = "2.2.3"
# ... 其他保持不变 ...

[plugins]
android-application = { id = "com.android.application", version.ref = "android-plugin" }
android-library = { id = "com.android.library", version.ref = "android-plugin" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin-plugin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version = "2.2.20" }  # ✅ 更新
rikka-tools-refine = { id = "dev.rikka.tools.refine", version.ref = "hiddenapi" }
```

---

## 🔗 官方资源链接

### Kotlin
- 官方发布页: https://github.com/JetBrains/kotlin/releases
- 更新日志: https://kotlinlang.org/docs/releases.html

### Android Gradle Plugin
- 发布说明: https://developer.android.com/studio/releases/gradle-plugin
- 版本兼容性: https://developer.android.com/studio/releases/gradle-plugin#updating-gradle

### Gradle
- 官方版本: https://gradle.org/releases/
- 兼容性矩阵: https://docs.gradle.org/current/userguide/compatibility.html

### Xposed/LSPosed
- LSPosed: https://github.com/LSPosed/LSPosed
- LSPatch: https://github.com/LSPosed/LSPatch
- API: https://github.com/libxposed/api

### 支付宝 SDK
- 官方文档: https://opendocs.alipay.com/common/02mvn0?pathHash=473d19a0

---

## ⚠️ 注意事项

### 1. AGP 版本号核实

**重要**: 需要核实 AGP 8.13.0 是否存在：

```bash
# 查看可用的 AGP 版本
# https://mvnrepository.com/artifact/com.android.tools.build/gradle
```

如果 8.13.0 不存在，建议使用:
- **8.7.3** (当前最新稳定版，推荐)
- **8.6.1** (次新稳定版)

### 2. Kotlin 版本统一

确保所有 Kotlin 相关版本一致:
- kotlin-plugin
- kotlin-stdlib
- kotlin-compose

### 3. Gradle 兼容性

| AGP 版本 | 需要的 Gradle 版本 |
|----------|-------------------|
| 8.7.x | 8.9 - 8.10 |
| 8.6.x | 8.9 |
| 9.0.x | 9.0+ |

### 4. 测试覆盖

更新后必须测试：
- ✅ Xposed Hook 功能
- ✅ RPC 调用
- ✅ 协程任务
- ✅ Compose UI
- ✅ 构建变体 (normal + compatible)

---

## 🎯 总结

### 必须更新 ⭐⭐⭐⭐⭐

1. **Kotlin 2.2.0-Beta2 → 2.2.20** (稳定版)
2. **kotlin-stdlib 2.1.20 → 2.2.20** (统一版本)
3. **kotlin-compose 2.0.0 → 2.2.20** (统一版本)

### 建议更新 ⭐⭐⭐⭐

4. **AGP 8.9.3 → 8.7.3** (核实版本后)
5. **Gradle → 8.10** (兼容性好)

### 可选更新 ⭐⭐

6. **OkHttp Alpha → 4.12.0 稳定版** (如果有问题)

### 预期收益

- ✅ 移除 Beta 版本的不稳定性
- ✅ 获得 Kotlin 2.2.20 的性能和功能改进
- ✅ 更好的 IDE 支持
- ✅ 更好的 Compose 编译器支持
- ✅ 减少潜在的兼容性问题

### 风险评估

- 🟢 **低风险**: Kotlin 小版本更新，向后兼容
- 🟡 **中风险**: AGP 更新，需要测试
- 🔴 **注意**: AGP 版本号需要核实

---

**建议**: 先更新 Kotlin 版本，测试通过后再考虑 AGP 更新。

**下一步**: 是否立即执行更新？
