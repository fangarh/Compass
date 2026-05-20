# Phase 28: IFF BLE Foreground Service

**Status:** completed

## Goal

Move IFF BLE radio ownership from the visible `IffActivity` lifecycle into an
Android foreground service so the field-radio path can survive leaving the IFF
screen and can later be tested with screen-off/background conditions.

## Scope

- Add `IffForegroundRadioService`.
- Declare the service with `android:foregroundServiceType="connectedDevice"`.
- Start the service from `IffActivity` with the current `THIS DEVICE` roster
  identity.
- Start BLE scan/advertise from the foreground service.
- Stop direct BLE shutdown from `IffActivity.onPause`.
- Show foreground service state in IFF team/map/contact UI.
- Log service start/stop and foreground lifecycle policy.

## Out Of Scope

- Two-phone screen-off/background BLE RX verification.
- Cryptographic identity proof.
- GPS position or direction inference.
- RSSI threshold changes.
- Samsung-specific or OnePlus-specific BLE logic.
- Replacing the UDP debug transport.

## Verification

- Official Android docs were checked for `connectedDevice` foreground service
  type requirements.
- `scripts/test-analyze-field-logs.ps1` passed.
- `:app:assembleDebug` completed successfully.
- APK installed on OnePlus `e089985a`.
- UIAutomator verified `Main -> IFF`.
- Team UI showed:
  - `RADIO SERVICE: iff radio service on local=local-you foreground connectedDevice`;
  - `BLE POLICY: FOREGROUND_SERVICE_CONNECTED_DEVICE / fresh<=15s stale<=60s then UNKNOWN`.
- After pressing Home, `dumpsys activity services net.afterday.compas` showed
  `IffForegroundRadioService` still running as `isForeground=true` with
  foreground type `0x00000010`.
- Diagnostic log `field-radio-20260520-151429.log` recorded
  `iff_radio_service_start`, BLE scan/advertise start, and `field_check` with
  `fieldRadioPolicy="FOREGROUND_SERVICE_CONNECTED_DEVICE / fresh<=15s stale<=60s then UNKNOWN"`.

## Result

IFF BLE radio now has a foreground-service owner. This validates the app-side
lifecycle skeleton on one phone. The next field check should use two phones to
prove whether BLE advertisements and scans continue across screen-off and app
background transitions.
