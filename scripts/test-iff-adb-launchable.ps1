$ErrorActionPreference = "Stop"

$manifestPath = Join-Path $PSScriptRoot "..\app\src\main\AndroidManifest.xml"
[xml]$manifest = Get-Content -LiteralPath $manifestPath
$androidNs = "http://schemas.android.com/apk/res/android"

$iffActivity = $manifest.manifest.application.activity | Where-Object {
    $_.GetAttribute("name", $androidNs) -eq "net.afterday.compas.IffActivity"
}

if ($null -eq $iffActivity) {
    throw "IffActivity is missing from AndroidManifest.xml"
}

$exported = $iffActivity.GetAttribute("exported", $androidNs)
if ($exported -ne "true") {
    throw "IffActivity must be android:exported=true so field setup can launch IFF directly through adb; got '$exported'."
}

Write-Host "IFF adb launchable manifest test passed."
