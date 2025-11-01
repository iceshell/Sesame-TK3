# Documentation Cleanup Script
# Keep only 3 core documents

$docsRoot = "D:\Sesame-TK-n\docs"

# Core documents to keep
$keepFiles = @(
    "PROJECT_SUMMARY.md",
    "DEVELOPMENT_GUIDE.md",
    "PERFORMANCE_OPTIMIZATION_PLAN.md"
)

Write-Host "Starting documentation cleanup..." -ForegroundColor Green
Write-Host "Will keep only these files:" -ForegroundColor Cyan
$keepFiles | ForEach-Object { Write-Host "  - $_" -ForegroundColor Gray }

# Get all files in docs root (excluding subdirectories)
$allFiles = Get-ChildItem -Path $docsRoot -File

$removedCount = 0
foreach ($file in $allFiles) {
    if ($keepFiles -notcontains $file.Name) {
        Write-Host "Removing: $($file.Name)" -ForegroundColor Yellow
        Remove-Item $file.FullName -Force
        $removedCount++
    }
}

# Remove subdirectories
$subdirs = @("archive", "bug-fixes", "migration", "testing", "daily-reports")
foreach ($dir in $subdirs) {
    $dirPath = Join-Path $docsRoot $dir
    if (Test-Path $dirPath) {
        Write-Host "Removing directory: $dir" -ForegroundColor Yellow
        Remove-Item $dirPath -Recurse -Force
    }
}

Write-Host "`n[SUCCESS] Cleanup completed!" -ForegroundColor Green
Write-Host "Removed $removedCount files and subdirectories" -ForegroundColor Cyan
Write-Host "`nRemaining files in docs/:" -ForegroundColor Cyan
Get-ChildItem -Path $docsRoot -File | ForEach-Object { Write-Host "  - $($_.Name)" -ForegroundColor Gray }
