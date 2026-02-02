param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('pull','pull-file','push','execute','restart','relogin','status','tail-runtime','logcat-dump','force-stop-alipay','start-alipay','regress')]
    [string]$Command,

    [string]$RemoteRoot = '/sdcard/Android/media/com.eg.android.AlipayGphone/sesame-TK',
    [string]$LocalRoot = '',

    [string]$LocalPath,
    [string]$RemotePath,

    [int]$TailLines = 120,

    [int]$WaitAfterStartSec = 3,
    [int]$WaitAfterExecuteSec = 3,

    [switch]$DoubleExecuteStress
)

$ErrorActionPreference = 'Stop'

if (-not $LocalRoot) {
    $scriptRoot = $PSScriptRoot
    if (-not $scriptRoot) {
        $scriptRoot = (Get-Location).Path
    }

    $LocalRoot = (Join-Path $scriptRoot '..\log\phone')
}

$utf8 = [System.Text.UTF8Encoding]::new()
$OutputEncoding = $utf8
[Console]::OutputEncoding = $utf8
[Console]::InputEncoding = $utf8

function Test-DeviceConnected {
    $devices = & adb devices | Select-String -Pattern '\tdevice$'
    if (-not $devices) {
        throw 'No adb device connected (adb devices shows no "device")'
    }
}

function Clear-RemoteLogs {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemoteRoot
    )

    $files = @(
        "$RemoteRoot/log/runtime.log",
        "$RemoteRoot/log/record.log",
        "$RemoteRoot/log/error.log"
    )

    foreach ($f in $files) {
        & adb shell "cat /dev/null > '$f'" 2>$null
    }
}

function Wait-RemoteRuntimeLog {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemoteRuntimeFile,

        [Parameter(Mandatory = $true)]
        [string]$Pattern,

        [int]$TimeoutSec = 20,

        [int]$PollIntervalMs = 800
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    while ((Get-Date) -lt $deadline) {
        try {
            $found = & adb shell sh -c "grep -F '$Pattern' '$RemoteRuntimeFile' 2>/dev/null | tail -n 1" 2>$null
            if ($found) {
                return $true
            }

            $tail = & adb shell tail -n 1200 $RemoteRuntimeFile 2>$null
            if ($tail -and ($tail | Select-String -SimpleMatch -Pattern $Pattern)) {
                return $true
            }
        } catch {
        }
        Start-Sleep -Milliseconds $PollIntervalMs
    }
    return $false
}

Test-DeviceConnected

function New-RunFolder {
    New-Item -ItemType Directory -Force -Path $LocalRoot | Out-Null
    $runFolder = Join-Path $LocalRoot ("run_" + (Get-Date -Format 'yyyyMMdd_HHmmss'))
    New-Item -ItemType Directory -Force -Path $runFolder | Out-Null
    return $runFolder
}

function New-StringFromCodePoints {
    param(
        [Parameter(Mandatory = $true)]
        [int[]]$CodePoints
    )
    return -join ($CodePoints | ForEach-Object { [char]$_ })
}

function Copy-RemoteTextFileRobust {
    param(
        [Parameter(Mandatory = $true)]
        [string]$RemotePath,

        [Parameter(Mandatory = $true)]
        [string]$LocalPath,

        [int]$TimeoutSec = 12,

        [int]$FallbackTimeoutSec = 10
    )

    try {
        $fullLocalPath = $LocalPath
        try {
            $fullLocalPath = [System.IO.Path]::GetFullPath($LocalPath)
        } catch {
            $fullLocalPath = $LocalPath
        }

        $parent = Split-Path -Parent $fullLocalPath
        if (-not [string]::IsNullOrWhiteSpace($parent)) {
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
        }

        Write-Output ("[regress] pull: {0}" -f $RemotePath)

        $pullOk = $false
        try {
            $proc = Start-Process -FilePath "adb" -ArgumentList @("pull", $RemotePath, $fullLocalPath) -NoNewWindow -PassThru
            if ($proc.WaitForExit($TimeoutSec * 1000)) {
                $pullOk = ($proc.ExitCode -eq 0)
            } else {
                try { $proc.Kill() } catch { }
                $pullOk = $false
            }
        } catch {
            $pullOk = $false
        }

        if ($pullOk) {
            $checkPath = $fullLocalPath
            $len = 0
            for ($i = 0; $i -lt 5; $i++) {
                try {
                    if (Test-Path -LiteralPath $checkPath) {
                        $len = (Get-Item -LiteralPath $checkPath).Length
                    }
                } catch {
                    $len = 0
                }

                if ($len -gt 0) {
                    return
                }
                Start-Sleep -Milliseconds 120
            }
        }

        $existingLen = 0
        try {
            if (Test-Path -LiteralPath $fullLocalPath) {
                $existingLen = (Get-Item -LiteralPath $fullLocalPath).Length
            }
        } catch {
            $existingLen = 0
        }

        Write-Output ("[regress] pull fallback(cat): {0}" -f $RemotePath)
        $tmpOut = [System.IO.Path]::GetTempFileName()
        $tmpErr = [System.IO.Path]::GetTempFileName()
        try {
            $catProc = Start-Process -FilePath "adb" -ArgumentList @("shell", "sh", "-c", "cat '$RemotePath' 2>/dev/null") -NoNewWindow -PassThru -RedirectStandardOutput $tmpOut -RedirectStandardError $tmpErr
            if (-not $catProc.WaitForExit($FallbackTimeoutSec * 1000)) {
                try { $catProc.Kill() } catch { }
            }
        } catch {
        }

        try {
            $tmpLen = 0
            if (Test-Path -LiteralPath $tmpOut) {
                $tmpLen = (Get-Item -LiteralPath $tmpOut).Length
            }

            if ($tmpLen -gt 0) {
                $lines = Get-Content -Path $tmpOut -Encoding utf8
                [System.IO.File]::WriteAllLines($fullLocalPath, $lines, $utf8)
            } else {
                if (-not (Test-Path -LiteralPath $fullLocalPath) -and ($existingLen -le 0)) {
                    New-Item -ItemType File -Force -Path $fullLocalPath | Out-Null
                }
            }
        } catch {
            if (-not (Test-Path -LiteralPath $fullLocalPath) -and ($existingLen -le 0)) {
                try { New-Item -ItemType File -Force -Path $fullLocalPath | Out-Null } catch { }
            }
        } finally {
            try { Remove-Item -Force $tmpOut -ErrorAction SilentlyContinue } catch { }
            try { Remove-Item -Force $tmpErr -ErrorAction SilentlyContinue } catch { }
        }
    } catch {
        try {
            $fallbackPath = $LocalPath
            try {
                $fallbackPath = [System.IO.Path]::GetFullPath($LocalPath)
            } catch {
                $fallbackPath = $LocalPath
            }
            New-Item -ItemType File -Force -Path $fallbackPath | Out-Null
        } catch {
        }
    }
}

function Write-GrepSummary {
    param(
        [Parameter(Mandatory = $true)]
        [string]$File,

        [Parameter(Mandatory = $true)]
        [string]$Title,

        [Parameter(Mandatory = $true)]
        [string[]]$Patterns,

        [switch]$SimpleMatch,

        [int]$MaxLines = 8
    )

    if (-not (Test-Path $File)) {
        Write-Output ("[Summary] {0}: file not found: {1}" -f $Title, $File)
        return
    }

    $lines = Get-Content -Path $File -Encoding utf8 -ErrorAction Stop
    $allMatches = @()

    foreach ($p in $Patterns) {
        $m = $lines | Select-String -Pattern $p -SimpleMatch:$SimpleMatch
        if ($m) {
            $allMatches += $m
        }
    }

    $count = if ($allMatches) { $allMatches.Count } else { 0 }
    Write-Output ("[Summary] {0}: {1}" -f $Title, $count)

    if ($count -gt 0) {
        $allMatches | Select-Object -First $MaxLines | ForEach-Object {
            $safeLine = ($_.Line -replace "[\r\n]+", " ")
            Write-Output ("  {0}:{1}: {2}" -f (Split-Path -Leaf $File), $_.LineNumber, $safeLine)
        }
        if ($count -gt $MaxLines) {
            Write-Output ("  ... ({0} more)" -f ($count - $MaxLines))
        }
    }
}

switch ($Command) {
    'pull' {
        New-Item -ItemType Directory -Force -Path $LocalRoot | Out-Null
        & adb pull "$RemoteRoot/config" (Join-Path $LocalRoot 'config')
        & adb pull "$RemoteRoot/log" (Join-Path $LocalRoot 'log')
        break
    }

    'pull-file' {
        if ([string]::IsNullOrWhiteSpace($RemotePath) -or [string]::IsNullOrWhiteSpace($LocalPath)) {
            throw 'pull-file requires -RemotePath and -LocalPath'
        }
        $parent = Split-Path -Parent $LocalPath
        if (-not [string]::IsNullOrWhiteSpace($parent)) {
            New-Item -ItemType Directory -Force -Path $parent | Out-Null
        }
        & adb pull $RemotePath $LocalPath
        break
    }

    'push' {
        if ([string]::IsNullOrWhiteSpace($LocalPath) -or [string]::IsNullOrWhiteSpace($RemotePath)) {
            throw 'push requires -LocalPath and -RemotePath'
        }
        & adb push $LocalPath $RemotePath
        break
    }

    'execute' {
        & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.execute --ez alarm_triggered true
        break
    }

    'restart' {
        & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.restart
        break
    }

    'relogin' {
        & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.reLogin
        break
    }

    'status' {
        & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.status
        break
    }

    'tail-runtime' {
        & adb shell tail -n $TailLines "$RemoteRoot/log/runtime.log"
        break
    }

    'logcat-dump' {
        New-Item -ItemType Directory -Force -Path $LocalRoot | Out-Null
        $outFile = Join-Path $LocalRoot ("logcat_" + (Get-Date -Format 'yyyyMMdd_HHmmss') + '.txt')
        & adb logcat -d | Out-File -FilePath $outFile -Encoding utf8
        Write-Output $outFile
        break
    }

    'force-stop-alipay' {
        & adb shell am force-stop com.eg.android.AlipayGphone
        break
    }

    'start-alipay' {
        & adb shell monkey -p com.eg.android.AlipayGphone -c android.intent.category.LAUNCHER 1
        break
    }

    'regress' {
        $runFolder = New-RunFolder

        Write-Output ("[regress] script build: 20260202-1426")
        Write-Output ("[regress] run folder: {0}" -f $runFolder)

        & adb logcat -c

        Clear-RemoteLogs -RemoteRoot $RemoteRoot

        & adb shell am force-stop com.eg.android.AlipayGphone
        Start-Sleep -Seconds $WaitAfterStartSec
        & adb shell monkey -p com.eg.android.AlipayGphone -c android.intent.category.LAUNCHER 1
        Start-Sleep -Seconds $WaitAfterStartSec

        $remoteRuntimeFile = "$RemoteRoot/log/runtime.log"
        $ready = Wait-RemoteRuntimeLog -RemoteRuntimeFile $remoteRuntimeFile -Pattern '[SESAME_TK_READY]' -TimeoutSec 25
        if (-not $ready) {
            Write-Output "[regress] WARN: broadcast receiver not ready after timeout, still sending execute"
        } else {
            Write-Output "[regress] OK: broadcast receiver ready"
        }

        if ($DoubleExecuteStress) {
            Write-Output "[regress] Stress: sending execute twice"
            & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.execute --ez alarm_triggered false
            Start-Sleep -Milliseconds 200
            & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.execute --ez alarm_triggered false
        } else {
            & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.execute --ez alarm_triggered false
        }
        Start-Sleep -Seconds $WaitAfterExecuteSec

        Write-Output "[regress] collecting files..."
        Copy-RemoteTextFileRobust -RemotePath "$RemoteRoot/log/runtime.log" -LocalPath (Join-Path $runFolder 'runtime.log')
        Write-Output "[regress] collected: runtime.log"
        Copy-RemoteTextFileRobust -RemotePath "$RemoteRoot/log/record.log" -LocalPath (Join-Path $runFolder 'record.log')
        Write-Output "[regress] collected: record.log"
        Copy-RemoteTextFileRobust -RemotePath "$RemoteRoot/log/error.log" -LocalPath (Join-Path $runFolder 'error.log')
        Write-Output "[regress] collected: error.log"
        Copy-RemoteTextFileRobust -RemotePath "$RemoteRoot/config/DataStore.json" -LocalPath (Join-Path $runFolder 'DataStore.json')
        Write-Output "[regress] collected: DataStore.json"
        Copy-RemoteTextFileRobust -RemotePath "$RemoteRoot/ModuleStatus.json" -LocalPath (Join-Path $runFolder 'ModuleStatus.json')
        Write-Output "[regress] collected: ModuleStatus.json"

        Write-Output "[regress] dump logcat..."
        $logcatFile = Join-Path $runFolder 'logcat.txt'
        $logcatFullFile = Join-Path $runFolder 'logcat_full.txt'

        $targetPids = @()
        try {
            $alipayPidText = (& adb shell pidof com.eg.android.AlipayGphone 2>$null)
            if ($alipayPidText) {
                $targetPids += ($alipayPidText -split '\s+' | Where-Object { $_ -match '^\d+$' })
            }
        } catch {
        }

        try {
            $modulePidText = (& adb shell pidof fansirsqi.xposed.sesame 2>$null)
            if ($modulePidText) {
                $targetPids += ($modulePidText -split '\s+' | Where-Object { $_ -match '^\d+$' })
            }
        } catch {
        }

        $targetPids = $targetPids | Select-Object -Unique

        try {
            $logcatProc = Start-Process -FilePath "adb" -ArgumentList @("logcat", "-d", "-v", "threadtime") -NoNewWindow -PassThru -RedirectStandardOutput $logcatFullFile -RedirectStandardError ([System.IO.Path]::GetTempFileName())
            if (-not $logcatProc.WaitForExit(15000)) {
                try { $logcatProc.Kill() } catch { }
            }
        } catch {
            try { New-Item -ItemType File -Force -Path $logcatFullFile | Out-Null } catch { }
        }

        try {
            if ((Test-Path $logcatFullFile) -and ($targetPids.Count -gt 0)) {
                $pidSet = @{}
                foreach ($p in $targetPids) { $pidSet[$p] = $true }

                $filtered = Get-Content -Path $logcatFullFile -Encoding utf8 | Where-Object {
                    $m = [regex]::Match($_, '^\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2}\.\d+\s+(\d+)\s+')
                    if (-not $m.Success) { return $false }
                    return $pidSet.ContainsKey($m.Groups[1].Value)
                }
                [System.IO.File]::WriteAllLines($logcatFile, $filtered, $utf8)
            } else {
                Copy-Item -Force $logcatFullFile $logcatFile
            }
        } catch {
            try { Copy-Item -Force $logcatFullFile $logcatFile } catch { }
        }

        $runtimeFile = Join-Path $runFolder 'runtime.log'
        $recordFile = Join-Path $runFolder 'record.log'
        $errorFile = Join-Path $runFolder 'error.log'

        $pExecSkipRunning = "execHandler: " + (New-StringFromCodePoints @(
            0x68C0,0x6D4B,0x5230,0x4EFB,0x52A1,0x6267,0x884C,0x4E2D,0xFF0C,0x8DF3,0x8FC7,0x672C,0x6B21,0x89E6,0x53D1
        ))
        $pExecSkipMainTaskThreadAlive = "execHandler: mainTask" + (New-StringFromCodePoints @(
            0x7EBF,0x7A0B,0x8FD0,0x884C,0x4E2D,0xFF0C,0x8DF3,0x8FC7,0x672C,0x6B21,0x89E6,0x53D1
        ))
        $pAlarmShortIntervalSkip = New-StringFromCodePoints @(
            0x95F9,0x949F,0x89E6,0x53D1,0x95F4,0x9694,0x8F83,0x77ED
        )
        $pEntryDebouncedSkip = New-StringFromCodePoints @(
            0x5165,0x53E3,0x5904,0x7406,0x8FC7,0x4E8E,0x9891,0x7E41
        )
        $pChildTaskCoroutineCancelled = New-StringFromCodePoints @(
            0x5B50,0x4EFB,0x52A1,0x534F,0x7A0B,0x88AB,0x53D6,0x6D88
        )
        $pChildTaskCancelled = New-StringFromCodePoints @(
            0x5B50,0x4EFB,0x52A1,0x88AB,0x53D6,0x6D88
        )

        Write-Output "================ Summary (regress) ================"
        Write-GrepSummary -File $runtimeFile -Title 'execute broadcast received' -Patterns @('Alipay got Broadcast com.eg.android.AlipayGphone.sesame.execute') -SimpleMatch
        Write-GrepSummary -File $runtimeFile -Title 'execHandler running skip' -Patterns @(
            $pExecSkipRunning,
            $pExecSkipMainTaskThreadAlive
        ) -SimpleMatch
        Write-GrepSummary -File $runtimeFile -Title 'alarm short-interval skip' -Patterns @($pAlarmShortIntervalSkip) -SimpleMatch
        Write-GrepSummary -File $runtimeFile -Title 'entry debounced skip' -Patterns @($pEntryDebouncedSkip) -SimpleMatch
        Write-GrepSummary -File $runtimeFile -Title 'child task cancelled' -Patterns @($pChildTaskCoroutineCancelled, $pChildTaskCancelled) -SimpleMatch
        Write-GrepSummary -File $runtimeFile -Title 'exceptions (runtime)' -Patterns @('NullPointerException', 'FATAL EXCEPTION', 'Exception')
        Write-GrepSummary -File $recordFile -Title 'task runner summary (record.log)' -Patterns @('协程任务执行统计摘要', '任务\[.*\]执行', '执行超时', '执行失败')
        Write-GrepSummary -File $errorFile -Title 'errors (error.log)' -Patterns @('.+')
        Write-GrepSummary -File $logcatFile -Title 'logcat exceptions' -Patterns @('FATAL EXCEPTION', 'NullPointerException') -SimpleMatch
        Write-Output "===================================================="

        Write-Output $runFolder
        break
    }
}
