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
    -BeaconSsids "COMPASS_BEACON*"

$timelinePath = Join-Path $OutputDir "beacon-timeline.csv"
$bucketPath = Join-Path $OutputDir "beacon-bucket-summary.csv"

if (-not (Test-Path $timelinePath)) {
    throw "Missing beacon-timeline.csv"
}
if (-not (Test-Path $bucketPath)) {
    throw "Missing beacon-bucket-summary.csv"
}

$timeline = Import-Csv $timelinePath
$buckets = Import-Csv $bucketPath

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

Write-Host "Beacon analyzer test passed."
