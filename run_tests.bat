@echo off
echo ========================================
echo Running All Unit Tests
echo ========================================
echo.

echo [1/3] Running TestFrameworkTest...
call gradlew :app:testNormalDebugUnitTest --tests "fansirsqi.xposed.sesame.TestFrameworkTest" --console=plain
echo.

echo [2/3] Running BaseTaskTest...
call gradlew :app:testNormalDebugUnitTest --tests "fansirsqi.xposed.sesame.task.BaseTaskTest" --console=plain
echo.

echo [3/3] Running ConfigTest...
call gradlew :app:testNormalDebugUnitTest --tests "fansirsqi.xposed.sesame.data.ConfigTest" --console=plain
echo.

echo ========================================
echo All Tests Completed!
echo ========================================
echo.
echo Check results at: app\build\reports\tests\testNormalDebugUnitTest\index.html
pause
