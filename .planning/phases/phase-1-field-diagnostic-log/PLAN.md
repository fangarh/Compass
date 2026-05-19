# Phase 1 Plan: Field Diagnostic Log

## Goal

Add an extended field diagnostic log on the physical Android phone for Wi-Fi and
radio tests. The log must be written to a file on the phone so it can be pulled
after a field run and analyzed offline.

## Scope

- Add a lightweight file logger for diagnostic events.
- Store logs in app-specific external files storage:
  `Android/data/net.afterday.compas/files/diagnostics/`.
- Instrument the existing Wi-Fi sensor path in `WifiImpl`.
- Include enough raw scan detail to compare Android behavior against expected
  Wi-Fi beacon behavior.
- Add a short field-test document with ADB commands for install, run, locate,
  and pull.

## Files

Expected app files:

- `app/src/main/java/net/afterday/compas/logging/FieldDiagnosticLog.java`
- `app/src/main/java/net/afterday/compas/sensors/WiFi/WifiImpl.java`

Expected docs:

- `docs/recovery/field-diagnostic-log.md`
- `stalker/Проверки/2026-05-19 Field Diagnostic Log.md`

## Implementation Notes

- Keep the logger best-effort: diagnostic failures must not crash gameplay.
- Use app-specific external storage instead of broad external storage.
- Use append-only text files with a timestamped filename per app process/run.
- Include local wall-clock time and elapsed realtime for correlation.
- Record Wi-Fi events:
  - sensor start and stop;
  - scan request accepted/rejected;
  - permission or policy denial;
  - scan results broadcast with `resultsUpdated`;
  - cached results publication;
  - periodic status with mode, interval, and fresh age;
  - raw scan entries with SSID, BSSID, RSSI level, frequency, timestamp, and
    capabilities.

## Verification

1. Build debug APK.
2. Install on physical device `R3CT20C8A8N`.
3. Grant runtime permissions needed by the app.
4. Launch the app.
5. Confirm a diagnostic file exists under:
   `/sdcard/Android/data/net.afterday.compas/files/diagnostics/`.
6. Pull logs into `artifacts/field-logs`.
7. Confirm the pulled log contains:
   - `FIELD_DIAG event=logger_start`;
   - `WIFI_DIAG event=request`;
   - `WIFI_DIAG event=results`;
   - `updated=true` or `updated=false`;
   - `WIFI_DIAG event=scan_entry`.

## Out Of Scope

- No gameplay logic changes.
- No BLE implementation.
- No Wi-Fi detection strategy rewrite.
- No UI redesign.
- No package/application id change.
- No broad cleanup of decompiled source.

## Field Pull Command

```powershell
New-Item -ItemType Directory -Force artifacts\field-logs
adb -s R3CT20C8A8N pull /sdcard/Android/data/net.afterday.compas/files/diagnostics artifacts\field-logs
```
