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

$androidJar = Join-Path $env:ANDROID_HOME "platforms\android-36\android.jar"
if (-not (Test-Path $androidJar)) {
    $androidJar = "C:\Users\Admin\AppData\Local\Android\Sdk\platforms\android-36\android.jar"
}
if (-not (Test-Path $androidJar)) {
    throw "android.jar not found"
}

$annotationsJar = Join-Path $env:USERPROFILE ".gradle\caches\modules-2\files-2.1\com.android.support\support-annotations\28.0.0\ed73f5337a002d1fd24339d5fb08c2c9d9ca60d8\support-annotations-28.0.0.jar"
if (-not (Test-Path $annotationsJar)) {
    throw "support annotations jar not found at $annotationsJar"
}

$outDir = Join-Path $root "artifacts\test-abstract-view-scale\classes"
New-Item -ItemType Directory -Force $outDir | Out-Null

$source = Join-Path $root "app\src\main\java\net\afterday\compas\view\AbstractView.java"
$test = Join-Path $root "scripts\test-data\abstract-view-scale\AbstractViewScaleTest.java"

& $javac -encoding UTF-8 -cp "$androidJar;$annotationsJar" -d $outDir $source $test
if ($LASTEXITCODE -ne 0) {
    throw "javac failed with exit code $LASTEXITCODE"
}

& $java -cp "$outDir;$androidJar;$annotationsJar" net.afterday.compas.view.AbstractViewScaleTest
if ($LASTEXITCODE -ne 0) {
    throw "AbstractViewScaleTest failed with exit code $LASTEXITCODE"
}

Write-Host "AbstractView scale test passed."
