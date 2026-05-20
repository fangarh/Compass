# Phase 25: IFF BLE Field Radio Skeleton

**Status:** completed

## Goal

Add a no-infrastructure field radio path for IFF so nearby phones can hear the
selected local roster identity without requiring all players to share one Wi-Fi
network.

## Scope

- Add a BLE advertising/scanning helper for the visible IFF screen.
- Advertise the current per-device `THIS DEVICE` roster identity.
- Scan for Compass IFF BLE manufacturer payloads from other phones.
- Convert accepted BLE advertisements into `IffRadioWitnessStore` witness
  snapshots.
- Show compact field-radio status in contact, team, and map views.
- Log BLE start, receive, witness, and field-check status.
- Export `fieldRadioStatus` through the field-log analyzer.
- Keep roster rows usable on landscape phone screens.

## Out Of Scope

- Cryptographic identity proof.
- Background/foreground service BLE operation.
- Direction or azimuth inference.
- GPS calibration.
- Wi-Fi calibration for specific phones.
- Replacing the UDP debug transport.
- Samsung-specific or OnePlus-specific radio logic.

## Verification

- `:app:assembleDebug` completed successfully.
- `scripts/test-analyze-field-logs.ps1` passed.
- APK installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- Both phones opened `Main -> IFF`.
- OnePlus defaulted to `THIS DEVICE: Вы`; Samsung persisted
  `THIS DEVICE: Петя`.
- OnePlus showed BLE field radio running:
  `ble adv=on scan=on ... local=local-you`.
- Samsung showed BLE field radio running:
  `ble adv=on scan=on ... local=petya`.
- OnePlus received Samsung's BLE claim for `Петя`:
  `rx petya -43dBm`, witness `BLE_IFF_PETYA`, age `5ms`.
- Samsung received OnePlus's BLE claim for `Вы`:
  `rx local-you -38dBm`, witness `BLE_IFF_YOU`, age `573ms`.
- `ЗАПИСАТЬ` produced `IFF_DIAG event=field_check` rows with
  `fieldRadioStatus` and BLE witness SSIDs.
- Analyzer completed on `artifacts/iff-field-session-20260520-1114-ble` and
  wrote `artifacts/iff-field-analysis-20260520-1114-ble`.

## Result

The IFF MVP now has a first no-common-Wi-Fi radio witness path. BLE proves that
a nearby phone recently advertised a roster claim, so it can support freshness
and coarse proximity. It still does not prove cryptographic identity, exact
position, or direction.
