param()

$ErrorActionPreference = "Stop"

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$landLayoutPath = Join-Path $root "app\src\main\res\layout-land\activity_main.xml"
$portLayoutPath = Join-Path $root "app\src\main\res\layout-port\activity_main.xml"
$mainActivityPath = Join-Path $root "app\src\main\java\net\afterday\compas\MainActivity.java"

function Assert-Contains([string]$Text, [string]$Pattern, [string]$Message) {
    if ($Text -notmatch $Pattern) {
        throw $Message
    }
}

$landLayout = Get-Content -Path $landLayoutPath -Raw
$portLayout = Get-Content -Path $portLayoutPath -Raw
$mainActivity = Get-Content -Path $mainActivityPath -Raw

foreach ($layout in @($landLayout, $portLayout)) {
    Assert-Contains $layout 'android:id="@id/log_background"' "Main layout must include the log background."
    Assert-Contains $layout 'android:id="@id/log_list"' "Main layout must include the small information log RecyclerView."
    Assert-Contains $layout 'android\.support\.v7\.widget\.RecyclerView\s+android:id="@id/log_list"' "The small information log must be a RecyclerView."
}

Assert-Contains $landLayout 'android:background="@drawable/log_h"' "Landscape layout must use the horizontal log window art."
Assert-Contains $portLayout 'android:background="@drawable/log_v"' "Portrait layout must use the vertical log window art."
Assert-Contains $mainActivity 'this\.logList\s*=\s*\(RecyclerView\)\s*findViewById\(R\.id\.log_list\)' "MainActivity must bind the small information log list."
Assert-Contains $mainActivity 'this\.logList\.setAdapter\(this\.logAdapter\)' "MainActivity must attach the small information log adapter."

Write-Host "Main information log window layout checks passed."
