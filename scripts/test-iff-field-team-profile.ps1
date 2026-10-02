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

$outDir = Join-Path $root "artifacts\test-iff-field-team-profile\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$rosterSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffTeamRosterStore.java"
$profileSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffFieldTeamProfile.java"
$test = Join-Path $root "scripts\test-data\iff-field-team-profile\IffFieldTeamProfileTest.java"

& $javac -encoding UTF-8 -d $outDir $rosterSource $profileSource $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffFieldTeamProfileTest
if ($LASTEXITCODE -ne 0) {
    throw "IffFieldTeamProfileTest failed with exit code $LASTEXITCODE"
}

Write-Host "IFF field team profile test passed."
