# Phase 3 Plan: Named Analysis Windows

## Goal

Make field-log analysis accept named movement windows so reports use real test
position names instead of reused default labels.

## Scope

- Add a `-Windows` parameter to `scripts/analyze-field-logs.ps1`.
- Support window specs such as
  `cabinet=10:45:10..10:47:10;corridor=10:47:10..10:49:10`.
- Generate movement deltas between adjacent named windows and between first and
  last windows.
- Regenerate the 2026-05-19 10:45 run with named windows.
- Document the command and output path.

## Verification

- Analyzer runs successfully with named windows.
- `summary.md` lists `cabinet`, `corridor`, `cabinet_return`, and `near_30cm`.
- `movement-deltas.csv` contains candidate rows using named windows.

## Out Of Scope

- No Android app code changes.
- No detection algorithm changes.
- No new field run required.
