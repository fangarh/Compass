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

$outDir = Join-Path $root "artifacts\test-wifi-mac-gate\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$scanResultStub = Join-Path $root "scripts\test-data\android-stubs\android\net\wifi\ScanResult.java"
$systemClockStub = Join-Path $root "scripts\test-data\android-stubs\android\os\SystemClock.java"
$logStub = Join-Path $root "scripts\test-data\android-stubs\android\util\Log.java"
$event = Join-Path $root "app\src\main\java\net\afterday\compas\core\events\Event.java"
$eventsPack = Join-Path $root "app\src\main\java\net\afterday\compas\core\events\EventsPack.java"
$influence = Join-Path $root "app\src\main\java\net\afterday\compas\core\influences\Influence.java"
$influencesPack = Join-Path $root "app\src\main\java\net\afterday\compas\core\influences\InfluencesPack.java"
$inflPack = Join-Path $root "app\src\main\java\net\afterday\compas\engine\influences\InflPack.java"
$strategyInterface = Join-Path $root "app\src\main\java\net\afterday\compas\engine\influences\InfluenceExtractionStrategy.java"
$converter = Join-Path $root "app\src\main\java\net\afterday\compas\engine\influences\WifiInfluences\WifiConverter.java"
$extractor = Join-Path $root "app\src\main\java\net\afterday\compas\engine\influences\WifiInfluences\AbstractWifiExtractor.java"
$strategy = Join-Path $root "app\src\main\java\net\afterday\compas\engine\influences\WifiInfluences\ByMacExtractionStrategy.java"
$test = Join-Path $root "scripts\test-data\wifi-mac-gate\ByMacExtractionStrategyTest.java"

& $javac -encoding UTF-8 -d $outDir $scanResultStub $systemClockStub $logStub $event $eventsPack $influence $influencesPack $inflPack $strategyInterface $converter $extractor $strategy $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir ByMacExtractionStrategyTest
if ($LASTEXITCODE -ne 0) {
    throw "ByMacExtractionStrategyTest failed with exit code $LASTEXITCODE"
}

Write-Host "Wi-Fi MAC gate test passed."
