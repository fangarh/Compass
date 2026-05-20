# Phase 34: IFF Local Trust Layer

## Goal

Add a minimal local trust state for IFF roster participants so the operator can
distinguish a plain roster claim from a locally trusted teammate claim.

## Scope

- Add a persisted per-player local trust flag.
- Add a `TRUST` / `UNTRUST` action on the IFF contact screen.
- Show trust state in `КОМАНДА` roster rows and `КОНТАКТ` details.
- Let local trust raise only the identity layer:
  - `ROSTER_ONLY 40%`
  - `LOCAL_TRUSTED_ROSTER 55%`
  - `LOCAL_TRUSTED_RADIO_CLAIM 75%`
- Keep proximity, position, and direction independent.
- Add trust fields to `IFF_DIAG event=field_check`.
- Extend the field-log analyzer with trust columns and Markdown output.
- Keep the team screen usable in landscape by compacting the team status block.

## Out Of Scope

- No cryptography.
- No network pairing protocol.
- No GPS calibration.
- No Wi-Fi calibration.
- No Samsung-specific logic.
- No change to BLE freshness or expiry thresholds.
- Do not touch `test.png`.

## Files

- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/java/net/afterday/compas/iff/IffConfidence.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`
- `scripts/analyze-field-logs.ps1`

## Verification Criteria

- Analyzer smoke test passes.
- Debug APK builds.
- APK installs on a connected phone.
- UI flow passes:
  `Main -> IFF -> КОМАНДА -> Петя -> КОНТАКТ -> TRUST -> ЗАПИСАТЬ`.
- Diagnostic log records:
  - `trustedPlayer=true`
  - `trustLabel=LOCAL_TRUSTED`
  - `identityLabel=LOCAL_TRUSTED_ROSTER`
  - `proximityLabel=UNKNOWN`
- Analyzer exports the trust fields from the pulled field log.

## Verification Result

Completed on 2026-05-20 with OnePlus `e089985a`.

- `scripts/test-analyze-field-logs.ps1` passed.
- `:app:assembleDebug` passed outside sandbox.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified:
  `Main -> IFF -> КОМАНДА -> Петя -> КОНТАКТ -> TRUST -> ЗАПИСАТЬ`.
- Team screen status was compacted after verification showed the old status
  block left too little roster space in landscape.
- Diagnostic log `field-radio-20260520-163259.log` recorded:
  `event=iff_trust_toggle playerId=petya trustedPlayer=true
  trustLabel=LOCAL_TRUSTED`.
- The same log recorded field check:
  `playerId=petya trustedPlayer=true trustLabel=LOCAL_TRUSTED
  identityLabel=LOCAL_TRUSTED_ROSTER identityScore=55
  proximityLabel=UNKNOWN proximityScore=0 positionLabel=UNKNOWN
  directionLabel=UNKNOWN`.
- Analyzer output
  `artifacts/field-analysis-phase34-trust-verify/iff-field-checks.csv`
  includes `TrustLabel=LOCAL_TRUSTED` and `TrustedPlayer=true`.

## Result

The MVP now has a first non-crypto trust layer. A locally trusted roster entry
can improve identity confidence, but without fresh radio it still has no
proximity proof, and position/direction remain unknown.
