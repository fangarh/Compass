# 2026-05-23 operator two-anchor snapshot

## What changed

- Added `IffOperatorFieldSnapshotStore`.
- The map now uses a smoothed operator snapshot instead of a raw single-tick `TWO_ANCHORS` result.
- The stabilizer keeps the last 8 seconds of two-anchor fixes, chooses the modal `clock` and `distance`, and holds the last valid fix during short relay gaps.
- Build version: `1845-operator-two-anchor-snapshot`.

## Why

The 11:42 Mi walk proved the MVP path:

- OnePlus and Samsung both observed Mi as target.
- Wi-Fi Direct relay exchanged target RSSI between anchors.
- Both anchors produced `WIFI_TARGET / TWO_ANCHORS`.

Raw ticks still jump between clock buckets, so the field UI needs an operator-level snapshot rather than one-tick rendering.

## Verified

- `scripts/test-iff-field-locator.ps1`
- `scripts/test-iff-wifi-target-locator.ps1`
- `scripts/test-field-locator-comparison.ps1`
- `gradle :app:assembleDebug`

Installed and launched on:

- Mi `83efb856`
- OnePlus `e089985a`

Samsung `R3CT20C8A8N` was not visible in `adb` during install and still needs build `1845` when reconnected.

## Next action

1. Reconnect Samsung and install `1845`.
2. Run a short indoor pass: two anchors separated, Mi as walker.
3. Watch the map on an anchor, not on Mi.
4. In logs, compare raw `wifiTargetStatus` with UI/operator `WIFI_TARGET_STABLE` / `WIFI_TARGET_HOLD` behavior.
