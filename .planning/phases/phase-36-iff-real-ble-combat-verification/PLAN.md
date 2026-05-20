# Phase 36: IFF Real BLE Combat Verification

## Goal

Verify the Phase 35 combat operator view against real two-phone BLE lifecycle,
not simulated stale witnesses.

## Scope

- Install the current APK on Samsung and OnePlus.
- Use Samsung `R3CT20C8A8N` as `THIS DEVICE: Петя`.
- Use OnePlus `e089985a` as `THIS DEVICE: Вы`.
- Confirm real BLE current evidence in both directions.
- Select `Петя` on OnePlus and record `CURRENT_SINGLE`.
- Stop Samsung to simulate transmitter disappearance.
- Verify OnePlus combat UI moves to `STALE`.
- Verify OnePlus combat UI moves to `UNKNOWN` after expiry.
- Pull the OnePlus field log and run analyzer.

## Out Of Scope

- No code changes.
- No cryptography or pairing.
- No GPS/direction logic.
- No Wi-Fi calibration.
- Do not touch `test.png`.

## Verification Result

Completed on 2026-05-20 with physical phones:

- Samsung `R3CT20C8A8N`
- OnePlus `e089985a`

Setup:

- APK installed on both phones.
- Samsung was initially on AOD/lock; ADB wake/swipe successfully returned it to
  the app.
- Samsung opened IFF as `THIS DEVICE: Петя`.
- OnePlus opened IFF as `THIS DEVICE: Вы`.
- Both phones showed real BLE current evidence:
  - Samsung saw `local-you` around `-40 dBm`.
  - OnePlus saw `petya` around `-49 dBm`.

OnePlus selected `Петя` and recorded:

- `17:23:41.364`: `combatState=CURRENT_SINGLE`,
  `combatAction=WATCH_CURRENT_CONTACT`,
  `identityLabel=LOCAL_TRUSTED_RADIO_CLAIM`,
  `proximityLabel=RADIO_NEAR`,
  `witness=RADIO_FRESH`, `ageMs=79`.

Samsung was force-stopped. OnePlus then recorded:

- `17:24:02.524`: automatic transition
  `RADIO_FRESH -> RADIO_STALE`, `ageMs=15123`.
- `17:24:32.630`: `combatState=STALE`,
  `combatAction=DO_NOT_TREAT_AS_NEAR`,
  `identityLabel=LOCAL_TRUSTED_ROSTER`,
  `proximityLabel=STALE_RADIO`,
  `witness=RADIO_STALE`, `ageMs=45229`.
- `17:24:48.936`: automatic transition
  `RADIO_STALE -> UNKNOWN`, `ageMs=61534`.
- `17:25:27.482`: `combatState=UNKNOWN`,
  `combatAction=NO_CURRENT_CONTACT`,
  `identityLabel=LOCAL_TRUSTED_ROSTER`,
  `proximityLabel=UNKNOWN`,
  `witness=UNKNOWN`, `ageMs=100081`.

Artifacts:

- Pulled log:
  `artifacts/field-logs/e089985a/field-radio-20260520-172157.log`
- Analyzer output:
  `artifacts/field-analysis-phase36-real-combat-verify/iff-field-checks.csv`

## Result

The combat operator view is field-verified on real BLE. Current radio evidence
shows as `CURRENT_SINGLE`, stale evidence shows as `STALE` with action
`DO_NOT_TREAT_AS_NEAR`, and expired evidence returns to `UNKNOWN` with action
`NO_CURRENT_CONTACT`.
