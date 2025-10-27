# 代码质量分析脚本
param(
    [string]$sourcePath = "D:\Sesame-TK-n\app\src\main\java"
)

$results = @{
    'NullPointerRisks' = @()
    'UnsafeCasts' = @()
    'EmptyCatchBlocks' = @()
    'TODOComments' = @()
    'LongMethods' = @()
    'MagicNumbers' = @()
}

Write-Host "====== 代码质量分析开始 ======" -ForegroundColor Green
Write-Host "扫描路径: $sourcePath`n" -ForegroundColor Cyan

# 1. 检查潜在的空指针风险 (Kotlin)
Write-Host "[1/6] 检查空指针风险..." -ForegroundColor Yellow
$nullRisks = Select-String -Path "$sourcePath\**\*.kt" -Pattern "as String|as Int|as Boolean|!!" -AllMatches
$results['NullPointerRisks'] = $nullRisks | Select-Object -First 20
Write-Host "  发现 $($nullRisks.Count) 处潜在空指针风险" -ForegroundColor White

# 2. 检查不安全的类型转换
Write-Host "[2/6] 检查不安全的类型转换..." -ForegroundColor Yellow
$unsafeCasts = Select-String -Path "$sourcePath\**\*.kt","$sourcePath\**\*.java" -Pattern "\s+as\s+\w+[^?]|(\(\w+\)\s*\w+)" -AllMatches
$results['UnsafeCasts'] = $unsafeCasts | Select-Object -First 15
Write-Host "  发现 $($unsafeCasts.Count) 处类型转换" -ForegroundColor White

# 3. 检查空的catch块
Write-Host "[3/6] 检查空的catch块..." -ForegroundColor Yellow
$emptyCatch = Select-String -Path "$sourcePath\**\*.java","$sourcePath\**\*.kt" -Pattern "catch.*\{[\s]*\}" -AllMatches
$results['EmptyCatchBlocks'] = $emptyCatch
Write-Host "  发现 $($emptyCatch.Count) 个空catch块" -ForegroundColor White

# 4. 统计TODO和FIXME
Write-Host "[4/6] 统计待办事项..." -ForegroundColor Yellow
$todos = Select-String -Path "$sourcePath\**\*.java","$sourcePath\**\*.kt" -Pattern "TODO|FIXME|XXX|HACK" -CaseSensitive:$false
$results['TODOComments'] = $todos | Select-Object -First 30
Write-Host "  发现 $($todos.Count) 个待办注释" -ForegroundColor White

# 5. 检查可能过长的方法 (>100行)
Write-Host "[5/6] 检查方法长度..." -ForegroundColor Yellow
$javaFiles = Get-ChildItem -Path $sourcePath -Recurse -Filter "*.java"
$longMethods = @()
foreach ($file in $javaFiles | Select-Object -First 50) {
    $content = Get-Content $file.FullName -Raw
    $methods = [regex]::Matches($content, '(public|private|protected)\s+\w+\s+\w+\s*\([^)]*\)\s*\{[^}]{800,}\}', [System.Text.RegularExpressions.RegexOptions]::Singleline)
    if ($methods.Count -gt 0) {
        $longMethods += [PSCustomObject]@{
            File = $file.Name
            Count = $methods.Count
        }
    }
}
$results['LongMethods'] = $longMethods
Write-Host "  发现 $($longMethods.Count) 个文件包含长方法" -ForegroundColor White

# 6. 检查魔术数字
Write-Host "[6/6] 检查魔术数字..." -ForegroundColor Yellow
$magicNumbers = Select-String -Path "$sourcePath\**\*.java" -Pattern "=\s*\d{3,}[^.0-9]|>\s*\d{3,}|<\s*\d{3,}" -AllMatches
$results['MagicNumbers'] = $magicNumbers | Select-Object -First 20
Write-Host "  发现 $($magicNumbers.Count) 处可能的魔术数字`n" -ForegroundColor White

# 生成报告
Write-Host "====== 分析结果摘要 ======" -ForegroundColor Green
Write-Host ""
Write-Host "🔍 潜在问题统计:" -ForegroundColor Cyan
Write-Host "  • 空指针风险: $($results['NullPointerRisks'].Count)" -ForegroundColor $(if ($results['NullPointerRisks'].Count -gt 50) { 'Red' } else { 'Yellow' })
Write-Host "  • 不安全类型转换: $($results['UnsafeCasts'].Count)" -ForegroundColor $(if ($results['UnsafeCasts'].Count -gt 100) { 'Red' } else { 'Yellow' })
Write-Host "  • 空catch块: $($results['EmptyCatchBlocks'].Count)" -ForegroundColor $(if ($results['EmptyCatchBlocks'].Count -gt 10) { 'Red' } else { 'Green' })
Write-Host "  • 待办注释: $($results['TODOComments'].Count)" -ForegroundColor Yellow
Write-Host "  • 过长方法: $($longMethods.Count) 个文件" -ForegroundColor $(if ($longMethods.Count -gt 10) { 'Red' } else { 'Yellow' })
Write-Host "  • 魔术数字: $($results['MagicNumbers'].Count)" -ForegroundColor Yellow
Write-Host ""

# 显示前几个问题示例
Write-Host "====== 优先级问题示例 ======" -ForegroundColor Green
Write-Host ""

if ($results['NullPointerRisks'].Count -gt 0) {
    Write-Host "⚠️ 空指针风险 (前5个):" -ForegroundColor Red
    $results['NullPointerRisks'] | Select-Object -First 5 | ForEach-Object {
        $relativePath = $_.Path -replace [regex]::Escape($sourcePath), ""
        Write-Host "  📄 $relativePath :$($_.LineNumber)" -ForegroundColor Gray
        Write-Host "     $($_.Line.Trim())" -ForegroundColor White
    }
    Write-Host ""
}

if ($results['EmptyCatchBlocks'].Count -gt 0) {
    Write-Host "⚠️ 空catch块:" -ForegroundColor Red
    $results['EmptyCatchBlocks'] | ForEach-Object {
        $relativePath = $_.Path -replace [regex]::Escape($sourcePath), ""
        Write-Host "  📄 $relativePath :$($_.LineNumber)" -ForegroundColor Gray
    }
    Write-Host ""
}

Write-Host "✓ 分析完成！详细结果已保存到内存。" -ForegroundColor Green
Write-Host ""

# 返回结果供后续使用
return $results
