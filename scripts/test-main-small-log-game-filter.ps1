param()

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mainActivityPath = Join-Path $root "app\src\main\java\net\afterday\compas\MainActivity.java"

function Assert-Contains([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

$mainActivity = Get-Content -Path $mainActivityPath -Raw

Assert-Contains $mainActivity 'filterSmallLogGameMessages\(log\)' "MainActivity must filter the small on-screen log before updating its adapter."
Assert-Contains $mainActivity 'private\s+List<LogLine>\s+filterSmallLogGameMessages\(List\s+log\)' "MainActivity must keep small-log filtering explicit and local."
Assert-Contains $mainActivity 'private\s+boolean\s+isSmallLogGameMessage\(LogLine\s+line\)' "MainActivity must classify which log lines are allowed in the small window."
Assert-Contains $mainActivity 'startsWith\("WIFI_DIAG "\)' "Small log must hide Wi-Fi diagnostic lines."
Assert-Contains $mainActivity 'startsWith\("IFF_DIAG "\)' "Small log must hide IFF diagnostic lines."
Assert-Contains $mainActivity 'startsWith\("FIELD_DIAG "\)' "Small log must hide field diagnostic lines."
Assert-Contains $mainActivity 'startsWith\("SENSOR_DIAG "\)' "Small log must hide sensor diagnostic lines."
Assert-Contains $mainActivity 'startsWith\("LOCATION_DIAG "\)' "Small log must hide location diagnostic lines."
Assert-Contains $mainActivity 'contains\("event="\)' "Small log must hide structured technical event lines."
Assert-Contains $mainActivity 'contains\("mode="\)' "Small log must hide mode/status technical lines."
Assert-Contains $mainActivity 'contains\("sdk="\)' "Small log must hide platform technical lines."

Write-Host "Main small log game filter checks passed."
