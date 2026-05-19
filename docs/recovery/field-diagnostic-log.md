# Field Diagnostic Log

This document describes the field diagnostic log added for Wi-Fi/radio tests on
the physical Android phone.

## Device

Primary field test device:

```text
R3CT20C8A8N - Samsung SM-S908B / S22 Ultra
```

The emulator can validate install and launch, but it cannot validate real Wi-Fi
radio behavior.

Second field test device:

```text
e089985a - NE2215
```

## Log Location

The app writes a timestamped text log under app-specific external storage:

```text
/sdcard/Android/data/net.afterday.compas/files/diagnostics/field-radio-YYYYMMDD-HHMMSS.log
```

The log is best-effort. A write failure must not crash the app.

## Install And Run

```powershell
adb -s R3CT20C8A8N install -r -d app\build\outputs\apk\debug\app-debug.apk

adb -s R3CT20C8A8N shell pm grant net.afterday.compas android.permission.ACCESS_FINE_LOCATION
adb -s R3CT20C8A8N shell pm grant net.afterday.compas android.permission.ACCESS_COARSE_LOCATION
adb -s R3CT20C8A8N shell pm grant net.afterday.compas android.permission.NEARBY_WIFI_DEVICES
adb -s R3CT20C8A8N shell pm grant net.afterday.compas android.permission.POST_NOTIFICATIONS

adb -s R3CT20C8A8N shell am start -n net.afterday.compas/.MainActivity
```

## Pull Logs

```powershell
New-Item -ItemType Directory -Force artifacts\field-logs
adb -s R3CT20C8A8N pull /sdcard/Android/data/net.afterday.compas/files/diagnostics artifacts\field-logs
```

## Analyze Logs

After pulling logs from field devices, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1
```

Default outputs:

```text
artifacts/field-analysis/summary.md
artifacts/field-analysis/scan-summary.csv
artifacts/field-analysis/bucket-summary.csv
artifacts/field-analysis/event-summary.csv
artifacts/field-analysis/window-device-summary.csv
artifacts/field-analysis/device-comparison.csv
artifacts/field-analysis/device-context.csv
artifacts/field-analysis/movement-deltas.csv
```

`device-context.csv` is populated by Phase 2 logs that contain
`FIELD_DIAG event=device_context`. Older Phase 1 logs do not have this header.
`movement-deltas.csv` highlights BSSID RSSI changes between field windows and
marks candidate rows when the absolute delta is at least 8 dB with enough
samples on both sides.

## Expected Lines

Useful anchors for analysis:

```text
FIELD_DIAG event=logger_start
WIFI_DIAG event=sensor_start
WIFI_DIAG event=request accepted=true
WIFI_DIAG event=request accepted=false
WIFI_DIAG event=results source=receiver updated=true
WIFI_DIAG event=results source=receiver updated=false
WIFI_DIAG event=results source=cached
WIFI_DIAG event=scan_entry
WIFI_DIAG event=status
```

Each `scan_entry` line includes SSID, BSSID, RSSI level, frequency, Android scan
timestamp, and capabilities.

## Device Variability

RSSI must be interpreted per device. Different phones have different Wi-Fi
chipsets, antenna placement, firmware behavior, power policy, and body
orientation sensitivity. The same BSSID can be reported several dB stronger or
weaker on two phones placed next to each other.

Battery level usually does not directly change the reported RSSI, but it can
change scan behavior through power saving, background limits, and firmware scan
policy. For field analysis, treat battery and charging state as test context.

Practical analysis rules:

- Do not convert a single RSSI value directly into distance.
- Prefer zones: strong, medium, weak, unstable edge, lost.
- Compare RSSI trends within the same phone first.
- Build a per-phone baseline before comparing two phones.
- For multi-phone analysis, group by device model/serial and BSSID.

Future log headers should include device model, manufacturer, SDK, battery
percent, charging state, power-save state, Wi-Fi state, and location state.

## 2026-05-19 Two-Phone Test Markers

Initial placement:

```text
R3CT20C8A8N and e089985a placed about 30 cm apart.
```

Movement marker:

```text
10:00 PC time - second phone moved to the other side of the room, about 5 m.
10:02 PC time +/- 5 s - second phone moved behind the door into the corridor.
Relative marker - about 15-20 s before the user message, second phone was
placed in another corner of the room.
Final marker - second phone returned and connected back over USB.
```

First analyzer run:

```text
Input logs: 3
Scan entries: 21416
Samsung R3CT20C8A8N receiver updated events: 838
NE2215 e089985a receiver updated events: 266
```

Initial observation: once the second phone moved away from the near baseline,
the two devices started diverging strongly on common BSSID RSSI. This confirms
that analysis must be windowed by field marker and grouped per device.

Phase 2 adds a startup context line:

```text
FIELD_DIAG event=device_context manufacturer=... model=... sdk=...
batteryPercent=... charging=... powerSave=... wifiEnabled=...
locationEnabled=...
```

This lets later analysis separate radio behavior from device state, battery
state, power saving, disabled Wi-Fi, or disabled location.

Phase 2 verification on 2026-05-19:

```text
R3CT20C8A8N: Samsung SM-S908B, SDK 36, battery 74, charging=false, powerSave=true
e089985a: OnePlus NE2215, SDK 35, battery 73, charging=true, powerSave=false
```
