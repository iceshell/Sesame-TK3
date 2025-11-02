# 🔴 严重Bug修复：AntSports无限递归导致StackOverflowError

**修复版本**: v0.3.0-rc160  
**发现时间**: 2025-11-02 10:30  
**严重程度**: P0 (Critical)  
**影响范围**: 100%运动模块用户

---

## 🐛 问题描述

### 错误日志

```
02日 10:16:39.10 [AntSports]: Throwable error: java.lang.StackOverflowError: stack size 1043KB
	at fansirsqi.xposed.sesame.task.antSports.AntSports.tiyubizGo
	at fansirsqi.xposed.sesame.task.antSports.AntSports.pathFeatureQuery
	at fansirsqi.xposed.sesame.task.antSports.AntSports.tiyubizGo
	at fansirsqi.xposed.sesame.task.antSports.AntSports.pathFeatureQuery
	... (重复4000+行)
```

### 问题原因

**无限递归调用**：

```
pathFeatureQuery() → tiyubizGo() → pathFeatureQuery() → ...
```

**调用链分析**：
1. `pathFeatureQuery()` (1357行) 检测到路径状态为GOING
2. 调用 `tiyubizGo()` (1380行) 执行行走任务
3. `tiyubizGo()` (1471行) 完成后又调用 `pathFeatureQuery()`
4. 形成无限循环，直到栈溢出崩溃

---

## 🔧 修复方案

### 代码修改

**文件**: `D:\Sesame-TK-n\app\src\main\java\fansirsqi\xposed\sesame\task\antSports\AntSports.kt`

**修改前** (1471行):
```kotlin
if (completed) {
    other(TAG, "完成线路🚶🏻‍♂️[" + title + "]")
    pathFeatureQuery() // ❌ 导致无限递归！
}
```

**修改后** (1471行):
```kotlin
if (completed) {
    other(TAG, "完成线路🚶🏻‍♂️[" + title + "]")
    // 🔴 修复无限递归Bug：移除递归调用，让下一轮任务执行时处理新路径
    // pathFeatureQuery() // 这会导致StackOverflowError
}
```

### 修复原理

**为什么移除递归调用是正确的**：

1. **周期性任务执行**：AntSports是周期性任务，下一轮执行时会自动调用`pathFeatureQuery()`
2. **避免栈溢出**：深度递归会耗尽栈空间
3. **逻辑清晰**：一次任务只处理一条路径，避免连锁反应
4. **资源管理**：避免长时间占用线程

**如果需要立即处理下一条路径**（可选优化）：
```kotlin
// 可以使用延迟调度而不是递归
GlobalThreadPools.schedule(delayMillis = 1000) {
    pathFeatureQuery()
}
```

---

## 📊 影响分析

### 问题影响

| 方面 | 影响 |
|------|------|
| **崩溃率** | 高（任何完成路径的用户都会触发） |
| **日志污染** | 严重（4000+行错误日志） |
| **任务执行** | 中断（崩溃后任务停止） |
| **用户体验** | 差（应用卡顿/无响应） |

### 触发条件

- ✅ 用户启用了运动模块
- ✅ 用户有未完成的行走路径
- ✅ 路径达到完成条件

**触发概率**: 高（几乎必现）

---

## ✅ 验证结果

### 编译状态
- ✅ BUILD SUCCESSFUL (1m 41s)
- ✅ 无新增编译警告
- ✅ 代码已提交到git

### 预期效果
- ✅ 消除StackOverflowError
- ✅ 减少错误日志污染
- ✅ 运动模块正常完成路径
- ✅ 下一轮任务会自动处理新路径

---

## 🔍 其他发现的错误

### 业务级错误（非代码问题）

以下错误是**服务器返回的业务限制**，无需修复：

1. **"请稍等哦，马上出来" (error:1002)**
   - 服务器繁忙/限流
   - 属于正常的业务逻辑
   - 无需代码修改

2. **"不支持rpc完成的任务" (code:400000040)**
   - 某些活动任务不支持自动完成
   - 服务端限制
   - 无法通过代码绕过

3. **"权益获取次数超过上限" (code:400000012)**
   - 达到每日限制
   - 正常的业务规则
   - 无需修改

4. **"同步运动步数失败:100000"**
   - 步数超过限制（10万步）
   - 服务端校验
   - 属于正常保护机制

---

## 📋 测试建议

### 功能测试
- [ ] 启用运动模块
- [ ] 完成一条行走路径
- [ ] 确认无StackOverflowError
- [ ] 确认下一轮任务正常执行新路径

### 日志验证
- [ ] error.log无大量递归日志
- [ ] 运动任务正常完成并记录
- [ ] 无异常栈溢出错误

---

## 🎯 总结

**问题**: AntSports运动模块存在严重的无限递归Bug，导致StackOverflowError  
**修复**: 移除tiyubizGo()中的递归调用pathFeatureQuery()  
**版本**: rc160  
**状态**: ✅ 已修复并编译

**重要性**: 这是一个P0级严重Bug，会导致运动模块完全不可用，必须立即修复！

---

**修复人员**: Cascade AI  
**修复日期**: 2025-11-02 10:35
