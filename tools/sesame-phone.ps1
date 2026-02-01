param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('pull','pull-file','push','execute','restart','relogin','status','tail-runtime','logcat-dump','force-stop-alipay','start-alipay','regress')]
    [string]$Command,

    [string]$RemoteRoot = '/sdcard/Android/media/com.eg.android.AlipayGphone/sesame-TK',
    [string]$LocalRoot = (Join-Path $PSScriptRoot '..\log\phone'),

    [string]$LocalPath,
    [string]$RemotePath,

    [int]$TailLines = 120,

    [int]$WaitAfterStartSec = 3,
    [int]$WaitAfterExecuteSec = 3
)

$ErrorActionPreference = 'Stop'

function Ensure-Device {
    $devices = & adb devices | Select-String -Pattern '\tdevice$'
    if (-not $devices) {
        throw 'No adb device connected (adb devices shows no "device")'
    }
}

Ensure-Device

function New-RunFolder {
    New-Item -ItemType Directory -Force -Path $LocalRoot | Out-Null
    $runFolder = Join-Path $LocalRoot ("run_" + (Get-Date -Format 'yyyyMMdd_HHmmss'))
    New-Item -ItemType Directory -Force -Path $runFolder | Out-Null
    return $runFolder
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

        & adb shell am force-stop com.eg.android.AlipayGphone
        Start-Sleep -Seconds $WaitAfterStartSec
        & adb shell monkey -p com.eg.android.AlipayGphone -c android.intent.category.LAUNCHER 1
        Start-Sleep -Seconds $WaitAfterStartSec

        & adb shell am broadcast -a com.eg.android.AlipayGphone.sesame.execute --ez alarm_triggered true
        Start-Sleep -Seconds $WaitAfterExecuteSec

        & adb pull "$RemoteRoot/log/runtime.log" (Join-Path $runFolder 'runtime.log')
        & adb pull "$RemoteRoot/log/record.log" (Join-Path $runFolder 'record.log')
        & adb pull "$RemoteRoot/log/error.log" (Join-Path $runFolder 'error.log')
        & adb pull "$RemoteRoot/config/DataStore.json" (Join-Path $runFolder 'DataStore.json')
        & adb pull "$RemoteRoot/ModuleStatus.json" (Join-Path $runFolder 'ModuleStatus.json')

        $logcatFile = Join-Path $runFolder 'logcat.txt'
        & adb logcat -d | Out-File -FilePath $logcatFile -Encoding utf8

        Write-Output $runFolder
        break
    }
}
