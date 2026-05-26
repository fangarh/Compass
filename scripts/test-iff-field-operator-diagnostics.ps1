$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$activityPath = Join-Path $root "app\src\main\java\net\afterday\compas\IffActivity.java"
$servicePath = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffForegroundRadioService.java"
$comparisonPath = Join-Path $root "scripts\compare-field-locator-options.ps1"

$activitySource = Get-Content -LiteralPath $activityPath -Raw
$serviceSource = Get-Content -LiteralPath $servicePath -Raw
$comparisonSource = Get-Content -LiteralPath $comparisonPath -Raw

if ($activitySource -notmatch "FLAG_KEEP_SCREEN_ON") {
    throw "IffActivity must keep the screen on during field/game runs."
}

if ($serviceSource -notmatch "IffOperatorFieldSnapshotStore") {
    throw "IffForegroundRadioService must maintain an operator snapshot store for auto diagnostics."
}

if ($serviceSource -notmatch "operatorFieldMapStatus") {
    throw "auto_field_check diagnostics must include operatorFieldMapStatus with WIFI_TARGET_STABLE/HOLD decisions."
}

foreach ($field in @(
        "gpsLocalLatE7",
        "gpsLocalLonE7",
        "gpsLocalAgeMs",
        "gpsRemoteLatE7",
        "gpsRemoteLonE7",
        "gpsRemoteAgeMs",
        "gpsRawDistanceM",
        "gpsRawBearingDeg")) {
    if ($serviceSource -notmatch $field) {
        throw "auto_field_check diagnostics must include $field for GPS post-run ground truth."
    }
}

if ($serviceSource -notmatch "statusLine=") {
    throw "operatorFieldMapStatus must use a flat statusLine field without nested quotes."
}

if ($comparisonSource -notmatch "OperatorFieldMapStatus") {
    throw "compare-field-locator-options must preserve operatorFieldMapStatus in locator-events.csv."
}

Write-Host "IFF field operator diagnostics test passed."
