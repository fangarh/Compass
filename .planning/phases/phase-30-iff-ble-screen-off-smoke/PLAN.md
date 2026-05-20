# Phase 30: IFF BLE Screen-Off Smoke

**Status:** completed with two-phone blocker

## Goal

Verify the first screen-off/background prerequisite for the IFF BLE foreground
radio path without claiming two-phone BLE receive until both phones are unlocked
and controllable.

## Scope

- Install the current debug APK on Samsung `R3CT20C8A8N` and OnePlus
  `e089985a`.
- Open IFF on OnePlus and confirm `RADIO ON`.
- Send OnePlus through `Home` and `SLEEP`.
- Check that `IffForegroundRadioService` remains a foreground service while the
  phone is screen-off.
- Attempt to use Samsung for the two-phone test and record the blocker if it is
  locked.

## Out Of Scope

- Claiming cross-phone BLE RX.
- Changing BLE code or thresholds.
- Cryptographic identity proof.
- GPS position or direction inference.

## Verification

- Git worktree was clean except untracked `test.png`.
- ADB saw Samsung `R3CT20C8A8N`, OnePlus `e089985a`, and emulator.
- APK installed successfully on Samsung and OnePlus.
- OnePlus opened `Main -> IFF` and showed:
  - `RADIO CONTROL: ON`;
  - `RADIO SERVICE: iff radio service on local=local-you foreground connectedDevice`;
  - `BLE POLICY: FOREGROUND_SERVICE_CONNECTED_DEVICE / fresh<=15s stale<=60s then UNKNOWN`.
- After `Home` and `SLEEP`, `dumpsys activity services net.afterday.compas`
  still showed `IffForegroundRadioService isForeground=true`, notification
  channel `compass_iff_radio`, and foreground type `0x00000010`.
- Samsung remained on lock/AOD/keyguard UI after ADB wake/swipe attempts, so
  IFF could not be opened there without manual unlock.
- Apps were force-stopped after the smoke test to avoid leaving BLE foreground
  radio running.

## Result

The foreground BLE service survives OnePlus screen-off as an Android foreground
service. The two-phone BLE RX field test is still pending because Samsung must
be manually unlocked and placed on the IFF screen with `RADIO ON`.
