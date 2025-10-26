# Kotlin 重构实施指南

> 本指南提供逐步重构指引，适合团队成员参考

---

## 📖 目录

1. [重构原则](#1-重构原则)
2. [重构前检查清单](#2-重构前检查清单)
3. [重构步骤模板](#3-重构步骤模板)
4. [常见重构模式](#4-常见重构模式)
5. [测试策略](#5-测试策略)
6. [Code Review 检查点](#6-code-review-检查点)
7. [常见陷阱](#7-常见陷阱)

---

## 1. 重构原则

### 核心原则

1. **小步快跑**: 每次提交只重构一个文件或模块
2. **测试先行**: 重构前必须有测试覆盖
3. **保持功能不变**: 重构不改变外部行为
4. **及时提交**: 每完成一个单元立即提交
5. **持续集成**: 确保CI通过再合并

### Boy Scout Rule

**让代码比你发现时更干净**

- 修复周围的小问题
- 改进命名
- 提取重复代码
- 添加注释或文档

---

## 2. 重构前检查清单

### 开始重构前

- [ ] 创建新分支 (命名: `refactor/module-name`)
- [ ] 确认原代码有测试覆盖 (至少30%)
- [ ] 理解代码功能和调用关系
- [ ] 检查是否有依赖此类的其他模块
- [ ] 备份关键配置

### 重构过程中

- [ ] 保持 IDE 代码检查无错误
- [ ] 每步修改后运行测试
- [ ] 提交信息清晰 (使用 Conventional Commits)
- [ ] 大重构时保留原Java文件(标记为@Deprecated)

### 完成后

- [ ] 所有测试通过
- [ ] ktlint 检查通过
- [ ] 代码覆盖率未降低
- [ ] 更新相关文档
- [ ] 创建 Pull Request

---

## 3. 重构步骤模板

### 步骤 1: 准备工作

```bash
# 创建重构分支
git checkout -b refactor/StringUtil

# 确保最新代码
git pull origin n
```

### 步骤 2: 分析原代码

```java
// 原Java代码示例: StringUtil.java
public class StringUtil {
    public static boolean isEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static String join(List<String> list, String delimiter) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(delimiter, list);
    }
}
```

**分析点**:
- ✅ 纯静态工具类 → 适合Kotlin扩展函数
- ✅ 无复杂依赖 → 简单迁移
- ✅ 可空处理 → 利用Kotlin空安全

### 步骤 3: 编写测试 (如果没有)

```kotlin
// StringUtilTest.kt
class StringUtilTest {
    @Test
    fun `isEmpty should handle null`() {
        assertTrue(StringUtil.isEmpty(null))
    }
    
    @Test
    fun `isEmpty should handle empty string`() {
        assertTrue(StringUtil.isEmpty(""))
    }
    
    @Test
    fun `join should work correctly`() {
        val result = StringUtil.join(listOf("a", "b", "c"), ",")
        assertEquals("a,b,c", result)
    }
}
```

### 步骤 4: 创建Kotlin版本

```kotlin
// StringUtil.kt
object StringUtil {
    // 兼容方法 (标记为过时，引导使用扩展函数)
    @Deprecated("Use String?.isNullOrEmpty() instead", ReplaceWith("str.isNullOrEmpty()"))
    @JvmStatic
    fun isEmpty(str: String?): Boolean = str.isNullOrEmpty()
    
    @Deprecated("Use joinToString() instead", ReplaceWith("list.joinToString(delimiter)"))
    @JvmStatic
    fun join(list: List<String>?, delimiter: String): String {
        return list?.joinToString(delimiter) ?: ""
    }
}

// 推荐的Kotlin风格扩展函数
fun String?.isNullOrEmpty(): Boolean = this == null || this.trim().isEmpty()
```

### 步骤 5: 运行测试

```bash
# 运行单元测试
./gradlew test --tests StringUtilTest

# 运行所有测试
./gradlew test
```

### 步骤 6: 逐步迁移调用方

```kotlin
// 旧调用方式 (Java风格)
if (StringUtil.isEmpty(userId)) { ... }

// 新调用方式 (Kotlin风格)
if (userId.isNullOrEmpty()) { ... }
```

**策略**: 
- IDE 自动重构: Analyze → Run Inspection by Name → "Deprecated API usage"
- 逐个文件替换并测试

### 步骤 7: 提交代码

```bash
git add .
git commit -m "refactor(util): migrate StringUtil to Kotlin

- Convert StringUtil to Kotlin object
- Add extension functions for idiomatic Kotlin
- Mark old methods as @Deprecated
- All tests passing

BREAKING CHANGE: Java callers should update to use extension functions"

git push origin refactor/StringUtil
```

### 步骤 8: 创建 PR

PR 模板:

```markdown
## 重构描述

迁移 `StringUtil.java` 到 `StringUtil.kt`

## 变更内容

- ✅ 转换为 Kotlin object
- ✅ 添加扩展函数
- ✅ 保持Java兼容性 (@JvmStatic)
- ✅ 标记旧方法为 @Deprecated

## 测试

- ✅ 所有单元测试通过
- ✅ ktlint 检查通过
- ✅ 代码覆盖率: 85%

## 迁移指南

Java调用方应更新为:
\`\`\`java
// 旧方式
StringUtil.isEmpty(str);

// 新方式 (在Kotlin)
str.isNullOrEmpty()
\`\`\`

## Checklist

- [x] 代码编译无错误
- [x] 测试全部通过
- [x] 更新文档
- [x] 更新重构跟踪表
```

---

## 4. 常见重构模式

### 模式1: 数据类转换

```java
// Java + Lombok
@Data
public class User {
    private String userId;
    private String name;
    private int energy;
}
```

```kotlin
// Kotlin data class
data class User(
    val userId: String,
    val name: String,
    var energy: Int
)
```

### 模式2: 单例转换

```java
// Java Singleton
public class Manager {
    private static Manager INSTANCE;
    
    public static Manager getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new Manager();
        }
        return INSTANCE;
    }
}
```

```kotlin
// Kotlin object
object Manager {
    // 自动单例
}
```

### 模式3: 异步回调转协程

```java
// Java 回调
public void loadData(Callback callback) {
    executor.execute(() -> {
        try {
            String data = fetchData();
            callback.onSuccess(data);
        } catch (Exception e) {
            callback.onError(e);
        }
    });
}
```

```kotlin
// Kotlin 协程
suspend fun loadData(): Result<String> = withContext(Dispatchers.IO) {
    try {
        val data = fetchData()
        Result.success(data)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
```

### 模式4: Builder 模式转具名参数

```java
// Java Builder
User user = User.builder()
    .userId("123")
    .name("Test")
    .energy(100)
    .build();
```

```kotlin
// Kotlin 具名参数
val user = User(
    userId = "123",
    name = "Test",
    energy = 100
)
```

---

## 5. 测试策略

### 测试金字塔

```
        /\      E2E Tests (10%)
       /  \     
      /____\    Integration Tests (20%)
     /      \   
    /________\  Unit Tests (70%)
```

### 重构时的测试顺序

1. **先写测试** (如果没有)
2. **运行测试** (确保通过)
3. **重构代码**
4. **再次运行测试** (确保仍然通过)
5. **重构测试** (使用Kotlin惯用法)

### 测试覆盖目标

| 模块类型 | 覆盖率目标 |
|---------|-----------|
| 工具类 | 80%+ |
| 数据模型 | 60%+ |
| 业务逻辑 | 70%+ |
| UI | 40%+ |

---

## 6. Code Review 检查点

### Kotlin 代码风格

- [ ] 使用 `val` 而非 `var` (优先不可变)
- [ ] 使用数据类 (`data class`)
- [ ] 使用扩展函数替代工具类
- [ ] 使用协程替代回调
- [ ] 正确使用空安全 (`?`, `!!`, `?.`, `?:`)
- [ ] 使用 `when` 替代多个 `if-else`
- [ ] 使用作用域函数 (`let`, `apply`, `run`, `also`, `with`)

### 性能考虑

- [ ] 避免不必要的对象创建
- [ ] 使用 `inline` 函数 (高阶函数)
- [ ] 合理使用序列 (`Sequence`) 而非集合
- [ ] 避免在循环中创建协程

### 兼容性

- [ ] 添加 `@JvmStatic` (Java调用静态方法)
- [ ] 添加 `@JvmOverloads` (默认参数)
- [ ] 添加 `@JvmName` (避免签名冲突)
- [ ] 使用 `@Deprecated` 标记旧API

---

## 7. 常见陷阱

### ❌ 陷阱1: 过度使用 `!!`

```kotlin
// ❌ 不好: 可能抛NPE
val user = getUserById(id)!!
val name = user.name!!

// ✅ 好: 安全处理
val user = getUserById(id) ?: return
val name = user.name ?: "Unknown"
```

### ❌ 陷阱2: 误用作用域函数

```kotlin
// ❌ 不好: let 没有意义
val result = data.let { it.process() }

// ✅ 好: 直接调用
val result = data.process()

// ✅ 好: let 用于空检查
val result = data?.let { it.process() }
```

### ❌ 陷阱3: 忘记 `@JvmStatic`

```kotlin
// ❌ Java调用困难
object Utils {
    fun doSomething() { }
}

// Java调用: Utils.INSTANCE.doSomething() // 难看!

// ✅ 添加 @JvmStatic
object Utils {
    @JvmStatic
    fun doSomething() { }
}

// Java调用: Utils.doSomething() // 完美!
```

### ❌ 陷阱4: 不必要的类型声明

```kotlin
// ❌ 不好: 类型显而易见
val name: String = "Test"
val list: List<String> = listOf("a", "b")

// ✅ 好: 类型推导
val name = "Test"
val list = listOf("a", "b")
```

### ❌ 陷阱5: 过早优化协程

```kotlin
// ❌ 不好: 简单计算不需要协程
suspend fun add(a: Int, b: Int) = withContext(Dispatchers.Default) {
    a + b  // 过度设计
}

// ✅ 好: 仅IO操作用协程
suspend fun fetchData() = withContext(Dispatchers.IO) {
    database.query()  // 真正的IO操作
}
```

---

## 📚 推荐阅读

1. **Kotlin官方文档**: https://kotlinlang.org/docs/
2. **Effective Kotlin**: https://kt.academy/book/effectivekotlin
3. **Kotlin协程指南**: https://kotlinlang.org/docs/coroutines-guide.html
4. **Android Kotlin风格指南**: https://developer.android.com/kotlin/style-guide
5. **Refactoring (Martin Fowler)**: 经典重构书籍

---

## 🆘 获取帮助

遇到问题时:

1. 查看本指南和分析报告
2. 搜索 Stack Overflow
3. 查阅 Kotlin 官方文档
4. 在团队群里提问
5. Code Review 时寻求帮助

---

**版本**: 1.0  
**最后更新**: 2025-10-25
