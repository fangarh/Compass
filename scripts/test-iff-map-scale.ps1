$ErrorActionPreference = "Stop"

$root = Resolve-Path (Join-Path $PSScriptRoot "..")
$javac = "C:\Program Files\Android\Android Studio\jbr\bin\javac.exe"
$java = "C:\Program Files\Android\Android Studio\jbr\bin\java.exe"

if (-not (Test-Path $javac)) {
    throw "javac not found at $javac"
}
if (-not (Test-Path $java)) {
    throw "java not found at $java"
}

$outDir = Join-Path $root "build\test-iff-map-scale"
New-Item -ItemType Directory -Force $outDir | Out-Null

$source = Join-Path $root "app\src\main\java\net\afterday\compas\iff\IffMapScale.java"
$test = Join-Path $root "scripts\test-data\iff-map-scale\IffMapScaleTest.java"

& $javac -encoding UTF-8 -d $outDir $source $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp $outDir IffMapScaleTest
if ($LASTEXITCODE -ne 0) {
    throw "IffMapScaleTest failed with exit code $LASTEXITCODE"
}

Write-Host "Iff map scale test passed."
