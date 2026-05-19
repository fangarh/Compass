# Phase 11 Plan: IFF Radio Witness

## Goal

Add the first radio proof layer for IFF:

```text
заявленный свой -> слышим его beacon сейчас? -> насколько свежо/сильно?
```

This phase uses Wi-Fi scan results as a witness signal. It does not implement a
cryptographic identity proof, exact direction, GPS calibration, or automatic
hotspot creation.

## Scope

- Detect known IFF beacon SSIDs in Android `ScanResult` lists:
  - `COMPASS_IFF_YOU`
  - `COMPASS_IFF_PETYA`
  - `COMPASS_IFF_VASYA`
  - `COMPASS_IFF_ZHENYA`
- Ignore unknown `COMPASS_IFF_*` tokens as untrusted/unknown.
- Store the newest witness per local roster participant.
- Use scan timestamps so cached scan results do not become falsely fresh.
- Show in `КОНТАКТ`:
  - identity claim source;
  - radio freshness;
  - RSSI;
  - rough proximity;
  - position and direction as `UNKNOWN`.
- Show in `КОМАНДА` whether each roster participant has a radio witness.

## Files

- `app/src/main/java/net/afterday/compas/iff/IffRadioWitnessStore.java`
- `app/src/main/java/net/afterday/compas/sensors/WiFi/WifiImpl.java`
- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `.planning/ROADMAP.md`
- `.planning/STATE.md`
- `stalker/Решения/2026-05-19 MVP IFF Roadmap.md`

## Verification

- Build debug APK with `:app:assembleDebug`.
- Install on Samsung `R3CT20C8A8N` and OnePlus `e089985a` if connected.
- On at least one physical phone, verify:
  - main PDA opens;
  - `IFF` opens;
  - `КОМАНДА` shows roster radio status;
  - selecting a participant opens `КОНТАКТ`;
  - `Я ПОДХОЖУ` remains local-only;
  - no absent beacon is shown as confirmed proximity.
- If a real hotspot is available, set its SSID to one of the known
  `COMPASS_IFF_*` names and verify fresh RSSI appears.

## Out Of Scope

- No automatic hotspot setup.
- No Wi-Fi Direct protocol.
- No cryptographic roster token.
- No GPS or direction calculation.
- No Samsung-specific logic.
