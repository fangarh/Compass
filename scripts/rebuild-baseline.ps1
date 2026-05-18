param(
    [string]$ProjectRoot = (Resolve-Path "$PSScriptRoot\..").Path
)

$ErrorActionPreference = "Stop"

$java = "C:\Program Files\Android\openjdk\jdk-21.0.8\bin\java.exe"
$sdkBuildTools = Join-Path $env:LOCALAPPDATA "Android\Sdk\build-tools\37.0.0"
$apktool = Join-Path $ProjectRoot "tools\apktool_3.0.2.jar"
$frameworkDir = Join-Path $ProjectRoot "tools\apktool-framework"
$decodedDir = Join-Path $ProjectRoot "recovered\apktool"
$unsignedApk = Join-Path $ProjectRoot "recovered\compassv33-rebuilt-unsigned.apk"
$alignedApk = Join-Path $ProjectRoot "recovered\compassv33-rebuilt-aligned.apk"
$signedApk = Join-Path $ProjectRoot "recovered\compassv33-rebuilt-debugsigned.apk"
$debugKeystore = Join-Path $env:USERPROFILE ".android\debug.keystore"

& $java -jar $apktool b $decodedDir -o $unsignedApk --frame-path $frameworkDir
& (Join-Path $sdkBuildTools "zipalign.exe") -f 4 $unsignedApk $alignedApk

$env:JAVA_HOME = "C:\Program Files\Android\openjdk\jdk-21.0.8"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

& (Join-Path $sdkBuildTools "apksigner.bat") sign `
    --ks $debugKeystore `
    --ks-key-alias androiddebugkey `
    --ks-pass pass:android `
    --key-pass pass:android `
    --out $signedApk `
    $alignedApk

& (Join-Path $sdkBuildTools "apksigner.bat") verify --verbose $signedApk

Write-Host "Built $signedApk"
