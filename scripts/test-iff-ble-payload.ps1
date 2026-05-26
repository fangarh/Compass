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

$outDir = Join-Path $root "artifacts\test-iff-ble-payload\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$payloadSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffBlePayload.java"
$restartPolicySource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffBleAdvertiseRestartPolicy.java"
$ageTrackerSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffBleGpsAgeTracker.java"
$scanRetryPolicySource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffBleScanRetryPolicy.java"
$test = Join-Path $root "scripts\test-data\iff-ble-payload\IffBlePayloadTest.java"

& $javac -encoding UTF-8 -d $outDir $payloadSource $restartPolicySource $ageTrackerSource $scanRetryPolicySource $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffBlePayloadTest
if ($LASTEXITCODE -ne 0) {
    throw "IffBlePayloadTest failed with exit code $LASTEXITCODE"
}

Write-Host "IFF BLE payload test passed."
