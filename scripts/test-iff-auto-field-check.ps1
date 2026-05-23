$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = $env:JAVA_HOME

if ([string]::IsNullOrWhiteSpace($javaHome)) {
    throw "JAVA_HOME is not set"
}

$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"

if (-not (Test-Path $javac)) {
    throw "javac not found at $javac"
}
if (-not (Test-Path $java)) {
    throw "java not found at $java"
}

$outDir = Join-Path $root "artifacts\test-iff-auto-field-check\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$source = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffAutoFieldCheckSnapshot.java"
$formatterSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffFieldSnapshotFormatter.java"
$gpsSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffGpsSnapshot.java"
$gpsStabilizerSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffGpsStabilizer.java"
$runSummarySource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffFieldRunSummary.java"
$officeSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffOfficeProximityVerdict.java"
$distanceSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffDistanceTrend.java"
$test = Join-Path $root "scripts\test-data\iff-auto-field-check\IffAutoFieldCheckSnapshotTest.java"

& $javac -encoding UTF-8 -d $outDir $source $formatterSource $gpsSource $gpsStabilizerSource $runSummarySource $officeSource $distanceSource $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffAutoFieldCheckSnapshotTest
if ($LASTEXITCODE -ne 0) {
    throw "IffAutoFieldCheckSnapshotTest failed with exit code $LASTEXITCODE"
}

Write-Host "Iff auto field check snapshot test passed."
