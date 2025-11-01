# ClassCastException Bug 深度分析与修复报告

## 🎯 问题概述

**症状：** 在Sesame-TK rc142中，Boolean类型的开关设置（如"启用模块"、"保持唤醒"等）无法正常保存，但数字类型的设置可以正常保存。

**错误日志：**
```
java.lang.ClassCastException: java.lang.Boolean cannot be cast to java.lang.Void
    at fansirsqi.xposed.sesame.model.ModelField.setConfigValue(ModelField.kt:219)
```

## 🔍 根本原因分析

### 1. 执行流程

当用户在WebView中修改配置并点击保存时，流程如下：

```
WebSettingsActivity.setModel()
  ↓
modelField.setConfigValue(configValue: String)
  ↓
fromConfigValue(configValue: String) → objectValue: Any
  ↓
valueType检查与修复
  ↓
JsonUtil.parseObject(configValue, valueType) → 抛出异常！
```

### 2. 问题的三重困境

#### 困境1：泛型擦除导致类型推断失败

在Xposed环境下，Kotlin的泛型类型推断会失败：

```kotlin
// ModelField.kt:54
constructor() {
    valueType = TypeUtil.getTypeArgument(this.javaClass.genericSuperclass, 0) ?: Any::class.java
}
```

`TypeUtil.getTypeArgument`在Xposed环境下返回`Void.TYPE`（即`void.class`），而不是正确的`Boolean::class.java`。

**原因：** Xposed的类加载机制破坏了泛型签名信息。

#### 困境2：父类构造函数过早调用setObjectValue

```kotlin
// ModelField.kt:71-84
constructor(code: String, name: String, value: T?) : this() {
    // ...
    if (valueType == Void.TYPE) {
        valueType = value.javaClass  // 尝试修复
    }
    setObjectValue(value)  // ⚠️ 过早调用！此时子类init块还没执行
}
```

执行顺序：
1. 父类无参构造 → valueType = Void.TYPE
2. 父类带参构造 → 修复valueType → 调用setObjectValue
3. **子类init块** → 再次修复valueType

问题：在步骤2调用`setObjectValue(value)`时，子类的init块还没执行，valueType可能仍然是错误的。

#### 困境3：fromConfigValue返回错误的类型

```kotlin
// ModelField.kt:179
open fun fromConfigValue(value: String?): Any? {
    return value  // ⚠️ 直接返回String！
}
```

当`configValue = "true"`时：
- `fromConfigValue("true")` 返回字符串 `"true"`（不是Boolean true！）
- `objectValue.javaClass` 是 `String.class`
- `valueType`被错误地设置为`String.class`
- `JsonUtil.parseObject("true", String.class)` 尝试解析...但实际字段是Boolean类型
- 内部类型检查发现`valueType`原本是`Void.TYPE`
- 抛出`ClassCastException: Boolean cannot be cast to Void`

### 3. 为什么IntegerModelField没有这个问题？

```kotlin
// IntegerModelField.kt:74-94
override fun setConfigValue(configValue: String?) {
    var newValue: Int = configValue.toInt()  // 直接解析为Int
    // ...
    this.value = newValue  // 直接赋值，不调用JsonUtil.parseObject
}
```

IntegerModelField**重写了setConfigValue**，完全绕过了父类的错误逻辑！

### 4. 为什么BooleanModelField有这个问题？

```kotlin
// BooleanModelField.kt
class BooleanModelField(...) : ModelField<Boolean>(...) {
    init {
        valueType = Boolean::class.java  // 在init块中修复
    }
    
    // ❌ 没有重写setConfigValue，使用父类的实现
}
```

BooleanModelField依赖父类的setConfigValue，而父类的实现有致命缺陷。

## 🛠️ 修复方案

### 方案1：添加调试日志（已实施）

在`ModelField.setConfigValue`中添加日志，确认类型修复逻辑是否执行：

```kotlin
// 如果反射类型推断失败，从objectValue推断真实类型
if (valueType == Any::class.java || valueType == Void::class.java || valueType == Void.TYPE) {
    valueType = objectValue.javaClass
    Log.runtime(TAG_FIELD, "setConfigValue: 类型已修复 $code: $valueTypeBefore -> $valueType (objectValue类型=${objectValue.javaClass})")
}
```

### 方案2：BooleanModelField重写setConfigValue（推荐）

模仿IntegerModelField的做法：

```kotlin
// BooleanModelField.kt
override fun setConfigValue(configValue: String?) {
    value = when {
        configValue.isNullOrBlank() -> defaultValue
        configValue.equals("true", ignoreCase = true) -> true
        configValue.equals("false", ignoreCase = true) -> false
        else -> {
            try {
                configValue.toBoolean()
            } catch (e: Exception) {
                Log.printStackTrace(e)
                defaultValue
            }
        }
    }
}
```

### 方案3：修复fromConfigValue（备选）

让fromConfigValue返回正确的类型：

```kotlin
// BooleanModelField.kt
override fun fromConfigValue(value: String?): Any? {
    return when {
        value.isNullOrBlank() -> null
        value.equals("true", ignoreCase = true) -> true
        value.equals("false", ignoreCase = true) -> false
        else -> value.toBoolean()
    }
}
```

## 📊 测试计划

### 测试步骤

1. 安装新版APK `sesame-tk-v0.3.0-rc143-debug.apk`
2. 完全卸载LSPosed和支付宝
3. 重新安装并配置
4. 测试以下场景：
   - ✅ Boolean开关保存（如"启用模块"）
   - ✅ Integer数字保存（如"执行间隔"）
   - ✅ Choice选择保存（如"定时任务模式"）
   - ✅ Select列表保存（如"不收能量列表"）

### 预期结果

- runtime.log中出现类型修复日志
- error.log中**没有**ClassCastException
- 所有类型的设置都能正常保存

## 📝 关键学习点

1. **Xposed环境的特殊性：** 泛型类型信息可能被破坏
2. **Kotlin构造顺序：** 父类构造 → 子类init → 子类构造体
3. **类型推断陷阱：** fromConfigValue返回String导致类型推断错误
4. **防御式编程：** 子类应该重写关键方法，不依赖父类的脆弱实现

## 🔗 相关文件

- `ModelField.kt` - 父类，包含错误的setConfigValue实现
- `BooleanModelField.kt` - 受影响的子类
- `IntegerModelField.kt` - 正确的实现参考
- `WebSettingsActivity.java` - 调用setConfigValue的地方
- `JsonUtil.kt` - JSON解析工具

## 📌 下一步

1. 查看新日志确认类型修复逻辑是否执行
2. 根据日志决定实施方案2或方案3
3. 编译rc143版本
4. 彻底测试所有功能

## ✅ 版本历史

- **rc138-rc141：** 尝试通过init块修复valueType，失败
- **rc142：** 添加调试日志
- **rc143（计划）：** 实施最终修复方案
