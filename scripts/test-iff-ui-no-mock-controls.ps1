param(
    [string]$IffActivityPath = "app\src\main\java\net\afterday\compas\IffActivity.java"
)

$ErrorActionPreference = "Stop"

$source = Get-Content -Path $IffActivityPath -Raw
$forbidden = @(
    "transmitWitnessStub",
    "simulateRemoteWitnesses",
    "simulatedReportAgeMs",
    "remote_witness_transport_stub",
    "remote_witness_simulated",
    "tx stub",
    "simulated fresh",
    "simulated stale"
)

foreach ($term in $forbidden) {
    if ($source.Contains($term)) {
        throw "IFF UI source still contains legacy mock/stub term: $term"
    }
}

Write-Host "IFF UI mock/stub cleanup test passed."
