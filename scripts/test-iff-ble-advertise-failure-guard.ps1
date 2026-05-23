$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\iff\IffBleFieldRadio.java"
$source = Get-Content -Path $sourcePath -Raw

$required = @(
    "MAX_ADVERTISE_FAILURES",
    "advertiseDisabledForSession",
    "advertise_disabled_after_failures",
    "advertise_start_pending",
    "if (isAdvertiseDisabled)",
    "advertiseFailureCount >= MAX_ADVERTISE_FAILURES"
)

foreach ($needle in $required) {
    if (-not $source.Contains($needle)) {
        throw "Missing BLE advertise failure guard marker: $needle"
    }
}

Write-Host "BLE advertise failure guard source checks passed."
