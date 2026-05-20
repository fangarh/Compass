# Phase 29: IFF Radio Service Control

**Status:** completed

## Goal

Add an explicit in-app operator control for the IFF foreground radio service so
field users can stop and restart BLE radio without force-stopping the app or
using only the notification action.

## Scope

- Add a `RADIO ON/OFF` action button to the IFF screen.
- Persist the operator radio preference in `SharedPreferences`.
- Start the foreground BLE service on resume only when radio is enabled.
- Stop the IFF foreground radio service and BLE radio when the operator turns
  radio off.
- Show `RADIO CONTROL: ON/OFF` in team and map status.
- Log `iff_radio_operator_toggle`.
- Add `fieldRadioEnabled` to `IFF_DIAG event=field_check`.
- Export `FieldRadioEnabled` through the analyzer.

## Out Of Scope

- Two-phone BLE range verification.
- Screen-off/background RX proof.
- Cryptographic identity proof.
- GPS position or direction inference.
- Changing RSSI thresholds.

## Verification

- `scripts/test-analyze-field-logs.ps1` passed.
- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified `Main -> IFF`.
- Team UI showed the new `RADIO ON` button and `RADIO CONTROL: ON`.
- Tapping `RADIO ON` changed UI to `RADIO OFF`, stopped BLE scan/advertise,
  and removed `IffForegroundRadioService` from `dumpsys`.
- Tapping `RADIO OFF` restarted the foreground service; `dumpsys` showed
  `IffForegroundRadioService isForeground=true` with type `0x00000010`.
- Diagnostic log `field-radio-20260520-152933.log` recorded
  `iff_radio_operator_toggle enabled=false`, `enabled=true`, service start/stop
  events, and a `field_check` with `fieldRadioEnabled=true`.

## Result

Operators can now deliberately disable and re-enable the IFF BLE foreground
radio from inside the IFF screen. This makes the foreground service usable in
field tests without relying on force-stop or notification controls.
