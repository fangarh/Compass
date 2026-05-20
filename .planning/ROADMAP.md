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

## Phase 20: IFF Remote Witness Expiry Fixture

**Status:** completed

**Goal:** Make remote witness freshness and expiry explicitly testable without
waiting for real time or adding network transport.

**Scope:**

- Split local simulation into `SIM FRESH` and `SIM STALE`.
- Use `SIM FRESH` to inject fresh synthetic remote reports.
- Use `SIM STALE` to replace reports with stale synthetic reports for the same
  selected contact.
- Show `STALE_REMOTE_WITNESS 0/3` when remote reports are present but no longer
  current.
- Log and analyze `remoteStaleSources`.
- Keep expired remote reports visible without treating them as current proof.

**Verification:** debug APK builds. Analyzer smoke test passes. APK installed
on OnePlus `e089985a`. UIAutomator/ADB verified main PDA -> `IFF` ->
`КОМАНДА` -> select `Петя` -> `SIM FRESH` -> `SIM STALE` -> `ЗАПИСАТЬ`.
Fresh simulation showed `MULTI_WITNESS 2/3`; stale simulation showed
`STALE_REMOTE_WITNESS 0/3` with two `REMOTE_STALE` reports.
`field-radio-20260519-173501.log` recorded
`remoteReportCount=2 remoteFreshSources=0 remoteStaleSources=2`. Analyzer
verification on `artifacts/iff-field-session-20260519-1735` produced
`2 reports / 0 fresh / 2 stale`.

## Phase 21: IFF Operator Witness Summary

**Status:** completed

**Goal:** Make the IFF screen answer the operator's immediate question faster:
is there current witness evidence, only stale evidence, or no current evidence.

**Scope:**

- Add a compact operator verdict on the contact screen.
- Add team-level current/stale witness evidence counters.
- Show roster entries with the operator verdict plus identity/proximity scores.
- Distinguish `CURRENT_MULTI_WITNESS`, `CURRENT_SINGLE_WITNESS`,
  `STALE_EVIDENCE_ONLY`, `LOCAL_DECLARED_ONLY`, and `NO_CURRENT_EVIDENCE`.
- Log and analyze `operatorVerdict`.
- Keep identity, proximity, position, and direction confidence separate.

**Verification:** debug APK builds. Analyzer smoke test passes. APK installed
on Samsung `R3CT20C8A8N`. UIAutomator/ADB verified main PDA -> `IFF` ->
`КОМАНДА` -> select `Петя` -> `SIM FRESH` -> `SIM STALE` -> `ЗАПИСАТЬ`.
Fresh simulation showed `OPERATOR: CURRENT_MULTI_WITNESS`; stale simulation
showed `OPERATOR: STALE_EVIDENCE_ONLY`. The diagnostic log
`field-radio-20260520-091403.log` recorded
`operatorVerdict=STALE_EVIDENCE_ONLY` with `STALE_REMOTE_WITNESS 0/3`.
Analyzer verification on `artifacts/iff-field-session-20260520-0914` produced
CSV/Markdown rows with `STALE_EVIDENCE_ONLY`.

## Phase 22: IFF UDP Transport Stub

**Status:** completed with RX topology note

**Goal:** Add the first real transport stub for remote IFF witness exchange
without treating unsigned packets as proof of identity or proximity.

**Scope:**

- Add `IffUdpWitnessTransport` for debug UDP broadcast on port `45873`.
- Add `TX STUB` to the IFF screen.
- Send `iff-remote-witness-v1` reports with `SIGNATURE_PENDING`.
- Listen while the IFF screen is visible and ingest accepted remote packets into
  `IffRemoteWitnessStore`.
- Show compact transport state in contact/team/map UI.
- Log `transportStatus` in `IFF_DIAG event=field_check`.
- Export `TransportStatus` from the field-log analyzer.
- Keep crypto, GPS/Wi-Fi calibration, network discovery, Samsung-specific
  logic, and direction inference out of this slice.

**Verification:** debug APK builds. Analyzer smoke test passes. APK installed
on Samsung `R3CT20C8A8N` and OnePlus `e089985a`. UIAutomator/ADB verified
`Main -> IFF` on both phones, both screens showed
`TRANSPORT: udp:45873 ... listening`, and `TX STUB` on each phone updated the
contact transport state to `tx=1 rx=0 rejected=0 rx self ignored` with
`SIGNATURE_PENDING`. Full cross-device RX was not claimed because Samsung had
no `wlan0` address during the test; it was USB-visible but only on `rmnet*`,
while OnePlus was on Wi-Fi `192.168.13.105/24`.

## Phase 23: IFF Phone-to-Phone UDP RX Verification

**Status:** completed

**Goal:** Verify the Phase 22 UDP transport stub between two physical phones on
the same Wi-Fi network.

**Scope:**

- Use existing `TX STUB`; no code changes.
- Put Samsung and OnePlus on one Wi-Fi/hotspot subnet.
- Verify `remote_witness_udp_rx` on the receiving phone.
- Confirm UI and diagnostics keep unsigned packets as remote witness evidence,
  not identity/proximity proof.

**Verification:** Samsung `R3CT20C8A8N` had `10.14.135.249/24` on `swlan0`;
OnePlus `e089985a` had `10.14.135.40/24` on `wlan0`; both shared broadcast
`10.14.135.255`. Both opened `Main -> IFF` and showed
`TRANSPORT: udp:45873 ... listening`. OnePlus `TX STUB` was received by
Samsung: Samsung logged `remote_witness_udp_rx accepted=true from=10.14.135.40`
and `remote_witness_received sourcePlayerId=debug-ne2215 targetPlayerId=local-you
freshness=REMOTE_FRESH signatureStatus=SIGNATURE_PENDING`. A recorded
`field_check` on Samsung showed `remoteReportCount=1` and
`transportStatus="udp:45873 tx=1 rx=1 rejected=0 rx local-you"`. Analyzer
completed on `artifacts/iff-field-session-20260520-1005` and wrote
`artifacts/iff-field-analysis-20260520-1005`.

## Phase 24: IFF Field Identity Selector

**Status:** completed

**Goal:** Remove the hidden assumption that every installed phone is
`local-you` before implementing no-infrastructure field radio.

**Scope:**

- Persist a per-device IFF identity from the roster.
- Show `THIS DEVICE: ...` in the team summary.
- Mark the roster row with `[THIS DEVICE]`.
- On a non-local contact, change the left action button to `ЭТОТ ТЕЛ.` so the
  operator can assign this phone to that participant.
- Keep `Я ПОДХОЖУ` scoped to the current local device identity.
- Log `localDevicePlayerId` and `selectedIsLocalDevice` in
  `IFF_DIAG event=field_check`.
- Export local identity fields from the analyzer.
- Add `BLUETOOTH_ADVERTISE` permission and startup request as preparation for
  BLE field radio.

**Verification:** debug APK builds. Analyzer smoke test passes. APK installed
on Samsung `R3CT20C8A8N` and OnePlus `e089985a`. Samsung UI verified
`Main -> IFF -> КОМАНДА -> Петя -> ЭТОТ ТЕЛ. -> КОМАНДА`; the screen showed
`THIS DEVICE: Петя`, roster showed `Петя [THIS DEVICE]`, and the contact
identity changed to `LOCAL_SELF 70%`.

## Phase 25: IFF BLE Field Radio Skeleton

**Status:** completed

**Goal:** Add the first no-infrastructure IFF radio path so nearby phones can
advertise and scan roster identity claims without a shared Wi-Fi network.

**Scope:**

- Add `IffBleFieldRadio` for BLE advertise/scan while the IFF screen is visible.
- Advertise the current per-device `THIS DEVICE` roster identity.
- Decode known Compass IFF BLE payloads from other phones.
- Convert received BLE advertisements into local radio witness snapshots.
- Show `FIELD RADIO` status in contact, team, and map views.
- Log BLE start/RX/witness events and include `fieldRadioStatus` in
  `IFF_DIAG event=field_check`.
- Export `FieldRadioStatus` from the analyzer.
- Keep the team roster usable on dense landscape phone screens.

**Verification:** debug APK builds. Analyzer smoke test passes. APK installed
on Samsung `R3CT20C8A8N` and OnePlus `e089985a`. Both phones opened
`Main -> IFF`. Samsung used `THIS DEVICE: Петя`; OnePlus used
`THIS DEVICE: Вы`. BLE ran in both directions:

- Samsung logged `ble_field_radio_rx playerId=local-you localPlayerId=petya`.
- OnePlus logged `ble_field_radio_rx playerId=petya localPlayerId=local-you`.
- OnePlus contact for `Петя` showed witness `BLE_IFF_PETYA`, RSSI `-43 dBm`,
  age `5 ms`.
- Samsung contact for `Вы` recorded witness `BLE_IFF_YOU`, RSSI `-38 dBm`,
  age `573 ms`.
- Analyzer completed on `artifacts/iff-field-session-20260520-1114-ble` and
  wrote `artifacts/iff-field-analysis-20260520-1114-ble`.

**Result:** BLE now proves fresh nearby radio presence without relying on a
common Wi-Fi network. It remains a roster claim, not cryptographic identity,
and it still does not provide exact position or direction.

## Phase 26: IFF Tactical Map Mock

**Status:** completed

**Goal:** Add a map-shaped tactical surface for IFF witness visualization while
remaining honest that there is no GPS placement or bearing yet.

**Scope:**

- Add a custom `IffTacticalMapView` to the `КАРТА` tab.
- Render fixed roster slots, local device identity, selected contact, and
  current/stale/unknown radio state colors.
- Show explicit `NO GPS POSITION / NO BEARING` labeling inside the map.
- Keep `POSITION/DIRECTION: UNKNOWN 0%` in the map status.
- Treat slots as roster order only, not direction.

**Verification:** debug APK builds. APK installed on Samsung `R3CT20C8A8N` and
OnePlus `e089985a`. UIAutomator verified `Main -> IFF -> КАРТА` on both
phones, with `POSITION/DIRECTION: UNKNOWN 0%` and a custom canvas view present.
OnePlus screenshot confirmed the mock map renders grid, roster points, BLE
freshness color, and `NO GPS POSITION / NO BEARING` without claiming exact
position or azimuth.

**Result:** the MVP now has a tactical map placeholder that can later receive
real position/witness geometry, but today it only visualizes roster/radio
freshness and keeps position/direction unknown.

## Phase 27: IFF BLE Expiry Policy

**Status:** completed

**Goal:** Make BLE field-radio freshness and lifecycle limits explicit so stale
radio evidence cannot look like current proof.

**Scope:**

- Add a shared radio freshness policy label:
  `fresh<=15s stale<=60s then UNKNOWN`.
- Show BLE lifecycle policy in contact, team, and map views.
- Mark witness source as `BLE_FIELD_RADIO` or `WIFI_SCAN_BEACON`.
- Show next witness transition timing in contact details.
- Log BLE stop reason and `fieldRadioPolicy`.
- Export `FieldRadioPolicy` through the field-log analyzer.

**Verification:** Android BLE/foreground-service docs were checked. Analyzer
smoke test passed. Debug APK built successfully outside sandbox and installed on
OnePlus `e089985a`. UIAutomator verified `Main -> IFF`; team UI showed
`BLE POLICY: VISIBLE_SCREEN_ONLY / fresh<=15s stale<=60s then UNKNOWN`, contact
UI showed `FIELD RADIO POLICY`, and `ЗАПИСАТЬ` logged
`fieldRadioPolicy="VISIBLE_SCREEN_ONLY / fresh<=15s stale<=60s then UNKNOWN"`.

**Result:** BLE remains visible-screen-only for now, but the UI and logs now
state the fresh/stale/unknown policy directly. The next slice can implement a
real foreground service lifecycle without changing the evidence model.

## Phase 28: IFF BLE Foreground Service

**Status:** completed

**Goal:** Move IFF BLE radio ownership from the visible IFF activity into an
Android foreground service.

**Scope:**

- Add `IffForegroundRadioService`.
- Declare it with `android:foregroundServiceType="connectedDevice"`.
- Start it from `IffActivity` using the current `THIS DEVICE` roster identity.
- Run BLE scan/advertise from the foreground service.
- Stop direct BLE shutdown from `IffActivity.onPause`.
- Show foreground service state in the IFF UI.
- Log foreground service start/stop and field-radio policy.

**Verification:** Android foreground-service docs were checked. Analyzer smoke
test passed. Debug APK built successfully and installed on OnePlus `e089985a`.
UIAutomator verified `Main -> IFF`; the team screen showed
`RADIO SERVICE: iff radio service on local=local-you foreground connectedDevice`
and `BLE POLICY: FOREGROUND_SERVICE_CONNECTED_DEVICE / fresh<=15s stale<=60s then UNKNOWN`.
After pressing Home, `dumpsys activity services net.afterday.compas` showed
`IffForegroundRadioService isForeground=true` with foreground type `0x00000010`.
The diagnostic log recorded `iff_radio_service_start` and a field check with
foreground-service `fieldRadioPolicy`.

**Result:** the BLE path now has a foreground-service lifecycle skeleton. Full
field proof still needs a two-phone screen-off/background BLE test.

## Phase 29: IFF Radio Service Control

**Status:** completed

**Goal:** Add explicit in-app operator control for starting and stopping the
IFF BLE foreground radio.

**Scope:**

- Add a `RADIO ON/OFF` action button to the IFF screen.
- Persist the radio enabled preference.
- Start the foreground BLE service on resume only when radio is enabled.
- Stop the service and BLE scan/advertise when the operator disables radio.
- Show `RADIO CONTROL: ON/OFF` in IFF status.
- Log `iff_radio_operator_toggle`.
- Add `fieldRadioEnabled` to field-check diagnostics and analyzer output.

**Verification:** analyzer smoke test passed. Debug APK built successfully and
installed on OnePlus `e089985a`. UIAutomator verified `Main -> IFF` and the new
`RADIO ON` button. Tapping it changed the UI to `RADIO OFF`, stopped BLE
scan/advertise, and removed `IffForegroundRadioService` from `dumpsys`. Tapping
again restarted the foreground service as `isForeground=true` with foreground
type `0x00000010`. The diagnostic log recorded operator toggles and a field
check with `fieldRadioEnabled=true`.

**Result:** field operators can now disable/re-enable IFF BLE radio from the
IFF screen without force-stopping the app.

## Phase 30: IFF BLE Screen-Off Smoke

**Status:** completed with two-phone blocker

**Goal:** Verify the first screen-off prerequisite for foreground BLE without
claiming two-phone RX.

**Scope:**

- Install the current APK on Samsung and OnePlus.
- Confirm OnePlus IFF radio is `RADIO ON`.
- Send OnePlus through `Home` and `SLEEP`.
- Check foreground service survival with `dumpsys`.
- Attempt to prepare Samsung and record blocker if locked.

**Verification:** APK installed on Samsung `R3CT20C8A8N` and OnePlus
`e089985a`. OnePlus showed `RADIO CONTROL: ON` and foreground connected-device
radio service in IFF. After `Home` and `SLEEP`, `dumpsys activity services
net.afterday.compas` still showed `IffForegroundRadioService isForeground=true`
with notification channel `compass_iff_radio` and foreground type `0x00000010`.
Samsung remained on lock/AOD/keyguard after ADB wake/swipe attempts, so IFF
could not be opened there without manual unlock.

**Result:** the foreground BLE service survives OnePlus screen-off. Cross-phone
BLE RX through screen-off/background remains pending until Samsung is unlocked
and controllable.

## Phase 31: IFF BLE Two-Phone Screen-Off

**Status:** completed

**Goal:** Verify the no-infrastructure BLE IFF path on two physical phones with
both screens off.

**Scope:**

- Use Samsung `R3CT20C8A8N` as `THIS DEVICE: Петя`.
- Use OnePlus `e089985a` as `THIS DEVICE: Вы`.
- Keep both phones on `RADIO ON`.
- Verify BLE witness RX in both directions while screens are on.
- Record `field_check` on both phones.
- Turn both screens off and verify foreground BLE RX remains visible in
  diagnostics about 30 seconds later.

**Verification:** Samsung saw `BLE_IFF_YOU` from OnePlus with screen-on RSSI
around `-39 dBm`; OnePlus saw `BLE_IFF_PETYA` from Samsung with screen-on RSSI
around `-46 dBm`. Field checks recorded `RADIO_FRESH` on both phones:
Samsung `field-radio-20260520-155212.log` recorded `playerId=local-you`
from local device `petya`, and OnePlus `field-radio-20260520-155213.log`
recorded `playerId=petya` from local device `local-you`. After both screens
were turned off, diagnostics still logged `ble_field_radio_rx` on both devices
about 30 seconds later.

**Result:** two-phone BLE IFF RX is verified without a shared Wi-Fi network,
including a short screen-off foreground-service pass. This remains fresh radio
presence only: identity is still a roster claim without crypto, and position
and direction remain `UNKNOWN`.

## Phase 32: IFF Witness Transition Logging

**Status:** completed

**Goal:** Make BLE expiry tests reliable by logging witness freshness
transitions automatically instead of depending on a manual tap during the short
stale window.

**Scope:**

- Track per-player witness freshness transitions:
  `NONE`, `RADIO_FRESH`, `RADIO_STALE`, and `UNKNOWN`.
- Log `IFF_DIAG event=witness_freshness_transition` with player id, previous
  state, next state, reason, source type, age, RSSI, SSID/BSSID, and policy.
- Run the transition logger from the IFF foreground radio service.
- Run the transition logger from the visible IFF screen refresh loop.
- Keep the evidence model and thresholds unchanged.

**Verification:** analyzer smoke test passed. Debug APK built successfully and
installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`. OnePlus runtime
smoke verified `Main -> IFF` opens after installation and shows foreground
radio service state. A pre-change manual expiry run recorded fresh evidence for
`Петя` at `16:06:06` and `UNKNOWN` at `16:09:28` with age `151041 ms`, proving
expired BLE does not remain current evidence, but the manual run missed the
stale interval.

**Result:** the next two-phone screen-off run can verify
`RADIO_FRESH -> RADIO_STALE -> UNKNOWN` from diagnostics without timing manual
`ЗАПИСАТЬ` taps.

## Phase 33: IFF BLE Expiry Transition Verification

**Status:** completed

**Goal:** Verify the automatic BLE witness transition diagnostics with two
physical phones.

**Scope:**

- Use Samsung `R3CT20C8A8N` as `THIS DEVICE: Петя`.
- Use OnePlus `e089985a` as `THIS DEVICE: Вы`.
- Confirm fresh BLE witness in both directions while screens are on.
- Stop Samsung to simulate the transmitter disappearing.
- Put OnePlus screen off during the expiry window.
- Verify OnePlus diagnostics record `RADIO_FRESH -> RADIO_STALE -> UNKNOWN`.

**Verification:** OnePlus diagnostics
`field-radio-20260520-161828.log` recorded automatic transition events for
`petya`: `NONE -> RADIO_FRESH` at `16:18:46.377` with age `53 ms`,
`RADIO_FRESH -> RADIO_STALE` at `16:19:32.682` with age `15540 ms`, and
`RADIO_STALE -> UNKNOWN` at `16:20:17.565` with age `60423 ms`. Both phones
were force-stopped after verification.

**Result:** the BLE IFF evidence path now has field-verified automatic expiry
diagnostics. Expired BLE does not remain current proximity proof.

## Phase 34: IFF Local Trust Layer

**Status:** completed

**Goal:** Add a minimal local trust state so the IFF UI separates an ordinary
roster claim from a locally trusted teammate claim without pretending this is
cryptographic identity.

**Scope:**

- Add a persisted per-player local trust flag.
- Add `TRUST` / `UNTRUST` to the IFF action row.
- Show trust state in `КОМАНДА` rows and `КОНТАКТ` details.
- Let trust affect only the identity layer:
  `LOCAL_TRUSTED_ROSTER` and `LOCAL_TRUSTED_RADIO_CLAIM`.
- Keep proximity, position, and direction independent.
- Add `trustedPlayer` and `trustLabel` to field-check diagnostics and analyzer
  output.
- Compact the team status block so roster selection remains usable in
  landscape.

**Verification:** analyzer smoke test passed. Debug APK built successfully
outside sandbox and installed on OnePlus `e089985a`. UIAutomator verified
`Main -> IFF -> КОМАНДА -> Петя -> КОНТАКТ -> TRUST -> ЗАПИСАТЬ`.
`field-radio-20260520-163259.log` recorded
`trustLabel=LOCAL_TRUSTED`, `trustedPlayer=true`,
`identityLabel=LOCAL_TRUSTED_ROSTER`, and `proximityLabel=UNKNOWN`.
Analyzer output
`artifacts/field-analysis-phase34-trust-verify/iff-field-checks.csv` exported
the same trust fields.

**Result:** the MVP now has a first non-crypto local trust layer. Trust can
increase identity confidence, but it does not prove radio proximity, GPS
position, or direction.

## Backlog

- Analyze customer Wi-Fi module behavior after the module is available.
- Add a foreground/background strategy for field BLE operation after the visible
  IFF-screen skeleton is stable.
