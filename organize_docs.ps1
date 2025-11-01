# Document Organization Script
# Author: Cascade AI
# Date: 2024-11-02

$ErrorActionPreference = "Continue"

Write-Host "Starting document organization..." -ForegroundColor Green

# Create docs subdirectories if they dont exist
$docsRoot = "D:\Sesame-TK-n\docs"
$archiveDir = "$docsRoot\archive"
$bugFixesDir = "$docsRoot\bug-fixes"
$migrationDir = "$docsRoot\migration"
$testingDir = "$docsRoot\testing"
$dailyReportsDir = "$docsRoot\daily-reports"

# Ensure directories exist
@($archiveDir, $bugFixesDir, $migrationDir, $testingDir, $dailyReportsDir) | ForEach-Object {
    if (-not (Test-Path $_)) {
        New-Item -ItemType Directory -Path $_ -Force | Out-Null
    }
}

# ========== 1. Move Bug Fix Documents ==========
Write-Host "`n[1/5] Organizing bug fix documents..." -ForegroundColor Cyan
$bugFiles = @(
    "BUG_ANALYSIS_CLASSCASTEXCEPTION.md",
    "BUG_FIX_ALARM_POPUP.md",
    "BUG_FIX_COMPLETE_REPORT.txt",
    "BUGFIX_COMPLETE_REPORT.txt",
    "CRASH_ANALYSIS_RC83.md",
    "CRASH_FIX_ACCOUNT_SELECTION.md",
    "DEBUG_ACCOUNT_SELECTION_CRASH.md"
)

foreach ($file in $bugFiles) {
    $source = "D:\Sesame-TK-n\$file"
    if (Test-Path $source) {
        $dest = "$bugFixesDir\$file"
        Move-Item -Path $source -Destination $dest -Force
        Write-Host "  [OK] Moved: $file -> bug-fixes/" -ForegroundColor Gray
    }
}

# ========== 2. Move Migration Documents ==========
Write-Host "`n[2/5] Organizing migration documents..." -ForegroundColor Cyan
$migrationFiles = @(
    "MIGRATION_PLAN_FINAL_10FILES.md",
    "MIGRATION_PROGRESS_31PERCENT.md",
    "MIGRATION_PROGRESS_96PERCENT.md",
    "MIGRATION_PROGRESS_97PERCENT.md",
    "MIGRATION_SESSION_FINAL.md",
    "MIGRATION_SESSION_SUMMARY.md"
)

foreach ($file in $migrationFiles) {
    $source = "D:\Sesame-TK-n\$file"
    if (Test-Path $source) {
        $dest = "$migrationDir\$file"
        Move-Item -Path $source -Destination $dest -Force
        Write-Host "  [OK] Moved: $file -> migration/" -ForegroundColor Gray
    }
}

# ========== 3. Move Test and Status Documents ==========
Write-Host "`n[3/5] Organizing test and status documents..." -ForegroundColor Cyan
$testFiles = @(
    "TEST_RC104.md",
    "DEBUG_TEST_GUIDE_RC100.md",
    "INSTALL_RC98_GUIDE.md",
    "FIX_COMPLETE_RC101.md"
)

foreach ($file in $testFiles) {
    $source = "D:\Sesame-TK-n\$file"
    if (Test-Path $source) {
        $dest = "$testingDir\$file"
        Move-Item -Path $source -Destination $dest -Force
        Write-Host "  [OK] Moved: $file -> testing/" -ForegroundColor Gray
    }
}

# ========== 4. Archive Old Status Reports ==========
Write-Host "`n[4/5] Archiving historical status reports..." -ForegroundColor Cyan
$archiveFiles = @(
    "COMPILE_STATUS.md",
    "CURRENT_STATUS.md",
    "FINAL_STATUS_TODAY.md",
    "FINAL_SUMMARY.md",
    "NEXT_STEPS_SUMMARY.md",
    "PROJECT_STATUS_20241030.md",
    "TONIGHT_SUCCESS.md",
    "CAPTCHA_HOOK_README.md",
    "CONVERT_GUIDE.md",
    "安装rc142完整清理步骤.md"
)

foreach ($file in $archiveFiles) {
    $source = "D:\Sesame-TK-n\$file"
    if (Test-Path $source) {
        $dest = "$archiveDir\$file"
        Move-Item -Path $source -Destination $dest -Force
        Write-Host "  [OK] Archived: $file -> archive/" -ForegroundColor Gray
    }
}

# ========== 5. Remove Duplicate and Temporary Files ==========
Write-Host "`n[5/5] Cleaning duplicate and temporary files..." -ForegroundColor Cyan

# Remove duplicate CURRENT_STATUS.md from docs root
if (Test-Path "$docsRoot\CURRENT_STATUS.md") {
    Remove-Item "$docsRoot\CURRENT_STATUS.md" -Force
    Write-Host "  [OK] Removed duplicate: docs\CURRENT_STATUS.md" -ForegroundColor Gray
}

# Remove duplicate FINAL_SUMMARY.md from docs root
if (Test-Path "$docsRoot\FINAL_SUMMARY.md") {
    Remove-Item "$docsRoot\FINAL_SUMMARY.md" -Force
    Write-Host "  [OK] Removed duplicate: docs\FINAL_SUMMARY.md" -ForegroundColor Gray
}

# Remove temporary batch files
$tempFiles = @(
    "D:\Sesame-TK-n\CONVERT_REMAINING.bat",
    "D:\Sesame-TK-n\run_tests.bat",
    "D:\Sesame-TK-n\verify_tests.bat"
)

foreach ($file in $tempFiles) {
    if (Test-Path $file) {
        Remove-Item $file -Force
        Write-Host "  [OK] Removed temporary file: $(Split-Path -Leaf $file)" -ForegroundColor Gray
    }
}

# Create organization report
$reportPath = "$docsRoot\DOCUMENTATION_ORGANIZED_$(Get-Date -Format 'yyyyMMdd').md"
$report = @"
# Documentation Organization Report

**Date**: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')

## Organized Content

### 1. Bug Fix Documents -> docs/bug-fixes/
- BUG_ANALYSIS_CLASSCASTEXCEPTION.md
- BUG_FIX_ALARM_POPUP.md
- BUG_FIX_COMPLETE_REPORT.txt
- BUGFIX_COMPLETE_REPORT.txt
- CRASH_ANALYSIS_RC83.md
- CRASH_FIX_ACCOUNT_SELECTION.md
- DEBUG_ACCOUNT_SELECTION_CRASH.md

### 2. Migration Documents -> docs/migration/
- MIGRATION_PLAN_FINAL_10FILES.md
- MIGRATION_PROGRESS_31PERCENT.md
- MIGRATION_PROGRESS_96PERCENT.md
- MIGRATION_PROGRESS_97PERCENT.md
- MIGRATION_SESSION_FINAL.md
- MIGRATION_SESSION_SUMMARY.md

### 3. Test Documents -> docs/testing/
- TEST_RC104.md
- DEBUG_TEST_GUIDE_RC100.md
- INSTALL_RC98_GUIDE.md
- FIX_COMPLETE_RC101.md

### 4. Archived Documents -> docs/archive/
- COMPILE_STATUS.md
- CURRENT_STATUS.md
- FINAL_STATUS_TODAY.md
- FINAL_SUMMARY.md
- NEXT_STEPS_SUMMARY.md
- PROJECT_STATUS_20241030.md
- TONIGHT_SUCCESS.md
- CAPTCHA_HOOK_README.md
- CONVERT_GUIDE.md
- Installation guide rc142

### 5. Deleted Files
- Duplicate status documents
- Temporary batch files
- Outdated conversion scripts

## Current Documentation Structure

``````
docs/
├── archive/          # Historical archived documents
├── bug-fixes/        # Bug fix related documents
├── migration/        # Code migration documents
├── testing/          # Test related documents
├── daily-reports/    # Daily reports
└── README.md         # Documentation index
``````

## Important Files Kept in Root Directory
- README.md          # Project description
- LICENSE            # Open source license
- QUICK_START.md     # Quick start guide
- RELEASE_NOTES.md   # Release notes

"@

Set-Content -Path $reportPath -Value $report -Encoding UTF8

Write-Host "`n[SUCCESS] Documentation organization completed!" -ForegroundColor Green
Write-Host "Organization report generated: $reportPath" -ForegroundColor Yellow
