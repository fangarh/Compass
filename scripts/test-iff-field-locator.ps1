$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $root "build\test-iff-field-locator"
if (Test-Path $outDir) {
    Remove-Item -Recurse -Force $outDir
}
New-Item -ItemType Directory -Force $outDir | Out-Null

$sources = @(
    "app\src\main\java\net\afterday\compas\iff\IffWifiTargetLocator.java",
    "app\src\main\java\net\afterday\compas\iff\IffDistanceTrend.java",
    "app\src\main\java\net\afterday\compas\iff\IffGpsSnapshot.java",
    "app\src\main\java\net\afterday\compas\iff\IffFieldLocatorSnapshot.java",
    "app\src\main\java\net\afterday\compas\iff\IffFieldMapSnapshot.java",
    "app\src\main\java\net\afterday\compas\iff\IffOperatorFieldSnapshotStore.java",
    "app\src\main\java\net\afterday\compas\iff\IffWifiTargetObservationStore.java",
    "app\src\main\java\net\afterday\compas\iff\IffTargetObservationPolicy.java",
    "scripts\test-data\iff-field-locator\IffTargetObservationPolicyTest.java",
    "scripts\test-data\iff-field-locator\IffFieldMapSnapshotTest.java",
    "scripts\test-data\iff-field-locator\IffOperatorFieldSnapshotStoreTest.java",
    "scripts\test-data\iff-field-locator\IffFieldLocatorSnapshotTest.java"
)

javac -d $outDir $sources
java -cp $outDir net.afterday.compas.iff.IffFieldLocatorSnapshotTest
