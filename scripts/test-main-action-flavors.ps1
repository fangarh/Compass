param()

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gradlePath = Join-Path $root "app\build.gradle.kts"
$mainActivityPath = Join-Path $root "app\src\main\java\net\afterday\compas\MainActivity.java"
$emulatorInstallPath = Join-Path $root "scripts\install-debug-on-emulator.ps1"
$prepareDevicePath = Join-Path $root "scripts\prepare-iff-device.ps1"

function Assert-Contains([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

$gradle = Get-Content -Path $gradlePath -Raw
$mainActivity = Get-Content -Path $mainActivityPath -Raw
$emulatorInstall = Get-Content -Path $emulatorInstallPath -Raw
$prepareDevice = Get-Content -Path $prepareDevicePath -Raw

Assert-Contains $gradle 'flavorDimensions\s*(\+=|\.add\()\s*"?mainActions"?' "Missing mainActions flavor dimension."
Assert-Contains $gradle 'create\("standard"\)[\s\S]*dimension\s*=\s*"mainActions"[\s\S]*buildConfigField\("boolean",\s*"SHOW_MAIN_ACTION_BUTTONS",\s*"true"\)' "standard flavor must enable SHOW_MAIN_ACTION_BUTTONS."
Assert-Contains $gradle 'create\("hiddenMainActions"\)[\s\S]*dimension\s*=\s*"mainActions"[\s\S]*buildConfigField\("boolean",\s*"SHOW_MAIN_ACTION_BUTTONS",\s*"false"\)' "hiddenMainActions flavor must disable SHOW_MAIN_ACTION_BUTTONS."

if ($gradle -match 'applicationIdSuffix') {
    throw "Main action flavors must not set applicationIdSuffix; hidden APK should replace the standard package."
}

Assert-Contains $mainActivity 'BuildConfig\.SHOW_MAIN_ACTION_BUTTONS' "MainActivity must use the flavor BuildConfig flag."
Assert-Contains $mainActivity 'mIffButton\.setVisibility\(View\.GONE\)' "Hidden flavor must remove the IFF button from layout/hit testing."
Assert-Contains $mainActivity 'mLogButton\.setVisibility\(View\.GONE\)' "Hidden flavor must remove the LOG button from layout/hit testing."
Assert-Contains $mainActivity 'mIffButton\.setEnabled\(false\)' "Hidden flavor must disable the IFF button."
Assert-Contains $mainActivity 'mLogButton\.setEnabled\(false\)' "Hidden flavor must disable the LOG button."
Assert-Contains $mainActivity 'mIffButton\.setClickable\(false\)' "Hidden flavor must make the IFF button non-clickable."
Assert-Contains $mainActivity 'mLogButton\.setClickable\(false\)' "Hidden flavor must make the LOG button non-clickable."
Assert-Contains $mainActivity 'mIffButton\.setFocusable\(false\)' "Hidden flavor must make the IFF button non-focusable."
Assert-Contains $mainActivity 'mLogButton\.setFocusable\(false\)' "Hidden flavor must make the LOG button non-focusable."
Assert-Contains $mainActivity 'if\s*\(\s*BuildConfig\.SHOW_MAIN_ACTION_BUTTONS\s*\)[\s\S]*mIffButton\.setOnClickListener' "IFF click listener must only be installed when main actions are visible."
Assert-Contains $mainActivity 'if\s*\(\s*BuildConfig\.SHOW_MAIN_ACTION_BUTTONS\s*\)[\s\S]*mLogButton\.setOnClickListener' "LOG click listener must only be installed when main actions are visible."

$standardApk = 'app\\build\\outputs\\apk\\standard\\debug\\app-standard-debug\.apk'
$hiddenApk = 'app\\build\\outputs\\apk\\hiddenMainActions\\debug\\app-hiddenMainActions-debug\.apk'

Assert-Contains $emulatorInstall $standardApk "Emulator install helper must default to the standard flavor APK."
Assert-Contains $emulatorInstall $hiddenApk "Emulator install helper must document the hiddenMainActions APK path."
Assert-Contains $prepareDevice $standardApk "Device prepare helper must default to the standard flavor APK."
Assert-Contains $prepareDevice $hiddenApk "Device prepare helper must document the hiddenMainActions APK path."

Write-Host "Main action flavor checks passed."
