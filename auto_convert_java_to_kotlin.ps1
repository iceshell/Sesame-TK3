# 自动批量转换Java到Kotlin
$files = @(
    "app\src\main\java\fansirsqi\xposed\sesame\task\antMember\AntMember.java",
    "app\src\main\java\fansirsqi\xposed\sesame\task\antOcean\AntOcean.java",
    "app\src\main\java\fansirsqi\xposed\sesame\task\antSports\AntSports.java",
    "app\src\main\java\fansirsqi\xposed\sesame\task\antStall\AntStall.java",
    "app\src\main\java\fansirsqi\xposed\sesame\ui\WebSettingsActivity.java",
    "app\src\main\java\fansirsqi\xposed\sesame\hook\ApplicationHook.java",
    "app\src\main\java\fansirsqi\xposed\sesame\task\antForest\AntForestRpcCall.java"
)

Write-Host "准备转换 $($files.Count) 个Java文件为Kotlin..." -ForegroundColor Green

foreach ($file in $files) {
    $fullPath = Join-Path $PSScriptRoot $file
    if (Test-Path $fullPath) {
        Write-Host "`n正在转换: $file" -ForegroundColor Yellow
        
        # 使用Android Studio的Java转Kotlin工具
        # 方法1: 使用IntelliJ IDEA命令行
        $ideaBin = "D:\Android\Android Studio\bin\idea64.exe"
        if (Test-Path $ideaBin) {
            & $ideaBin convertJ2K "$fullPath"
            Write-Host "已转换: $file" -ForegroundColor Green
        } else {
            Write-Host "未找到Android Studio，请手动转换" -ForegroundColor Red
        }
    } else {
        Write-Host "文件不存在: $file" -ForegroundColor Red
    }
}

Write-Host "`n转换完成！" -ForegroundColor Green
