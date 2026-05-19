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
