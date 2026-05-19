# Phase 7: Multisensor Field Diagnostics

## Goal

Collect all practical phone-side signals that may help indoor route/zone
classification, while keeping the gameplay logic unchanged.

## Scope

- Add a diagnostic-only sampler for Android motion, orientation, environment,
  step, and location signals.
- Write sensor/location diagnostics into the same phone-owned field log as
  `WIFI_DIAG`.
- Extend the field-log analyzer with sensor and location CSV outputs.
- Keep Wi-Fi 1 Hz diagnostics active for the next controlled route test.

## Files

- `app/src/main/AndroidManifest.xml`
- `app/src/main/java/net/afterday/compas/MainActivity.java`
- `app/src/main/java/net/afterday/compas/logging/FieldDiagnosticLog.java`
- `app/src/main/java/net/afterday/compas/logging/FieldSensorDiagnosticSampler.java`
- `app/src/main/java/net/afterday/compas/sensors/WiFi/WifiImpl.java`
- `scripts/analyze-field-logs.ps1`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `docs/recovery/field-diagnostic-log.md`
- `stalker/Проверки/2026-05-19 Field Diagnostic Log.md`

## Verification Criteria

- `:app:assembleDebug` succeeds.
- App version is `1816-diagnostic-sensors-1s`.
- Field log contains `WIFI_DIAG`, `SENSOR_DIAG`, and `LOCATION_DIAG`.
- Analyzer exports:
  - `sensor-timeline.csv`
  - `sensor-summary.csv`
  - `location-timeline.csv`
  - `location-summary.csv`
- Existing Wi-Fi freshness analysis still works.

## Out Of Scope

- Runtime route classifier.
- UI changes.
- Changing gameplay influence calculations.
- Treating GPS as reliable indoor truth.
