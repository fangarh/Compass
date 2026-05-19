# State

## Current Phase

Phase 3: Named Analysis Windows completed.

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

Use named analyzer windows for future field runs. Latest named output:
`artifacts/field-analysis-run-20260519-1045-named`.

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
