$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = $env:JAVA_HOME

if ([string]::IsNullOrWhiteSpace($javaHome)) {
    $javaHome = "C:\Program Files\Android\Android Studio\jbr"
}

$javac = Join-Path $javaHome "bin\javac.exe"
$java = Join-Path $javaHome "bin\java.exe"

if (-not (Test-Path $javac)) {
    throw "javac not found at $javac"
}
if (-not (Test-Path $java)) {
    throw "java not found at $java"
}

$outDir = Join-Path $root "artifacts\test-pda-scene-metrics\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$source = Join-Path $root "app\src\main\java\net\afterday\compas\view\PdaSceneMetrics.java"
$test = Join-Path $root "scripts\test-data\pda-scene-metrics\PdaSceneMetricsTest.java"

& $javac -encoding UTF-8 -d $outDir $source $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir PdaSceneMetricsTest
if ($LASTEXITCODE -ne 0) {
    throw "PdaSceneMetricsTest failed with exit code $LASTEXITCODE"
}

Write-Host "PDA scene metrics test passed."
