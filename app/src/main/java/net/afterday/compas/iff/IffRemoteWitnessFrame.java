package net.afterday.compas.iff;

import java.util.HashMap;
import java.util.Map;

public final class IffRemoteWitnessFrame {
    public static final String FRAME_PREFIX = "COMPASS_IFF_REMOTE";
    public static final String CONTRACT_VERSION = "iff-remote-witness-v1";
    public static final int GPS_UNAVAILABLE_INT = Integer.MIN_VALUE;
    public static final long GPS_UNAVAILABLE_AGE_MS = -1L;

    public final String version;
    public final String sourcePlayerId;
    public final String targetPlayerId;
    public final String targetBeaconSsid;
    public final String bssid;
    public final int rssi;
    public final int frequency;
    public final long ageMs;
    public final String signatureStatus;
    public final int gpsLatE7;
    public final int gpsLonE7;
    public final int gpsAccuracyM;
    public final long gpsAgeMs;

    public IffRemoteWitnessFrame(
            String version,
            String sourcePlayerId,
            String targetPlayerId,
            String targetBeaconSsid,
            String bssid,
            int rssi,
            int frequency,
            long ageMs,
            String signatureStatus,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs) {
        this.version = cleanToken(version);
        this.sourcePlayerId = cleanToken(sourcePlayerId);
        this.targetPlayerId = cleanToken(targetPlayerId);
        this.targetBeaconSsid = cleanToken(targetBeaconSsid);
        this.bssid = cleanToken(bssid);
        this.rssi = rssi;
        this.frequency = frequency;
        this.ageMs = Math.max(0L, ageMs);
        this.signatureStatus = cleanToken(signatureStatus);
        this.gpsLatE7 = gpsLatE7;
        this.gpsLonE7 = gpsLonE7;
        this.gpsAccuracyM = gpsAccuracyM;
        this.gpsAgeMs = gpsAgeMs;
    }

    public boolean hasGps() {
        return gpsLatE7 != GPS_UNAVAILABLE_INT
                && gpsLonE7 != GPS_UNAVAILABLE_INT
                && gpsAccuracyM >= 0
                && gpsAgeMs >= 0L;
    }

    public String toWire() {
        String wire = FRAME_PREFIX
                + "|v=" + cleanToken(version)
                + "|source=" + cleanToken(sourcePlayerId)
                + "|target=" + cleanToken(targetPlayerId)
                + "|ssid=" + cleanToken(targetBeaconSsid)
                + "|bssid=" + cleanToken(bssid)
                + "|rssi=" + rssi
                + "|freq=" + frequency
                + "|ageMs=" + Math.max(0L, ageMs);
        if (hasGps()) {
            wire += "|gpsLatE7=" + gpsLatE7
                    + "|gpsLonE7=" + gpsLonE7
                    + "|gpsAccuracyM=" + gpsAccuracyM
                    + "|gpsAgeMs=" + gpsAgeMs;
        }
        return wire + "|signature=" + cleanToken(signatureStatus);
    }

    public static IffRemoteWitnessFrame parse(String wire) {
        if (wire == null || !wire.startsWith(FRAME_PREFIX + "|")) {
            return null;
        }
        Map<String, String> fields = new HashMap<String, String>();
        String[] parts = wire.split("\\|");
        for (int i = 1; i < parts.length; i++) {
            int split = parts[i].indexOf('=');
            if (split <= 0 || split >= parts[i].length() - 1) {
                continue;
            }
            fields.put(parts[i].substring(0, split), parts[i].substring(split + 1));
        }
        if (!CONTRACT_VERSION.equals(fields.get("v"))) {
            return null;
        }
        long ageMs = parseLong(fields.get("ageMs"), -1L);
        int rssi = parseInt(fields.get("rssi"), 0);
        int frequency = parseInt(fields.get("freq"), 0);
        if (ageMs < 0L || rssi == 0 || frequency == 0) {
            return null;
        }

        int gpsLatE7 = parseInt(fields.get("gpsLatE7"), GPS_UNAVAILABLE_INT);
        int gpsLonE7 = parseInt(fields.get("gpsLonE7"), GPS_UNAVAILABLE_INT);
        int gpsAccuracyM = parseInt(fields.get("gpsAccuracyM"), GPS_UNAVAILABLE_INT);
        long gpsAgeMs = parseLong(fields.get("gpsAgeMs"), GPS_UNAVAILABLE_AGE_MS);

        return new IffRemoteWitnessFrame(
                fields.get("v"),
                fields.get("source"),
                fields.get("target"),
                fields.get("ssid"),
                fields.get("bssid"),
                rssi,
                frequency,
                ageMs,
                fields.get("signature"),
                gpsLatE7,
                gpsLonE7,
                gpsAccuracyM,
                gpsAgeMs);
    }

    public static int coordinateE7(double coordinate) {
        return (int) Math.round(coordinate * 10000000.0);
    }

    public static double coordinateFromE7(int coordinateE7) {
        return coordinateE7 / 10000000.0;
    }

    static String cleanToken(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('|', '_').replace('=', '_').replace('\n', ' ').replace('\r', ' ').replace(' ', '-');
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (Exception e) {
            return fallback;
        }
    }
}
