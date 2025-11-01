# 🔍 账号选择闪退调试指南

## 📋 当前状态

**报告时间**: 2025-10-28 20:46  
**问题状态**: 🔴 仍在调查  
**测试版本**: v0.3.0-rc96-debug

---

## 🎯 问题描述

用户反馈：点击芝麻粒的"设置"按钮，选择支付宝账号后，应用**仍然闪退**。

### 已完成的修复

1. ✅ 数组初始化同步
2. ✅ 异常处理同步  
3. ✅ 添加数组边界检查
4. ✅ 添加空数组检查

### 问题现象

- 日志显示"加载用户配置: 共5个账号" → 新代码已生效
- 日志中**无明显异常栈** → 静默崩溃
- 闪退发生在点击账号选择后

---

## 🔍 调试策略

### 第1步: 安装调试版本

**最新APK**: 
```
D:\Sesame-TK-n\app\build\outputs\apk\debug\sesame-tk-v0.3.0-rc96-debug.apk
```

**编译时间**: 2025-10-28 20:46  
**大小**: ~22 MB

### 第2步: 复现问题并收集日志

请执行以下操作：

1. **卸载旧版本**
   ```bash
   # 或直接覆盖安装
   ```

2. **安装新版本**
   ```bash
   adb install sesame-tk-v0.3.0-rc96-debug.apk
   ```

3. **清空旧日志**
   ```bash
   # 删除 D:\Sesame-TK-n\log\ 下的所有日志
   # 或重命名为 log_backup
   ```

4. **复现问题**
   - 打开芝麻粒
   - 点击"设置"按钮
   - 选择任意账号
   - 观察是否闪退

5. **收集日志**
   - 立即查看 `D:\Sesame-TK-n\log\runtime.log`
   - 查看 `D:\Sesame-TK-n\log\error.log`
   - 查看 `D:\Sesame-TK-n\log\system.log`

---

## 📊 新增的调试日志

### 正常流程应该看到的日志

```log
[MainActivity]: 准备载入用户配置: index=X, showName=XXX, userId=XXX
[MainActivity]: 目标Activity: fansirsqi.xposed.sesame.ui.SettingActivity
[MainActivity]: Intent已配置: userId=XXX, userName=XXX
[MainActivity]: 正在启动SettingActivity...
[MainActivity]: SettingActivity启动成功
```

### 如果出现异常

```log
[MainActivity]: goSettingActivity发生异常: XXX
异常堆栈信息...
```

---

## 🔎 可能的原因分析

### 原因1: Activity启动失败

**表现**: 
- 日志显示"正在启动SettingActivity..."
- 但没有"SettingActivity启动成功"
- 应用闪退

**可能原因**:
- SettingActivity的onCreate()抛出异常
- Intent参数问题
- Context问题

### 原因2: 数组访问仍然越界

**表现**:
- 日志没有显示"准备载入用户配置"
- 直接闪退

**可能原因**:
- userEntityArray和userNameArray长度仍不一致
- 索引计算错误

### 原因3: 使用了旧版本APK

**表现**:
- 日志中没有"准备载入用户配置"等新日志
- 仍然看到旧的"载入用户配置"（无详细参数）

**解决方案**:
- 确认安装了最新的rc96版本

### 原因4: SettingActivity初始化异常

**表现**:
- 日志显示"SettingActivity启动成功"
- 但随后闪退

**可能原因**:
- SettingActivity.onCreate()中的初始化代码异常
- Model.initAllModel()失败
- UserMap.load()失败
- Config.load()失败

---

## 🧪 关键检查点

### 检查点1: 版本确认

```bash
# 查看APK编译时间
Get-ChildItem "D:\Sesame-TK-n\app\build\outputs\apk\debug\*.apk" | 
  Select-Object Name, LastWriteTime

# 应该显示 2025/10/28 20:46 或更晚
```

### 检查点2: 日志关键字

在 `runtime.log` 中搜索：
- `准备载入用户配置` - 新版本标志
- `目标Activity` - 确认跳转目标
- `Intent已配置` - 确认参数
- `正在启动SettingActivity` - 确认调用startActivity
- `SettingActivity启动成功` - 确认启动成功

### 检查点3: 异常信息

在 `error.log` 和 `system.log` 中搜索：
- `Exception`
- `Error`
- `Crash`
- `goSettingActivity发生异常`

---

## 📝 下一步行动

### 如果日志显示"正在启动SettingActivity..."但无"成功"

**说明**: 闪退发生在startActivity()调用时或SettingActivity.onCreate()中

**需要检查**:
1. SettingActivity的onCreate()方法
2. Model.initAllModel()
3. UserMap相关操作
4. Config.load()

### 如果日志完全没有新的调试信息

**说明**: 使用了旧版本APK

**操作**: 
1. 确认卸载旧版本
2. 重新安装最新APK
3. 重启手机

### 如果日志显示异常堆栈

**说明**: 捕获到异常了！

**操作**:
1. 复制完整的异常堆栈
2. 分析异常类型和位置
3. 针对性修复

---

## 📞 反馈格式

请按以下格式反馈：

```markdown
### 测试结果

**安装版本**: sesame-tk-v0.3.0-rc96-debug.apk
**安装时间**: 2025-10-28 XX:XX
**是否闪退**: 是/否

**runtime.log关键日志**:
```log
粘贴相关日志...
```

**error.log内容**:
```log
粘贴异常信息...
```

**其他观察**:
- 闪退时机：点击账号后立即闪退 / 延迟几秒闪退
- 有无提示：Toast提示内容
- 能否进入SettingActivity：能/不能
```

---

## 🎯 预期结果

### 最佳情况
- 日志完整显示所有步骤
- 捕获到具体异常
- 定位到问题代码行

### 次佳情况  
- 日志显示到某一步就停止
- 确定闪退发生的位置
- 缩小问题范围

---

**调试负责人**: AI Assistant  
**状态**: ⏳ 等待用户测试反馈  
**下次更新**: 根据测试结果决定
