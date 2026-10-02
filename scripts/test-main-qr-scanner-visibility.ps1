param()

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$mainActivityPath = Join-Path $root "app\src\main\java\net\afterday\compas\MainActivity.java"
$levelIndicatorPath = Join-Path $root "app\src\main\java\net\afterday\compas\view\LevelIndicator.java"

function Assert-Contains([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

function Assert-NotContains([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -match $Pattern) {
        throw $Message
    }
}

$mainActivity = Get-Content -Path $mainActivityPath -Raw
$levelIndicator = Get-Content -Path $levelIndicatorPath -Raw

Assert-Contains $mainActivity 'mQrButton\.setVisibility\(View\.VISIBLE\)' "QR scanner button must be explicitly kept visible."
Assert-Contains $mainActivity 'mQrButton\.setEnabled\(true\)' "QR scanner button must stay enabled when IFF/LOG are hidden."
Assert-Contains $mainActivity 'mQrButton\.setClickable\(true\)' "QR scanner button must stay clickable when IFF/LOG are hidden."
Assert-NotContains $levelIndicator 'setAlpha\(0\)' "LevelIndicator must not clear its own alpha during draw; that hides the scanner button."
Assert-Contains $levelIndicator 'mScaleFactorX\s*=\s*this\.mWidth\s*/\s*\(float\)\s*this\.backgroundWidth' "LevelIndicator must use float division for X scale; integer division can make the scanner icon zero-sized."
Assert-Contains $levelIndicator 'mScaleFactorY\s*=\s*this\.mHeight\s*/\s*\(float\)\s*this\.backgroundHeight' "LevelIndicator must use float division for Y scale; integer division can make the scanner icon zero-sized."

Write-Host "Main QR scanner visibility checks passed."
