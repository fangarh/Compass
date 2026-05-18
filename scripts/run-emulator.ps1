param(
    [string]$AvdName = "Compass_API36",
    [string]$AvdHome = "D:\Android\avd"
)

$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$env:ANDROID_AVD_HOME = $AvdHome
$env:PATH = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"

$emulator = Join-Path $env:ANDROID_HOME "emulator\emulator.exe"
$logDir = "D:\Android\logs"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$args = @(
    "-avd", $AvdName,
    "-no-snapshot",
    "-no-boot-anim",
    "-gpu", "swiftshader_indirect"
)

Start-Process `
    -FilePath $emulator `
    -ArgumentList $args `
    -WorkingDirectory "D:\Android" `
    -RedirectStandardOutput (Join-Path $logDir "$AvdName.out.log") `
    -RedirectStandardError (Join-Path $logDir "$AvdName.err.log") `
    -WindowStyle Hidden
