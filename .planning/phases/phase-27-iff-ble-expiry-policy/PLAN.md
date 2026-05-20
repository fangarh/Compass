# Phase 27: IFF BLE Expiry Policy

**Status:** completed

## Goal

Make BLE field-radio freshness and lifecycle limits explicit in the IFF UI and
diagnostic logs so stale radio evidence cannot look like current proof.

## Scope

- Add a shared radio freshness policy label:
  `fresh<=15s stale<=60s then UNKNOWN`.
- Mark witness source type as `BLE_FIELD_RADIO` or `WIFI_SCAN_BEACON`.
- Show the next witness transition window on the contact screen.
- Show BLE lifecycle policy in team, contact, and map views.
- Log BLE stop reason and lifecycle policy.
- Add `fieldRadioPolicy` to `IFF_DIAG event=field_check`.
- Export `FieldRadioPolicy` through the field-log analyzer.

## Out Of Scope

- Foreground service implementation.
- Background BLE scanning or advertising.
- Cryptographic identity proof.
- GPS position or direction inference.
- Samsung-specific or OnePlus-specific radio logic.
- Changing BLE RSSI thresholds.

## Verification

- Official Android documentation was checked for BLE scan/advertise permissions
  and Android 14 foreground service type requirements.
- `scripts/test-analyze-field-logs.ps1` passed.
- `:app:assembleDebug` completed successfully outside sandbox.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified `Main -> IFF`.
- Team view showed:
  `BLE POLICY: VISIBLE_SCREEN_ONLY / fresh<=15s stale<=60s then UNKNOWN`.
- Contact view for `Петя` showed `FIELD RADIO POLICY`, visible-screen
  lifecycle, stale handling, and expiry-to-UNKNOWN wording.
- `ЗАПИСАТЬ` produced a diagnostic row with
  `fieldRadioPolicy="VISIBLE_SCREEN_ONLY / fresh<=15s stale<=60s then UNKNOWN"`.

## Result

BLE remains a visible-screen skeleton, but the operator and logs now state the
fresh/stale/unknown policy directly. This keeps the MVP honest until a real
foreground service lifecycle is implemented.
