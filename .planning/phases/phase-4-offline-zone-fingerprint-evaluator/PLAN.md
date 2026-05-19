# Phase 4 Plan: Offline Zone Fingerprint Evaluator

## Goal

Test whether field logs can identify a phone's named test zone from Wi-Fi scan
fingerprints without changing runtime gameplay.

## Scope

- Build per-device/per-window fingerprints from `scan-summary.csv` data.
- Compare each 30-second bucket against fingerprints for the same device.
- Score candidates by shared BSSID overlap and RSSI distance.
- Output `zone-fingerprints.csv` and `zone-evaluation.csv`.
- Add a compact evaluator summary to `summary.md`.

## Verification

- Run analyzer on the 2026-05-19 10:45 controlled run with named windows.
- Confirm evaluator output files are generated.
- Inspect bucket predictions for `cabinet`, `corridor`, `cabinet_return`, and
  `near_30cm`.

## Out Of Scope

- No Android app code changes.
- No runtime detection algorithm changes.
- No gameplay integration.
