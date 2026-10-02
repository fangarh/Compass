$ErrorActionPreference = "Stop"

$sourcePath = Join-Path $PSScriptRoot "..\app\src\main\java\net\afterday\compas\sensors\Bluetooth\BluetoothImpl.java"
$source = Get-Content -Raw $sourcePath

function Assert-Contains($Pattern, $Message) {
    if ($source -notmatch $Pattern) {
        throw $Message
    }
}

Assert-Contains 'if\s*\(\s*this\.bla\s*==\s*null\s*\)' "BluetoothImpl.start must handle devices/emulators without a Bluetooth adapter."
Assert-Contains '\(\(Subject\)\s*this\.resultStream\)\.onNext\(Double\.valueOf\(0\.0d\)\)' "BluetoothImpl must emit a neutral value when Bluetooth is unavailable."
Assert-Contains 'if\s*\(\s*BluetoothImpl\.access\$700\(BluetoothImpl\.this\)\s*!=\s*null\s*\)' "Bluetooth callback must not stopLeScan on a null adapter."
Assert-Contains 'if\s*\(\s*BluetoothImpl\.access\$700\(BluetoothImpl\.this\)\s*!=\s*null\s*\)' "Bluetooth receiver must not startDiscovery on a null adapter."

Write-Host "Bluetooth null adapter guard test passed."
