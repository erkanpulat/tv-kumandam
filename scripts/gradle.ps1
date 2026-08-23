[CmdletBinding()]
param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $GradleArguments = @("tasks")
)

$ErrorActionPreference = "Stop"
$scriptDirectory = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = (Resolve-Path (Join-Path $scriptDirectory "..")).Path
$executionRoot = $projectRoot
$isWindowsHost = $PSVersionTable.Platform -eq "Win32NT" -or $env:OS -eq "Windows_NT"

if ($isWindowsHost -and $projectRoot -match "[^\x00-\x7F]") {
    $junctionPath = Join-Path ([System.IO.Path]::GetTempPath()) "tv-kumandam-workspace"

    if (Test-Path -LiteralPath $junctionPath) {
        $junction = Get-Item -LiteralPath $junctionPath -Force
        $target = @($junction.Target)[0]
        if (-not $target -or (Resolve-Path -LiteralPath $target).Path -ne $projectRoot) {
            throw "The existing junction '$junctionPath' points to another project. Remove it manually and retry."
        }
    } else {
        New-Item -ItemType Junction -Path $junctionPath -Target $projectRoot | Out-Null
    }

    $executionRoot = $junctionPath
}

$bundledJdk = Join-Path $executionRoot ".tooling\jdk-17"
if (Test-Path -LiteralPath $bundledJdk) {
    $env:JAVA_HOME = $bundledJdk
}

$bundledSdk = Join-Path $executionRoot ".android-sdk"
if (Test-Path -LiteralPath $bundledSdk) {
    $env:ANDROID_HOME = $bundledSdk
    $env:ANDROID_SDK_ROOT = $bundledSdk
}

if ($isWindowsHost -and -not $env:ANDROID_USER_HOME) {
    $env:ANDROID_USER_HOME = Join-Path $executionRoot ".android-user-home"
}

if ($isWindowsHost -and (!$env:GRADLE_USER_HOME -or $env:GRADLE_USER_HOME -match "[^\x00-\x7F]")) {
    $env:GRADLE_USER_HOME = Join-Path ([System.IO.Path]::GetTempPath()) "tv-kumandam-gradle-home"
}

Push-Location $executionRoot
try {
    if ($isWindowsHost) {
        & ".\gradlew.bat" @GradleArguments
    } else {
        & "./gradlew" @GradleArguments
    }
    $gradleExitCode = $LASTEXITCODE
} finally {
    Pop-Location
}

exit $gradleExitCode
