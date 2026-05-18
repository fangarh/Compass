# Wi-Fi anomaly migration notes

The recovered app uses Wi-Fi scan results as one source of game influences:

- `WifiImpl` reads Android `ScanResult` values.
- `WifiInfluenceProviderImpl` maps scan results through the registered Wi-Fi modules.
- `AbstractWifiExtractor` converts matched SSID/MAC/RSSI values into influence strength.
- `InfluenceProviderImpl` combines Wi-Fi, Bluetooth, and GPS influences for the game loop.

## Legacy-compatible Wi-Fi mode

Modern Android throttles `WifiManager.startScan()`, so the app no longer requests scans every second in normal mode.

Current behavior:

- Dynamic receiver listens for `WifiManager.SCAN_RESULTS_AVAILABLE_ACTION`.
- Normal scan interval is 35 seconds.
- Cached scan results are still published every tick so the legacy influence pipeline keeps working.
- Per-SSID log spam was removed.
- Throttling warnings are rate-limited.

## Debug bypass mode

There is no public Android API that lets a normal app disable Wi-Fi scan throttling. For controlled event/test devices, Android's Developer Options can disable the system throttle.

The app enables aggressive 1-second Wi-Fi scan requests only when all conditions are true:

- The APK is a debug build.
- Android Developer Options are enabled.
- The system setting `wifi_scan_throttle_enabled` is `0`.

For emulator or adb-managed test devices:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\configure-wifi-debug-bypass.ps1
```

To restore throttling:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\configure-wifi-debug-bypass.ps1 -Disable
```

## Test APK

Current handoff APK:

```text
artifacts\PDA-Compass-wifi-debug-1hz.apk
```

Package:

```text
net.afterday.compas
```

SHA-256:

```text
0FF1B805E55F9E1F62BCC69D9A76F6A0DEAF63ECA7309B319D1A339544D9589C
```

## Enabling 1 Hz Wi-Fi scan test mode

The 1 Hz mode is intentionally available only in debug APKs. It is intended for controlled test devices where organizers can change Developer Options.

### Manual phone setup

1. Install `artifacts\PDA-Compass-wifi-debug-1hz.apk`.
2. Open Android Settings.
3. Open About phone.
4. Tap Build number 7 times to enable Developer Options.
5. Go back to Settings and open System > Developer options. On some phones this is Settings > Developer options.
6. Find Wi-Fi scan throttling.
7. Turn Wi-Fi scan throttling off.
8. Start PDA Compass.
9. Grant requested permissions, especially location/nearby devices/Wi-Fi related permissions.

Expected app/log mode:

```text
WIFI_DIAG WiFi scan mode=debug-1hz; intervalMs=1000; debugBuild=true
```

If the log says this instead, the bypass is not active:

```text
WIFI_DIAG WiFi scan mode=normal-throttled; intervalMs=35000; debugBuild=true
```

### ADB setup

Install and grant permissions:

```powershell
adb install -r -d artifacts\PDA-Compass-wifi-debug-1hz.apk
adb shell pm grant net.afterday.compas android.permission.ACCESS_FINE_LOCATION
adb shell pm grant net.afterday.compas android.permission.ACCESS_COARSE_LOCATION
adb shell pm grant net.afterday.compas android.permission.NEARBY_WIFI_DEVICES
adb shell pm grant net.afterday.compas android.permission.BLUETOOTH_SCAN
adb shell pm grant net.afterday.compas android.permission.BLUETOOTH_CONNECT
adb shell pm grant net.afterday.compas android.permission.CAMERA
adb shell pm grant net.afterday.compas android.permission.POST_NOTIFICATIONS
```

Enable debug bypass:

```powershell
adb shell settings put global development_settings_enabled 1
adb shell settings put global wifi_scan_throttle_enabled 0
adb shell am start -n net.afterday.compas/.MainActivity
```

Watch diagnostics:

```powershell
adb logcat | findstr WIFI_DIAG
```

Restore normal Android throttling after the test:

```powershell
adb shell settings put global wifi_scan_throttle_enabled 1
```

### Important limitation

The APK can request scans once per second in debug bypass mode, but the final decision still belongs to Android firmware. Some emulator/device builds can continue to reject some requests even when the setting is off. For field testing, judge success by fresh receiver events:

```text
WIFI_DIAG event=results source=receiver updated=true
```

Repeated `updated=false` means Android is mostly returning cached results.

## BLE transition path

Keep the Wi-Fi influence provider as legacy support. Add a new BLE-backed influence source that emits the same influence model, then let game organizers migrate anomaly hardware from Wi-Fi access points to BLE beacons without changing the higher-level game loop.
