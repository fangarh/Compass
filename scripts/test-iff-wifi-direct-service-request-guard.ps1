$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\iff\IffWifiDirectDiscoveryTransport.java"
$source = Get-Content -Path $sourcePath -Raw

if ($source -match "addServiceRequest\s*\(\s*currentChannel\s*,\s*serviceRequest") {
    throw "Wifi Direct service request is passed directly without a null guard."
}

$required = @(
    "newServiceRequest()",
    "ensureServiceRequest()",
    "reason=null_request",
    "WifiP2pDnsSdServiceRequest.newInstance()",
    "addServiceRequest(currentChannel, request"
)

foreach ($needle in $required) {
    if (-not $source.Contains($needle)) {
        throw "Missing Wifi Direct service request guard marker: $needle"
    }
}

Write-Host "Wifi Direct service request null guard source checks passed."
