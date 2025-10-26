# APK 构建状态

> **开始时间**: 2025-10-25 23:28  
> **目标**: 构建Debug和Release APK

---

## 🚀 构建命令

### 已执行命令

```bash
./gradlew assembleDebug --console=plain --stacktrace
```

**说明**:
- `assembleDebug`: 构建Debug版本APK
- `--console=plain`: 纯文本输出
- `--stacktrace`: 显示详细错误堆栈

---

## 📊 构建目标

### Debug APK (开发版本)

将生成以下APK文件：

1. **Normal版本**
   - 路径: `app/build/outputs/apk/normal/debug/`
   - 文件名: `Sesame-TK-Normal-*.apk`
   - 签名: Debug签名

2. **Compatible版本**
   - 路径: `app/build/outputs/apk/compatible/debug/`
   - 文件名: `Sesame-TK-Compatible-*.apk`
   - 签名: Debug签名

---

## ⏱️ 预计构建时间

### 首次构建

如果是首次构建，需要：
1. 下载Gradle 9.1.0 (~140MB) - **2-5分钟**
2. 下载依赖包 (~300MB) - **3-8分钟**
3. 编译代码 - **2-5分钟**

**总计**: 约 **7-18分钟**

### 增量构建

如果已有缓存：
- 无代码变更: 10-30秒
- 少量变更: 30秒-2分钟
- 大量变更: 2-5分钟

---

## 🔍 检查构建状态

### 方法1: 查看进程

```powershell
# 查看Java/Gradle进程
Get-Process | Where-Object {$_.ProcessName -like '*java*'}
```

有Java进程运行 = 构建正在进行

### 方法2: 查看日志

```bash
# 实时查看构建日志（如果输出到文件）
Get-Content build.log -Wait -Tail 50
```

### 方法3: 查看输出目录

```powershell
# 检查APK是否已生成
Get-ChildItem -Path "app\build\outputs\apk" -Recurse -Filter "*.apk"
```

---

## ✅ 构建成功标志

### 终端输出

```
BUILD SUCCESSFUL in 3m 45s
142 actionable tasks: 142 executed
```

### 文件生成

- ✅ Normal Debug APK 存在
- ✅ Compatible Debug APK 存在
- ✅ APK大小合理 (约30-50MB)

---

## ❌ 可能的错误

### 错误1: Gradle下载失败

**症状**: 
```
Could not get resource 'https://...'
```

**解决方案**: 检查网络，或切换镜像源

### 错误2: 依赖下载失败

**症状**:
```
Could not resolve all dependencies
```

**解决方案**: 配置Maven镜像

### 错误3: 编译错误

**症状**:
```
Compilation failed
```

**解决方案**: 查看具体错误信息，修复代码

### 错误4: 内存不足

**症状**:
```
Out of memory error
```

**解决方案**: 
```kotlin
// gradle.properties 增加内存
org.gradle.jvmargs=-Xmx4096m
```

---

## 📋 完整构建命令集

### Debug版本（开发测试）

```bash
# 仅构建Normal Debug
./gradlew assembleNormalDebug

# 仅构建Compatible Debug
./gradlew assembleCompatibleDebug

# 构建所有Debug版本
./gradlew assembleDebug
```

### Release版本（生产发布）

```bash
# 构建Release版本（需要签名配置）
./gradlew assembleRelease

# 构建所有版本
./gradlew assemble
```

### 完整构建+测试

```bash
# 完整构建流程
./gradlew clean build

# 包含测试
./gradlew clean test assembleDebug
```

---

## 📦 APK信息

### 版本信息

根据 `app/build.gradle.kts`:
- versionCode: Git提交数
- versionName: v0.2.8.魔改版rc{count}-beta

### APK特征

- **Normal版本**: 标准功能
- **Compatible版本**: 兼容模式，JVM 11

### APK大小预估

- 未压缩: ~50-70MB
- ProGuard优化后: ~30-40MB

---

## 🔄 构建后操作

### 1. 验证APK

```bash
# 使用aapt工具查看APK信息
aapt dump badging app/build/outputs/apk/normal/debug/*.apk
```

### 2. 安装测试

```bash
# 通过ADB安装
adb install -r app/build/outputs/apk/normal/debug/*.apk
```

### 3. 查看签名

```bash
# 查看签名信息
keytool -printcert -jarfile app/build/outputs/apk/normal/debug/*.apk
```

---

## 📊 构建性能优化

### 启用并行构建

在 `gradle.properties`:
```properties
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=true
```

### 配置构建缓存

```properties
org.gradle.caching=true
android.enableBuildCache=true
```

---

## 🎯 下一步

构建成功后：

1. ✅ 验证APK完整性
2. ✅ 安装到测试设备
3. ✅ 测试核心功能
4. ✅ 提交构建日志
5. ✅ 更新进度文档

---

**状态**: 🔄 构建中...

**最后更新**: 2025-10-25 23:28
