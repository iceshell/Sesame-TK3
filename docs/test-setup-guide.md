# 单元测试框架配置指南

## 测试框架已配置

项目已经配置了测试框架，包括：

### 1. 依赖配置

在 `app/build.gradle.kts` 中添加测试依赖：

```kotlin
dependencies {
    // 单元测试
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    
    // Android 测试
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    // Compose 测试
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
```

## 2. 测试目录结构

```
app/src/
├── test/java/fansirsqi/xposed/sesame/     # 单元测试
│   ├── util/
│   │   ├── StringUtilTest.kt
│   │   ├── TimeUtilTest.kt
│   │   └── JsonUtilTest.kt
│   ├── model/
│   │   └── ModelTaskTest.kt
│   └── task/
│       └── AntForestTest.kt
│
└── androidTest/java/fansirsqi/xposed/sesame/  # 集成测试
    ├── HookTest.kt
    └── UITest.kt
```

## 3. 示例测试代码

### 单元测试示例 (StringUtilTest.kt)

```kotlin
package fansirsqi.xposed.sesame.util

import org.junit.Test
import org.junit.Assert.*

class StringUtilTest {
    
    @Test
    fun `isEmpty should return true for null string`() {
        val result = StringUtil.isEmpty(null)
        assertTrue(result)
    }
    
    @Test
    fun `isEmpty should return true for empty string`() {
        val result = StringUtil.isEmpty("")
        assertTrue(result)
    }
    
    @Test
    fun `isEmpty should return false for non-empty string`() {
        val result = StringUtil.isEmpty("test")
        assertFalse(result)
    }
}
```

### 协程测试示例 (ModelTaskTest.kt)

```kotlin
package fansirsqi.xposed.sesame.task

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

@OptIn(ExperimentalCoroutinesApi::class)
class ModelTaskTest {
    
    @Test
    fun `task should start successfully`() = runTest {
        val task = object : ModelTask() {
            override fun getName() = "TestTask"
            override fun getFields() = null
            override fun check() = true
            
            override suspend fun runSuspend() {
                // 测试逻辑
            }
        }
        
        val job = task.startTask()
        assertNotNull(job)
        job?.join()
    }
}
```

### Mock测试示例 (使用MockK)

```kotlin
import io.mockk.*
import org.junit.Test
import org.junit.Assert.*

class RpcBridgeTest {
    
    @Test
    fun `RPC call should handle errors gracefully`() {
        // Mock RequestManager
        mockkObject(RequestManager)
        every { 
            RequestManager.requestString(any(), any(), any()) 
        } returns """{"success": true}"""
        
        // 测试逻辑
        val result = AntForestRpcCall.queryFriendsEnergyRanking()
        
        assertNotNull(result)
        assertTrue(result.contains("success"))
        
        // 验证调用
        verify { RequestManager.requestString(any(), any(), any()) }
        
        unmockkAll()
    }
}
```

## 4. 运行测试

### 命令行运行

```bash
# 运行所有单元测试
./gradlew test

# 运行特定测试类
./gradlew test --tests StringUtilTest

# 运行Android集成测试
./gradlew connectedAndroidTest

# 生成测试覆盖率报告
./gradlew testDebugUnitTestCoverage
```

### Android Studio 运行

1. 右键点击测试类或测试方法
2. 选择 "Run 'TestClassName'"
3. 查看测试结果窗口

## 5. 测试覆盖率配置

在 `app/build.gradle.kts` 中添加：

```kotlin
android {
    buildTypes {
        debug {
            enableUnitTestCoverage = true
            enableAndroidTestCoverage = true
        }
    }
    
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}
```

## 6. 测试最佳实践

### DO ✅

1. **遵循 AAA 模式**: Arrange（准备）, Act（执行）, Assert（断言）
2. **测试命名清晰**: 使用反引号描述测试场景
3. **一个测试一个场景**: 不要在一个测试中测试多个功能
4. **使用 @Before/@After**: 统一初始化和清理
5. **Mock 外部依赖**: 使用 MockK mock Xposed、RPC等
6. **测试边界条件**: null、空字符串、极限值

### DON'T ❌

1. **不要测试私有方法**: 通过公共API测试
2. **不要依赖测试顺序**: 每个测试应该独立
3. **不要硬编码数据**: 使用测试数据构建器
4. **不要忽略失败测试**: 修复或标记为 @Ignore

## 7. 关键模块测试优先级

### 高优先级（必须测试）⭐⭐⭐⭐⭐

- [ ] `util/StringUtil` - 工具类
- [ ] `util/TimeUtil` - 时间工具
- [ ] `entity/*` - 数据模型
- [ ] `model/ModelTask` - 核心任务框架
- [ ] `hook/RequestManager` - RPC调用

### 中优先级（重点测试）⭐⭐⭐

- [ ] `task/antForest/AntForest` - 森林任务
- [ ] `task/antFarm/AntFarm` - 农场任务
- [ ] `hook/AlarmScheduler` - 定时调度

### 低优先级（选择性测试）⭐

- [ ] UI组件
- [ ] 辅助工具

## 8. 测试覆盖率目标

```
阶段0目标: 30% (准备阶段)
阶段1目标: 40% (工具类完成)
阶段2目标: 50% (数据模型完成)
最终目标: 60%+ (核心功能全覆盖)
```

## 9. CI集成

测试将集成到 GitHub Actions，每次 PR 自动运行。详见下一节 CI/CD 配置。

---

**下一步**: 配置 CI/CD 自动化测试
