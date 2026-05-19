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

## Backlog

- Phase 5: Cross-Validated Zone Evaluator.
- Analyze customer Wi-Fi module behavior after the module is available.
- Tune long-range Wi-Fi detection intervals from field evidence.
- Keep BLE as a deferred architecture option, not a near-term implementation.
- Preserve unified object identity across possible future radio identifiers.
