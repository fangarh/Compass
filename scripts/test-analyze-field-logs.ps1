param(
    [string]$OutputDir = "artifacts\script-tests\beacon-analysis"
)

$ErrorActionPreference = "Stop"

if (Test-Path $OutputDir) {
    $OutputDir = "$OutputDir-$([datetime]::UtcNow.ToString('yyyyMMddHHmmssfff'))"
}

& "$PSScriptRoot\analyze-field-logs.ps1" `
    -InputRoot "$PSScriptRoot\test-data\beacon-field-logs" `
    -OutputDir $OutputDir `
    -TestStart "2026-05-19 13:00:00" `
    -Windows "beacon=13:00:00..13:00:20" `
    -BucketSeconds 5 `
    -OfficeBaselineSideA -61 `
    -OfficeBaselineSideB -47 `
    -BeaconSsids "COMPASS_BEACON*"

$timelinePath = Join-Path $OutputDir "beacon-timeline.csv"
$bucketPath = Join-Path $OutputDir "beacon-bucket-summary.csv"
$iffPath = Join-Path $OutputDir "iff-field-checks.csv"
$bleSummaryPath = Join-Path $OutputDir "ble-rssi-summary.csv"
$officeVerdictPath = Join-Path $OutputDir "office-proximity-verdict.csv"

if (-not (Test-Path $timelinePath)) {
    throw "Missing beacon-timeline.csv"
}
if (-not (Test-Path $bucketPath)) {
    throw "Missing beacon-bucket-summary.csv"
}
if (-not (Test-Path $iffPath)) {
    throw "Missing iff-field-checks.csv"
}
if (-not (Test-Path $bleSummaryPath)) {
    throw "Missing ble-rssi-summary.csv"
}
if (-not (Test-Path $officeVerdictPath)) {
    throw "Missing office-proximity-verdict.csv"
}

$timeline = Import-Csv $timelinePath
$buckets = Import-Csv $bucketPath
$iffChecks = @(Import-Csv $iffPath)
$bleSummary = @(Import-Csv $bleSummaryPath)
$officeVerdicts = @(Import-Csv $officeVerdictPath)

if ($timeline.Count -ne 3) {
    throw "Expected 3 beacon timeline rows, got $($timeline.Count)"
}
if (($timeline | Where-Object Ssid -ne "COMPASS_BEACON_A").Count -ne 0) {
    throw "Non-beacon SSID leaked into beacon timeline"
}
if (($buckets | Where-Object Trend -eq "stronger").Count -lt 1) {
    throw "Expected at least one stronger trend bucket"
}
if (($buckets | Where-Object Trend -eq "weaker").Count -lt 1) {
    throw "Expected at least one weaker trend bucket"
}
if ($iffChecks.Count -ne 1) {
    throw "Expected 1 IFF field check row, got $($iffChecks.Count)"
}
if ($iffChecks[0].OfficeRole -ne "PHONE_A_WITNESS") {
    throw "Expected OfficeRole PHONE_A_WITNESS, got '$($iffChecks[0].OfficeRole)'"
}
if ($iffChecks[0].SelectedOfficeRole -ne "PHONE_C_MOVING_TARGET") {
    throw "Expected SelectedOfficeRole PHONE_C_MOVING_TARGET, got '$($iffChecks[0].SelectedOfficeRole)'"
}
if ($iffChecks[0].OfficeProximityVerdict -ne "CLOSER_TO_A") {
    throw "Expected OfficeProximityVerdict CLOSER_TO_A, got '$($iffChecks[0].OfficeProximityVerdict)'"
}
if ($iffChecks[0].OfficeProximityDeltaDb -ne "25") {
    throw "Expected OfficeProximityDeltaDb 25, got '$($iffChecks[0].OfficeProximityDeltaDb)'"
}
$blePetya = $bleSummary | Where-Object {
    $_.Window -eq "beacon" -and
    $_.Device -eq "test-device" -and
    $_.LocalPlayerId -eq "vasya" -and
    $_.SeenPlayerId -eq "petya"
}
if (@($blePetya).Count -ne 1) {
    throw "Expected one BLE RSSI summary row for vasya seeing petya, got $(@($blePetya).Count)"
}
if ($blePetya.Count -ne "2") {
    throw "Expected BLE sample count 2, got '$($blePetya.Count)'"
}
if ($blePetya.ValidCount -ne "1") {
    throw "Expected BLE valid count 1 after filtering rssi=127, got '$($blePetya.ValidCount)'"
}
if ($blePetya.Outlier127 -ne "1") {
    throw "Expected one rssi=127 outlier, got '$($blePetya.Outlier127)'"
}
if ($blePetya.AvgRssi -ne "-50") {
    throw "Expected BLE average RSSI -50, got '$($blePetya.AvgRssi)'"
}

$closerToA = $officeVerdicts | Where-Object {
    $_.Window -eq "beacon" -and
    $_.Bucket -eq "13:00:10-13:00:15" -and
    $_.Device -eq "test-device" -and
    $_.LocalPlayerId -eq "petya"
}
if (@($closerToA).Count -ne 1) {
    throw "Expected one office proximity verdict row for petya bucket, got $(@($closerToA).Count)"
}
if ($closerToA.RawVerdict -ne "CLOSER_TO_A") {
    throw "Expected raw office proximity verdict CLOSER_TO_A, got '$($closerToA.RawVerdict)'"
}
if ($closerToA.RawDeltaDb -ne "25") {
    throw "Expected raw office proximity delta 25, got '$($closerToA.RawDeltaDb)'"
}
if ($closerToA.Verdict -ne "CLOSER_TO_A") {
    throw "Expected calibrated office proximity verdict CLOSER_TO_A, got '$($closerToA.Verdict)'"
}
if ($closerToA.DeltaDb -ne "39") {
    throw "Expected calibrated office proximity delta 39, got '$($closerToA.DeltaDb)'"
}
if ($closerToA.Calibration -ne "sideA=-61 sideB=-47") {
    throw "Expected calibration label, got '$($closerToA.Calibration)'"
}
if ($closerToA.SideARssi -ne "-41") {
    throw "Expected side A RSSI -41, got '$($closerToA.SideARssi)'"
}
if ($closerToA.SideBRssi -ne "-66") {
    throw "Expected side B RSSI -66, got '$($closerToA.SideBRssi)'"
}

Write-Host "Beacon analyzer test passed."
