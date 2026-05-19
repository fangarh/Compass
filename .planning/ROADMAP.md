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

**Status:** in progress

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

## Backlog

- Phase 10: local roster and trusted player identity model.
- Phase 11: phone-to-phone approach beacon/witness exchange.
- Phase 12: confidence model for identity, proximity, position, and direction.
- Phase 13: IFF field MVP test with two or more teammates.
- Analyze customer Wi-Fi module behavior after the module is available.
- Keep BLE as a deferred architecture option, not a near-term implementation.
