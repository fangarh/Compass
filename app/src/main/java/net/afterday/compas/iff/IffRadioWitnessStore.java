package net.afterday.compas.iff;

import android.net.wifi.ScanResult;
import android.os.SystemClock;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffRadioWitnessStore {
    public static final String SSID_PREFIX = "COMPASS_IFF_";
    public static final long FRESH_MS = 15000L;
    public static final long STALE_MS = 60000L;

    private static final Object LOCK = new Object();
    private static final Map<String, WitnessSnapshot> WITNESSES = new HashMap<>();

    private IffRadioWitnessStore() {
    }

    public static void updateFromScanResults(boolean updated, List<ScanResult> results) {
        if (results == null) {
            return;
        }
        long now = SystemClock.elapsedRealtime();
        for (int i = 0; i < results.size(); i++) {
            ScanResult result = results.get(i);
            if (result == null) {
                continue;
            }
            String playerId = playerIdFromSsid(result.SSID);
            if (playerId == null) {
                continue;
            }
            long seenElapsedMs = result.timestamp > 0 ? result.timestamp / 1000L : now;
            WitnessSnapshot next = new WitnessSnapshot(
                    playerId,
                    result.SSID,
                    result.BSSID,
                    result.level,
                    result.frequency,
                    seenElapsedMs,
                    now,
                    updated);
            boolean changed = putIfNewer(next);
            if (changed) {
                FieldDiagnosticLog.event("IFF_DIAG", "event=radio_witness playerId=" + playerId
                        + " ssid=\"" + safe(result.SSID) + "\""
                        + " bssid=" + safe(result.BSSID)
                        + " rssi=" + result.level
                        + " frequency=" + result.frequency
                        + " ageMs=" + Math.max(0L, now - seenElapsedMs)
                        + " updated=" + updated);
            }
        }
    }

    public static WitnessSnapshot getWitness(String playerId) {
        synchronized (LOCK) {
            return WITNESSES.get(playerId);
        }
    }

    public static String expectedBeaconSsid(String playerId) {
        if ("local-you".equals(playerId)) {
            return SSID_PREFIX + "YOU";
        }
        if ("petya".equals(playerId)) {
            return SSID_PREFIX + "PETYA";
        }
        if ("vasya".equals(playerId)) {
            return SSID_PREFIX + "VASYA";
        }
        if ("zhenya".equals(playerId)) {
            return SSID_PREFIX + "ZHENYA";
        }
        return SSID_PREFIX + playerId.toUpperCase(Locale.US);
    }

    private static boolean putIfNewer(WitnessSnapshot next) {
        synchronized (LOCK) {
            WitnessSnapshot previous = WITNESSES.get(next.playerId);
            if (previous != null && previous.seenElapsedMs > next.seenElapsedMs) {
                return false;
            }
            if (previous != null
                    && previous.seenElapsedMs == next.seenElapsedMs
                    && previous.rssi == next.rssi
                    && safe(previous.bssid).equals(safe(next.bssid))) {
                return false;
            }
            WITNESSES.put(next.playerId, next);
            return true;
        }
    }

    private static String playerIdFromSsid(String ssid) {
        if (ssid == null) {
            return null;
        }
        String normalized = ssid.trim().toUpperCase(Locale.US);
        if (!normalized.startsWith(SSID_PREFIX)) {
            return null;
        }
        String token = normalized.substring(SSID_PREFIX.length());
        if ("YOU".equals(token) || "LOCAL".equals(token) || "LOCAL_YOU".equals(token) || "LOCAL-YOU".equals(token)) {
            return "local-you";
        }
        if ("PETYA".equals(token)) {
            return "petya";
        }
        if ("VASYA".equals(token)) {
            return "vasya";
        }
        if ("ZHENYA".equals(token)) {
            return "zhenya";
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=unknown_radio_beacon ssid=\"" + safe(ssid) + "\"");
        return null;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    public static final class WitnessSnapshot {
        public final String playerId;
        public final String ssid;
        public final String bssid;
        public final int rssi;
        public final int frequency;
        public final long seenElapsedMs;
        public final long updateElapsedMs;
        public final boolean scanUpdated;

        WitnessSnapshot(String playerId, String ssid, String bssid, int rssi, int frequency,
                        long seenElapsedMs, long updateElapsedMs, boolean scanUpdated) {
            this.playerId = playerId;
            this.ssid = ssid;
            this.bssid = bssid;
            this.rssi = rssi;
            this.frequency = frequency;
            this.seenElapsedMs = seenElapsedMs;
            this.updateElapsedMs = updateElapsedMs;
            this.scanUpdated = scanUpdated;
        }

        public long ageMs() {
            return Math.max(0L, SystemClock.elapsedRealtime() - this.seenElapsedMs);
        }

        public boolean isFresh() {
            return ageMs() <= FRESH_MS;
        }

        public String freshnessLabel() {
            long age = ageMs();
            if (age <= FRESH_MS) {
                return "RADIO_FRESH";
            }
            if (age <= STALE_MS) {
                return "RADIO_STALE";
            }
            return "UNKNOWN";
        }

        public String proximityLabel() {
            if (!isFresh()) {
                return "UNKNOWN";
            }
            if (this.rssi >= -55) {
                return "RADIO_NEAR";
            }
            if (this.rssi >= -70) {
                return "RADIO_WEAK_HINT";
            }
            return "RADIO_EDGE_HINT";
        }
    }
}
