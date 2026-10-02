$ErrorActionPreference = "Stop"

$buildFile = Join-Path $PSScriptRoot "..\app\build.gradle.kts"
$content = Get-Content -Raw $buildFile

function Assert-Contains($Pattern, $Message) {
    if ($content -notmatch $Pattern) {
        throw $Message
    }
}

Assert-Contains 'create\("legacy"\)' "Missing legacy product flavor"
Assert-Contains 'minSdk\s*=\s*18' "Legacy APK must support Android 4.3+ with minSdk 18"
Assert-Contains 'create\("legacy"\)\s*\{[^}]*buildConfigField\("boolean",\s*"SHOW_MAIN_ACTION_BUTTONS",\s*"false"\)' "Legacy APK must hide IFF/LOG main action buttons"
Assert-Contains 'android-4-5' "Legacy APK must be copied into artifacts/android-4-5"
Assert-Contains 'compass-android-4-5-hidden-iff-debug\.apk' "Legacy APK artifact must be named as hidden IFF build"
Assert-Contains 'packageLegacyDebugApk' "Missing packageLegacyDebugApk copy task"

Write-Host "Android legacy APK config OK"
