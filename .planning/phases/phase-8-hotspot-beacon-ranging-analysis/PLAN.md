# Phase 8 Plan: Hotspot Beacon Ranging Analysis

## Goal

Use the two available phones to approximate the real forest task: one device
acts as a Wi-Fi beacon, the other records how the beacon appears in scan
results over movement and distance.

## Scope

- Extend the offline field-log analyzer, not the game runtime.
- Filter beacon candidates by configured SSID patterns, defaulting to
  `COMPASS_BEACON*`.
- Produce beacon-specific CSV outputs with RSSI, range bands, and short-term
  stronger/weaker/stable trends.
- Add a small analyzer regression test with a synthetic beacon log.
- Document how to run the next controlled two-phone test.

## Files

- `scripts/analyze-field-logs.ps1`
- `scripts/test-analyze-field-logs.ps1`
- `scripts/test-data/beacon-field-logs/test-device/field-radio-20260519-130000.log`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `docs/recovery/field-diagnostic-log.md`
- `stalker/Проверки/2026-05-19 Field Diagnostic Log.md`

## Verification

- Run `powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1`.
- Confirm `beacon-timeline.csv` contains only matching `COMPASS_BEACON*`
  entries.
- Confirm `beacon-bucket-summary.csv` reports both `stronger` and `weaker`
  trend labels on the fixture.

## Out Of Scope

- No gameplay influence formula changes.
- No new APK build or install.
- No GPS/direction fusion for this increment.
- No conclusions from office AP topology as a substitute for an object beacon.
