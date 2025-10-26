# 阶段3: Hook层迁移计划

**开始时间**: 2025-10-26  
**基于版本**: rc2296  
**前置条件**: ✅ 阶段2 Entity类迁移已完成

---

## 📋 阶段2回顾

**已完成**:
- ✅ CollectEnergyEntity.kt (131→76行, -42%)
- ✅ UserEntity.kt (102→67行, -34%)
- ✅ ReserveEntity.kt (65→54行, -17%)
- ✅ AlipayBeach.kt (56→50行, -11%)
- ✅ ParadiseCoinBenefit.kt (26→31行, -19%)

**总计**: 5个Entity类, 减少102行代码(27%)

---

## 🎯 阶段3目标

将Hook层的Java类迁移到Kotlin，按复杂度从低到高逐步迁移。

---

## 📊 Hook层Java文件清单

### 核心Hook文件
- `ApplicationHook.java` (~1140行) - ⚠️ **最复杂，留到最后**

### RPC桥接层
- `RpcVersion.java` (25行) - 枚举类 ✅ **优先级1**
- `RpcBridge.java` (58行) - 接口 ✅ **优先级2**
- `DebugRpcCall.java` (53行) - 工具类 ✅ **优先级2**
- `OldRpcBridge.java` (~300行) - 实现类 🔶 **优先级3**
- `NewRpcBridge.java` (~400行) - 实现类 🔶 **优先级3**

### 工具类
- `Toast.java` (76行) - 工具类 ✅ **优先级1**

### 调试工具
- `DebugRpc.java` (~100行) - 调试工具 ✅ **优先级2**

### 组件管理
- `AlipayComponentHelper.java` (~300行) - 组件管理 🔶 **优先级3**

---

## 📝 迁移策略

### 第一批: 简单工具类和枚举 (优先级1)
**预计时间**: 30分钟  
**文件数**: 2个

1. **RpcVersion.java → RpcVersion.kt**
   - 枚举类
   - 使用`enum class`
   - companion object替代静态map

2. **Toast.java → Toast.kt**
   - 静态工具类
   - 使用`object`单例
   - 简化线程切换逻辑

---

### 第二批: 接口和简单工具 (优先级2)
**预计时间**: 45分钟  
**文件数**: 3个

3. **RpcBridge.java → RpcBridge.kt**
   - 接口定义
   - default方法转为扩展函数或默认实现

4. **DebugRpcCall.java → DebugRpcCall.kt**
   - 静态工具类
   - 使用`object`单例

5. **DebugRpc.java → DebugRpc.kt**
   - 调试工具类
   - 简化JSON处理

---

### 第三批: 复杂实现类 (优先级3)
**预计时间**: 2小时  
**文件数**: 3个

6. **OldRpcBridge.java → OldRpcBridge.kt**
   - RpcBridge实现
   - 处理复杂的反射逻辑
   - 保持Java互操作性

7. **NewRpcBridge.java → NewRpcBridge.kt**
   - RpcBridge实现
   - 处理复杂的反射逻辑
   - 保持Java互操作性

8. **AlipayComponentHelper.java → AlipayComponentHelper.kt**
   - 组件管理类
   - 简化广播接收器逻辑

---

### 第四批: 核心Hook类 (优先级4 - 可选)
**预计时间**: 4+小时  
**文件数**: 1个

9. **ApplicationHook.java → ApplicationHook.kt** ⚠️
   - 最复杂的核心文件
   - 需要特别谨慎
   - 建议单独规划，分阶段迁移
   - **可以推迟到后续阶段**

---

## ⚙️ 迁移原则

### 1. 代码质量
- ✅ 使用Kotlin惯用法
- ✅ 空安全特性
- ✅ 数据类和密封类
- ✅ 扩展函数

### 2. 兼容性
- ✅ 保持`@JvmStatic`注解
- ✅ 保持公开API不变
- ✅ 每次迁移后构建验证

### 3. 安全性
- ✅ 小步迁移，逐个验证
- ✅ 保留Git提交记录
- ✅ 每批迁移后构建测试

---

## 📈 预期效果

### 代码减少
- **预计减少**: 150-200行 (15-20%)
- **第一批**: ~20行 (20%)
- **第二批**: ~40行 (25%)
- **第三批**: ~100行 (10%)

### 质量提升
- ✅ 空安全
- ✅ 更简洁
- ✅ 更易维护
- ✅ 更少样板代码

---

## ✅ 验证标准

每批迁移完成后需验证：
1. ✅ 构建成功 (`./gradlew assembleDebug`)
2. ✅ 无新增警告或错误
3. ✅ 功能测试通过
4. ✅ 生成新版APK

---

## 🚀 开始执行

**当前批次**: 第一批 (优先级1)  
**待迁移**:
1. RpcVersion.java (25行) → RpcVersion.kt
2. Toast.java (76行) → Toast.kt

**预计完成时间**: 30分钟  
**预计代码减少**: ~20行 (20%)

---

准备开始阶段3第一批迁移！🎯
