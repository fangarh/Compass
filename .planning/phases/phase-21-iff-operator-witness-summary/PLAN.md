# Phase 21: IFF Operator Witness Summary

## Goal

Make the IFF screen answer the operator's immediate question faster: is there
current witness evidence, only stale evidence, or no current evidence.

## Scope

- Add a compact operator verdict on the contact screen.
- Add team-level current/stale witness evidence counters.
- Show roster entries with the operator verdict plus identity/proximity scores.
- Distinguish:
  - `CURRENT_MULTI_WITNESS`;
  - `CURRENT_SINGLE_WITNESS`;
  - `STALE_EVIDENCE_ONLY`;
  - `LOCAL_DECLARED_ONLY`;
  - `NO_CURRENT_EVIDENCE`.
- Keep identity, proximity, position, and direction layers separate.
- Log `operatorVerdict` in `IFF_DIAG event=field_check`.
- Extend analyzer CSV/Markdown output with operator verdict fields.

## Out Of Scope

- Real remote transport.
- Cryptographic identity.
- GPS position confidence.
- Direction/azimuth inference.
- Wi-Fi threshold changes.
- Two-phone radio conclusions.

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `scripts/analyze-field-logs.ps1`

## Verification Criteria

- Debug APK builds.
- Analyzer smoke test passes.
- APK installs on Samsung when visible through ADB.
- UI flow works:
  `Main -> IFF -> КОМАНДА -> Петя -> SIM FRESH -> SIM STALE -> ЗАПИСАТЬ`.
- `SIM FRESH` shows `OPERATOR: CURRENT_MULTI_WITNESS`.
- `SIM STALE` shows `OPERATOR: STALE_EVIDENCE_ONLY`.
- Field-check log includes `operatorVerdict=STALE_EVIDENCE_ONLY`.
- Analyzer CSV/Markdown includes the operator verdict.

## Verification Result

Completed on 2026-05-20.

- `:app:assembleDebug` passed.
- `scripts/test-analyze-field-logs.ps1` passed.
- APK installed on Samsung `R3CT20C8A8N`.
- UIAutomator/ADB verified:
  `Main -> IFF -> КОМАНДА -> Петя -> SIM FRESH -> SIM STALE -> ЗАПИСАТЬ`.
- Contact screen showed:
  - `OPERATOR: CURRENT_MULTI_WITNESS` after `SIM FRESH`;
  - `OPERATOR: STALE_EVIDENCE_ONLY` after `SIM STALE`;
  - identity remained `ROSTER_ONLY 40%`;
  - proximity/position/direction remained `UNKNOWN 0%`.
- Diagnostic log `field-radio-20260520-091403.log` recorded:
  `operatorVerdict=STALE_EVIDENCE_ONLY witnessQuorum=STALE_REMOTE_WITNESS remoteReportCount=2 remoteFreshSources=0 remoteStaleSources=2`.
- Analyzer completed on `artifacts/iff-field-session-20260520-0914` with
  1893 scan entries and reported `STALE_EVIDENCE_ONLY` in CSV/Markdown.

## Decision

Operator verdict is a display and logging summary over existing evidence. It
does not replace the independent confidence layers and does not promote stale
or unsigned evidence into identity, position, or direction proof.
