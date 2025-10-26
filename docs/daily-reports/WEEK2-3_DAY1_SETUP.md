# Week 2-3 Day 1: 测试框架搭建

> **日期**: 2025-10-26  
> **任务**: 搭建测试基础设施  
> **预计时间**: 4-5小时

---

## 📋 任务清单

### ✅ 已完成

1. **创建测试计划** ✅
   - WEEK2-3_TEST_PLAN.md (详细计划)
   - 定义测试优先级
   - 制定时间表

### 🔄 进行中

2. **检查现有测试状态** ✅
   - 现有测试文件: 1个 (StringUtilTest.kt)
   - 测试覆盖率: ~15% (估算)
   - 测试依赖: 不完整 ⚠️

### ⏳ 待完成

3. **添加测试依赖**
   - [ ] JUnit 4.13.2
   - [ ] Kotlin Test
   - [ ] MockK 1.13.8
   - [ ] Coroutines Test
   - [ ] Truth (断言库)

4. **创建测试基础架构**
   - [ ] BaseTest.kt (测试基类)
   - [ ] TestUtils.kt (测试工具)
   - [ ] MockHelper.kt (Mock辅助)

5. **编写示例测试**
   - [ ] 验证测试环境
   - [ ] 运行测试确认

---

## 🛠️ 实施步骤

### Step 1: 添加测试依赖

**需要添加到 app/build.gradle.kts**:

```kotlin
dependencies {
    // 现有依赖...
    
    // ========== 测试依赖 (新增) ==========
    
    // JUnit - 基础测试框架
    testImplementation("junit:junit:4.13.2")
    
    // Kotlin 测试
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.20")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:2.2.20")
    
    // 协程测试
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
    // MockK - Kotlin Mock框架
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("io.mockk:mockk-android:1.13.8")
    
    // Truth - Google断言库 (可选，更友好的断言)
    testImplementation("com.google.truth:truth:1.1.5")
    
    // Robolectric - Android单元测试 (可选)
    testImplementation("org.robolectric:robolectric:4.11.1")
}
```

**添加位置**: 在 `dependencies {` 块的末尾，在现有依赖之后

---

### Step 2: 创建测试基础架构

#### 2.1 创建测试基类

**文件**: `app/src/test/java/fansirsqi/xposed/sesame/BaseTest.kt`

```kotlin
package fansirsqi.xposed.sesame

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName

/**
 * 测试基类
 * 提供通用的测试设置和清理
 */
abstract class BaseTest {
    
    @get:Rule
    val testName = TestName()
    
    @Before
    fun baseSetUp() {
        println("▶️ 开始测试: ${testName.methodName}")
    }
    
    @After
    fun baseTearDown() {
        println("✅ 完成测试: ${testName.methodName}")
    }
    
    /**
     * 子类可以override进行额外设置
     */
    open fun setUp() {}
    
    /**
     * 子类可以override进行额外清理
     */
    open fun tearDown() {}
}
```

---

#### 2.2 创建测试工具类

**文件**: `app/src/test/java/fansirsqi/xposed/sesame/TestUtils.kt`

```kotlin
package fansirsqi.xposed.sesame

import java.io.File

/**
 * 测试工具类
 * 提供测试中常用的辅助方法
 */
object TestUtils {
    
    /**
     * 创建临时测试目录
     */
    fun createTempDir(prefix: String = "test"): File {
        return kotlin.io.path.createTempDirectory(prefix).toFile().apply {
            deleteOnExit()
        }
    }
    
    /**
     * 创建临时测试文件
     */
    fun createTempFile(prefix: String = "test", suffix: String = ".tmp"): File {
        return kotlin.io.path.createTempFile(prefix, suffix).toFile().apply {
            deleteOnExit()
        }
    }
    
    /**
     * 创建测试用JSON字符串
     */
    fun createTestJson(vararg pairs: Pair<String, Any>): String {
        val entries = pairs.joinToString(",") { (key, value) ->
            val valueStr = when (value) {
                is String -> "\"$value\""
                is Number -> value.toString()
                is Boolean -> value.toString()
                else -> "\"$value\""
            }
            "\"$key\":$valueStr"
        }
        return "{$entries}"
    }
    
    /**
     * 延迟执行（用于测试异步代码）
     */
    fun delay(millis: Long) {
        Thread.sleep(millis)
    }
}
```

---

#### 2.3 创建Mock辅助类

**文件**: `app/src/test/java/fansirsqi/xposed/sesame/MockHelper.kt`

```kotlin
package fansirsqi.xposed.sesame

import io.mockk.mockk
import io.mockk.every
import java.io.File

/**
 * Mock辅助类
 * 提供常用的Mock对象创建
 */
object MockHelper {
    
    /**
     * 创建Mock文件对象
     */
    fun createMockFile(
        path: String = "/test/file.txt",
        exists: Boolean = true,
        content: String = "test content"
    ): File {
        return mockk<File>(relaxed = true) {
            every { this@mockk.exists() } returns exists
            every { this@mockk.path } returns path
            every { this@mockk.name } returns File(path).name
            every { this@mockk.readText() } returns content
        }
    }
    
    /**
     * 创建测试用的配置数据
     */
    fun createTestConfig(): Map<String, String> {
        return mapOf(
            "enableModule" to "true",
            "userId" to "test_user_123",
            "collectInterval" to "60"
        )
    }
}
```

---

### Step 3: 创建示例测试

**文件**: `app/src/test/java/fansirsqi/xposed/sesame/TestFrameworkTest.kt`

```kotlin
package fansirsqi.xposed.sesame

import org.junit.Test
import org.junit.Assert.*
import io.mockk.mockk
import io.mockk.every
import io.mockk.verify

/**
 * 测试框架验证测试
 * 确保测试环境正确配置
 */
class TestFrameworkTest : BaseTest() {
    
    @Test
    fun `test JUnit is working`() {
        // Given
        val expected = 42
        val actual = 42
        
        // Then
        assertEquals(expected, actual)
        assertTrue(actual > 0)
    }
    
    @Test
    fun `test Kotlin test extensions`() {
        // Given
        val list = listOf(1, 2, 3)
        
        // Then
        kotlin.test.assertEquals(3, list.size)
        kotlin.test.assertTrue(list.contains(2))
    }
    
    @Test
    fun `test MockK is working`() {
        // Given
        val mockFile = mockk<java.io.File>()
        every { mockFile.exists() } returns true
        every { mockFile.name } returns "test.txt"
        
        // When
        val exists = mockFile.exists()
        val name = mockFile.name
        
        // Then
        assertTrue(exists)
        assertEquals("test.txt", name)
        verify { mockFile.exists() }
        verify { mockFile.name }
    }
    
    @Test
    fun `test TestUtils is working`() {
        // Given & When
        val tempDir = TestUtils.createTempDir("test")
        val tempFile = TestUtils.createTempFile("test", ".txt")
        val json = TestUtils.createTestJson(
            "name" to "Test",
            "age" to 25,
            "active" to true
        )
        
        // Then
        assertTrue(tempDir.exists())
        assertTrue(tempFile.exists())
        assertTrue(json.contains("\"name\":\"Test\""))
        assertTrue(json.contains("\"age\":25"))
        assertTrue(json.contains("\"active\":true"))
        
        // Cleanup
        tempDir.deleteRecursively()
        tempFile.delete()
    }
    
    @Test
    fun `test MockHelper is working`() {
        // Given & When
        val mockFile = MockHelper.createMockFile(
            path = "/test/mock.txt",
            exists = true,
            content = "mock content"
        )
        val config = MockHelper.createTestConfig()
        
        // Then
        assertTrue(mockFile.exists())
        assertEquals("mock.txt", mockFile.name)
        assertEquals("mock content", mockFile.readText())
        assertEquals("true", config["enableModule"])
        assertEquals("test_user_123", config["userId"])
    }
}
```

---

### Step 4: 运行测试

```bash
# 运行所有测试
./gradlew test

# 运行特定测试
./gradlew test --tests TestFrameworkTest

# 查看测试报告
start app/build/reports/tests/testNormalDebugUnitTest/index.html
```

---

## 📊 验证清单

### 依赖验证
- [ ] JUnit依赖添加成功
- [ ] MockK依赖添加成功
- [ ] Kotlin Test依赖添加成功
- [ ] Coroutines Test依赖添加成功
- [ ] Gradle sync成功

### 基础架构验证
- [ ] BaseTest.kt创建成功
- [ ] TestUtils.kt创建成功
- [ ] MockHelper.kt创建成功
- [ ] 无编译错误

### 测试验证
- [ ] TestFrameworkTest.kt创建成功
- [ ] 所有测试用例通过
- [ ] 测试报告生成成功

---

## 🎯 成功标准

### Day 1 完成标准
- [x] 测试计划完成
- [ ] 测试依赖添加完成
- [ ] 测试基础架构创建完成
- [ ] 示例测试运行成功
- [ ] 5个测试用例全部通过

---

## 💡 实施建议

### 添加依赖的步骤
1. 打开 `app/build.gradle.kts`
2. 找到 `dependencies {` 块
3. 在末尾添加测试依赖
4. 点击 "Sync Now" 或运行 `./gradlew sync`
5. 等待同步完成

### 创建文件的步骤
1. 创建测试目录（如果不存在）:
   ```
   app/src/test/java/fansirsqi/xposed/sesame/
   ```
2. 在目录下创建kt文件
3. 粘贴代码
4. 保存并格式化

### 运行测试的步骤
1. 在IDE中右键点击测试类
2. 选择 "Run 'TestFrameworkTest'"
3. 或使用命令行: `./gradlew test`

---

## 🚨 常见问题

### Q1: Gradle sync失败？
**解决**: 
- 检查网络连接
- 清理缓存: `./gradlew clean`
- 删除 `.gradle` 目录重试

### Q2: 找不到MockK?
**解决**:
- 确认依赖版本正确
- 检查Maven仓库配置
- 尝试使用代理或镜像

### Q3: 测试运行失败?
**解决**:
- 检查JVM版本 (需要JDK 17+)
- 确认测试代码无语法错误
- 查看详细错误日志

---

## 📝 进度记录

### 当前状态
```
Day 1 进度: [███░░░░░░░] 30%

✅ 创建测试计划
✅ 检查现有状态
⏳ 添加测试依赖 (待执行)
⏳ 创建基础架构 (待执行)
⏳ 运行示例测试 (待执行)
```

### 下一步
1. 添加测试依赖到build.gradle.kts
2. 创建测试基础架构文件
3. 运行测试验证

---

**创建时间**: 2025-10-26 18:50  
**预计完成**: 2025-10-26 或 2025-10-27  
**状态**: 准备执行 🚀
