param(
    [string]$OutputDir = "artifacts\script-tests\field-locator-comparison"
)

$ErrorActionPreference = "Stop"

if (Test-Path $OutputDir) {
    $OutputDir = "$OutputDir-$([datetime]::UtcNow.ToString('yyyyMMddHHmmssfff'))"
}

& "$PSScriptRoot\compare-field-locator-options.ps1" `
    -InputRoot "$PSScriptRoot\test-data\field-locator-comparison" `
    -OutputDir $OutputDir

$eventsPath = Join-Path $OutputDir "locator-events.csv"
$sourcePath = Join-Path $OutputDir "locator-source-summary.csv"
$signalPath = Join-Path $OutputDir "signal-source-summary.csv"
$twoAnchorPath = Join-Path $OutputDir "two-anchor-summary.csv"
$summaryPath = Join-Path $OutputDir "summary.md"

foreach ($path in @($eventsPath, $sourcePath, $signalPath, $twoAnchorPath, $summaryPath)) {
    if (-not (Test-Path $path)) {
        throw "Missing output: $path"
    }
}

$events = @(Import-Csv $eventsPath)
$sources = @(Import-Csv $sourcePath)
$signals = @(Import-Csv $signalPath)
$twoAnchor = @(Import-Csv $twoAnchorPath)
$summary = Get-Content $summaryPath -Raw

if ($events.Count -ne 7) {
    throw "Expected 7 parsed events, got $($events.Count)"
}
if (@($sources | Where-Object Source -eq "FIELD_RADIO_RSSI").Count -ne 1) {
    throw "Expected FIELD_RADIO_RSSI source summary"
}
if (@($signals | Where-Object Source -eq "BLE_ANCHOR").Count -ne 1) {
    throw "Expected BLE_ANCHOR signal summary"
}
if (@($signals | Where-Object Source -eq "WIFI_DIRECT_RELAY").Count -ne 1) {
    throw "Expected WIFI_DIRECT_RELAY signal summary"
}
if (@($signals | Where-Object Source -eq "WIFI_SSID").Count -ne 1) {
    throw "Expected WIFI_SSID signal summary"
}
if (@($twoAnchor | Where-Object Readiness -eq "NO_ANCHORS").Count -ne 1) {
    throw "Expected NO_ANCHORS two-anchor summary"
}
$oneAnchor = @($twoAnchor | Where-Object Readiness -eq "ONE_ANCHOR")
if ($oneAnchor.Count -ne 1 -or $oneAnchor[0].Count -ne "2") {
    throw "Expected ONE_ANCHOR two-anchor summary with 2 events"
}
$twoAnchorReady = @($twoAnchor | Where-Object Readiness -eq "TWO_ANCHORS")
if ($twoAnchorReady.Count -ne 1) {
    throw "Expected TWO_ANCHORS two-anchor summary"
}
if ($twoAnchorReady[0].Clock -ne "10" -or $twoAnchorReady[0].AvgDistanceM -ne "15") {
    throw "Expected TWO_ANCHORS clock=10 distance=15, got clock=$($twoAnchorReady[0].Clock) distance=$($twoAnchorReady[0].AvgDistanceM)"
}
$gpsEvent = @($events | Where-Object GpsRawDistanceM -eq "6")
if ($gpsEvent.Count -ne 1 -or $gpsEvent[0].GpsLocalLatE7 -ne "551234567" -or $gpsEvent[0].GpsRawBearingDeg -ne "37") {
    throw "Expected locator-events.csv to preserve GPS ground truth fields."
}
if ($summary -notlike "*BLE_ANCHOR*") {
    throw "Expected summary to mention BLE_ANCHOR"
}
if ($summary -notlike "*Two-Anchor Readiness*") {
    throw "Expected summary to mention Two-Anchor Readiness"
}

Write-Host "Field locator comparison test passed."
