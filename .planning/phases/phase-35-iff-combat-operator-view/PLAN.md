# Phase 35: IFF Combat Operator View

## Goal

Make the IFF screen faster to read in field play by surfacing a separate combat
state and operator action before the detailed confidence layers.

## Scope

- Add a derived combat state for the selected contact:
  - `CURRENT_MULTI`
  - `CURRENT_SINGLE`
  - `STALE`
  - `UNKNOWN`
  - `LOCAL_DECLARED`
- Add a derived combat action:
  - `TRACK_CURRENT_CONTACT`
  - `WATCH_CURRENT_CONTACT`
  - `DO_NOT_TREAT_AS_NEAR`
  - `NO_CURRENT_CONTACT`
  - `LOCAL_STATUS_ONLY`
- Show combat state/action at the top of the contact status.
- Add a `БОЕВОЙ ВИД` block to contact details.
- Show combat counts on the team tab:
  `current / stale / unknown`.
- Prefix roster rows with the combat state and color current/stale rows.
- Log `combatState` and `combatAction` in `IFF_DIAG event=field_check`.
- Export combat fields in the field-log analyzer.

## Out Of Scope

- No new radio transport.
- No change to BLE freshness thresholds.
- No cryptography or pairing.
- No GPS/direction logic.
- No Samsung-specific behavior.
- Do not touch `test.png`.

## Verification Criteria

- Analyzer smoke test passes.
- Debug APK builds.
- APK installs on a connected phone.
- UI shows `COMBAT` state on contact and team screens.
- Debug stale witness simulation produces `COMBAT: STALE`.
- Field diagnostics include `combatState` and `combatAction`.
- Analyzer exports those fields.

## Verification Result

Completed on 2026-05-20 with OnePlus `e089985a`.

- `scripts/test-analyze-field-logs.ps1` passed.
- `:app:assembleDebug` passed outside sandbox.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified `Main -> IFF -> КОМАНДА -> Петя -> КОНТАКТ`.
- The contact screen showed:
  `COMBAT: UNKNOWN / NO_CURRENT_CONTACT` without radio evidence.
- After `SIM STALE`, the contact screen showed:
  `COMBAT: STALE / DO_NOT_TREAT_AS_NEAR`.
- Diagnostic log `field-radio-20260520-170038.log` recorded:
  `combatState=STALE combatAction=DO_NOT_TREAT_AS_NEAR
  operatorVerdict=STALE_EVIDENCE_ONLY proximityLabel=UNKNOWN`.
- Analyzer output
  `artifacts/field-analysis-phase35-combat-verify/iff-field-checks.csv`
  exported the same combat fields.

## Result

The IFF MVP now has an operator-facing combat state that is distinct from
identity, proximity, position, and direction. Stale radio evidence remains
visible, but the recommended action explicitly says not to treat it as current
near contact.
