param(
    [string]$Serial = "emulator-5554",
    [switch]$Disable
)

$ErrorActionPreference = "Stop"

$adb = if ($env:ANDROID_HOME) {
    Join-Path $env:ANDROID_HOME "platform-tools\adb.exe"
} else {
    Join-Path $env:LOCALAPPDATA "Android\Sdk\platform-tools\adb.exe"
}

if (-not (Test-Path $adb)) {
    throw "adb.exe not found. Set ANDROID_HOME or install Android SDK platform-tools."
}

if ($Disable) {
    & $adb -s $Serial shell settings put global wifi_scan_throttle_enabled 1
    Write-Host "Wi-Fi scan throttling restored on $Serial."
    exit 0
}

& $adb -s $Serial shell settings put global development_settings_enabled 1
& $adb -s $Serial shell settings put global wifi_scan_throttle_enabled 0
Write-Host "Wi-Fi scan throttling disabled for debug testing on $Serial."
