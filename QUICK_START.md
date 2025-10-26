# 🚀 测试快速开始指南

## 📋 当前状态

✅ **46个测试用例已创建**  
✅ **编译成功 (0错误)**  
🔄 **等待运行验证**

---

## ⚡ 快速运行测试

### 方法1: IDE运行 (最简单) ⭐⭐⭐⭐⭐

1. **在项目树中找到**:
   ```
   app/src/test/java/fansirsqi/xposed/sesame/
   ```

2. **右键点击** `sesame` 文件夹

3. **选择**: `Run 'Tests in 'fansirsqi.xposed.sesame''`

4. **等待结果** (约30秒)

---

### 方法2: 运行单个测试类

#### TestFrameworkTest (12个测试)
1. 打开 `TestFrameworkTest.kt`
2. 右键类名 `TestFrameworkTest`
3. 选择 "Run 'TestFrameworkTest'"

#### BaseTaskTest (17个测试)
1. 打开 `BaseTaskTest.kt`
2. 右键类名 `BaseTaskTest`
3. 选择 "Run 'BaseTaskTest'"

#### ConfigTest (17个测试)
1. 打开 `ConfigTest.kt`
2. 右键类名 `ConfigTest`
3. 选择 "Run 'ConfigTest'"

---

### 方法3: 使用Gradle命令

```bash
# 运行所有测试
./gradlew :app:testNormalDebugUnitTest

# 查看测试报告
start app\build\reports\tests\testNormalDebugUnitTest\index.html
```

---

## 📊 预期结果

### ✅ 成功输出
```
TestFrameworkTest
  ✓ test basic assertions work correctly
  ✓ test assertEquals compares values
  ... (12个测试全部通过)

BaseTaskTest
  ✓ test task getId returns correct id
  ✓ test task check returns true by default
  ... (17个测试全部通过)

ConfigTest
  ✓ test Config INSTANCE is singleton
  ✓ test isLoaded returns boolean
  ... (17个测试全部通过)

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
总计: 46 tests passed ✅
```

---

## ❌ 如果测试失败

### 请提供以下信息:

1. **失败的测试名称**
   ```
   例如: test task getId returns correct id - FAILED
   ```

2. **错误信息**
   ```
   例如: Expected: "test-task" but was: "wrong-task"
   ```

3. **堆栈跟踪** (如果有)

我会立即修复！

---

## 📁 测试文件位置

```
D:\Sesame-TK-n\app\src\test\java\fansirsqi\xposed\sesame\
├── TestFrameworkTest.kt    ← 12个测试
├── task\
│   └── BaseTaskTest.kt     ← 17个测试
└── data\
    └── ConfigTest.kt       ← 17个测试
```

---

## 🎯 测试完成后

### 如果全部通过 ✅
告诉我: "测试全部通过"

我会继续创建:
- **Day 4: Status测试** (10-15个测试)
- 其他核心类测试

### 如果有失败 ❌
告诉我: "XX测试失败" + 错误信息

我会立即修复问题

---

## 💡 提示

- 测试运行时间: 约30秒
- 如果IDE卡住: 重启IDE
- 如果Gradle出错: 运行 `./gradlew clean`

---

**准备好了吗？请运行测试！** 🚀
