param(
    [string]$Message = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-RepoRoot {
    $scriptDir = $PSScriptRoot
    if ([string]::IsNullOrWhiteSpace($scriptDir)) {
        $scriptDir = Split-Path -Parent $PSCommandPath
    }
    return (Resolve-Path (Join-Path $scriptDir "..") ).Path
}

function Get-CommitMessage {
    param(
        [string[]]$ChangedFiles
    )

    $scope = "core"

    if ($ChangedFiles | Where-Object { $_ -match "(^|/)app/build\.gradle\.kts$" -or $_ -match "(^|/)build\.gradle\.kts$" -or $_ -match "(^|/)settings\.gradle\.kts$" -or $_ -match "(^|/)gradle\.properties$" }) {
        $scope = "build"
    } elseif ($ChangedFiles | Where-Object { $_ -match "(^|/)app/src/main/java/.*/hook/" }) {
        $scope = "hook"
    } elseif ($ChangedFiles | Where-Object { $_ -match "(^|/)app/src/main/java/.*/rpc/" }) {
        $scope = "rpc"
    } elseif ($ChangedFiles | Where-Object { $_ -match "(^|/)app/src/main/java/.*/task/" }) {
        $scope = "task"
    } elseif ($ChangedFiles | Where-Object { $_ -match "(^|/)app/src/main/java/.*/ui/" }) {
        $scope = "ui"
    }

    $type = "chore"
    if ($scope -eq "build") {
        $type = "build"
    } elseif ($scope -in @("hook", "rpc", "task")) {
        $type = "fix"
    }

    $summary = "update"
    return "$type($scope): $summary"
}

$repoRoot = Get-RepoRoot
Set-Location $repoRoot

Write-Host "[1/3] Running Gradle verification (detekt + assembleRelease)..."
& .\gradlew detekt assembleRelease
if ($LASTEXITCODE -ne 0) {
    throw "Gradle verification failed with exit code $LASTEXITCODE"
}

Write-Host "[2/3] Checking git working tree..."
$porcelain = & git status --porcelain
$porcelainLines = $porcelain -split "`n" | ForEach-Object { $_.TrimEnd() } | Where-Object { $_ -ne "" }
$meaningfulLines = $porcelainLines | Where-Object {
    ($_ -notmatch "^\?\?\s+log/.*\.log$") -and
    ($_ -notmatch "^[ MADRCU\?]{2}\s+log/.*\.log$") -and
    ($_ -notmatch "^\?\?\s+[^/\\\\]+\.log$") -and
    ($_ -notmatch "^[ MADRCU\?]{2}\s+[^/\\\\]+\.log$")
}

if (@($meaningfulLines).Count -eq 0) {
    Write-Host "No changes to commit."
    exit 0
}

& git add -A -- . ':(exclude)log/*.log' ':(exclude)*.log'

& git diff --cached --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "No staged changes to commit (logs are excluded)."
    exit 0
}

if ([string]::IsNullOrWhiteSpace($Message)) {
    $changed = (& git diff --cached --name-only) -split "`n" | ForEach-Object { $_.Trim() } | Where-Object { $_ -ne "" }
    $Message = Get-CommitMessage -ChangedFiles $changed
}

Write-Host "[3/3] Committing with message: $Message"
& git commit -m $Message

Write-Host "Done. (No push performed)"
