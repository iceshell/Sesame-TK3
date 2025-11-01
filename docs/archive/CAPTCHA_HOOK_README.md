# 🛡️ 支付宝滑块验证码Hook方案

## 📊 问题分析

根据抓包数据分析，支付宝验证码机制包含：

1. **验证码API**: `alipay.security.antcaptcha.verify`
2. **加密载荷**: payload包含滑块轨迹加密数据
3. **设备指纹**: encryptedExt包含设备环境信息
4. **安全Cookie**: x5SecCookie用于会话验证

## 🎯 三层Hook方案（已实现）

### ✅ 第一层：UI层拦截
**Hook点**: `com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog.show()`  
**作用**: 阻止验证码对话框显示，用户看不到验证码  
**风险**: 🟢 低 - 仅影响UI展示

### ✅ 第二层：RPC层拦截
**Hook点**: `com.alipay.edge.observer.rpc.RpcRdsUtilImpl.rdsCaptchaHandle()`  
**作用**: 返回0，系统认为不需要处理验证码  
**风险**: 🟡 中 - 可能触发风控检测

### 🚧 第三层：RPC API拦截（待完善）
**Hook点**: `NewRpcBridge.requestObject()` 检测验证码API  
**作用**: 直接返回验证通过的响应  
**风险**: 🔴 高 - 高度可能被检测

## 🔧 使用方法

### 1. 启用验证码Hook

打开芝麻粒APP：
1. 进入`设置` → 选择用户配置
2. 进入`基础`设置
3. 找到 **"启用验证码拦截"** 开关并开启 ✅
4. 选择拦截级别：
   - 🔓 **普通验证**（放行滑块）：只拦截普通验证码
   - 🛡️ **滑块验证**（屏蔽所有）：拦截所有验证码（推荐）
5. 保存配置
6. 重启支付宝

### 2. 验证是否生效

查看日志 `runtime.log`：
```
✅ [UI层拦截] 已阻止验证码对话框显示
✅ [RPC层拦截] 已跳过验证处理
```

## ⚠️ 风险提示

### 可能的问题

1. **支付宝风控升级**
   - 支付宝可能检测到Hook行为
   - 可能触发账号风控或限制操作
   - 建议定期查看支付宝公告

2. **Hook失效**
   - 支付宝更新可能导致类名/方法名改变
   - 需要根据新版本调整Hook点
   - 参考最新SDK文档

3. **验证失败**
   - 部分敏感操作仍可能要求验证
   - 双层Hook无法覆盖所有场景
   - 建议结合手动验证

## 🔬 技术原理

### 支付宝验证码流程

```
用户操作 → 触发风控 → 显示验证码对话框
         ↓
    调用 rdsCaptchaHandle() → 判断是否需要验证
         ↓
    调用 alipay.security.antcaptcha.verify → 提交验证结果
         ↓
    返回 result: "pass" or "fail"
```

### Hook拦截点

```
[第一层] CaptchaDialog.show()
         ↓ (阻止显示)
    用户看不到对话框
         
[第二层] rdsCaptchaHandle() → return 0
         ↓ (跳过验证)
    系统认为无需验证
         
[第三层] NewRpcBridge.requestObject()
         ↓ (拦截API调用)
    直接返回通过结果
```

## 📚 支付宝SDK参考

### 官方文档
- **最新SDK**: https://opendocs.alipay.com/common/02mvn0?pathHash=473d19a0
- **风控系统**: https://opendocs.alipay.com/open/54/cyz7do
- **验证码服务**: https://opendocs.alipay.com/open/200/igz7bq

### 关键类名（可能随版本变化）

```java
// 验证码对话框
com.alipay.rdssecuritysdk.v3.captcha.view.CaptchaDialog

// RPC验证处理
com.alipay.edge.observer.rpc.RpcRdsUtilImpl

// RPC桥接
com.alibaba.ariver.commonability.network.rpc.RpcBridgeExtension
```

## 🛠️ 高级方案（待实现）

### 方案一：自动化滑块（复杂度高）

**原理**: 分析滑块轨迹算法，生成合理的滑动轨迹

```kotlin
// 伪代码
fun generateSlideTrack(distance: Int): List<Point> {
    // 1. 模拟人类滑动曲线（贝塞尔曲线）
    // 2. 添加随机抖动
    // 3. 计算滑动速度变化
    // 4. 加密轨迹数据
    return encryptedTrack
}
```

**难点**:
- ❌ 滑块距离计算（需要图像识别）
- ❌ 轨迹加密算法（需要逆向分析）
- ❌ 设备指纹伪造（容易被检测）

### 方案二：RPC响应伪造（风险极高）

**原理**: Hook RPC响应，直接返回验证通过

```kotlin
fun hookCaptchaResponse(method: String, response: JSONObject) {
    if (method == "alipay.security.antcaptcha.verify") {
        // 伪造通过响应
        return JSONObject().apply {
            put("result", "pass")
            put("errorCode", "00000")
            put("token", generateFakeToken())
        }
    }
}
```

**风险**:
- 🔴 支付宝服务端验证
- 🔴 Token有效性检查
- 🔴 行为模式分析

## ✅ 推荐方案

### 当前最佳实践（低风险）

1. **启用前两层Hook**（UI层 + RPC层）
   - 覆盖大部分场景
   - 风险可控
   - 易于维护

2. **配合手动验证**
   - 敏感操作时手动完成验证
   - 降低风控风险
   - 保持账号安全

3. **定期更新Hook代码**
   - 关注支付宝版本更新
   - 及时调整Hook点
   - 参考社区反馈

## 📝 更新日志

### v1.1 (2025-10-27)
- ✅ 添加三层Hook框架
- ✅ 完善文档说明
- 🚧 第三层RPC拦截待实现

### v1.0 (2025-10-23)
- ✅ 实现UI层拦截
- ✅ 实现RPC层拦截
- ✅ 支持两种拦截级别

## ⚖️ 免责声明

本功能仅供学习研究使用，请勿用于商业用途。使用本功能导致的任何账号问题，开发者概不负责。建议：
- ✅ 在测试环境使用
- ✅ 定期备份数据
- ✅ 遵守支付宝用户协议
- ❌ 不用于刷单、作弊等违规操作
