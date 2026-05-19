# Phase 19: IFF Remote Witness Simulation

## Goal

Validate multi-witness quorum behavior before real network transport by adding a
local-only fixture that injects synthetic remote witness reports for the
selected IFF contact.

## Scope

- Add a `SIM WITNESS` action on the IFF screen.
- Generate synthetic `iff-remote-witness-v1` reports for the selected roster
  participant from two other roster sources.
- Keep signatures as `SIGNATURE_PENDING`.
- Feed simulated reports through the same `IffRemoteWitnessStore` and
  `IffWitnessQuorum` path as future real reports.
- Show `MULTI_WITNESS` only while enough reports are fresh.
- Log `remote_witness_simulated` and include remote witness counts in
  `IFF_DIAG event=field_check`.
- Keep confidence layers honest: simulation may change witness quorum, but it
  must not upgrade identity, proximity, position, or direction by itself.

## Out Of Scope

- Real network exchange.
- Cryptographic signing or verification.
- GPS calibration.
- Wi-Fi calibration for specific phone models.
- Samsung-specific logic.
- Complex UI redesign.

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/java/net/afterday/compas/iff/IffWitnessQuorum.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`

## Verification Criteria

- Debug APK builds with `:app:assembleDebug`.
- APK installs on the connected phone when one is available.
- UI flow works:
  `Main -> IFF -> КОМАНДА -> select participant -> SIM WITNESS -> КОНТАКТ`.
- Contact tab shows `WITNESSES: MULTI_WITNESS 2/3` while reports are fresh.
- Contact tab still shows identity as roster/radio claim only, with no crypto
  upgrade.
- `ЗАПИСАТЬ` logs `witnessQuorum=MULTI_WITNESS`,
  `remoteReportCount=2`, and `remoteFreshSources=2`.
- Field-log analyzer exports those remote witness fields.

## Verification Result

Completed on 2026-05-19.

- `:app:assembleDebug` passed.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified:
  `Main -> IFF -> КОМАНДА -> Петя -> SIM WITNESS -> КОНТАКТ`.
- Contact screen showed:
  - `WITNESSES: MULTI_WITNESS 2/3`;
  - two fresh remote reports;
  - `SIGNATURE_PENDING`;
  - identity not upgraded by quorum without crypto.
- `ЗАПИСАТЬ` logged:
  `witnessQuorum=MULTI_WITNESS witnessFreshSources=2 witnessPossibleSources=3 remoteReportCount=2 remoteFreshSources=2`.
- `scripts/test-analyze-field-logs.ps1` passed.
- Analyzer completed on `artifacts/iff-field-session-20260519-1716` and
  produced an IFF row for `Петя` with `MULTI_WITNESS 2/3` and
  `2 reports / 2 fresh`.

## Decision

The fixture validates quorum UI, logging, and analysis only. It is not evidence
of real transport, cryptographic identity, direction, or precise position.
