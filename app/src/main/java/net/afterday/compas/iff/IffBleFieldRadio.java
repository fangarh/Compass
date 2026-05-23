package net.afterday.compas.iff;

import android.Manifest;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.SystemClock;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.logging.FieldDiagnosticLog;

@TargetApi(21)
public final class IffBleFieldRadio {
    private static final Object LOCK = new Object();
    private static final int MANUFACTURER_ID = 0xffff;
    private static final int MAX_ADVERTISE_FAILURES = 3;
    private static final long GPS_ADVERTISE_FRESH_MS = 15000L;
    private static final boolean GPS_IN_BLE_ADVERTISE_ENABLED = false;

    private static BluetoothLeAdvertiser advertiser;
    private static BluetoothLeScanner scanner;
    private static AdvertiseCallback advertiseCallback;
    private static ScanCallback scanCallback;
    private static boolean running;
    private static boolean advertising;
    private static boolean advertiseStartPending;
    private static boolean advertiseDisabledForSession;
    private static boolean scanning;
    private static int rxCount;
    private static int rejectedCount;
    private static int rxOutlier127Count;
    private static int advertiseFailureCount;
    private static String localPlayerId = "";
    private static String lastStatus = "idle";
    private static String lifecycleStatus = "VISIBLE_SCREEN_ONLY";
    private static Location latestLocalGps;
    private static byte[] lastAdvertisedPayload;
    private static long lastAdvertiseRestartElapsedMs;

    private IffBleFieldRadio() {
    }

    public static void start(Context context, String nextLocalPlayerId) {
        start(context, nextLocalPlayerId, "VISIBLE_SCREEN_ONLY");
    }

    public static void startFromForegroundService(Context context, String nextLocalPlayerId) {
        start(context, nextLocalPlayerId, "FOREGROUND_SERVICE_CONNECTED_DEVICE");
    }

    private static void start(Context context, String nextLocalPlayerId, String lifecycle) {
        if (context == null || Build.VERSION.SDK_INT < 21) {
            setStatus("unsupported_api", false, false);
            return;
        }
        synchronized (LOCK) {
            if (running && safe(nextLocalPlayerId).equals(localPlayerId) && safe(lifecycle).equals(lifecycleStatus)) {
                return;
            }
        }
        stop("restart");
        synchronized (LOCK) {
            running = true;
            localPlayerId = safe(nextLocalPlayerId);
            lifecycleStatus = safe(lifecycle);
            lastStatus = "starting " + localPlayerId;
        }
        startLocked(context.getApplicationContext(), localPlayerId);
    }

    public static void stop() {
        stop("manual");
    }

    public static void stop(String reason) {
        String stoppedLifecycle;
        synchronized (LOCK) {
            running = false;
            stoppedLifecycle = lifecycleStatus;
        }
        try {
            if (advertiser != null && advertiseCallback != null) {
                advertiser.stopAdvertising(advertiseCallback);
            }
        } catch (Exception ignored) {
        }
        try {
            if (scanner != null && scanCallback != null) {
                scanner.stopScan(scanCallback);
            }
        } catch (Exception ignored) {
        }
        synchronized (LOCK) {
            advertiser = null;
            scanner = null;
            advertiseCallback = null;
            scanCallback = null;
            advertising = false;
            advertiseStartPending = false;
            advertiseDisabledForSession = false;
            scanning = false;
            advertiseFailureCount = 0;
            lastAdvertisedPayload = null;
            lastAdvertiseRestartElapsedMs = 0L;
            lastStatus = "stopped " + safe(reason);
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_stop"
                + " reason=" + safe(reason)
                + " lifecycle=" + stoppedLifecycle
                + " policy=\"" + clean(IffRadioWitnessStore.freshnessPolicyLabel()) + "\"");
    }

    public static String compactStatus() {
        synchronized (LOCK) {
            return "ble adv=" + (advertising ? "on" : "off")
                    + " scan=" + (scanning ? "on" : "off")
                    + " rx=" + rxCount
                    + " rejected=" + rejectedCount
                    + " local=" + localPlayerId
                    + " " + lastStatus;
        }
    }

    public static String lifecycleStatus() {
        synchronized (LOCK) {
            return lifecycleStatus + " / " + IffRadioWitnessStore.freshnessPolicyLabel();
        }
    }

    public static void updateLocalGps(Location location) {
        BluetoothLeAdvertiser nextAdvertiser;
        String playerId;
        boolean isRunning;
        boolean isAdvertising;
        boolean isAdvertiseStartPending;
        boolean isAdvertiseDisabled;
        byte[] previousPayload;
        long previousRestartElapsedMs;
        synchronized (LOCK) {
            latestLocalGps = location == null ? null : new Location(location);
            if (!running || advertiser == null) {
                return;
            }
            nextAdvertiser = advertiser;
            playerId = localPlayerId;
            isRunning = running;
            isAdvertising = advertising;
            isAdvertiseStartPending = advertiseStartPending;
            isAdvertiseDisabled = advertiseDisabledForSession;
            previousPayload = lastAdvertisedPayload;
            previousRestartElapsedMs = lastAdvertiseRestartElapsedMs;
        }
        if (isAdvertiseDisabled) {
            return;
        }
        int code = IffRadioWitnessStore.playerIndexCode(playerId);
        if (code < 0) {
            return;
        }
        byte[] nextPayload = payloadFor(code);
        if (!IffBleAdvertiseRestartPolicy.shouldRestart(
                isRunning,
                nextAdvertiser != null,
                isAdvertising,
                isAdvertiseStartPending,
                previousRestartElapsedMs,
                SystemClock.elapsedRealtime(),
                previousPayload,
                nextPayload)) {
            return;
        }
        restartAdvertise(nextAdvertiser, playerId);
    }

    private static void startLocked(Context context, String playerId) {
        boolean canScan = hasBleScanPermissions(context);
        boolean canAdvertise = hasBleAdvertisePermission(context);
        if (!canScan && !canAdvertise) {
            setStatus("missing_permission", false, false);
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_status state=missing_permission");
            return;
        }
        BluetoothManager manager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = manager == null ? BluetoothAdapter.getDefaultAdapter() : manager.getAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            setStatus("bluetooth_off", false, false);
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_status state=bluetooth_off");
            return;
        }

        BluetoothLeScanner nextScanner = adapter.getBluetoothLeScanner();
        BluetoothLeAdvertiser nextAdvertiser = adapter.isMultipleAdvertisementSupported()
                ? adapter.getBluetoothLeAdvertiser()
                : null;
        scanner = nextScanner;
        advertiser = nextAdvertiser;

        if (canScan && nextScanner == null) {
            setStatus("scan_unavailable", false, false);
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_status state=scan_unavailable");
            return;
        }

        if (canScan) {
            startScan(nextScanner, playerId);
        } else {
            synchronized (LOCK) {
                scanning = false;
                lastStatus = "scan_missing_permission";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_scan started=false reason=missing_permission");
        }
        if (!canAdvertise) {
            synchronized (LOCK) {
                advertising = false;
                lastStatus = "advertise_missing_permission";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=false localPlayerId="
                    + playerId + " reason=missing_permission");
            return;
        }
        if (nextAdvertiser == null) {
            synchronized (LOCK) {
                rejectedCount++;
                advertising = false;
                lastStatus = "advertise_unavailable";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_status state=advertise_unavailable localPlayerId=" + playerId);
            return;
        }
        startAdvertise(nextAdvertiser, playerId);
    }

    private static void startAdvertise(BluetoothLeAdvertiser nextAdvertiser, final String playerId) {
        synchronized (LOCK) {
            if (advertiseDisabledForSession) {
                advertising = false;
                advertiseStartPending = false;
                lastStatus = "advertise_disabled_after_failures";
                return;
            }
            if (advertiseStartPending
                    && lastAdvertiseRestartElapsedMs > 0L
                    && SystemClock.elapsedRealtime() - lastAdvertiseRestartElapsedMs
                    < IffBleAdvertiseRestartPolicy.START_GRACE_MS) {
                lastStatus = "advertise_start_pending";
                return;
            }
        }
        int code = IffRadioWitnessStore.playerIndexCode(playerId);
        if (code < 0) {
            setStatus("unknown_player", false, scanning);
            return;
        }
        final byte[] advertisePayload = payloadFor(code);
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(MANUFACTURER_ID, advertisePayload)
                .build();
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                synchronized (LOCK) {
                    advertising = true;
                    advertiseStartPending = false;
                    advertiseFailureCount = 0;
                    lastStatus = "advertising " + playerId;
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=true localPlayerId=" + playerId);
            }

            @Override
            public void onStartFailure(int errorCode) {
                synchronized (LOCK) {
                    advertising = false;
                    advertiseStartPending = false;
                    advertiseFailureCount++;
                    if (advertiseFailureCount >= MAX_ADVERTISE_FAILURES) {
                        advertiseDisabledForSession = true;
                    }
                    rejectedCount++;
                    lastStatus = advertiseDisabledForSession
                            ? "advertise_disabled_after_failures"
                            : "advertise_error_" + errorCode;
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=false localPlayerId="
                        + playerId
                        + " errorCode=" + errorCode
                        + " failureCount=" + advertiseFailureCount
                        + " disabled=" + advertiseDisabledForSession);
            }
        };
        try {
            synchronized (LOCK) {
                advertiseStartPending = true;
                lastAdvertisedPayload = advertisePayload;
                lastAdvertiseRestartElapsedMs = SystemClock.elapsedRealtime();
            }
            nextAdvertiser.startAdvertising(settings, data, advertiseCallback);
        } catch (Exception e) {
            synchronized (LOCK) {
                advertising = false;
                advertiseStartPending = false;
                advertiseFailureCount++;
                if (advertiseFailureCount >= MAX_ADVERTISE_FAILURES) {
                    advertiseDisabledForSession = true;
                }
                rejectedCount++;
                lastStatus = advertiseDisabledForSession
                        ? "advertise_disabled_after_failures"
                        : "advertise_exception";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=false localPlayerId="
                    + playerId
                    + " error=\"" + clean(e.getClass().getSimpleName()) + "\""
                    + " failureCount=" + advertiseFailureCount
                    + " disabled=" + advertiseDisabledForSession);
        }
    }

    private static void restartAdvertise(BluetoothLeAdvertiser nextAdvertiser, String playerId) {
        try {
            if (advertiseCallback != null) {
                nextAdvertiser.stopAdvertising(advertiseCallback);
            }
        } catch (Exception ignored) {
        }
        synchronized (LOCK) {
            advertising = false;
            advertiseStartPending = false;
            lastStatus = "advertise_restart " + playerId;
        }
        startAdvertise(nextAdvertiser, playerId);
    }

    private static void startScan(BluetoothLeScanner nextScanner, final String playerId) {
        ScanFilter filter = new ScanFilter.Builder()
                .setManufacturerData(MANUFACTURER_ID, new byte[] {IffBlePayload.MARKER_0, IffBlePayload.MARKER_1},
                        new byte[] {(byte) 0xff, (byte) 0xff})
                .build();
        List<ScanFilter> filters = new ArrayList<>();
        filters.add(filter);
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
        scanCallback = new ScanCallback() {
            @Override
            public void onScanResult(int callbackType, ScanResult result) {
                handleScanResult(result, playerId);
            }

            @Override
            public void onBatchScanResults(List<ScanResult> results) {
                if (results == null) {
                    return;
                }
                for (int i = 0; i < results.size(); i++) {
                    handleScanResult(results.get(i), playerId);
                }
            }

            @Override
            public void onScanFailed(int errorCode) {
                synchronized (LOCK) {
                    scanning = false;
                    rejectedCount++;
                    lastStatus = "scan_error_" + errorCode;
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_scan started=false errorCode=" + errorCode);
            }
        };
        try {
            nextScanner.startScan(filters, settings, scanCallback);
            synchronized (LOCK) {
                scanning = true;
                lastStatus = "scanning " + playerId;
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_scan started=true localPlayerId=" + playerId);
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_policy"
                    + " lifecycle=" + lifecycleStatus
                    + " policy=\"" + clean(IffRadioWitnessStore.freshnessPolicyLabel()) + "\"");
        } catch (Exception e) {
            synchronized (LOCK) {
                scanning = false;
                rejectedCount++;
                lastStatus = "scan_exception";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_scan started=false error=\""
                    + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void handleScanResult(ScanResult result, String currentLocalPlayerId) {
        if (result == null || result.getScanRecord() == null) {
            return;
        }
        ScanRecord record = result.getScanRecord();
        byte[] payload = record.getManufacturerSpecificData(MANUFACTURER_ID);
        IffBlePayload.Parsed parsed = IffBlePayload.parse(payload);
        String playerId = parsed == null ? null : IffRadioWitnessStore.playerIdFromCode(parsed.playerCode);
        if (playerId == null) {
            synchronized (LOCK) {
                rejectedCount++;
                lastStatus = "rx invalid";
            }
            return;
        }
        if (playerId.equals(currentLocalPlayerId)) {
            synchronized (LOCK) {
                lastStatus = "rx self ignored";
            }
            return;
        }
        String address = result.getDevice() == null ? "" : result.getDevice().getAddress();
        IffRadioWitnessStore.updateFromBleAdvert(playerId, address, result.getRssi());
        recordTargetObservationFromBle(currentLocalPlayerId, playerId, address, result.getRssi());
        if (parsed.hasGps) {
            long now = SystemClock.elapsedRealtime();
            IffRemoteWitnessStore.receiveReport(new IffRemoteWitnessReport(
                    "ble-" + playerId,
                    playerId,
                    IffRadioWitnessStore.expectedBeaconSsid(playerId),
                    address,
                    result.getRssi(),
                    0,
                    now,
                    now,
                    IffRemoteWitnessReport.SIGNATURE_PENDING,
                    parsed.gpsLatE7,
                    parsed.gpsLonE7,
                    parsed.gpsAccuracyM,
                    now - parsed.gpsAgeMs));
        }
        synchronized (LOCK) {
            rxCount++;
            if (result.getRssi() == IffOfficeProximityVerdict.RSSI_OUTLIER_127) {
                rxOutlier127Count++;
            }
            lastStatus = IffFieldSnapshotFormatter.bleRxStatus(
                    playerId,
                    result.getRssi(),
                    lastStatus,
                    rxOutlier127Count);
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_rx"
                + " playerId=" + playerId
                + " localPlayerId=" + currentLocalPlayerId
                + " address=" + clean(address)
                + " rssi=" + result.getRssi()
                + " gps=" + parsed.hasGps
                + " contract=ble-iff-v" + parsed.contractVersion);
    }

    private static void recordTargetObservationFromBle(
            String currentLocalPlayerId,
            String observedPlayerId,
            String address,
            int rssi) {
        if (!IffTargetObservationPolicy.shouldRecordAnchorObservation(currentLocalPlayerId, observedPlayerId)) {
            return;
        }
        if (rssi >= 0) {
            return;
        }
        IffWifiTargetObservationStore.updateLocalObservation(currentLocalPlayerId, observedPlayerId, rssi);
        IffWifiDirectDiscoveryTransport.updateTargetObservation(observedPlayerId, rssi);
        FieldDiagnosticLog.event("IFF_DIAG", "event=ble_target_observation"
                + " localDevicePlayerId=" + clean(currentLocalPlayerId)
                + " targetPlayerId=" + clean(observedPlayerId)
                + " address=" + clean(address)
                + " rssi=" + rssi
                + " source=ble_scan");
    }

    private static byte[] payloadFor(int playerCode) {
        if (!GPS_IN_BLE_ADVERTISE_ENABLED) {
            return IffBlePayload.forPlayer(playerCode);
        }
        Location gps;
        synchronized (LOCK) {
            gps = latestLocalGps == null ? null : new Location(latestLocalGps);
        }
        if (hasFreshGps(gps)) {
            return IffBlePayload.forPlayerWithGps(
                    playerCode,
                    IffRemoteWitnessFrame.coordinateE7(gps.getLatitude()),
                    IffRemoteWitnessFrame.coordinateE7(gps.getLongitude()),
                    gps.hasAccuracy() ? Math.round(gps.getAccuracy()) : 0,
                    Math.max(0L, System.currentTimeMillis() - gps.getTime()));
        }
        return IffBlePayload.forPlayer(playerCode);
    }

    private static boolean hasFreshGps(Location location) {
        return location != null
                && Math.abs(location.getLatitude()) <= 90.0d
                && Math.abs(location.getLongitude()) <= 180.0d
                && System.currentTimeMillis() - location.getTime() <= GPS_ADVERTISE_FRESH_MS;
    }

    private static boolean hasBleScanPermissions(Context context) {
        if (Build.VERSION.SDK_INT < 23) {
            return true;
        }
        if (Build.VERSION.SDK_INT >= 31) {
            return context.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
                    && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
        }
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private static boolean hasBleAdvertisePermission(Context context) {
        if (Build.VERSION.SDK_INT < 31) {
            return true;
        }
        return context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED
                && context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private static void setStatus(String status, boolean adv, boolean scan) {
        synchronized (LOCK) {
            advertising = adv;
            scanning = scan;
            lastStatus = status;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String clean(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
