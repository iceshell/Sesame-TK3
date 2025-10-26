# Week 2-3: 测试建设计划

> **阶段**: 方案A - 巩固成果  
> **目标**: 测试覆盖率达到30%+  
> **开始日期**: 2025-10-26  
> **预计时长**: 2-3周

---

## 🎯 总体目标

### 核心目标

**建立完善的测试体系，为已迁移的Kotlin代码提供保护**

| 指标 | 当前 | 目标 | 优先级 |
|------|------|------|--------|
| **测试覆盖率** | ~15% | **30%+** | ⭐⭐⭐⭐⭐ |
| **核心类测试** | 0 | 10+ | ⭐⭐⭐⭐⭐ |
| **工具类测试** | 部分 | 完整 | ⭐⭐⭐⭐ |
| **集成测试** | 0 | 5+ | ⭐⭐⭐ |

---

## 📋 测试优先级分类

### P0: 必须测试（核心基础设施）

**BaseTask 测试** ⭐⭐⭐⭐⭐
```kotlin
// 最高优先级 - 任务系统基类
app/src/test/java/fansirsqi/xposed/sesame/task/BaseTaskTest.kt

测试要点:
- 任务启动/停止
- 线程管理
- 子任务管理
- 状态管理
```

**Config 测试** ⭐⭐⭐⭐⭐
```kotlin
// 核心配置管理
app/src/test/java/fansirsqi/xposed/sesame/data/ConfigTest.kt

测试要点:
- 配置加载/保存
- 默认值处理
- 配置验证
- 并发安全
```

**Status 测试** ⭐⭐⭐⭐⭐
```kotlin
// 状态管理
app/src/test/java/fansirsqi/xposed/sesame/data/StatusTest.kt

测试要点:
- 状态保存/加载
- 状态清理
- 多用户状态
```

---

### P1: 应该测试（已迁移核心类）

**UserMap 测试** ⭐⭐⭐⭐
```kotlin
// 用户映射管理
app/src/test/java/fansirsqi/xposed/sesame/util/maps/UserMapTest.kt

测试要点:
- 用户加载/保存
- 映射操作
- 操作符重载
- 线程安全
```

**RpcBridge 测试** ⭐⭐⭐⭐
```kotlin
// RPC调用桥接
app/src/test/java/fansirsqi/xposed/sesame/hook/rpc/RpcBridgeTest.kt

测试要点:
- RPC调用格式化
- 参数处理
- 错误处理
```

**TypeUtil 测试** ⭐⭐⭐⭐
```kotlin
// 类型工具
app/src/test/java/fansirsqi/xposed/sesame/util/TypeUtilTest.kt

测试要点:
- 类型转换
- 反射操作
- 空安全处理
```

---

### P2: 建议测试（工具类补充）

**已迁移工具类测试补充** ⭐⭐⭐
```kotlin
// 补充现有测试
- StringUtilTest.kt (已有35个，补充边界情况)
- TimeUtilTest.kt (新增)
- FilesTest.kt (新增)
- JsonUtilTest.kt (新增)
```

---

## 📅 执行时间表

### Week 2: 核心测试（15-20小时）

#### Day 1-2: 测试框架搭建（4-5小时）

**任务清单**:
- [ ] 检查测试依赖配置
- [ ] 创建测试基类和工具
- [ ] 设置Mock框架
- [ ] 编写第一个示例测试
- [ ] 验证测试运行

**交付物**:
```
test/java/fansirsqi/xposed/sesame/
├── BaseTest.kt          - 测试基类
├── TestUtils.kt         - 测试工具
└── MockHelper.kt        - Mock辅助
```

---

#### Day 3-4: BaseTask 测试（4-5小时）

**测试用例清单**:
```kotlin
class BaseTaskTest {
    @Test fun `test task starts successfully`()
    @Test fun `test task stops gracefully`()
    @Test fun `test interrupted task`()
    @Test fun `test child task management`()
    @Test fun `test task status tracking`()
    @Test fun `test concurrent task execution`()
    @Test fun `test task timeout`()
    @Test fun `test task error handling`()
}
```

**预期覆盖率**: 70%+

---

#### Day 5-6: Config 测试（4-5小时）

**测试用例清单**:
```kotlin
class ConfigTest {
    @Test fun `test load config from file`()
    @Test fun `test save config to file`()
    @Test fun `test default values`()
    @Test fun `test invalid config handling`()
    @Test fun `test config migration`()
    @Test fun `test concurrent config access`()
    @Test fun `test config validation`()
}
```

**预期覆盖率**: 60%+

---

#### Day 7: Status 测试（3-4小时）

**测试用例清单**:
```kotlin
class StatusTest {
    @Test fun `test save status`()
    @Test fun `test load status`()
    @Test fun `test clear status`()
    @Test fun `test multi-user status`()
    @Test fun `test status persistence`()
}
```

**预期覆盖率**: 60%+

---

### Week 3: 扩展测试（16-22小时）

#### Day 1-2: RPC层测试（4-5小时）

**测试清单**:
```kotlin
// RpcBridge 测试
class RpcBridgeTest {
    @Test fun `test RPC call formatting`()
    @Test fun `test parameter serialization`()
    @Test fun `test response parsing`()
    @Test fun `test error handling`()
}

// RpcVersion 测试
class RpcVersionTest {
    @Test fun `test version detection`()
    @Test fun `test version compatibility`()
}
```

**预期覆盖率**: 50%+

---

#### Day 3-4: Map管理测试（4-5小时）

**测试清单**:
```kotlin
// UserMap 测试
class UserMapTest {
    @Test fun `test load users`()
    @Test fun `test save users`()
    @Test fun `test operator get`()
    @Test fun `test thread safety`()
}

// IdMapManager 测试
class IdMapManagerTest {
    @Test fun `test map operations`()
    @Test fun `test persistence`()
}
```

**预期覆盖率**: 60%+

---

#### Day 5-6: 工具类测试补充（4-6小时）

**测试清单**:
```kotlin
// TimeUtil 测试（新增）
class TimeUtilTest {
    @Test fun `test time formatting`()
    @Test fun `test time parsing`()
    @Test fun `test timezone handling`()
}

// Files 测试（新增）
class FilesTest {
    @Test fun `test file read write`()
    @Test fun `test directory operations`()
    @Test fun `test file existence check`()
}

// JsonUtil 测试（新增）
class JsonUtilTest {
    @Test fun `test JSON serialization`()
    @Test fun `test JSON deserialization`()
    @Test fun `test error handling`()
}
```

**预期覆盖率**: 70%+

---

#### Day 7: 集成测试（4-6小时）

**测试场景**:
```kotlin
class IntegrationTest {
    @Test fun `test config load and task execution`()
    @Test fun `test user data persistence`()
    @Test fun `test RPC call flow`()
    @Test fun `test error recovery`()
}
```

---

## 🛠️ 技术准备

### 测试依赖检查

**必需依赖**:
```kotlin
// build.gradle.kts
dependencies {
    // JUnit
    testImplementation("junit:junit:4.13.2")
    
    // Kotlin测试
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    
    // 协程测试
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // MockK (Kotlin Mock框架)
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    
    // Truth (断言库)
    testImplementation("com.google.truth:truth:1.1.5")
}
```

---

### 测试基础架构

**创建测试基类**:
```kotlin
// BaseTest.kt
abstract class BaseTest {
    @Before
    fun setUp() {
        // 通用设置
    }
    
    @After
    fun tearDown() {
        // 清理
    }
}
```

**创建测试工具**:
```kotlin
// TestUtils.kt
object TestUtils {
    fun createTestConfig(): Config { ... }
    fun createTestUser(): UserEntity { ... }
    fun mockRpcResponse(): String { ... }
}
```

---

## 📊 成功标准

### Week 2 结束标准

- [ ] 测试覆盖率 > 20%
- [ ] BaseTask 测试完成 (8+用例)
- [ ] Config 测试完成 (7+用例)
- [ ] Status 测试完成 (5+用例)
- [ ] 所有测试通过

### Week 3 结束标准

- [ ] 测试覆盖率 > 30%
- [ ] RPC层测试完成
- [ ] Map管理测试完成
- [ ] 工具类测试补充完成
- [ ] 集成测试完成 (3+用例)
- [ ] 所有测试通过

---

## 🎯 测试覆盖率目标

### 模块覆盖率

| 模块 | 当前 | 目标 | 优先级 |
|------|------|------|--------|
| **task/BaseTask** | 0% | 70%+ | P0 |
| **data/Config** | 0% | 60%+ | P0 |
| **data/Status** | 0% | 60%+ | P0 |
| **util/maps** | 0% | 60%+ | P1 |
| **hook/rpc** | 0% | 50%+ | P1 |
| **util (已迁移)** | 15% | 70%+ | P2 |

### 总体覆盖率路线图

```
Week 1: ~15% (基线)
Week 2: ~20% (核心类测试)
Week 3: ~30% (扩展测试)
目标:   30%+ ✅
```

---

## 💡 测试最佳实践

### 测试命名规范

```kotlin
// ✅ 推荐: 描述性命名
@Test
fun `test load config from file successfully`() { ... }

@Test
fun `test save config throws exception when file is read-only`() { ... }

// ❌ 避免: 不清晰的命名
@Test
fun testConfig1() { ... }
```

### Given-When-Then 模式

```kotlin
@Test
fun `test user map returns user when exists`() {
    // Given: 准备测试数据
    val userId = "12345"
    val user = UserEntity(userId, "Test User")
    userMap[userId] = user
    
    // When: 执行操作
    val result = userMap[userId]
    
    // Then: 验证结果
    assertEquals(user, result)
    assertEquals("Test User", result?.userName)
}
```

### Mock使用

```kotlin
@Test
fun `test RPC call with mocked response`() {
    // Mock RPC响应
    val mockResponse = """{"result": "success"}"""
    every { rpcBridge.call(any()) } returns mockResponse
    
    // 执行测试
    val result = service.execute()
    
    // 验证
    assertTrue(result.isSuccess)
    verify { rpcBridge.call(any()) }
}
```

---

## 🚨 常见问题预案

### Q1: 如何测试依赖Android API的代码？

**方案**:
1. 使用Robolectric模拟Android环境
2. 抽象Android依赖，注入Mock
3. 使用@RunWith(AndroidJUnit4::class)

### Q2: 如何测试协程代码？

**方案**:
```kotlin
@Test
fun `test suspend function`() = runTest {
    // 使用runTest提供的测试协程作用域
    val result = suspendFunction()
    assertEquals(expected, result)
}
```

### Q3: 如何测试文件操作？

**方案**:
```kotlin
@Test
fun `test file operations`() {
    // 使用临时目录
    val tempDir = Files.createTempDirectory("test")
    try {
        // 执行测试
        val file = File(tempDir.toFile(), "test.txt")
        Files.write2File("content", file)
        
        // 验证
        assertTrue(file.exists())
    } finally {
        // 清理
        tempDir.toFile().deleteRecursively()
    }
}
```

---

## 📝 Daily Checklist

### 每日测试工作流

```markdown
## 日期: YYYY-MM-DD

### 今日目标
- [ ] 完成XX类测试
- [ ] 编写X个测试用例
- [ ] 修复X个测试失败

### 执行情况
- [x] 已完成测试
- [ ] 进行中测试

### 覆盖率
- 今日新增: X%
- 累计: X%

### 问题
- 问题描述
- 解决方案

### 明日计划
- [ ] 下一个测试目标
```

---

## 🎊 激励机制

### 里程碑奖励

- 🎯 **覆盖率20%**: 基础测试框架建立
- 🎯 **覆盖率25%**: 核心类保护完成
- 🎯 **覆盖率30%**: 目标达成！🎉

### 进度追踪

```
测试覆盖率: [░░░░░░░░░░] 15% (基线)
           ↓
测试覆盖率: [███░░░░░░░] 20% (Week 2)
           ↓
测试覆盖率: [██████░░░░] 30%+ (Week 3) ✅
```

---

## 📚 参考资源

### 测试指南

- [Kotlin测试官方文档](https://kotlinlang.org/docs/jvm-test-using-junit.html)
- [MockK用户指南](https://mockk.io/)
- [协程测试指南](https://kotlinlang.org/docs/coroutines-testing.html)

### 项目文档

- [测试设置指南](./docs/test-setup-guide.md)
- [Week 1完成总结](./WEEK1_COMPLETION_SUMMARY.md)
- [下一步计划](./NEXT_STEPS.md)

---

## 🔄 持续改进

### 每周回顾

**检查项**:
- [ ] 覆盖率是否达标
- [ ] 测试质量如何
- [ ] 有哪些改进空间
- [ ] 遇到什么问题

### 调整策略

根据实际进度调整:
- 优先级重排
- 时间分配调整
- 测试范围调整

---

**创建时间**: 2025-10-26 18:45  
**计划周期**: 2-3周  
**目标**: 测试覆盖率30%+  
**状态**: 准备启动 🚀
