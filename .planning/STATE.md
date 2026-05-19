# State

## Current Phase

Phase 6: 1 Hz Wi-Fi Freshness Test in progress.

## Last Verified Baseline

2026-05-19:

- Git worktree was clean before planning.
- GSD agents were installed and checked by the user.
- Physical phone `R3CT20C8A8N` was visible through ADB as Samsung SM-S908B.
- Emulator `emulator-5554` was also visible.
- `.planning/` existed but lacked full GSD files.

## Decisions

- Use `stalker/Компас - индекс.md` as the project context entry point.
- Use Wi-Fi for the current radio-detection line.
- Do not use the emulator for real Wi-Fi radio conclusions.
- First increment is a diagnostic logging slice, not a gameplay change.

## Next Action

Phase 7 added multisensor diagnostics. Before the next blind route test, make
sure both phones are unlocked after install and all startup permissions are
granted, especially physical activity on OnePlus for step counter data.

## Verification

2026-05-19:

- `:app:assembleDebug` completed successfully outside sandbox.
- Debug APK installed on physical device `R3CT20C8A8N`.
- Runtime permissions were granted with ADB.
- App launched on the physical phone.
- Diagnostic file was created on the phone under
  `/sdcard/Android/data/net.afterday.compas/files/diagnostics/`.
- Log was pulled to
  `artifacts/field-logs/diagnostics/field-radio-20260519-092839.log`.
- Log contains `FIELD_DIAG event=logger_start`, `WIFI_DIAG event=sensor_start`,
  `WIFI_DIAG event=request`, receiver results with `updated=true`, cached
  results with `updated=false`, and raw `WIFI_DIAG event=scan_entry` lines.

2026-05-19 Phase 2:

- `:app:assembleDebug` completed successfully outside sandbox.
- Phase 2 APK installed and launched on `R3CT20C8A8N`.
- Phase 2 APK installed and launched on `e089985a`.
- Both devices produced `FIELD_DIAG event=device_context`.
- Analyzer successfully parsed both context headers into
  `artifacts/field-analysis-phase2/device-context.csv`.

2026-05-19 Phase 3:

- Analyzer accepts named windows via `-Windows`.
- The 10:45 controlled run was regenerated with `cabinet`, `corridor`,
  `cabinet_return`, and `near_30cm` windows.
- Named report output:
  `artifacts/field-analysis-run-20260519-1045-named/summary.md`.

2026-05-19 Phase 4:

- Analyzer now exports zone fingerprints and bucket-level zone predictions.
- The 10:45 controlled run produced 29/32 correct bucket predictions (90.6%).
- Errors were concentrated on movement boundary buckets, which is expected for
  30-second buckets around manual transition times.

2026-05-19 Phase 5:

- Leave-one-bucket-out cross-validation added.
- Same-data evaluator accuracy remained 29/32 (90.6%).
- Cross-validated accuracy dropped to 11/32 (34.4%).
- This means the naive fingerprint score is useful for exploration but is not
  ready as runtime detection logic. Next model should focus on discriminative
  BSSID, transition smoothing, and device-specific stability.

2026-05-19 Phase 6:

- Diagnostic scan request interval changed to 1000 ms.
- App version changed to `1816-diagnostic-1s`.
- `WIFI_DIAG event=tick` added with `freshAgeMs` and cached result count.
- Analyzer now exports `freshness-timeline.csv` and `freshness-summary.csv`.
- Verification output:
  `artifacts/field-analysis-phase6-verify/freshness-summary.csv`.
- Short verification freshness:
  - `e089985a`: avg fresh age 918 ms, max fresh age 2231 ms, avg receiver gap
    2.41 s, max receiver gap 3.19 s.
  - `R3CT20C8A8N`: avg fresh age 1068 ms, max fresh age 2140 ms, avg receiver
    gap 2.17 s, max receiver gap 2.38 s.

2026-05-19 Phase 7:

- App version changed to `1816-diagnostic-sensors-1s`.
- Added diagnostic-only `FieldSensorDiagnosticSampler`.
- Field logs now include:
  - `SENSOR_DIAG event=tick` with accelerometer, gyroscope, magnetic field,
    yaw/pitch/roll, pressure, light, proximity, step counter, and location age.
  - `LOCATION_DIAG` provider status, last-known locations, and updates.
- Analyzer now exports `sensor-timeline.csv`, `sensor-summary.csv`,
  `location-timeline.csv`, and `location-summary.csv`.
- `:app:assembleDebug` completed successfully.
- Samsung verification log produced 34 sensor ticks, 8 registered sensors,
  9 location events, and intact Wi-Fi freshness output.
- OnePlus installed the APK but still needs manual unlock/permission handling
  for the new physical-activity permission before the next test.
