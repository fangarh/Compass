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
import android.os.Build;
import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.logging.FieldDiagnosticLog;

@TargetApi(21)
public final class IffBleFieldRadio {
    private static final Object LOCK = new Object();
    private static final int MANUFACTURER_ID = 0xffff;
    private static final byte MARKER_0 = 0x43; // C
    private static final byte MARKER_1 = 0x49; // I
    private static final byte CONTRACT_VERSION = 1;

    private static BluetoothLeAdvertiser advertiser;
    private static BluetoothLeScanner scanner;
    private static AdvertiseCallback advertiseCallback;
    private static ScanCallback scanCallback;
    private static boolean running;
    private static boolean advertising;
    private static boolean scanning;
    private static int rxCount;
    private static int rejectedCount;
    private static String localPlayerId = "";
    private static String lastStatus = "idle";

    private IffBleFieldRadio() {
    }

    public static void start(Context context, String nextLocalPlayerId) {
        if (context == null || Build.VERSION.SDK_INT < 21) {
            setStatus("unsupported_api", false, false);
            return;
        }
        synchronized (LOCK) {
            if (running && safe(nextLocalPlayerId).equals(localPlayerId)) {
                return;
            }
        }
        stop();
        synchronized (LOCK) {
            running = true;
            localPlayerId = safe(nextLocalPlayerId);
            lastStatus = "starting " + localPlayerId;
        }
        startLocked(context.getApplicationContext(), localPlayerId);
    }

    public static void stop() {
        synchronized (LOCK) {
            running = false;
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
            scanning = false;
            if ("idle".equals(lastStatus) || lastStatus.startsWith("starting")) {
                lastStatus = "stopped";
            }
        }
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
        int code = IffRadioWitnessStore.playerIndexCode(playerId);
        if (code < 0) {
            setStatus("unknown_player", false, scanning);
            return;
        }
        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(false)
                .build();
        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .setIncludeTxPowerLevel(false)
                .addManufacturerData(MANUFACTURER_ID, payloadFor(code))
                .build();
        advertiseCallback = new AdvertiseCallback() {
            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                synchronized (LOCK) {
                    advertising = true;
                    lastStatus = "advertising " + playerId;
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=true localPlayerId=" + playerId);
            }

            @Override
            public void onStartFailure(int errorCode) {
                synchronized (LOCK) {
                    advertising = false;
                    rejectedCount++;
                    lastStatus = "advertise_error_" + errorCode;
                }
                FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=false localPlayerId="
                        + playerId + " errorCode=" + errorCode);
            }
        };
        try {
            nextAdvertiser.startAdvertising(settings, data, advertiseCallback);
        } catch (Exception e) {
            synchronized (LOCK) {
                advertising = false;
                rejectedCount++;
                lastStatus = "advertise_exception";
            }
            FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_advertise started=false localPlayerId="
                    + playerId + " error=\"" + clean(e.getClass().getSimpleName()) + "\"");
        }
    }

    private static void startScan(BluetoothLeScanner nextScanner, final String playerId) {
        ScanFilter filter = new ScanFilter.Builder()
                .setManufacturerData(MANUFACTURER_ID, new byte[] {MARKER_0, MARKER_1},
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
        String playerId = playerIdFromPayload(payload);
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
        synchronized (LOCK) {
            rxCount++;
            lastStatus = "rx " + playerId + " " + result.getRssi() + "dBm";
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=ble_field_radio_rx"
                + " playerId=" + playerId
                + " localPlayerId=" + currentLocalPlayerId
                + " address=" + clean(address)
                + " rssi=" + result.getRssi()
                + " contract=ble-iff-v1");
    }

    private static byte[] payloadFor(int playerCode) {
        return new byte[] {MARKER_0, MARKER_1, CONTRACT_VERSION, (byte) playerCode};
    }

    private static String playerIdFromPayload(byte[] payload) {
        if (payload == null || payload.length < 4) {
            return null;
        }
        if (payload[0] != MARKER_0 || payload[1] != MARKER_1 || payload[2] != CONTRACT_VERSION) {
            return null;
        }
        return IffRadioWitnessStore.playerIdFromCode(payload[3] & 0xff);
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
