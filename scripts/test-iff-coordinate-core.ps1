$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$javaHome = $env:JAVA_HOME

if (-not [string]::IsNullOrWhiteSpace($javaHome)) {
    $javac = Join-Path $javaHome "bin\javac.exe"
    $java = Join-Path $javaHome "bin\java.exe"
} else {
    $javac = "javac"
    $java = "java"
}

if (-not (Get-Command $javac -ErrorAction SilentlyContinue)) {
    throw "javac not found via JAVA_HOME or PATH"
}
if (-not (Get-Command $java -ErrorAction SilentlyContinue)) {
    throw "java not found via JAVA_HOME or PATH"
}

$buildDir = Join-Path $root "scripts\test-data\iff-coordinate-core\build"
$outDir = Join-Path $buildDir "classes"
if (Test-Path $buildDir) {
    Remove-Item -LiteralPath $buildDir -Recurse -Force
}

$stateSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffParticipantState.java"
$storeSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffParticipantStore.java"
$messageSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffCoordinateMessage.java"
$mapSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffParticipantMapModel.java"
$approachSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffApproachState.java"
$stabilizerSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffGpsStabilizer.java"
$sanitySource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffGpsSanity.java"
$freshnessSource = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffLocationFreshness.java"
$test = Join-Path $root "scripts\test-data\iff-coordinate-core\IffCoordinateCoreTest.java"

try {
    New-Item -ItemType Directory -Force $outDir | Out-Null

    & $javac -encoding UTF-8 -d $outDir $stateSource $storeSource $messageSource $mapSource $approachSource $stabilizerSource $sanitySource $freshnessSource $test
    if ($LASTEXITCODE -ne 0) {
        throw "javac failed with exit code $LASTEXITCODE"
    }

    & $java -cp $outDir IffCoordinateCoreTest
    if ($LASTEXITCODE -ne 0) {
        throw "IffCoordinateCoreTest failed with exit code $LASTEXITCODE"
    }

    Write-Host "IFF coordinate core test passed."
} finally {
    if (Test-Path $buildDir) {
        Remove-Item -LiteralPath $buildDir -Recurse -Force
    }
}
