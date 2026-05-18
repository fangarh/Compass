# Radio detection strategy

This note captures the working decisions for preserving the original Wi-Fi
anomaly detection while evaluating BLE beacons as a lower-power proximity layer.

## Current Wi-Fi state

The recovered app still uses Wi-Fi scan results as one source of game
influences. On modern Android, frequent `WifiManager.startScan()` calls are
restricted by the OS and by vendor firmware.

Current test build:

- APK name: `artifacts\PDA-Compass-default-5s-debug.apk`
- versionCode: `1816`
- versionName: `1816-default-5s`
- Wi-Fi scan request interval: 5 seconds
- diagnostic mode name: `default-5s`

Earlier test build:

- APK name: `artifacts\PDA-Compass-default-1hz-debug.apk`
- Wi-Fi scan request interval: 1 second
- diagnostic mode name: `default-1hz`

Important limitation: these builds request scans at the configured interval, but
Android can still throttle requests or return cached results. Fresh Wi-Fi scans
must be judged by:

```text
WIFI_DIAG event=results source=receiver updated=true
```

## Why keep Wi-Fi

The original choice of Wi-Fi is still technically justified for long-distance
forest detection. A normal Android phone can passively discover 2.4 GHz Wi-Fi AP
beacons without any external receiver. A properly mounted Wi-Fi object beacon
can be detected at 100 m or more in usable field conditions.

Recommended Wi-Fi role:

- long-range object presence detection;
- legacy fallback for existing game modules;
- slow background scan interval, around 35-60 seconds, for two-day events;
- optional aggressive diagnostic mode only for short tests.

Recommended Wi-Fi object setup:

- 2.4 GHz access point mode;
- stable SSID and BSSID;
- fixed channel 1, 6, or 11;
- beacon interval around 100-500 ms;
- antenna mounted above ground and away from metal/body shielding;
- power from power bank or larger battery pack.

## BLE role

Standard BLE iBeacon/Eddystone beacons are a better fit for close and medium
range proximity than for guaranteed 100 m forest detection.

Practical expected ranges:

- reliable detection: 3-10 m;
- common outdoor detection: 10-30 m;
- favorable open-area detection: 50-100 m;
- 100 m in forest: possible sometimes, not a design guarantee.

Recommended BLE role:

- fast proximity updates near an object;
- lower-power continuous scanning on the phone;
- close-range zone refinement when Wi-Fi says an object exists;
- future replacement for short-range anomaly logic.

Suggested BLE signal zones:

```text
RSSI stronger than -60 dBm    very near, roughly 0-3 m
-60 to -75 dBm                near/medium, roughly 3-10 m
-75 to -85 dBm                far/weak, roughly 10-25 m
weaker than -85 dBm           unstable edge
no packets for 10-15 sec      lost
```

These are starting points only. Field calibration is required on the actual
phones and beacons.

## Mixed Wi-Fi + BLE mode

For a two-day game, the preferred runtime mode is:

```text
Wi-Fi: 1 scan every 35-60 seconds
BLE: continuous scan in balanced mode
BLE low-latency scan: short bursts during active search or after weak detection
```

This preserves the Wi-Fi long-range behavior while using BLE for lower-latency
near-object updates.

Estimated phone-side battery impact:

```text
Wi-Fi scan every 5 seconds:
  about 720 Wi-Fi scan requests per hour
  rough cost: +3-8% battery per hour

Wi-Fi scan every 35 seconds + BLE balanced:
  about 103 Wi-Fi scan requests per hour
  rough cost: +1-3% battery per hour

Expected saving:
  roughly 2-5% battery per hour
  roughly 40-70% less radio scan overhead than Wi-Fi every 5 seconds
```

For a two-day game with 16-24 active hours, continuous Wi-Fi every 5 seconds is
too expensive as a default mode. It should remain a diagnostic or active-search
mode. The target for production should be near 1-2% app battery cost per active
hour where possible.

## Unified object identity

Wi-Fi and BLE should not be treated as separate game objects. Each physical
object should have a single game object ID and multiple radio identifiers.

Example:

```json
{
  "id": "anomaly_042",
  "name": "Electra near bridge",
  "wifi": {
    "bssid": "AA:BB:CC:11:22:33",
    "ssid": "STALKER_A042"
  },
  "ble": {
    "uuid": "f3b2a000-0000-4000-8000-000000000042",
    "major": 1,
    "minor": 42
  }
}
```

The app should map both radio inputs to the same `objectId`:

```text
Wi-Fi BSSID AA:BB:CC:11:22:33 -> anomaly_042
BLE UUID/Major/Minor 1/42     -> anomaly_042
```

Distance should not be merged as a single meter value. Wi-Fi and BLE RSSI lie in
different ways. Convert each source to game zones and then combine them.

Example priority:

```text
BLE strong                    very near
BLE near                      near
Wi-Fi seen recently           far/object present
Wi-Fi strong + BLE missing    possible distant object or BLE shielding
BLE seen + Wi-Fi missing      near object, Wi-Fi scan may not have refreshed
```

## Beacon candidates

The best ready-made BLE beacon candidates found on Wildberries:

### Feasycom FSC-BP108B DA14531 IP67

- WB: `https://www.wildberries.ru/catalog/1026821313/detail.aspx`
- ready-made configurable beacon;
- Bluetooth 5.1, DA14531;
- iBeacon, Eddystone, AltBeacon;
- advertising interval range: 100-10000 ms;
- TX power range: about -19.5 to +2.5 dBm;
- replaceable CR3032;
- IP67;
- official open-area range claim: up to 400 m;
- preferred first purchase among BP108B cards because the card name is precise.

### Feasycom FSC-BP103C

- WB: `https://www.wildberries.ru/catalog/1026863065/detail.aspx`
- ready-made configurable beacon;
- Bluetooth 5.1, DA14531;
- iBeacon, Eddystone, AltBeacon;
- advertising interval range: 100-10000 ms;
- replaceable coin cell battery;
- IP66;
- official open-area range claim: up to 450 m;
- useful as the main comparison beacon against BP108B.

Recommended first order:

```text
2x Feasycom FSC-BP108B
2x Feasycom FSC-BP103C
```

## Beacon configuration

These Feasycom beacons do not need custom firmware for the first tests. They are
configured through the FeasyBeacon mobile app.

Initial test settings:

```text
Protocol: iBeacon
UUID: one shared project UUID
Major: game or field ID
Minor: object ID
Advertising interval: 500-1000 ms
TX power: maximum or near maximum
Device name: STALKER_A042
Default PIN: 000000 unless changed
```

Verification app:

- FeasyBeacon for configuration;
- nRF Connect for independent scan verification.

## BLE Long Range and LoRa notes

BLE Long Range / Coded PHY is technically interesting but not a drop-in
replacement for standard beacons:

- the beacon hardware and firmware must support Coded PHY;
- the Android phone must support scanning on Coded PHY;
- common iBeacon tags often use normal BLE 1M advertising, not Long Range;
- support must be verified on the actual Samsung S22/S25 devices.

Wildberries search found nRF52840 development boards and dongles, but not a
ready-made BLE Long Range beacon with clear Coded PHY support.

For reliable 100+ m forest detection, LoRa is more suitable than BLE, but it
requires an external receiver:

```text
object LoRa beacon -> player LoRa receiver -> BLE/USB/Wi-Fi -> Android app
```

This is likely too expensive/complex for the current phase, so the immediate
plan is to test standard BLE beacons while preserving Wi-Fi for long-range
detection.

## Proposed staged plan

1. Keep Wi-Fi behavior in the app as the long-range fallback.
2. Test the current 5-second Wi-Fi debug build in the field.
3. Buy BP108B and BP103C BLE beacons.
4. Measure BLE visibility on Samsung S22 Ultra and S25:
   - open field;
   - forest;
   - phone in hand;
   - phone in pocket;
   - screen on/off.
5. Add BLE scanning to the app as a second influence source.
6. Add unified object mapping so Wi-Fi and BLE resolve to the same game object.
7. Move production default toward Wi-Fi 35-60 sec + BLE balanced.
8. Keep Wi-Fi 5 sec / 1 Hz only as diagnostic or active-search builds.
