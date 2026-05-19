# Phase 20: IFF Remote Witness Expiry Fixture

## Goal

Make remote witness freshness and expiry explicitly testable without waiting for
real time or adding network transport.

## Scope

- Split the local fixture into `SIM FRESH` and `SIM STALE` actions.
- Let `SIM FRESH` inject fresh synthetic remote witness reports for the selected
  contact.
- Let `SIM STALE` replace those reports with stale synthetic reports for the
  same contact.
- Show the transition from `MULTI_WITNESS 2/3` to
  `STALE_REMOTE_WITNESS 0/3`.
- Log `remoteStaleSources` in `IFF_DIAG event=field_check`.
- Extend analyzer outputs and Markdown summaries to show fresh and stale remote
  report counts separately.

## Out Of Scope

- Real remote transport.
- Cryptographic signatures.
- GPS position confidence.
- Direction/azimuth inference.
- Wi-Fi threshold changes.
- Phone-specific calibration.

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/java/net/afterday/compas/iff/IffRemoteWitnessStore.java`
- `app/src/main/java/net/afterday/compas/iff/IffWitnessQuorum.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`
- `scripts/analyze-field-logs.ps1`

## Verification Criteria

- Debug APK builds.
- Analyzer smoke test passes.
- APK installs on a connected phone.
- UI flow works:
  `Main -> IFF -> КОМАНДА -> Петя -> SIM FRESH -> SIM STALE -> ЗАПИСАТЬ`.
- `SIM FRESH` shows `MULTI_WITNESS 2/3`.
- `SIM STALE` shows `STALE_REMOTE_WITNESS 0/3` with `REMOTE_STALE` reports.
- Field-check log includes `remoteReportCount=2`, `remoteFreshSources=0`, and
  `remoteStaleSources=2` after stale simulation.
- Analyzer summary shows `2 reports / 0 fresh / 2 stale`.

## Verification Result

Completed on 2026-05-19.

- `:app:assembleDebug` passed.
- `scripts/test-analyze-field-logs.ps1` passed.
- APK installed on OnePlus `e089985a`.
- UIAutomator/ADB verified:
  `Main -> IFF -> КОМАНДА -> Петя -> SIM FRESH -> SIM STALE -> ЗАПИСАТЬ`.
- UI showed:
  - `SIM FRESH`: `WITNESSES: MULTI_WITNESS 2/3`;
  - `SIM STALE`: `WITNESSES: STALE_REMOTE_WITNESS 0/3`;
  - two `REMOTE_STALE` reports with `SIGNATURE_PENDING`.
- Diagnostic log `field-radio-20260519-173501.log` recorded:
  `witnessQuorum=STALE_REMOTE_WITNESS remoteReportCount=2 remoteFreshSources=0 remoteStaleSources=2`.
- Analyzer completed on `artifacts/iff-field-session-20260519-1735` with
  743 scan entries and reported `2 reports / 0 fresh / 2 stale`.

## Decision

Expired remote reports remain visible as stale evidence, but they do not count
as current witness proof and must not raise identity, proximity, position, or
direction confidence.
