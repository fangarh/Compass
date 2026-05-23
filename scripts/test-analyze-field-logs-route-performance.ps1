param(
    [string]$InputRoot = "artifacts\field-analysis-input-20260521-route-auto",
    [string]$OutputDir = "artifacts\script-tests\route-analysis-performance",
    [int]$MaxSeconds = 60
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
if (-not [System.IO.Path]::IsPathRooted($InputRoot)) {
    $InputRoot = Join-Path $repoRoot $InputRoot
}
if (-not [System.IO.Path]::IsPathRooted($OutputDir)) {
    $OutputDir = Join-Path $repoRoot $OutputDir
}

if (-not (Test-Path $InputRoot)) {
    throw "Missing route analysis input '$InputRoot'. Collect or prepare route logs before running this performance check."
}

if (Test-Path $OutputDir) {
    $OutputDir = "$OutputDir-$([datetime]::UtcNow.ToString('yyyyMMddHHmmssfff'))"
}

$scriptPath = Join-Path $PSScriptRoot "analyze-field-logs.ps1"
$analyzerParameters = @{
    InputRoot = $InputRoot
    OutputDir = $OutputDir
    TestStart = "2026-05-21 20:43:00"
    Windows = "start=20:43:00..20:44:00;near_one=20:44:00..20:45:00;near_mi=20:45:00..20:45:40;stop_far=20:46:00..20:47:40;return=20:47:40..20:50:30;auto_smoke=21:14:00..21:16:30"
    BucketSeconds = 15
    IncludeLegacyRootLogs = $true
}

$stopwatch = [System.Diagnostics.Stopwatch]::StartNew()
$job = Start-Job -ScriptBlock {
    param($ScriptPath, $AnalyzerParameters)
    & $ScriptPath @AnalyzerParameters
} -ArgumentList $scriptPath, $analyzerParameters
if (-not (Wait-Job $job -Timeout $MaxSeconds)) {
    Stop-Job $job
    Remove-Job $job -Force
    throw "Route field log analysis exceeded ${MaxSeconds}s."
}
$jobResult = Receive-Job $job
$jobState = $job.State
Remove-Job $job
$stopwatch.Stop()

if ($jobState -ne "Completed") {
    throw "Route field log analysis failed with job state $jobState.`n$jobResult"
}

$summaryPath = Join-Path $OutputDir "summary.md"
$iffPath = Join-Path $OutputDir "iff-field-checks.csv"
$zonePath = Join-Path $OutputDir "zone-predictions.csv"
$crossValidationPath = Join-Path $OutputDir "zone-cross-validation-predictions.csv"

foreach ($path in @($summaryPath, $iffPath, $zonePath, $crossValidationPath)) {
    if (-not (Test-Path $path)) {
        throw "Missing expected analysis output '$path'."
    }
}

$iffChecks = @(Import-Csv $iffPath)
if (($iffChecks | Where-Object Source -eq "auto").Count -lt 1) {
    throw "Expected route analysis to include auto IFF field checks."
}

Write-Host "Route analyzer performance test passed in $([math]::Round($stopwatch.Elapsed.TotalSeconds, 1))s."
