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
$wallShadowPath = Join-Path $OutputDir "office-wall-shadow-summary.csv"
$gpsVectorsPath = Join-Path $OutputDir "gps-device-vectors.csv"

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
if (-not (Test-Path $wallShadowPath)) {
    throw "Missing office-wall-shadow-summary.csv"
}
if (-not (Test-Path $gpsVectorsPath)) {
    throw "Missing gps-device-vectors.csv"
}

$timeline = Import-Csv $timelinePath
$buckets = Import-Csv $bucketPath
$iffChecks = @(Import-Csv $iffPath)
$bleSummary = @(Import-Csv $bleSummaryPath)
$officeVerdicts = @(Import-Csv $officeVerdictPath)
$wallShadow = @(Import-Csv $wallShadowPath)
$gpsVectors = @(Import-Csv $gpsVectorsPath)

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
if ($iffChecks.Count -ne 3) {
    throw "Expected 3 IFF field check rows, got $($iffChecks.Count)"
}
$manualIffCheck = $iffChecks | Where-Object Source -eq "manual"
$autoIffChecks = @($iffChecks | Where-Object Source -eq "auto")
$autoIffCheck = $autoIffChecks | Where-Object OfficeProximityVerdict -eq "CLOSER_TO_A"
$autoWallShadowCheck = $autoIffChecks | Where-Object OfficeProximityVerdict -eq "ONLY_B_VISIBLE"
if (@($manualIffCheck).Count -ne 1) {
    throw "Expected one manual IFF field check row, got $(@($manualIffCheck).Count)"
}
if ($autoIffChecks.Count -ne 2) {
    throw "Expected two auto IFF field check rows, got $($autoIffChecks.Count)"
}
if (@($autoIffCheck).Count -ne 1) {
    throw "Expected one CLOSER_TO_A auto IFF field check row, got $(@($autoIffCheck).Count)"
}
if (@($autoWallShadowCheck).Count -ne 1) {
    throw "Expected one ONLY_B_VISIBLE auto IFF field check row, got $(@($autoWallShadowCheck).Count)"
}
if ($manualIffCheck.OfficeRole -ne "PHONE_A_WITNESS") {
    throw "Expected OfficeRole PHONE_A_WITNESS, got '$($manualIffCheck.OfficeRole)'"
}
if ($manualIffCheck.SelectedOfficeRole -ne "PHONE_C_MOVING_TARGET") {
    throw "Expected SelectedOfficeRole PHONE_C_MOVING_TARGET, got '$($manualIffCheck.SelectedOfficeRole)'"
}
if ($manualIffCheck.OfficeProximityVerdict -ne "CLOSER_TO_A") {
    throw "Expected OfficeProximityVerdict CLOSER_TO_A, got '$($manualIffCheck.OfficeProximityVerdict)'"
}
if ($manualIffCheck.OfficeProximityDeltaDb -ne "25") {
    throw "Expected OfficeProximityDeltaDb 25, got '$($manualIffCheck.OfficeProximityDeltaDb)'"
}
if ($autoIffCheck.LocalDevicePlayerId -ne "petya") {
    throw "Expected auto LocalDevicePlayerId petya, got '$($autoIffCheck.LocalDevicePlayerId)'"
}
if ($autoIffCheck.PlayerId -ne "") {
    throw "Expected auto PlayerId to be empty, got '$($autoIffCheck.PlayerId)'"
}
if ($autoIffCheck.SnapshotSource -ne "foreground_service_tick") {
    throw "Expected auto SnapshotSource foreground_service_tick, got '$($autoIffCheck.SnapshotSource)'"
}
if ($autoIffCheck.OfficeRole -ne "PHONE_C_MOVING_TARGET") {
    throw "Expected auto OfficeRole PHONE_C_MOVING_TARGET, got '$($autoIffCheck.OfficeRole)'"
}
if ($autoIffCheck.OfficeProximityVerdict -ne "CLOSER_TO_A") {
    throw "Expected auto OfficeProximityVerdict CLOSER_TO_A, got '$($autoIffCheck.OfficeProximityVerdict)'"
}
if ($autoIffCheck.OfficeProximityDeltaDb -ne "25") {
    throw "Expected auto OfficeProximityDeltaDb 25, got '$($autoIffCheck.OfficeProximityDeltaDb)'"
}
if ($autoIffCheck.FieldRadioStatus -notlike "*local=petya*") {
    throw "Expected auto FieldRadioStatus to include local=petya, got '$($autoIffCheck.FieldRadioStatus)'"
}
if ($autoIffCheck.WifiFingerprintStatus -ne "ok") {
    throw "Expected auto WifiFingerprintStatus ok, got '$($autoIffCheck.WifiFingerprintStatus)'"
}
if ($autoIffCheck.WifiRefreshRequested -ne "true") {
    throw "Expected auto WifiRefreshRequested true, got '$($autoIffCheck.WifiRefreshRequested)'"
}
if ($autoIffCheck.WifiRefreshAccepted -ne "true") {
    throw "Expected auto WifiRefreshAccepted true, got '$($autoIffCheck.WifiRefreshAccepted)'"
}
if ($autoIffCheck.WifiFreshness -ne "fresh") {
    throw "Expected auto WifiFreshness fresh, got '$($autoIffCheck.WifiFreshness)'"
}
if ($autoIffCheck.WifiFreshAgeMs -ne "1200") {
    throw "Expected auto WifiFreshAgeMs 1200, got '$($autoIffCheck.WifiFreshAgeMs)'"
}
if ($autoIffCheck.WifiFingerprint -notlike "count=2 strongest=*Office B*bb:bb:bb:bb:bb:bb@-42dBm*") {
    throw "Expected auto WifiFingerprint to include strongest Office B, got '$($autoIffCheck.WifiFingerprint)'"
}
if ($autoIffCheck.DistanceClass -ne "VERY_NEAR") {
    throw "Expected auto DistanceClass VERY_NEAR, got '$($autoIffCheck.DistanceClass)'"
}
if ($autoIffCheck.DistanceTargetPlayerId -ne "vasya") {
    throw "Expected auto DistanceTargetPlayerId vasya, got '$($autoIffCheck.DistanceTargetPlayerId)'"
}
if ($autoIffCheck.DistanceConfidence -ne "90") {
    throw "Expected auto DistanceConfidence 90, got '$($autoIffCheck.DistanceConfidence)'"
}
if ($autoIffCheck.MovementTrend -ne "APPROACHING") {
    throw "Expected auto MovementTrend APPROACHING, got '$($autoIffCheck.MovementTrend)'"
}
if ($autoIffCheck.MovementConfidence -ne "65") {
    throw "Expected auto MovementConfidence 65, got '$($autoIffCheck.MovementConfidence)'"
}
if ($autoIffCheck.MovementRssiDeltaDb -ne "9") {
    throw "Expected auto MovementRssiDeltaDb 9, got '$($autoIffCheck.MovementRssiDeltaDb)'"
}
if ($autoIffCheck.GpsStatus -ne "GPS_UNAVAILABLE") {
    throw "Expected auto GpsStatus GPS_UNAVAILABLE, got '$($autoIffCheck.GpsStatus)'"
}
if ($autoIffCheck.GpsAccuracyM -ne "") {
    throw "Expected auto GpsAccuracyM to be empty for na, got '$($autoIffCheck.GpsAccuracyM)'"
}
if ($autoWallShadowCheck.OfficeProximityReason -notlike "*PHONE_B_WITNESS*") {
    throw "Expected wall-shadow auto check to explain visible B side, got '$($autoWallShadowCheck.OfficeProximityReason)'"
}
if ($autoWallShadowCheck.DistanceClass -ne "MID") {
    throw "Expected wall-shadow DistanceClass MID, got '$($autoWallShadowCheck.DistanceClass)'"
}
if ($autoWallShadowCheck.DistanceTargetPlayerId -ne "zhenya") {
    throw "Expected wall-shadow DistanceTargetPlayerId zhenya, got '$($autoWallShadowCheck.DistanceTargetPlayerId)'"
}
if ($autoWallShadowCheck.MovementTrend -ne "STABLE") {
    throw "Expected wall-shadow MovementTrend STABLE, got '$($autoWallShadowCheck.MovementTrend)'"
}
if ($autoIffCheck.WitnessRssi -ne "") {
    throw "Expected auto WitnessRssi to be empty, got '$($autoIffCheck.WitnessRssi)'"
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

$onlyBWallShadow = $wallShadow | Where-Object {
    $_.Window -eq "beacon" -and
    $_.Device -eq "test-device" -and
    $_.LocalDevicePlayerId -eq "petya" -and
    $_.Verdict -eq "ONLY_B_VISIBLE"
}
if (@($onlyBWallShadow).Count -ne 1) {
    throw "Expected one ONLY_B_VISIBLE wall-shadow summary row, got $(@($onlyBWallShadow).Count)"
}
if ($onlyBWallShadow.Count -ne "1") {
    throw "Expected ONLY_B_VISIBLE wall-shadow count 1, got '$($onlyBWallShadow.Count)'"
}
if ($onlyBWallShadow.VisibleSide -ne "B") {
    throw "Expected wall-shadow visible side B, got '$($onlyBWallShadow.VisibleSide)'"
}
if ($onlyBWallShadow.MissingSide -ne "A") {
    throw "Expected wall-shadow missing side A, got '$($onlyBWallShadow.MissingSide)'"
}

$gpsAB = $gpsVectors | Where-Object {
    $_.Window -eq "beacon" -and
    $_.Bucket -eq "13:00:00-13:00:05" -and
    $_.FromDevice -eq "gps-a" -and
    $_.ToDevice -eq "gps-b"
}
if (@($gpsAB).Count -ne 1) {
    throw "Expected one GPS vector gps-a -> gps-b, got $(@($gpsAB).Count)"
}
if ($gpsAB.DistanceM -ne "111") {
    throw "Expected GPS distance 111m, got '$($gpsAB.DistanceM)'"
}
if ($gpsAB.BearingDeg -ne "0") {
    throw "Expected GPS bearing 0deg, got '$($gpsAB.BearingDeg)'"
}
if ($gpsAB.Status -ne "GPS_OK") {
    throw "Expected GPS vector status GPS_OK, got '$($gpsAB.Status)'"
}

$summary = Get-Content -Path (Join-Path $OutputDir "summary.md") -Raw
if ($summary -notlike "*## Office Wall Shadow Summary*") {
    throw "Expected summary.md to include Office Wall Shadow Summary section"
}
if ($summary -notlike "*ONLY_B_VISIBLE*") {
    throw "Expected summary.md to include ONLY_B_VISIBLE"
}
if ($summary -notlike "*## GPS Device Vectors*") {
    throw "Expected summary.md to include GPS Device Vectors section"
}

Write-Host "Beacon analyzer test passed."
