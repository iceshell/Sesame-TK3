@echo off
echo ============================================
echo 测试验证脚本
echo ============================================
echo.

echo [1/3] 清理构建...
call gradlew clean

echo.
echo [2/3] 编译测试代码...
call gradlew compileNormalDebugUnitTestKotlin compileCompatibleDebugJavaWithJavac

echo.
echo [3/3] 运行单元测试...
call gradlew testNormalDebugUnitTest --rerun-tasks

echo.
echo ============================================
echo 测试完成！
echo ============================================
pause
