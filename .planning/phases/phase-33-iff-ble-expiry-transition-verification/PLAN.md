# Phase 33: IFF BLE Expiry Transition Verification

**Status:** completed

## Goal

Verify the Phase 32 automatic witness transition diagnostics on two physical
phones and prove the BLE evidence path moves from fresh to stale to unknown
without manual timing.

## Scope

- Use Samsung `R3CT20C8A8N` as `THIS DEVICE: Петя`.
- Use OnePlus `e089985a` as `THIS DEVICE: Вы`.
- Open IFF on both phones with `RADIO ON`.
- Wait for fresh BLE witness on OnePlus for `Петя`.
- Stop the Samsung app to simulate the transmitter disappearing.
- Put OnePlus screen off during the expiry window.
- Read OnePlus diagnostics for `witness_freshness_transition`.

## Out Of Scope

- Runtime code changes.
- Cryptographic identity proof.
- GPS position or bearing.
- UDP transport changes.
- RSSI threshold tuning.
- Samsung-specific behavior.

## Verification

- Git worktree was clean except untracked `test.png`.
- ADB saw Samsung `R3CT20C8A8N`, OnePlus `e089985a`, and emulator.
- Both phones were unlocked and opened to `Main -> IFF`.
- Screen-on baseline:
  - Samsung showed `THIS DEVICE: Петя` and `rx local-you -43dBm`.
  - OnePlus showed `THIS DEVICE: Вы` and `rx petya -48dBm`.
- OnePlus diagnostics file:
  `field-radio-20260520-161828.log`.
- Automatic transition results:
  - `16:18:46.377`: `petya from=NONE to=RADIO_FRESH`, age `53 ms`,
    RSSI `-55`, source `BLE_FIELD_RADIO`.
  - `16:19:32.682`: `petya from=RADIO_FRESH to=RADIO_STALE`, age
    `15540 ms`, RSSI `-49`, reason `foreground_service_tick`.
  - `16:20:17.565`: `petya from=RADIO_STALE to=UNKNOWN`, age `60423 ms`,
    RSSI `-49`, reason `iff_activity_refresh`.
- Both phones were force-stopped after verification to avoid leaving foreground
  BLE radio running.

## Result

The BLE IFF evidence path now has verified automatic expiry diagnostics:
`RADIO_FRESH -> RADIO_STALE -> UNKNOWN`. Expired radio evidence does not remain
current proximity proof, and the operator can later rely on diagnostics without
manual timing during field runs.
