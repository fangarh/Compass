$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$script = Join-Path $root "scripts\simulate-two-anchor-locator.ps1"

$right = & $script -LeftRssi -66 -RightRssi -54
if ($right -notmatch "locator=15m clock=2") {
    throw "Expected right-side estimate with clock=2, got: $right"
}

$left = & $script -LeftRssi -54 -RightRssi -66
if ($left -notmatch "locator=15m clock=10") {
    throw "Expected left-side estimate with clock=10, got: $left"
}

$missing = & $script -LeftRssi -54
if ($missing -notmatch "locator=INSUFFICIENT_DATA") {
    throw "Expected insufficient data when one anchor is missing, got: $missing"
}

Write-Host "Two-anchor locator simulation test passed."
