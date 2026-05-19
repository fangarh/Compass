# Roadmap

## Phase 1: Field Diagnostic Log

**Status:** completed

**Goal:** Add an app-owned diagnostic file on the physical phone for Wi-Fi/radio
field testing.

**Scope:**

- Create a small field diagnostic logger.
- Write log files under app-specific external storage.
- Instrument the current Wi-Fi scan path with request, result, cached-result,
  status, and raw scan-entry events.
- Document how to install, run, and pull logs from `R3CT20C8A8N`.

**Success Criteria:**

- Debug APK builds.
- Debug APK installs and launches on `R3CT20C8A8N`.
- A diagnostic file appears on the phone after app launch and Wi-Fi scan activity.
- The file can be pulled with ADB into `artifacts/field-logs`.
- The log contains `WIFI_DIAG` request/result/status entries and raw scan entries.

**Verification:** completed on `R3CT20C8A8N` on 2026-05-19. Pulled sample log:
`artifacts/field-logs/diagnostics/field-radio-20260519-092839.log`.

## Phase 2: Diagnostic Context And Movement Analysis

**Status:** completed

**Goal:** Add device context headers to field logs and extend analyzer output
with movement delta candidates.

**Scope:**

- Log model, SDK, app version, battery, charging, power-save, Wi-Fi, and
  location state at startup.
- Parse `FIELD_DIAG event=device_context` in the analyzer.
- Generate `device-context.csv` and `movement-deltas.csv`.

**Verification:** completed on `R3CT20C8A8N` and `e089985a` on 2026-05-19.
Both devices produced `FIELD_DIAG event=device_context`, and analyzer output
confirmed context rows for Samsung SM-S908B and OnePlus NE2215.

## Phase 3: Named Analysis Windows

**Status:** completed

**Goal:** Allow field-log reports to use real movement point names.

**Scope:**

- Analyzer accepts `-Windows` with named time ranges.
- Movement deltas are generated between adjacent named windows and the first to
  last window.
- The 2026-05-19 10:45 run was regenerated as
  `artifacts/field-analysis-run-20260519-1045-named`.

## Phase 4: Offline Zone Fingerprint Evaluator

**Status:** completed

**Goal:** Test whether named zones can be inferred from Wi-Fi fingerprints in
field logs.

**Scope:**

- Analyzer exports `zone-fingerprints.csv`, `zone-evaluation.csv`, and
  `zone-predictions.csv`.
- Analyzer scores 30-second buckets against per-device/per-zone fingerprints.
- The 2026-05-19 10:45 run achieved 29/32 correct bucket predictions.

## Phase 5: Cross-Validated Zone Evaluator

**Status:** completed

**Goal:** Check whether the offline Wi-Fi zone evaluator generalizes across
30-second buckets instead of only matching fingerprints built from the same
data.

**Scope:**

- Analyzer exports leave-one-bucket-out cross-validation outputs.
- The 2026-05-19 10:45 run showed same-data accuracy of 90.6% and
  cross-validated accuracy of 34.4%.
- The result blocks direct runtime use of the naive fingerprint score.

## Phase 6: 1 Hz Wi-Fi Freshness Test

**Status:** completed

**Goal:** Measure whether Wi-Fi scan freshness can support a 3-5 second game
reaction window.

**Scope:**

- Request scans every 1 second in the diagnostic build.
- Log freshness ticks and cached scan result count every second.
- Analyze fresh receiver update intervals in 1s/3s/5s buckets.

**Verification:** completed on both physical phones on 2026-05-19. Short
verification logs showed `diagnostic-1s`, `intervalMs=1000`, `event=tick`, and
fresh receiver updates. Analyzer emitted `freshness-summary.csv`.

## Phase 7: Multisensor Field Diagnostics

**Status:** completed

**Goal:** Capture all practical phone-side signals that may help classify short
indoor route segments in later offline analysis.

**Scope:**

- Add diagnostic logging for accelerometer, gyroscope, magnetic field, rotation
  vector orientation, pressure, light, proximity, step counter, and location.
- Keep the sampler diagnostic-only; do not feed it into gameplay calculations.
- Extend the analyzer with sensor and location timeline/summary CSV files.

**Verification:** `:app:assembleDebug` completed on 2026-05-19. Samsung
SM-S908B produced `SENSOR_DIAG` and `LOCATION_DIAG` lines with app version
`1816-diagnostic-sensors-1s`; analyzer emitted sensor/location CSV files and
existing Wi-Fi freshness output remained valid.

## Phase 8: Hotspot Beacon Ranging Analysis

**Status:** completed

**Goal:** Return the field work to the original forest problem by analyzing one
phone as a Wi-Fi object beacon and the other phone as the receiver.

**Scope:**

- Keep the current diagnostic APK unchanged; raw `scan_entry` lines already
  contain SSID/BSSID/RSSI/frequency.
- Extend `scripts/analyze-field-logs.ps1` with `-BeaconSsids`.
- Export `beacon-timeline.csv`, `beacon-bucket-summary.csv`, and
  `beacon-summary.csv`.
- Classify rough RSSI range bands and short-term stronger/weaker/stable trends.

**Verification:** `scripts/test-analyze-field-logs.ps1` completed on
2026-05-19 with a synthetic `COMPASS_BEACON_A` log and detected both stronger
and weaker beacon trends.

## Phase 9: IFF MVP Skeleton

**Status:** completed

**Goal:** Start the real MVP path from Obsidian: a separate `свой-чужой` tactical
layer for confirming known teammates, not a generic object-beacon finder.

**Scope:**

- Add an explicit `IFF` entry point on the main PDA screen.
- Add a separate IFF screen with `КОНТАКТ`, `КОМАНДА`, and `КАРТА` tabs.
- Add a local `Я ПОДХОЖУ` prototype state without pretending that radio
  confirmation, roster, or witnesses already exist.
- Keep existing gameplay, Wi-Fi influence calculation, QR, inventory, and field
  diagnostics unchanged.

**Verification:** debug APK builds. OnePlus `e089985a` installed the APK and
verified the normal flow: main PDA -> `IFF` -> `Я ПОДХОЖУ` -> `ВЫ ПОДХОДИТЕ`.

## Phase 10: Local IFF Roster

**Status:** completed

**Goal:** Turn the IFF team tab from a placeholder into a local roster of known
teammates while keeping all unverified proximity, position, and direction
signals as `UNKNOWN`.

**Scope:**

- Add a local roster with `Вы`, `Петя`, `Вася`, and `Женя`.
- Render the roster on the `КОМАНДА` tab.
- Let a selected roster participant open the `КОНТАКТ` tab.
- Keep `Я ПОДХОЖУ` scoped to the local player.
- Keep radio, proximity, GPS, direction, network exchange, and cryptography out
  of this slice.

**Verification:** debug APK builds. OnePlus `e089985a` installed the APK and
verified the flow: main PDA -> `IFF` -> `КОМАНДА` -> select `Петя` ->
`КОНТАКТ` -> `Я ПОДХОЖУ`. The roster scrolls to `Женя`, and radio/proximity,
position, and direction remain explicitly `UNKNOWN`.

## Phase 11: IFF Radio Witness

**Status:** completed

**Goal:** Add the first phone-to-phone proximity proof layer by detecting known
IFF Wi-Fi beacon SSIDs in scan results and showing freshness/RSSI as radio
witness evidence.

**Scope:**

- Recognize only known roster beacon SSIDs:
  - `COMPASS_IFF_YOU`
  - `COMPASS_IFF_PETYA`
  - `COMPASS_IFF_VASYA`
  - `COMPASS_IFF_ZHENYA`
- Store the newest witness per player from Android `ScanResult` data.
- Show radio freshness, age, RSSI, and rough proximity on `КОНТАКТ`.
- Show radio witness status on `КОМАНДА`.
- Keep identity, proximity, position, and direction separate.
- Treat unknown `COMPASS_IFF_*` SSIDs as unknown, not teammates.

**Verification:** debug APK builds. APK installed on Samsung `R3CT20C8A8N` and
OnePlus `e089985a`. Samsung UIAutomator flow verified: main PDA -> `IFF` ->
`КОМАНДА` -> select `Петя` -> `КОНТАКТ` -> `Я ПОДХОЖУ`. A real two-phone check
then used Samsung hotspot SSID `COMPASS_IFF_PETYA` and OnePlus as receiver:
`Петя` showed `RADIO_FRESH`, `RADIO_NEAR`, RSSI down to `-55 dBm`, age `1s`,
and frequency `2462 MHz`. After hotspot shutdown, proximity moved from fresh to
stale at about 20 s and to `UNKNOWN` at about 60 s.

## Phase 12: IFF Confidence Model

**Status:** completed

**Goal:** Convert raw roster/radio witness states into an explicit confidence
model for the four independent MVP layers: identity, proximity, position, and
direction.

**Scope:**

- Add a small local confidence model for IFF contact decisions.
- Score identity from local self, local roster, and fresh radio claim.
- Score proximity from fresh/stale RSSI witness without treating local
  `Я ПОДХОЖУ` as radio proof.
- Keep position and direction as explicit `UNKNOWN 0%` layers.
- Update `КОНТАКТ`, `КОМАНДА`, and `КАРТА` to show confidence scores.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified `КОМАНДА` shows `RADIO FRESH: 0`, `PROXIMITY OK: 0`, and
per-player identity/proximity percentages. `Петя` without beacon shows
`IDENTITY: ROSTER_ONLY 40%`, `PROXIMITY: UNKNOWN 0%`, `POSITION: UNKNOWN 0%`,
and `DIRECTION: UNKNOWN 0%`. `Я ПОДХОЖУ` shows local self approach at 80% while
proximity remains `LOCAL_DECLARED_UNKNOWN 20%` and explicitly not radio proof.

## Phase 13: IFF Field MVP Test Flow

**Status:** completed

**Goal:** Make the IFF MVP field check repeatable by letting the operator record
the current selected contact verdict, confidence layers, and radio witness into
the diagnostic log.

**Scope:**

- Add a `ЗАПИСАТЬ` action to the IFF screen.
- Record selected player identity/proximity/position/direction confidence.
- Record current witness SSID/BSSID/RSSI/age when present.
- Show the last recorded field-check summary on `КОНТАКТ` and `КОМАНДА`.
- Keep the flow manual and local; no sync, crypto, GPS, or direction geometry.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified `IFF` -> `КОМАНДА` -> select `Петя` -> `ЗАПИСАТЬ`.
The contact screen showed the last field-check summary for `Петя`, and
`field-radio-20260519-160400.log` contained
`IFF_DIAG event=field_check playerId=petya identityScore=40 proximityScore=0
witness=none`. Follow-up near/far/off session with Samsung hotspot
`COMPASS_IFF_PETYA` and OnePlus receiver recorded:
near `RADIO_NEAR 75% rssi=-39`, far `RADIO_MID 55% rssi=-68`, off/stale
`STALE_RADIO 25%`, and off/unknown `UNKNOWN 0%`.

## Phase 14: IFF Field Log Analysis

**Status:** completed

**Goal:** Convert the captured near/far/off IFF session into analyzer outputs
and threshold notes for the MVP confidence model.

**Scope:**

- Parse `IFF_DIAG event=field_check` in the field-log analyzer.
- Export `iff-field-checks.csv` and `iff-field-check-summary.csv`.
- Add an IFF field-check section to `summary.md`.
- Keep Android runtime behavior unchanged.

**Verification:** analyzer completed on
`artifacts/iff-field-session-20260519-1613` with 1 log file and 8036 scan
entries. It produced IFF timeline/summary CSV outputs and a Markdown section
with the near/far/off field-check table. Field notes:
near `RADIO_NEAR 75% rssi=-39 age=2269ms`, far `RADIO_MID 55% rssi=-68
age=12161ms`, off/stale `STALE_RADIO 25% age=28759ms`, off/unknown
`UNKNOWN 0% age=70886ms`.

## Phase 15: IFF Second Field Run

**Status:** completed

**Goal:** Repeat the IFF field check with multiple samples per state to see
whether current proximity labels remain stable under office distance and
shielding.

**Scope:**

- Use Samsung `SM-S908B` as hotspot `COMPASS_IFF_PETYA`.
- Use OnePlus `NE2215` as receiver.
- Record near, far, return-near, body-shielded, cabinet-shielded, and off
  states through the IFF `ЗАПИСАТЬ` action.
- Analyze the pulled diagnostic log with named windows.

**Verification:** analyzer completed on
`artifacts/iff-field-session-20260519-1643` with 1 log file and 27644 scan
entries. Summary:
near 3 samples avg `-28 dBm` -> `RADIO_NEAR`; far 3 samples avg `-59.7 dBm`
through office/walls -> `RADIO_MID`; return-near 2 samples avg `-21 dBm` ->
`RADIO_NEAR`; body-shielded 3 samples avg `-45.3 dBm` -> `RADIO_NEAR`;
cabinet-shielded 3 samples avg `-43.7 dBm` -> `RADIO_NEAR`; off 3 samples
transitioned from `STALE_RADIO` to `UNKNOWN`.

## Phase 16: IFF Cautious Proximity UI

**Status:** completed

**Goal:** Make the IFF proximity wording safer so medium RSSI is a weak hint,
not a strong or exact distance confirmation.

**Scope:**

- Keep RSSI thresholds unchanged.
- Rename medium fresh radio from `RADIO_MID` to `RADIO_WEAK_HINT`.
- Rename edge fresh radio from `RADIO_WEAK` to `RADIO_EDGE_HINT`.
- Lower medium/edge scores to 45%/30%.
- Count only `RADIO_NEAR` in the team summary as `PROXIMITY STRONG`.
- Keep identity, position, and direction separate.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified main PDA -> `IFF`; team screen showed
`RADIO FRESH: 0`, `PROXIMITY STRONG: 0`, and `DIRECTION: UNKNOWN`.

## Phase 17: IFF Local Witness Quorum

**Status:** completed

**Goal:** Add a multi-witness foundation while keeping the current MVP honest:
only the local phone reports today, remote teammate reports are pending.

**Scope:**

- Add `IffWitnessQuorum`.
- Show `WITNESSES` / `WITNESS QUORUM` in contact details.
- Show `MULTI-WITNESS` count on the team screen.
- Show quorum state on the map list.
- Add quorum fields to `IFF_DIAG event=field_check`.
- Extend the field-log analyzer to export quorum fields.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified main PDA -> `IFF`; team screen showed `MULTI-WITNESS: 0`.
Selecting `Петя` showed `WITNESSES: NO_CURRENT_WITNESS 0/3`, with remote
teammate reports explicitly `PENDING`. `ЗАПИСАТЬ` logged
`witnessQuorum=NO_CURRENT_WITNESS witnessFreshSources=0 witnessPossibleSources=3`.
Analyzer smoke test passed.

## Phase 18: IFF Remote Witness Contract

**Status:** completed

**Goal:** Define the report contract needed for future remote teammate witness
exchange without adding transport or crypto yet.

**Scope:**

- Add `IffRemoteWitnessReport`.
- Add `IffRemoteWitnessStore`.
- Define contract version `iff-remote-witness-v1`.
- Include source player, target player, target beacon SSID, BSSID, RSSI,
  frequency, observed/received monotonic times, and signature status.
- Keep signatures as `SIGNATURE_PENDING`.
- Feed remote report lists into quorum calculation.
- Show remote report count and contract/signature placeholder in the IFF UI.
- Add remote contract fields to `IFF_DIAG event=field_check`.
- Extend analyzer CSV/Markdown output with remote fields.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified main PDA -> `IFF`; team screen showed
`REMOTE REPORTS: 0`, contract `iff-remote-witness-v1`, and
`SIGNATURE_PENDING`. `ЗАПИСАТЬ` for `Петя` logged
`remoteWitnessContract=iff-remote-witness-v1 remoteReportCount=0 remoteFreshSources=0`.
Analyzer smoke test passed, and the fresh contract log analysis produced CSV
rows with `0 reports / 0 fresh`.

## Phase 19: IFF Remote Witness Simulation

**Status:** completed

**Goal:** Validate multi-witness quorum behavior before real network transport
by adding a local-only fixture that injects synthetic remote witness reports for
the selected IFF contact.

**Scope:**

- Add a `SIM WITNESS` action on the IFF screen.
- Generate two synthetic `iff-remote-witness-v1` reports for the selected roster
  participant from other roster sources.
- Keep signatures as `SIGNATURE_PENDING`.
- Feed simulated reports through `IffRemoteWitnessStore` and
  `IffWitnessQuorum`.
- Show `MULTI_WITNESS` only while enough reports are fresh.
- Keep identity, proximity, position, and direction confidence separate and
  unchanged by simulation alone.
- Keep real transport, cryptography, GPS calibration, phone-specific Wi-Fi
  calibration, and Samsung-specific logic out of this slice.

**Verification:** debug APK builds. APK installed on OnePlus `e089985a`.
UIAutomator verified main PDA -> `IFF` -> `КОМАНДА` -> select `Петя` ->
`SIM WITNESS` -> `КОНТАКТ`. The contact screen showed
`WITNESSES: MULTI_WITNESS 2/3`, two fresh remote reports, and
`SIGNATURE_PENDING`; identity remained roster-only without crypto upgrade.
`ЗАПИСАТЬ` logged
`witnessQuorum=MULTI_WITNESS witnessFreshSources=2 witnessPossibleSources=3 remoteReportCount=2 remoteFreshSources=2`.
Analyzer verification on `artifacts/iff-field-session-20260519-1716` produced
an IFF row for `Петя` with `MULTI_WITNESS 2/3` and `2 reports / 2 fresh`.

## Backlog

- Analyze customer Wi-Fi module behavior after the module is available.
- Keep BLE as a deferred architecture option, not a near-term implementation.
