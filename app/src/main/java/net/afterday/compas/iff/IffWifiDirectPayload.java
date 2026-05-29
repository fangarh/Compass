package net.afterday.compas.iff;

import java.util.HashMap;
import java.util.Map;

public final class IffWifiDirectPayload {
    public static final String SERVICE_TYPE = "_compassiff._tcp";
    public static final String KEY_CONTRACT = "c";
    public static final String KEY_PLAYER = "p";
    public static final String KEY_DISPLAY_NAME = "dn";
    public static final String KEY_SEQUENCE = "s";
    public static final String KEY_TIMESTAMP = "t";
    public static final String KEY_COORDINATES = "coords";
    public static final String KEY_GPS_LAT_E7 = "glat";
    public static final String KEY_GPS_LON_E7 = "glon";
    public static final String KEY_GPS_ACCURACY_M = "gacc";
    public static final String KEY_GPS_AGE_MS = "gage";
    public static final String KEY_TARGET_PLAYER = "tp";
    public static final String KEY_TARGET_RSSI = "tr";
    public static final String KEY_TARGET_GPS_LAT_E7 = "tglat";
    public static final String KEY_TARGET_GPS_LON_E7 = "tglon";
    public static final String KEY_TARGET_GPS_ACCURACY_M = "tgacc";
    public static final String KEY_TARGET_GPS_AGE_MS = "tgage";
    public static final String CONTRACT = "iff-wfd-v1";
    private static final String INSTANCE_PREFIX = "ci";

    private IffWifiDirectPayload() {
    }

    public static Map<String, String> build(String playerId, long sequence, long timestampMs) {
        return build(playerId, playerId, sequence, timestampMs);
    }

    public static Map<String, String> build(
            String playerId,
            String displayName,
            long sequence,
            long timestampMs) {
        Map<String, String> txt = new HashMap<>();
        txt.put(KEY_CONTRACT, CONTRACT);
        txt.put(KEY_PLAYER, safe(playerId));
        txt.put(KEY_DISPLAY_NAME, normalizedDisplayName(displayName, playerId));
        txt.put(KEY_SEQUENCE, String.valueOf(Math.max(0L, sequence)));
        txt.put(KEY_TIMESTAMP, String.valueOf(Math.max(0L, timestampMs)));
        return txt;
    }

    public static void putCoordinateMessage(Map<String, String> txt, String coordinateMessage) {
        if (txt == null || coordinateMessage == null || coordinateMessage.length() == 0) {
            return;
        }
        txt.put(KEY_COORDINATES, coordinateMessage);
    }

    public static Map<String, String> build(
            String playerId,
            long sequence,
            long timestampMs,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs) {
        return build(playerId, playerId, sequence, timestampMs, gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs);
    }

    public static Map<String, String> build(
            String playerId,
            String displayName,
            long sequence,
            long timestampMs,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs) {
        Map<String, String> txt = build(playerId, displayName, sequence, timestampMs);
        putGps(txt, KEY_GPS_LAT_E7, KEY_GPS_LON_E7, KEY_GPS_ACCURACY_M, KEY_GPS_AGE_MS,
                gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs);
        return txt;
    }

    public static Map<String, String> build(
            String playerId,
            long sequence,
            long timestampMs,
            String targetPlayerId,
            int targetRssi,
            int targetGpsLatE7,
            int targetGpsLonE7,
            int targetGpsAccuracyM,
            long targetGpsAgeMs) {
        return build(playerId, playerId, sequence, timestampMs, targetPlayerId, targetRssi,
                targetGpsLatE7, targetGpsLonE7, targetGpsAccuracyM, targetGpsAgeMs);
    }

    public static Map<String, String> build(
            String playerId,
            String displayName,
            long sequence,
            long timestampMs,
            String targetPlayerId,
            int targetRssi,
            int targetGpsLatE7,
            int targetGpsLonE7,
            int targetGpsAccuracyM,
            long targetGpsAgeMs) {
        Map<String, String> txt = build(playerId, displayName, sequence, timestampMs);
        putTarget(txt, targetPlayerId, targetRssi);
        putGps(txt, KEY_TARGET_GPS_LAT_E7, KEY_TARGET_GPS_LON_E7,
                KEY_TARGET_GPS_ACCURACY_M, KEY_TARGET_GPS_AGE_MS,
                targetGpsLatE7, targetGpsLonE7, targetGpsAccuracyM, targetGpsAgeMs);
        return txt;
    }

    public static Map<String, String> build(
            String playerId,
            long sequence,
            long timestampMs,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs,
            String targetPlayerId,
            int targetRssi,
            int targetGpsLatE7,
            int targetGpsLonE7,
            int targetGpsAccuracyM,
            long targetGpsAgeMs) {
        return build(playerId, playerId, sequence, timestampMs, gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs,
                targetPlayerId, targetRssi, targetGpsLatE7, targetGpsLonE7, targetGpsAccuracyM, targetGpsAgeMs);
    }

    public static Map<String, String> build(
            String playerId,
            String displayName,
            long sequence,
            long timestampMs,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs,
            String targetPlayerId,
            int targetRssi,
            int targetGpsLatE7,
            int targetGpsLonE7,
            int targetGpsAccuracyM,
            long targetGpsAgeMs) {
        Map<String, String> txt = build(playerId, displayName, sequence, timestampMs,
                gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs);
        putTarget(txt, targetPlayerId, targetRssi);
        putGps(txt, KEY_TARGET_GPS_LAT_E7, KEY_TARGET_GPS_LON_E7,
                KEY_TARGET_GPS_ACCURACY_M, KEY_TARGET_GPS_AGE_MS,
                targetGpsLatE7, targetGpsLonE7, targetGpsAccuracyM, targetGpsAgeMs);
        return txt;
    }

    public static String buildInstanceName(String playerId, long sequence) {
        return INSTANCE_PREFIX + "-" + safe(playerId) + "-" + Math.max(0L, sequence);
    }

    public static String buildInstanceName(
            String playerId,
            long sequence,
            String targetPlayerId,
            int targetRssi) {
        if (targetPlayerId == null || targetPlayerId.length() == 0) {
            return buildInstanceName(playerId, sequence);
        }
        return buildInstanceName(playerId, sequence)
                + "-" + safe(targetPlayerId)
                + "-" + encodeRssi(targetRssi);
    }

    public static Parsed parseInstanceName(String instanceName) {
        if (instanceName == null) {
            return null;
        }
        String[] parts = instanceName.split("-");
        if (parts.length < 3 || !INSTANCE_PREFIX.equals(parts[0])) {
            return null;
        }
        String targetPlayerId = "";
        int targetRssi = 0;
        if (parts.length >= 5) {
            targetPlayerId = parts[3];
            targetRssi = parseRssi(parts[4]);
        }
        return new Parsed(parts[1], parts[1], parseLong(parts[2]), -1L, targetPlayerId, targetRssi, "",
                false, 0, 0, -1, -1L,
                false, 0, 0, -1, -1L);
    }

    public static Parsed parse(Map<String, String> txt) {
        if (txt == null || !CONTRACT.equals(txt.get(KEY_CONTRACT))) {
            return null;
        }
        String playerId = txt.get(KEY_PLAYER);
        if (playerId == null || playerId.length() == 0) {
            return null;
        }
        String displayName = normalizedDisplayName(txt.get(KEY_DISPLAY_NAME), playerId);
        String targetPlayerId = txt.get(KEY_TARGET_PLAYER);
        int targetRssi = parseRssi(txt.get(KEY_TARGET_RSSI));
        String coordinateMessage = safe(txt.get(KEY_COORDINATES));
        int gpsLatE7 = parseInt(txt.get(KEY_GPS_LAT_E7), 0);
        int gpsLonE7 = parseInt(txt.get(KEY_GPS_LON_E7), 0);
        int gpsAccuracyM = parseInt(txt.get(KEY_GPS_ACCURACY_M), -1);
        long gpsAgeMs = parseLong(txt.get(KEY_GPS_AGE_MS));
        int targetGpsLatE7 = parseInt(txt.get(KEY_TARGET_GPS_LAT_E7), 0);
        int targetGpsLonE7 = parseInt(txt.get(KEY_TARGET_GPS_LON_E7), 0);
        int targetGpsAccuracyM = parseInt(txt.get(KEY_TARGET_GPS_ACCURACY_M), -1);
        long targetGpsAgeMs = parseLong(txt.get(KEY_TARGET_GPS_AGE_MS));
        return new Parsed(
                playerId,
                displayName,
                parseLong(txt.get(KEY_SEQUENCE)),
                parseLong(txt.get(KEY_TIMESTAMP)),
                targetPlayerId,
                targetRssi,
                coordinateMessage,
                hasGps(gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs),
                gpsLatE7,
                gpsLonE7,
                gpsAccuracyM,
                gpsAgeMs,
                hasGps(targetGpsLatE7, targetGpsLonE7, targetGpsAccuracyM, targetGpsAgeMs),
                targetGpsLatE7,
                targetGpsLonE7,
                targetGpsAccuracyM,
                targetGpsAgeMs);
    }

    private static long parseLong(String value) {
        if (value == null) {
            return -1L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static int parseInt(String value, int fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizedDisplayName(String displayName, String fallbackPlayerId) {
        String normalized = safe(displayName).trim();
        if (normalized.length() == 0) {
            normalized = safe(fallbackPlayerId).trim();
        }
        if (normalized.length() == 0) {
            return "phone";
        }
        return normalized.length() > 18 ? normalized.substring(0, 18) : normalized;
    }

    private static void putTarget(Map<String, String> txt, String targetPlayerId, int targetRssi) {
        if (targetPlayerId == null || targetPlayerId.length() == 0) {
            return;
        }
        txt.put(KEY_TARGET_PLAYER, safe(targetPlayerId));
        txt.put(KEY_TARGET_RSSI, String.valueOf(targetRssi));
    }

    private static void putGps(
            Map<String, String> txt,
            String latKey,
            String lonKey,
            String accuracyKey,
            String ageKey,
            int latE7,
            int lonE7,
            int accuracyM,
            long ageMs) {
        if (!hasGps(latE7, lonE7, accuracyM, ageMs)) {
            return;
        }
        txt.put(latKey, String.valueOf(latE7));
        txt.put(lonKey, String.valueOf(lonE7));
        txt.put(accuracyKey, String.valueOf(accuracyM));
        txt.put(ageKey, String.valueOf(Math.max(0L, ageMs)));
    }

    private static boolean hasGps(int latE7, int lonE7, int accuracyM, long ageMs) {
        return latE7 != 0
                && lonE7 != 0
                && accuracyM >= 0
                && ageMs >= 0L;
    }

    private static String encodeRssi(int rssi) {
        if (rssi < 0) {
            return "m" + Math.abs(rssi);
        }
        return String.valueOf(rssi);
    }

    private static int parseRssi(String value) {
        if (value == null || value.length() == 0) {
            return 0;
        }
        if (value.charAt(0) == 'm') {
            return -1 * (int) parseLong(value.substring(1));
        }
        return (int) parseLong(value);
    }

    public static final class Parsed {
        public final String playerId;
        public final String displayName;
        public final long sequence;
        public final long timestampMs;
        public final String targetPlayerId;
        public final int targetRssi;
        public final String coordinateMessage;
        public final boolean hasGps;
        public final int gpsLatE7;
        public final int gpsLonE7;
        public final int gpsAccuracyM;
        public final long gpsAgeMs;
        public final boolean hasTargetGps;
        public final int targetGpsLatE7;
        public final int targetGpsLonE7;
        public final int targetGpsAccuracyM;
        public final long targetGpsAgeMs;

        private Parsed(
                String playerId,
                String displayName,
                long sequence,
                long timestampMs,
                String targetPlayerId,
                int targetRssi,
                String coordinateMessage,
                boolean hasGps,
                int gpsLatE7,
                int gpsLonE7,
                int gpsAccuracyM,
                long gpsAgeMs,
                boolean hasTargetGps,
                int targetGpsLatE7,
                int targetGpsLonE7,
                int targetGpsAccuracyM,
                long targetGpsAgeMs) {
            this.playerId = playerId;
            this.displayName = normalizedDisplayName(displayName, playerId);
            this.sequence = sequence;
            this.timestampMs = timestampMs;
            this.targetPlayerId = safe(targetPlayerId);
            this.targetRssi = targetRssi;
            this.coordinateMessage = safe(coordinateMessage);
            this.hasGps = hasGps;
            this.gpsLatE7 = gpsLatE7;
            this.gpsLonE7 = gpsLonE7;
            this.gpsAccuracyM = gpsAccuracyM;
            this.gpsAgeMs = gpsAgeMs;
            this.hasTargetGps = hasTargetGps;
            this.targetGpsLatE7 = targetGpsLatE7;
            this.targetGpsLonE7 = targetGpsLonE7;
            this.targetGpsAccuracyM = targetGpsAccuracyM;
            this.targetGpsAgeMs = targetGpsAgeMs;
        }
    }
}
