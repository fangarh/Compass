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

## Backlog

- Analyze customer Wi-Fi module behavior after the module is available.
- Tune long-range Wi-Fi detection intervals from field evidence.
- Keep BLE as a deferred architecture option, not a near-term implementation.
- Preserve unified object identity across possible future radio identifiers.
