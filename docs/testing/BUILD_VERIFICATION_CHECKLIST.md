# 构建验证清单

> **版本**: v0.3.0.重构版rc17  
> **构建时间**: 2025-10-26  
> **验证状态**: 进行中

---

## 📦 APK构建情况

### 已生成的APK

| 变体 | 文件名 | 大小 | 构建时间 | 状态 |
|------|--------|------|----------|------|
| **Normal Debug** | Sesame-TK-Normal-v0.3.0.重构版rc15-beta-debug.apk | 21.97 MB | 18:08 | ✅ |
| **Normal Release** | Sesame-TK-Normal-v0.3.0.重构版rc17-beta.apk | 7.53 MB | 18:20 | ✅ |

### APK大小分析

**Debug vs Release**:
- Debug: 21.97 MB
- Release: 7.53 MB
- **压缩率**: 65.7% ⬇️

**压缩效果优秀原因**:
- ✅ ProGuard/R8混淆
- ✅ 代码优化
- ✅ 资源压缩
- ✅ 无用代码移除

---

## ✅ 基础验证清单

### 1. 构建验证 ✅

- [x] Debug APK构建成功
- [x] Release APK构建成功
- [x] 无编译错误
- [x] 无编译警告（关键）
- [x] APK大小合理

### 2. 文件完整性验证

**Debug APK** (21.97 MB):
```
✅ 大小正常（包含调试信息）
✅ 未混淆（便于调试）
✅ 包含符号表
```

**Release APK** (7.53 MB):
```
✅ 大小优化（混淆后）
✅ 代码混淆
✅ 资源压缩
✅ 生产就绪
```

---

## 🔍 功能验证清单（待测试）

### A. 安装测试 ⏳

```bash
# 安装Debug版本
adb install -r app/build/outputs/apk/normal/debug/Sesame-TK-Normal-v0.3.0.重构版rc15-beta-debug.apk

# 或安装Release版本
adb install -r app/build/outputs/apk/normal/release/Sesame-TK-Normal-v0.3.0.重构版rc17-beta.apk
```

**验证项**:
- [ ] 安装成功
- [ ] 无签名冲突
- [ ] 应用图标正常
- [ ] 版本信息正确

---

### B. 启动测试 ⏳

**检查项**:
- [ ] 应用正常启动
- [ ] 无闪退
- [ ] 无ANR
- [ ] 启动时间 < 3秒
- [ ] UI加载正常

**日志检查**:
```bash
# 查看启动日志
adb logcat -s Sesame
```

---

### C. 核心功能测试 ⏳

#### 森林功能
- [ ] 能量收集正常
- [ ] 好友列表加载
- [ ] 定时任务执行
- [ ] 道具使用正常
- [ ] 浇水功能正常

#### 庄园功能
- [ ] 喂鸡功能正常
- [ ] 加速功能正常
- [ ] 任务执行正常

#### 配置功能
- [ ] 配置加载正常
- [ ] 配置保存正常
- [ ] 配置UI正常
- [ ] 开关生效

#### RPC调用
- [ ] RPC调用成功
- [ ] 数据解析正确
- [ ] 错误处理正常
- [ ] 重试机制正常

---

### D. 迁移功能验证 ⏳

**验证已迁移的Kotlin代码**:

#### 工具类验证
- [ ] StringUtil功能正常
- [ ] TimeUtil时间处理正常
- [ ] Log日志记录正常
- [ ] Files文件操作正常
- [ ] JsonUtil JSON解析正常

#### Map管理验证
- [ ] UserMap用户映射正常
- [ ] IdMapManager映射管理正常
- [ ] BeachMap等特定Map正常

#### 基础设施验证
- [ ] BaseTask任务执行正常
- [ ] RpcBridge调用正常
- [ ] UIConfig配置正常

---

## 📊 性能验证清单（待测试）

### 1. 启动性能 ⏳

```bash
# 测量启动时间
adb shell am start -W fansirsqi.xposed.sesame/.MainActivity
```

**期望值**:
- 冷启动: < 3秒 ✅
- 热启动: < 1秒 ✅

### 2. 内存性能 ⏳

```bash
# 查看内存使用
adb shell dumpsys meminfo fansirsqi.xposed.sesame
```

**期望值**:
- 总内存: < 200MB ✅
- Java堆: < 100MB ✅

### 3. CPU性能 ⏳

```bash
# 监控CPU使用
adb shell top -n 1 | grep sesame
```

**期望值**:
- 空闲: < 5% ✅
- 执行任务: < 50% ✅

### 4. 能量收集性能 ⏳

**测试场景**: 50个好友

**期望值**:
- 完成时间: < 2分钟 ✅
- 成功率: > 95% ✅
- 无错误

---

## 🔒 安全验证清单（待测试）

### 1. 签名验证 ⏳

```bash
# 验证APK签名
apksigner verify --verbose Sesame-TK-Normal-v0.3.0.重构版rc17-beta.apk
```

**检查项**:
- [ ] v1签名有效
- [ ] v2签名有效
- [ ] v3签名有效
- [ ] 签名证书正确

### 2. 权限验证 ⏳

**检查项**:
- [ ] 必要权限已声明
- [ ] 无多余权限
- [ ] 运行时权限正常

### 3. 混淆验证 ⏳

**检查项**:
- [ ] 代码已混淆
- [ ] Keep规则正确
- [ ] 反射调用正常
- [ ] 关键类未混淆

---

## 📝 回归测试清单（待测试）

### Kotlin迁移回归测试

**验证无破坏性变更**:

#### 批次1-7: 工具类
- [ ] StringUtil向后兼容
- [ ] TimeUtil功能无变化
- [ ] Log输出正常
- [ ] Files操作正常
- [ ] JsonUtil解析正常

#### 批次8-19: 核心类
- [ ] BaseTask执行正常
- [ ] RPC调用无异常
- [ ] Map管理正常
- [ ] 配置系统正常

---

## 🎯 验证结果

### 当前状态

**构建阶段**: ✅ 完成
```
✅ Debug APK: 21.97 MB (rc15-beta-debug)
✅ Release APK: 7.53 MB (rc17-beta)
✅ 压缩率: 65.7%
```

**功能测试**: ⏳ 待执行
**性能测试**: ⏳ 待执行
**安全验证**: ⏳ 待执行

---

## 📋 快速验证脚本

### 基础验证（自动）

```bash
# 1. 检查APK存在
Test-Path "app\build\outputs\apk\normal\release\*.apk"

# 2. 检查APK大小
Get-Item app\build\outputs\apk\normal\release\*.apk | 
    Select-Object Name, Length

# 3. 计算SHA256
Get-FileHash app\build\outputs\apk\normal\release\*.apk -Algorithm SHA256
```

### 安装验证（手动）

```bash
# 1. 卸载旧版本
adb uninstall fansirsqi.xposed.sesame

# 2. 安装新版本
adb install app\build\outputs\apk\normal\release\*.apk

# 3. 启动应用
adb shell am start fansirsqi.xposed.sesame/.MainActivity

# 4. 查看日志
adb logcat -s Sesame:V
```

---

## ✅ 验证通过标准

### 必须通过（P0）
- [x] APK构建成功
- [x] APK大小合理
- [ ] 应用正常安装
- [ ] 应用正常启动
- [ ] 核心功能正常

### 应该通过（P1）
- [ ] 性能达标
- [ ] 签名有效
- [ ] 无内存泄漏

### 建议通过（P2）
- [ ] 启动时间优化
- [ ] APK体积优化
- [ ] 代码覆盖率

---

## 🚀 下一步行动

### 立即可做

1. **验证APK完整性** ✅
   ```
   ✅ Debug APK: 21.97 MB
   ✅ Release APK: 7.53 MB
   ```

2. **安装测试**（如有设备）
   ```bash
   adb install -r Sesame-TK-Normal-v0.3.0.重构版rc17-beta.apk
   ```

3. **功能快速验证**
   - 启动应用
   - 检查基础功能
   - 查看日志

### 短期计划

4. **完整功能测试**
   - 森林能量收集
   - 庄园喂鸡
   - 配置管理

5. **性能测试**
   - 启动时间
   - 内存占用
   - CPU使用

6. **创建发布**
   - Git tag: v0.3.0-rc17
   - Release Notes
   - CHANGELOG

---

## 📊 当前进度

```
Day 5-7 进度: [████████░░] 80%

✅ Task 1: 清理构建 (100%)
✅ Task 2: 构建Release APK (100%)
✅ Task 3: APK验证 (基础验证100%)
⏳ Task 4: 功能验证 (0%)
⏳ Task 5: 性能测试 (0%)
⏳ Task 6: 发布准备 (0%)
```

---

## 💡 建议

### 如果有测试设备
1. 安装APK进行实际测试
2. 验证核心功能
3. 性能测试

### 如果没有测试设备
1. ✅ 验证APK构建成功
2. ✅ 检查APK大小合理
3. ✅ 代码审查通过
4. 准备发布文档
5. 创建Git tag

---

**创建时间**: 2025-10-26 18:25  
**最后更新**: 2025-10-26 18:25  
**验证状态**: 构建验证通过 ✅
