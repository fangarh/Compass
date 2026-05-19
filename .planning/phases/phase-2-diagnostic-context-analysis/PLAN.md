# Phase 2 Plan: Diagnostic Context And Movement Analysis

## Goal

Improve the next Wi-Fi field test by recording phone context in each diagnostic
log and by extending the analyzer to highlight BSSID changes between movement
windows.

## Scope

- Add a device context header to `FieldDiagnosticLog`.
- Include app, Android, device, battery, charging, power-save, Wi-Fi, and
  location state.
- Keep the logger best-effort and non-crashing.
- Extend `scripts/analyze-field-logs.ps1` to parse context headers.
- Add movement delta outputs for BSSID RSSI changes between windows.
- Document the new outputs.

## Files

- `app/src/main/java/net/afterday/compas/logging/FieldDiagnosticLog.java`
- `scripts/analyze-field-logs.ps1`
- `docs/recovery/field-diagnostic-log.md`
- `stalker/Проверки/2026-05-19 Field Diagnostic Log.md`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`

## Verification

1. Run `scripts/analyze-field-logs.ps1` on existing pulled logs.
2. Confirm new analysis outputs are generated.
3. Build debug APK.
4. Install and launch on at least one physical device.
5. Confirm a new log contains `FIELD_DIAG event=device_context`.

## Out Of Scope

- No gameplay logic changes.
- No detection threshold changes.
- No BLE implementation.
- No UI work.
