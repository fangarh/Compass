$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\iff\IffBleFieldRadio.java"
$source = Get-Content -Path $sourcePath -Raw

$required = @(
    "GPS_IN_BLE_ADVERTISE_ENABLED = false",
    "if (!GPS_IN_BLE_ADVERTISE_ENABLED)",
    "return IffBlePayload.forPlayer(playerCode);"
)

foreach ($needle in $required) {
    if (-not $source.Contains($needle)) {
        throw "Missing BLE field radio stability guard marker: $needle"
    }
}

Write-Host "BLE field radio stability guard source checks passed."
