# State

## Current Phase

Phase 17: IFF Local Witness Quorum completed.

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

Next useful slice: design the local transport contract for remote witness
reports: source player id, target player id, freshness, RSSI, and signature
placeholder.

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

2026-05-19 Phase 8:

- Office topology testing was stopped as the primary direction because it does
  not represent the forest distance/direction problem well enough.
- The current app logs already contain raw Wi-Fi scan entries, so no APK change
  was needed for the next step.
- Analyzer now supports controlled hotspot SSID filtering through
  `-BeaconSsids`.
- New outputs:
  - `beacon-timeline.csv`
  - `beacon-bucket-summary.csv`
  - `beacon-summary.csv`
- Verification script:
  `powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1`.

2026-05-19 Phase 9:

- Obsidian corrected the MVP target: real MVP is IFF / `свой-чужой`, not
  object-beacon search.
- Added `IffActivity` as a separate tactical layer with tabs:
  `КОНТАКТ`, `КОМАНДА`, `КАРТА`.
- Added explicit `IFF` button on the main PDA screen.
- Added local `Я ПОДХОЖУ` prototype state.
- Build verification passed with `:app:assembleDebug`.
- Installed and checked on OnePlus `e089985a`:
  main PDA -> `IFF` -> `Я ПОДХОЖУ` -> `ВЫ ПОДХОДИТЕ`.
- Samsung `R3CT20C8A8N` was not visible over ADB during this install check.

2026-05-19 Phase 10:

- Added fixed local IFF roster:
  - `Вы`
  - `Петя`
  - `Вася`
  - `Женя`
- `КОМАНДА` renders the roster as selectable entries.
- Selecting a roster entry opens `КОНТАКТ` for that participant.
- `Я ПОДХОЖУ` remains scoped to local player `Вы`.
- Contact confidence is split into identity, proximity, position, and
  direction, with unimplemented proof layers shown as `UNKNOWN`.
- `:app:assembleDebug` completed successfully.
- Installed and checked on OnePlus `e089985a`:
  main PDA -> `IFF` -> `КОМАНДА` -> select `Петя` -> `КОНТАКТ` ->
  `Я ПОДХОЖУ`.
- Roster scrolling exposed `Женя`; Samsung is still not required for this
  slice.

2026-05-19 Phase 11:

- Added `IffRadioWitnessStore` for known IFF Wi-Fi beacon SSIDs.
- Connected the store to the existing 1 Hz `WifiImpl` scan pipeline.
- Known beacon SSIDs:
  - `COMPASS_IFF_YOU`
  - `COMPASS_IFF_PETYA`
  - `COMPASS_IFF_VASYA`
  - `COMPASS_IFF_ZHENYA`
- Unknown `COMPASS_IFF_*` tokens are logged as unknown and are not promoted to
  teammates.
- Cached Wi-Fi results use `ScanResult.timestamp`, so cached entries do not
  become falsely fresh.
- `КОМАНДА` now shows radio witness status and fresh witness count.
- `КОНТАКТ` shows expected beacon SSID, witness age, RSSI, frequency, rough
  proximity, and keeps position/direction separate as `UNKNOWN`.
- `:app:assembleDebug` completed successfully.
- APK installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- Samsung UIAutomator verification passed:
  main PDA -> `IFF` -> `КОМАНДА` -> select `Петя` -> `КОНТАКТ` ->
  `Я ПОДХОЖУ`.
- No real `COMPASS_IFF_*` hotspot was active during this verification, so
  `RADIO FRESH: 0` and proximity `UNKNOWN` were the expected result.
- Follow-up two-phone check succeeded with Samsung hotspot SSID
  `COMPASS_IFF_PETYA` and OnePlus receiver:
  - `RADIO FRESH: 1` appeared on `КОМАНДА`.
  - `Петя` showed `IDENTITY: ROSTER_ONLY + RADIO_CLAIM - не crypto`.
  - `Петя` showed `PROXIMITY: RADIO_NEAR rssi=-55 age=1s`.
  - Witness details showed `ssid: COMPASS_IFF_PETYA`,
    `bssid: 4a:56:ff:b8:21:0b`, `frequency: 2462 MHz`.
  - After hotspot shutdown, witness aged from fresh to stale around 20 s and
    to `UNKNOWN` around 60 s.

2026-05-19 Phase 12:

- Added `IffConfidence` as the local decision model for:
  - identity;
  - proximity;
  - position;
  - direction.
- `КОНТАКТ` now shows a `CONFIDENCE` block with layer labels and percentages.
- `КОМАНДА` now shows identity/proximity percentages per participant and
  `PROXIMITY OK` count.
- `КАРТА` keeps position and direction explicitly at `UNKNOWN 0%`.
- Local `Я ПОДХОЖУ` produces `LOCAL_SELF_APPROACH 80%` but proximity remains
  `LOCAL_DECLARED_UNKNOWN 20%`, not radio proof.
- `:app:assembleDebug` completed successfully.
- APK installed and checked on OnePlus `e089985a`:
  - no active beacon: `Петя` is `ROSTER_ONLY 40%`, proximity `UNKNOWN 0%`;
  - local approach: `Вы` is `LOCAL_SELF_APPROACH 80%`, proximity
    `LOCAL_DECLARED_UNKNOWN 20%`.

2026-05-19 Phase 13:

- Added `ЗАПИСАТЬ` action to the IFF screen.
- The action records `IFF_DIAG event=field_check` with:
  - selected player id/name;
  - identity/proximity/position/direction confidence labels and scores;
  - current witness state;
  - local approach state.
- `КОНТАКТ` and `КОМАНДА` show the last recorded field-check summary.
- `:app:assembleDebug` completed successfully.
- APK installed and checked on OnePlus `e089985a`.
- UIAutomator verification:
  `IFF` -> `КОМАНДА` -> select `Петя` -> `ЗАПИСАТЬ`.
- Diagnostic file verified:
  `field-radio-20260519-160400.log` contains
  `IFF_DIAG event=field_check playerId=petya identityScore=40
  proximityScore=0 witness=none`.
- Follow-up near/far/off session with Samsung hotspot `COMPASS_IFF_PETYA` and
  OnePlus receiver was recorded in the same diagnostic file:
  - near: `ROSTER_PLUS_RADIO_CLAIM`, `RADIO_NEAR`, proximity `75`,
    RSSI `-39`, witness age `2269 ms`;
  - far: `ROSTER_PLUS_RADIO_CLAIM`, `RADIO_MID`, proximity `55`,
    RSSI `-68`, witness age `12161 ms`;
  - off/stale: `ROSTER_ONLY`, `STALE_RADIO`, proximity `25`,
    RSSI `-57`, witness age `28759 ms`;
  - off/unknown: `ROSTER_ONLY`, `UNKNOWN`, proximity `0`,
    witness age `70886 ms`.

2026-05-19 Phase 14:

- `scripts/analyze-field-logs.ps1` now parses
  `IFF_DIAG event=field_check`.
- New analyzer outputs:
  - `iff-field-checks.csv`;
  - `iff-field-check-summary.csv`;
  - `summary.md` section `IFF Field Checks`.
- Analyzer verification completed:
  `artifacts/iff-field-session-20260519-1613` ->
  `artifacts/iff-field-analysis-20260519-1613`.
- Input: 1 log file, 8036 scan entries.
- Field-check summary:
  - near: `RADIO_NEAR 75%`, RSSI `-39`, age `2269 ms`;
  - far: `RADIO_MID 55%`, RSSI `-68`, age `12161 ms`;
  - off/stale: `STALE_RADIO 25%`, age `28759 ms`;
  - off/unknown: `UNKNOWN 0%`, age `70886 ms`.
- Threshold note: current labels are usable as coarse MVP proximity hints, but
  RSSI still must not raise direction or position confidence.

2026-05-19 Phase 15:

- Ran a second controlled IFF field session:
  - Samsung `SM-S908B` hotspot `COMPASS_IFF_PETYA`;
  - OnePlus `NE2215` / `e089985a` receiver;
  - selected player `Петя`;
  - repeated `ЗАПИСАТЬ` snapshots per state.
- Pulled the updated diagnostic log to
  `artifacts/iff-field-session-20260519-1643`.
- Analyzer verification completed:
  `artifacts/iff-field-session-20260519-1643` ->
  `artifacts/iff-field-analysis-20260519-1643`.
- Input: 1 log file, 27644 scan entries.
- Field-check summary:
  - near: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-28.0`;
  - far through office/walls: 3 samples, `RADIO_MID 55%`, avg RSSI `-59.7`;
  - return near: 2 samples, `RADIO_NEAR 75%`, avg RSSI `-21.0`;
  - body-shielded: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-45.3`;
  - cabinet-shielded: 3 samples, `RADIO_NEAR 75%`, avg RSSI `-43.7`;
  - off: 3 samples, `STALE_RADIO` then `UNKNOWN`, avg age `77337 ms`.
- Threshold note: office shielding did not break near detection in this run.
  Distance/walls moved the signal into `RADIO_MID`. After hotspot shutdown,
  freshness gating correctly overrode the old RSSI and dropped proximity to
  stale/unknown.

2026-05-19 Phase 16:

- Kept RSSI threshold bands unchanged.
- Renamed cautious proximity labels:
  - `RADIO_MID` -> `RADIO_WEAK_HINT`;
  - `RADIO_WEAK` -> `RADIO_EDGE_HINT`.
- Updated confidence scores:
  - `RADIO_NEAR`: `75%`;
  - `RADIO_WEAK_HINT`: `45%`;
  - `RADIO_EDGE_HINT`: `30%`;
  - `STALE_RADIO`: `25%`;
  - `UNKNOWN`: `0%`.
- Team summary now says `PROXIMITY STRONG` and counts only `RADIO_NEAR`.
- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified main PDA -> `IFF`; team screen showed
  `RADIO FRESH: 0`, `PROXIMITY STRONG: 0`, and `DIRECTION: UNKNOWN`.

2026-05-19 Phase 17:

- Added `IffWitnessQuorum` as the first multi-witness foundation.
- Current phone is modeled as `local-device`.
- Remote teammate reports are explicitly `PENDING`; no network exchange exists
  yet.
- Contact screen now shows:
  - `WITNESSES: NO_CURRENT_WITNESS 0/3` when no beacon is heard;
  - `WITNESS QUORUM` details;
  - a note that identity is not upgraded by quorum without crypto.
- Team screen now shows `MULTI-WITNESS: 0`.
- Map list includes quorum state per roster participant.
- `IFF_DIAG event=field_check` now records:
  - `witnessQuorum`;
  - `witnessFreshSources`;
  - `witnessPossibleSources`.
- Analyzer now exports quorum fields into IFF field-check CSV/Markdown output.
- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified main PDA -> `IFF` -> `КОМАНДА` -> select `Петя`.
- `ЗАПИСАТЬ` produced
  `witnessQuorum=NO_CURRENT_WITNESS witnessFreshSources=0 witnessPossibleSources=3`.
- `scripts/test-analyze-field-logs.ps1` passed.
- Fresh quorum log analyzer check completed:
  `artifacts/iff-field-session-20260519-1659` ->
  `artifacts/iff-field-analysis-20260519-1659`, 2398 scan entries, CSV row
  `NO_CURRENT_WITNESS 0/3`.
