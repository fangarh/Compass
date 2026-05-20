# Phase 32: IFF Witness Transition Logging

**Status:** completed

## Goal

Make BLE witness expiry field tests reliable by logging freshness state
transitions automatically instead of depending on a manual `ЗАПИСАТЬ` tap during
the short stale window.

## Scope

- Add a diagnostic transition event for local radio witnesses.
- Track transitions per roster player:
  - `NONE -> RADIO_FRESH`
  - `RADIO_FRESH -> RADIO_STALE`
  - `RADIO_STALE -> UNKNOWN`
  - `UNKNOWN -> RADIO_FRESH`
- Emit the transition logger from the foreground radio service while the IFF
  radio is running.
- Emit the same transition logger from the visible IFF screen refresh loop.
- Keep the evidence model unchanged.

## Out Of Scope

- Changing RSSI thresholds.
- Changing the fresh/stale/unknown policy.
- Changing identity, proximity, position, or direction scoring.
- Cryptographic identity proof.
- GPS or bearing logic.
- UDP transport changes.
- Samsung-specific behavior.

## Verification

- Analyzer smoke test passed:
  `powershell -ExecutionPolicy Bypass -File scripts\test-analyze-field-logs.ps1`.
- Debug APK built successfully with `:app:assembleDebug`.
- APK installed successfully on:
  - Samsung `R3CT20C8A8N`;
  - OnePlus `e089985a`.
- OnePlus runtime smoke passed:
  `Main -> IFF` opened after installation, foreground radio service started,
  and the IFF team screen showed `RADIO SERVICE` and `FIELD RADIO` status.

## Field Note

Before this code change, a manual expiry run recorded:

- `field-radio-20260520-160443.log` at `16:06:06`:
  `field_check playerId=petya`, `witness=RADIO_FRESH`, RSSI `-54`,
  age `92 ms`.
- The last BLE RX from Samsung was at `16:06:57`.
- A later manual field check at `16:09:28` recorded `witness=UNKNOWN`,
  RSSI `-53`, age `151041 ms`.

This proves expired BLE does not remain current evidence, but the manual test
missed the `RADIO_STALE` interval. The new `witness_freshness_transition`
event is specifically for catching that interval in the next screen-off field
run.

## Result

The app now logs automatic IFF witness freshness transitions with player id,
previous state, next state, source type, age, RSSI, SSID/BSSID, and policy. The
next two-phone run can verify `RADIO_FRESH -> RADIO_STALE -> UNKNOWN` from
diagnostics without timing manual taps.
