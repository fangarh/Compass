# Phase 22: IFF UDP Transport Stub

**Status:** completed with RX topology note

## Goal

Add the first real transport stub for remote IFF witness exchange while keeping
identity, proximity, position, and direction confidence honest.

The phase answers only:

```text
Can this device emit and listen for an unsigned debug remote witness report?
```

It does not prove cross-device identity or proximity.

## Scope

- Add a debug-only UDP broadcast transport for `iff-remote-witness-v1`.
- Add `TX STUB` to the IFF screen.
- Send an unsigned `SIGNATURE_PENDING` remote witness report for the selected
  contact.
- Start a UDP listener while the IFF screen is visible.
- Feed accepted remote packets into `IffRemoteWitnessStore`.
- Show compact transport state in team/contact/map UI.
- Log transport status in `IFF_DIAG event=field_check`.
- Export transport status in the field-log analyzer.

## Files

- `app/src/main/java/net/afterday/compas/iff/IffUdpWitnessTransport.java`
- `app/src/main/java/net/afterday/compas/IffActivity.java`
- `app/src/main/res/layout/iff_activity.xml`
- `app/src/main/res/values/ids.xml`
- `scripts/analyze-field-logs.ps1`

## Out Of Scope

- Cryptographic signatures.
- Trusting received identity.
- Network service discovery.
- GPS calibration.
- Wi-Fi calibration for specific phones.
- Samsung-specific logic.
- Direction/azimuth inference from RSSI.

## Verification

- `:app:assembleDebug` completed successfully.
- `scripts/test-analyze-field-logs.ps1` passed.
- APK installed on Samsung `R3CT20C8A8N` and OnePlus `e089985a`.
- UIAutomator/ADB verified `Main -> IFF` on both phones.
- Both IFF screens showed `TRANSPORT: udp:45873 ... listening`.
- `TX STUB` on OnePlus produced `tx=1 rx=0 rejected=0 rx self ignored`.
- `TX STUB` on Samsung produced `tx=1 rx=0 rejected=0 rx self ignored`.

## Topology Note

Full phone-to-phone RX was not marked as proven in this phase because Samsung
was visible over USB but had no `wlan0` address during the test; it only showed
`rmnet*` mobile interfaces. OnePlus was on Wi-Fi `192.168.13.105/24`.

Next RX verification requires both phones on the same Wi-Fi network or hotspot.
