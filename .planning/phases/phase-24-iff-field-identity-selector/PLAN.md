# Phase 24: IFF Field Identity Selector

**Status:** completed

## Goal

Remove the hidden assumption that every installed phone is `local-you`.

Field IFF needs each physical device to declare which roster participant it is
before BLE or any other no-infrastructure radio beacon can be meaningful.

## Scope

- Add a per-device local identity setting on the IFF screen.
- Persist the selected roster player in `SharedPreferences`.
- Show `THIS DEVICE: ...` in the team summary.
- Mark the selected roster row with `[THIS DEVICE]`.
- On a non-local contact, use the left action button as `ЭТОТ ТЕЛ.` to assign
  that contact as this physical device.
- Keep `Я ПОДХОЖУ` for the currently assigned local participant.
- Log selected local identity in `IFF_DIAG event=field_check`.
- Export local identity fields from the analyzer.
- Add `BLUETOOTH_ADVERTISE` permission and startup request for the next BLE
  beacon slice.

## Out Of Scope

- BLE advertising/scanning implementation.
- Cryptographic identity proof.
- GPS or Wi-Fi calibration.
- Samsung-specific or OnePlus-specific identity defaults.
- Using a common Wi-Fi network as a field requirement.

## Verification

- `:app:assembleDebug` completed successfully.
- `scripts/test-analyze-field-logs.ps1` passed.
- APK installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- Samsung UI verified:
  - `Main -> IFF -> КОМАНДА`;
  - roster appears before explanatory text;
  - selecting `Петя` opens contact;
  - action button changes to `ЭТОТ ТЕЛ.`;
  - tapping it changes the device identity to `Петя`;
  - team screen shows `THIS DEVICE: Петя`;
  - roster row shows `Петя [THIS DEVICE]`;
  - contact identity changes to `LOCAL_SELF 70%`.

## Result

The app now has the required device-level identity state for a field radio
beacon. Next BLE work can advertise the chosen roster participant instead of a
hardcoded local player.
