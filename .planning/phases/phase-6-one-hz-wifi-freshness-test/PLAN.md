# Phase 6 Plan: 1 Hz Wi-Fi Freshness Test

## Goal

Measure whether Android can provide fresh Wi-Fi scan results fast enough for a
3-5 second game reaction window when the app requests Wi-Fi scans at 1 Hz.

## Scope

- Change the diagnostic Wi-Fi request interval from 5 seconds to 1 second.
- Rename diagnostic mode from `default-5s` to `diagnostic-1s`.
- Log one freshness tick per second with cached result count and age of the last
  fresh receiver result.
- Extend the analyzer with freshness timeline outputs.
- Build and install the APK on both physical phones.

## Verification

1. Build debug APK.
2. Install and launch on `R3CT20C8A8N` and `e089985a`.
3. Confirm logs contain:
   - `mode=diagnostic-1s`
   - `intervalMs=1000`
   - `WIFI_DIAG event=tick`
   - receiver `updated=true` events.
4. Confirm analyzer emits freshness outputs.

## Out Of Scope

- No gameplay integration.
- No runtime zone detection.
- No BLE implementation.
