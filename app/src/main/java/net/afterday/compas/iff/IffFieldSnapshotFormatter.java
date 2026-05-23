package net.afterday.compas.iff;

import java.util.Arrays;
import java.util.Comparator;

public final class IffFieldSnapshotFormatter {
    private static final long WIFI_FRESH_MAX_AGE_MS = 3000L;
    private static final long WIFI_CACHED_MAX_AGE_MS = 15000L;

    private IffFieldSnapshotFormatter() {
    }

    public static String bleRxStatus(String playerId, int rssi, String previousStatus, int outlier127Count) {
        StringBuilder builder = new StringBuilder();
        builder.append("rx ").append(safe(playerId)).append(' ');
        if (rssi == IffOfficeProximityVerdict.RSSI_OUTLIER_127) {
            builder.append("no-valid-rssi");
        } else {
            builder.append(rssi).append("dBm");
        }
        if (outlier127Count > 0) {
            builder.append(" out127=").append(outlier127Count);
        }
        return builder.toString();
    }

    public static String wifiFingerprint(WifiEntry[] entries, int maxEntries) {
        if (entries == null || entries.length == 0) {
            return "count=0 strongest=none";
        }
        WifiEntry[] copy = Arrays.copyOf(entries, entries.length);
        Arrays.sort(copy, new Comparator<WifiEntry>() {
            @Override
            public int compare(WifiEntry left, WifiEntry right) {
                if (left == null && right == null) {
                    return 0;
                }
                if (left == null) {
                    return 1;
                }
                if (right == null) {
                    return -1;
                }
                return Integer.compare(right.rssi, left.rssi);
            }
        });

        int count = 0;
        for (WifiEntry entry : copy) {
            if (entry != null) {
                count++;
            }
        }
        if (count == 0) {
            return "count=0 strongest=none";
        }

        int limit = Math.max(0, Math.min(maxEntries, count));
        StringBuilder builder = new StringBuilder();
        builder.append("count=").append(count).append(" strongest=");
        if (limit == 0) {
            builder.append("none");
            return builder.toString();
        }
        int appended = 0;
        for (WifiEntry entry : copy) {
            if (entry == null) {
                continue;
            }
            if (appended > 0) {
                builder.append(';');
            }
            builder.append(safe(entry.ssid).replace("\"", "'"))
                    .append('/').append(safe(entry.bssid))
                    .append('@').append(entry.rssi).append("dBm");
            if (entry.frequency > 0) {
                builder.append('/').append(entry.frequency).append("MHz");
            }
            appended++;
            if (appended >= limit) {
                break;
            }
        }
        return builder.toString();
    }

    public static String wifiFreshness(int resultCount, long newestAgeMs) {
        if (resultCount <= 0) {
            return "empty";
        }
        if (newestAgeMs < 0L) {
            return "unknown";
        }
        if (newestAgeMs <= WIFI_FRESH_MAX_AGE_MS) {
            return "fresh";
        }
        if (newestAgeMs <= WIFI_CACHED_MAX_AGE_MS) {
            return "cached";
        }
        return "stale";
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    public static final class WifiEntry {
        public final String ssid;
        public final String bssid;
        public final int rssi;
        public final int frequency;

        public WifiEntry(String ssid, String bssid, int rssi, int frequency) {
            this.ssid = ssid;
            this.bssid = bssid;
            this.rssi = rssi;
            this.frequency = frequency;
        }
    }
}
