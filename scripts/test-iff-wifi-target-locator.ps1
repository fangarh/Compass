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

$outDir = Join-Path $root "artifacts\test-iff-wifi-target-locator\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$source = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffWifiTargetLocator.java"
$storeSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffWifiTargetObservationStore.java"
$test = Join-Path $root "scripts\test-data\iff-wifi-target-locator\IffWifiTargetLocatorTest.java"
$storeTest = Join-Path $root "scripts\test-data\iff-wifi-target-locator\IffWifiTargetObservationStoreTest.java"

& $javac -encoding UTF-8 -d $outDir $source $storeSource $test $storeTest
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffWifiTargetLocatorTest
if ($LASTEXITCODE -ne 0) {
    throw "IffWifiTargetLocatorTest failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffWifiTargetObservationStoreTest
if ($LASTEXITCODE -ne 0) {
    throw "IffWifiTargetObservationStoreTest failed with exit code $LASTEXITCODE"
}

Write-Host "IFF Wi-Fi target locator test passed."
