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

Zone fingerprint outputs:

```text
artifacts/field-analysis/zone-fingerprints.csv
artifacts/field-analysis/zone-evaluation.csv
artifacts/field-analysis/zone-predictions.csv
```

`zone-fingerprints.csv` stores per-device/per-window BSSID fingerprints.
`zone-predictions.csv` compares each time bucket against the fingerprints for
the same device and selects the best matching zone.

Cross-validation outputs:

```text
artifacts/field-analysis/zone-cross-validation-evaluation.csv
artifacts/field-analysis/zone-cross-validation-predictions.csv
```

These files use leave-one-bucket-out scoring: the bucket being classified is
removed before building the fingerprints.

Named movement windows can be passed directly:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\analyze-field-logs.ps1 `
  -InputRoot artifacts\field-logs\run-20260519-1045 `
  -OutputDir artifacts\field-analysis-run-20260519-1045-named `
  -Windows "cabinet=10:45:10..10:47:10;corridor=10:47:10..10:49:10;cabinet_return=10:49:10..10:51:10;near_30cm=10:51:10.."
```

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

## 2026-05-19 Phase 2 Controlled Run

Logs were cleared before the run. Both phones were unlocked, charging, Wi-Fi
enabled, location enabled, and power saving disabled.

Raw logs:

```text
artifacts/field-logs/run-20260519-1045/R3CT20C8A8N/diagnostics/field-radio-20260519-104237.log
artifacts/field-logs/run-20260519-1045/e089985a/diagnostics/field-radio-20260519-104238.log
```

Analyzer output:

```text
artifacts/field-analysis-run-20260519-1045/
artifacts/field-analysis-run-20260519-1045-named/
```

Samsung movement markers:

```text
10:45:10 - placed on cabinet
10:47:10 - moved to corridor
10:49:10 - returned to cabinet
10:51:10 - placed about 30 cm from the other phone
```

Named analyzer windows for this run:

```text
cabinet        => 10:45:10..10:47:10
corridor       => 10:47:10..10:49:10
cabinet_return => 10:49:10..10:51:10
near_30cm      => from 10:51:10
```

Run summary:

```text
Scan entries: 9352
R3CT20C8A8N: Samsung SM-S908B, battery 71, charging=true, powerSave=false
e089985a: OnePlus NE2215, battery 72, charging=true, powerSave=false
```

Every window had fresh receiver results and no rejected scan requests. The
strongest movement candidates were on Samsung when moving from corridor back to
the later near/cabinet state, with common office BSSID RSSI deltas around
25-28 dB.

Offline zone evaluator output:

```text
artifacts/field-analysis-run-20260519-1045-zone-eval/
```

Result:

```text
30-second bucket predictions: 29/32 correct (90.6%)
```

The three incorrect predictions are concentrated at movement boundary buckets,
where the physical transition and the 30-second bucket boundary can overlap.

Cross-validation output:

```text
artifacts/field-analysis-run-20260519-1045-cv/
```

Result:

```text
same-data bucket predictions: 29/32 correct (90.6%)
leave-one-bucket-out predictions: 11/32 correct (34.4%)
```

Interpretation: the simple fingerprint score overfits this single run. It is
good for exploration and finding discriminative BSSID, but it should not be
ported to runtime as-is. Runtime detection should use a smaller set of stable
discriminative BSSID, device-specific calibration, and smoothing over multiple
consecutive windows.

## 2026-05-19 Phase 6 1 Hz Freshness Verification

APK version:

```text
1816-diagnostic-1s
```

The diagnostic build requests Wi-Fi scans every second and logs:

```text
WIFI_DIAG event=tick mode=diagnostic-1s intervalMs=1000 freshAgeMs=... cachedCount=...
```

Freshness analyzer outputs:

```text
artifacts/field-analysis/freshness-timeline.csv
artifacts/field-analysis/freshness-summary.csv
```

Verification logs:

```text
artifacts/field-logs/phase6-verify/R3CT20C8A8N/field-radio-20260519-111900.log
artifacts/field-logs/phase6-verify/e089985a/field-radio-20260519-111851.log
```

Analyzer output:

```text
artifacts/field-analysis-phase6-verify/
```

Short-run freshness:

```text
e089985a: avg fresh age 918 ms, max fresh age 2231 ms, avg receiver gap 2.41 s, max receiver gap 3.19 s
R3CT20C8A8N: avg fresh age 1068 ms, max fresh age 2140 ms, avg receiver gap 2.17 s, max receiver gap 2.38 s
```

No tick exceeded 3 seconds of fresh-age staleness in the short verification run.

## 2026-05-19 Phase 7 Multisensor Diagnostics

APK version:

```text
1816-diagnostic-sensors-1s
```

The diagnostic build now keeps 1 Hz Wi-Fi logging and also writes:

```text
SENSOR_DIAG event=tick intervalMs=1000 accel=... gyro=... magnetic=... yawDeg=... pitchDeg=... rollDeg=... pressureHpa=... lightLux=... proximityCm=... stepCounter=... locationAgeMs=...
LOCATION_DIAG event=provider_status ...
LOCATION_DIAG event=update provider=... lat=... lon=... accuracyM=... speedMps=... bearingDeg=...
```

The sampler is diagnostic-only. It does not change gameplay influence
calculation.

Analyzer outputs added:

```text
sensor-timeline.csv
sensor-summary.csv
location-timeline.csv
location-summary.csv
```

Samsung verification:

```text
artifacts/field-logs/phase7-sensors-verify/R3CT20C8A8N/field-radio-20260519-120208.log
artifacts/field-analysis-phase7-sensors-verify/
```

Result:

```text
sensor-summary: 34 ticks, 8 registered sensors, yaw/pressure/light/step data present
location-summary: 9 events, 4 network updates, best accuracy 6 m, average accuracy 75.3 m
freshness-summary: avg fresh age 1048 ms, max fresh age 2031 ms, >3s = 0
```

OnePlus installed the APK, but the device was locked during verification and
the new physical-activity permission still needs manual approval before the next
field run if step-counter data is needed.
