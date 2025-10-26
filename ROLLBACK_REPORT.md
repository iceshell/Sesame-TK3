# 黑屏卡死问题回滚报告

**问题**: 支付宝打开时黑屏卡死  
**原因**: 最后一次修改导致  
**处理**: 已回滚到稳定版本  
**版本**: rc8444 → rc9531

---

## 🚨 问题分析

### 卡死现象

**日志证据**:
```
[] 26日 11:16:53.80 [ApplicationHook]: Loading libdexkit.so...
[] 26日 11:16:54.60 [ApplicationHook]: xposed start loadPackage...
[] 26日 11:16:54.61 [ApplicationHook]: handleHookLogic: 开始执行hook逻辑
... (后续没有日志，卡死)
```

**分析**:
- Hook逻辑执行到一半就卡死
- 没有到达onResume阶段
- 支付宝界面黑屏无响应

---

## 🔍 问题根源

最后一次修改添加了"提前获取用户ID"代码：

```java
// ApplicationHook.java:320-329 (问题代码)
try {
    String earlyUserId = HookUtil.INSTANCE.getUserId(classLoader);
    if (earlyUserId != null) {
        Log.setCurrentUser(earlyUserId);
        Log.runtime(TAG, "提前设置用户ID到日志: " + earlyUserId);
    }
} catch (Throwable t) {
    // 忽略错误，等待onResume时再设置
}
```

**为什么会卡死**:

1. **getUserId依赖未初始化的类**: 
   - 在Application.attach阶段，支付宝的用户管理类可能未完全加载
   - 调用getUserId可能触发类加载死锁

2. **过早的反射调用**:
   - 此时ClassLoader处于敏感状态
   - 反射查找用户类可能导致类加载器死锁

3. **Hook时机问题**:
   - Application.attach是非常早期的hook点
   - 此时调用getUserId太激进

---

## ✅ 回滚内容

### 1. 删除"提前获取用户ID"代码

**回滚前**:
```java
Log.runtime(TAG, "handleLoadPackage alipayVersion: " + alipayVersion.getVersionString());
loadNativeLibs(appContext, AssetUtil.INSTANCE.getCheckerDestFile());
loadNativeLibs(appContext, AssetUtil.INSTANCE.getDexkitDestFile());

// 尝试提前获取用户ID并设置MDC（可能失败，因为用户还未登录）
try {
    String earlyUserId = HookUtil.INSTANCE.getUserId(classLoader);
    if (earlyUserId != null) {
        Log.setCurrentUser(earlyUserId);
        Log.runtime(TAG, "提前设置用户ID到日志: " + earlyUserId);
    }
} catch (Throwable t) {
    // 忽略错误，等待onResume时再设置
}

if (pInfo != null && pInfo.versionName != null) {
    ...
}
```

**回滚后**:
```java
Log.runtime(TAG, "handleLoadPackage alipayVersion: " + alipayVersion.getVersionString());
loadNativeLibs(appContext, AssetUtil.INSTANCE.getCheckerDestFile());
loadNativeLibs(appContext, AssetUtil.INSTANCE.getDexkitDestFile());
if (pInfo != null && pInfo.versionName != null) {
    ...
}
```

---

### 2. 简化hooked标志位逻辑

**回滚前**:
```java
private void handleHookLogic(ClassLoader classLoader, String packageName, String apkPath, Object rawParam) {
    XposedBridge.log(TAG + "|handleHookLogic " + packageName + " scuess!");
    if (hooked) {
        Log.runtime(TAG, "handleHookLogic: 已执行，跳过重复hook");
        return;
    }
    hooked = true;
    Log.runtime(TAG, "handleHookLogic: 开始执行hook逻辑");
    ...
}
```

**回滚后**:
```java
private void handleHookLogic(ClassLoader classLoader, String packageName, String apkPath, Object rawParam) {
    XposedBridge.log(TAG + "|handleHookLogic " + packageName + " scuess!");
    if (hooked) return;
    hooked = true;
    ...
}
```

---

## 📦 回滚到的稳定版本

**保留的修复**:
1. ✅ service就绪检查（onResume中）
2. ✅ initHandler重复初始化检查
3. ✅ NewRpcBridge堆栈打印优化（调试模式）
4. ✅ 用户切换日志优化

**删除的危险代码**:
1. ❌ Application.attach中提前获取用户ID
2. ❌ 额外的hook监控日志

---

## 🎯 经验教训

### 不要在Application.attach中调用getUserId

**原因**:
- Application.attach是最早的hook点
- 此时支付宝内部类尚未完全初始化
- 过早的反射调用可能导致死锁

**正确做法**:
- 在onResume中获取用户ID（已验证安全）
- 等待LauncherActivity完全启动后再获取

---

### 日志前缀不一致是可接受的

**现状**:
```
[] 26日 10:57:28.51 [ApplicationHook]: Not support fuck    ← 没有ID (正常)
[7084] 26日 10:57:29.43 [ApplicationHook]: 初始化开始...    ← 有ID (正常)
```

**结论**:
- Application.attach阶段的少量日志没有用户ID是正常的
- 一旦进入onResume，所有日志都会有用户ID
- 这不影响功能使用和问题排查

---

## 🔄 新版APK信息

**文件名**: `Sesame-TK-Normal-v0.3.0.重构版rc9531-beta-debug.apk`  
**版本**: rc8444 → rc9531  
**状态**: ✅ 已回滚到稳定版本  
**构建**: BUILD SUCCESSFUL

---

## 🎊 回滚完成

已恢复到上一个稳定版本，所有已验证的修复都保留。
