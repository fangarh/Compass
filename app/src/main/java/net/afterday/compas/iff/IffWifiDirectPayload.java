package net.afterday.compas.iff;

import java.util.HashMap;
import java.util.Map;

public final class IffWifiDirectPayload {
    public static final String SERVICE_TYPE = "_compassiff._tcp";
    public static final String KEY_CONTRACT = "c";
    public static final String KEY_PLAYER = "p";
    public static final String KEY_SEQUENCE = "s";
    public static final String KEY_TIMESTAMP = "t";
    public static final String CONTRACT = "iff-wfd-v1";
    private static final String INSTANCE_PREFIX = "ci";

    private IffWifiDirectPayload() {
    }

    public static Map<String, String> build(String playerId, long sequence, long timestampMs) {
        Map<String, String> txt = new HashMap<>();
        txt.put(KEY_CONTRACT, CONTRACT);
        txt.put(KEY_PLAYER, safe(playerId));
        txt.put(KEY_SEQUENCE, String.valueOf(Math.max(0L, sequence)));
        txt.put(KEY_TIMESTAMP, String.valueOf(Math.max(0L, timestampMs)));
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
        return new Parsed(parts[1], parseLong(parts[2]), -1L, targetPlayerId, targetRssi);
    }

    public static Parsed parse(Map<String, String> txt) {
        if (txt == null || !CONTRACT.equals(txt.get(KEY_CONTRACT))) {
            return null;
        }
        String playerId = txt.get(KEY_PLAYER);
        if (playerId == null || playerId.length() == 0) {
            return null;
        }
        return new Parsed(playerId, parseLong(txt.get(KEY_SEQUENCE)), parseLong(txt.get(KEY_TIMESTAMP)), "", 0);
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

    private static String safe(String value) {
        return value == null ? "" : value;
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
        public final long sequence;
        public final long timestampMs;
        public final String targetPlayerId;
        public final int targetRssi;

        private Parsed(
                String playerId,
                long sequence,
                long timestampMs,
                String targetPlayerId,
                int targetRssi) {
            this.playerId = playerId;
            this.sequence = sequence;
            this.timestampMs = timestampMs;
            this.targetPlayerId = safe(targetPlayerId);
            this.targetRssi = targetRssi;
        }
    }
}
