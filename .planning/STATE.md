# State

## Current Phase

Phase 5: Cross-Validated Zone Evaluator completed.

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

Do not move the naive fingerprint evaluator into runtime yet. Cross-validation
showed the current scoring overfits the same log and needs a stronger model.
Latest cross-validation output:
`artifacts/field-analysis-run-20260519-1045-cv`.

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
