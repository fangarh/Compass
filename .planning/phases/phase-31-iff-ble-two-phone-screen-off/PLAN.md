# Phase 31: IFF BLE Two-Phone Screen-Off

**Status:** completed

## Goal

Verify the no-infrastructure IFF BLE path with two physical phones, including a
screen-off foreground-service pass on both devices.

## Scope

- Use Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- Keep both phones on `RADIO ON`.
- Set Samsung to `THIS DEVICE: Петя`.
- Keep OnePlus as `THIS DEVICE: Вы`.
- Verify fresh BLE witness RX in both directions while screens are on.
- Record `field_check` on both phones.
- Turn both screens off and verify BLE RX still appears in diagnostics after
  about 30 seconds.

## Out Of Scope

- Common Wi-Fi assumptions.
- UDP transport changes.
- Cryptographic identity proof.
- GPS position, bearing, or direction inference.
- Samsung-specific runtime logic.
- UI redesign.

## Verification

- Git worktree was clean except untracked `test.png`.
- ADB saw Samsung `R3CT20C8A8N`, OnePlus `e089985a`, and emulator.
- Both phones were already installed, unlocked, and opened on the IFF team
  screen.
- Initial blocker was corrected: both phones were `THIS DEVICE: Вы`, causing
  BLE RX to be ignored as self. Samsung was changed to `THIS DEVICE: Петя`.
- Screen-on BLE verification:
  - Samsung team screen showed `THIS DEVICE: Петя`,
    `OPERATOR: SINGLE CURRENT`, and
    `FIELD RADIO: ble adv=on scan=on rx=49 rejected=0 local=petya rx local-you -39dBm`.
  - OnePlus team screen showed `THIS DEVICE: Вы`,
    `OPERATOR: SINGLE CURRENT`, and
    `FIELD RADIO: ble adv=on scan=on rx=86 rejected=0 local=local-you rx petya -46dBm`.
- Field checks were recorded:
  - Samsung `field-radio-20260520-155212.log` recorded
    `event=field_check playerId=local-you localDevicePlayerId=petya`
    with `witness=RADIO_FRESH`, RSSI `-43`, and
    `ssid="BLE_IFF_YOU"`.
  - OnePlus `field-radio-20260520-155213.log` recorded
    `event=field_check playerId=petya localDevicePlayerId=local-you`
    with `witness=RADIO_FRESH`, RSSI `-49`, and
    `ssid="BLE_IFF_PETYA"`.
- Screen-off BLE verification:
  - Both phones were sent to sleep with ADB `input keyevent 26`.
  - About 30 seconds later, Samsung diagnostics still logged
    `ble_field_radio_rx playerId=local-you localPlayerId=petya` with RSSI
    around `-33..-41 dBm`.
  - About 30 seconds later, OnePlus diagnostics still logged
    `ble_field_radio_rx playerId=petya localPlayerId=local-you` with RSSI
    around `-42..-43 dBm`.

## Result

Two-phone BLE IFF RX is verified without a shared Wi-Fi network, including a
short screen-off foreground-service pass on both devices. This proves fresh
radio presence only: identity remains a roster claim without crypto, position
and direction remain `UNKNOWN`, and RSSI still must not be treated as azimuth.
