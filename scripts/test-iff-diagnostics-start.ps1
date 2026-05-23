$ErrorActionPreference = "Stop"

$activityPath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\IffActivity.java"
$activitySource = Get-Content -LiteralPath $activityPath -Raw

if ($activitySource -notmatch 'protected void onCreate\(@Nullable Bundle savedInstanceState\)\s*\{(?s).*?FieldDiagnosticLog\.start\(this\);') {
    throw "IffActivity.onCreate must call FieldDiagnosticLog.start(this) so direct field launches write IFF_DIAG logs."
}

Write-Host "IFF diagnostics start test passed."
