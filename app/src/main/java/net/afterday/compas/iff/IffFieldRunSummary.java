package net.afterday.compas.iff;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

public final class IffFieldRunSummary {
    private static final int MAX_RECENT = 5;
    private static final String[] OFFICE_ORDER = new String[] {
            "CLOSER_TO_B",
            "CLOSER_TO_A",
            "BETWEEN_OR_AMBIGUOUS",
            "ONLY_A_VISIBLE",
            "ONLY_B_VISIBLE",
            "INSUFFICIENT_DATA"
    };
    private static final String[] DISTANCE_ORDER = new String[] {
            "VERY_NEAR",
            "NEAR",
            "MID",
            "FAR",
            "EDGE",
            "LOST"
    };
    private static final String[] MOVEMENT_ORDER = new String[] {
            "APPROACHING",
            "LEAVING",
            "STABLE",
            "UNKNOWN"
    };
    private static final String[] GPS_ORDER = new String[] {
            "GPS_OK",
            "GPS_WEAK",
            "GPS_OUTLIER",
            "GPS_STALE",
            "GPS_UNAVAILABLE"
    };

    private static String localPlayerId = "";
    private static int checkCount;
    private static final Map<String, Integer> officeCounts = new LinkedHashMap<String, Integer>();
    private static final Map<String, Integer> distanceCounts = new LinkedHashMap<String, Integer>();
    private static final Map<String, Integer> movementCounts = new LinkedHashMap<String, Integer>();
    private static final Map<String, Integer> gpsCounts = new LinkedHashMap<String, Integer>();
    private static final Deque<String> recent = new ArrayDeque<String>();

    private IffFieldRunSummary() {
    }

    public static synchronized void reset(String nextLocalPlayerId) {
        localPlayerId = safe(nextLocalPlayerId);
        checkCount = 0;
        officeCounts.clear();
        distanceCounts.clear();
        movementCounts.clear();
        gpsCounts.clear();
        recent.clear();
    }

    public static synchronized void record(Check check) {
        if (check == null) {
            return;
        }
        if (!safe(check.localPlayerId).equals(localPlayerId)) {
            localPlayerId = safe(check.localPlayerId);
        }
        checkCount++;
        increment(officeCounts, check.officeVerdict);
        increment(distanceCounts, check.distanceClass);
        increment(movementCounts, check.movementTrend);
        increment(gpsCounts, check.gpsStatus);
        recent.addFirst(check.compact());
        while (recent.size() > MAX_RECENT) {
            recent.removeLast();
        }
    }

    public static synchronized String compact() {
        if (checkCount <= 0) {
            return "checks=0";
        }
        return "checks=" + checkCount
                + " office=" + orderedCounts(officeCounts, OFFICE_ORDER)
                + " distance=" + orderedCounts(distanceCounts, DISTANCE_ORDER)
                + " movement=" + orderedCounts(movementCounts, MOVEMENT_ORDER)
                + " gps=" + orderedCounts(gpsCounts, GPS_ORDER);
    }

    public static synchronized String details() {
        StringBuilder builder = new StringBuilder();
        builder.append("RUN SUMMARY\n")
                .append("- local: ").append(localPlayerId.length() == 0 ? "unknown" : localPlayerId).append("\n")
                .append("- ").append(compact()).append("\n");
        if (!recent.isEmpty()) {
            builder.append("- recent:\n");
            for (String line : recent) {
                builder.append("  ").append(line).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private static void increment(Map<String, Integer> counts, String key) {
        String safeKey = safe(key);
        if (safeKey.length() == 0) {
            safeKey = "UNKNOWN";
        }
        Integer current = counts.get(safeKey);
        counts.put(safeKey, current == null ? 1 : current + 1);
    }

    private static String orderedCounts(Map<String, Integer> counts, String[] order) {
        StringBuilder builder = new StringBuilder();
        for (String key : order) {
            appendCount(builder, key, counts.get(key));
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!contains(order, entry.getKey())) {
                appendCount(builder, entry.getKey(), entry.getValue());
            }
        }
        return builder.length() == 0 ? "none" : builder.toString();
    }

    private static void appendCount(StringBuilder builder, String key, Integer count) {
        if (count == null || count <= 0) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(' ');
        }
        builder.append(key).append(':').append(count);
    }

    private static boolean contains(String[] values, String value) {
        for (String candidate : values) {
            if (candidate.equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    public static final class Check {
        final String localPlayerId;
        final String officeRole;
        final String officeVerdict;
        final String distanceClass;
        final String movementTrend;
        final String gpsStatus;
        final String wifiFreshness;

        public Check(
                String localPlayerId,
                String officeRole,
                String officeVerdict,
                String distanceClass,
                String movementTrend,
                String gpsStatus,
                String wifiFreshness) {
            this.localPlayerId = safe(localPlayerId);
            this.officeRole = safe(officeRole);
            this.officeVerdict = safe(officeVerdict);
            this.distanceClass = safe(distanceClass);
            this.movementTrend = safe(movementTrend);
            this.gpsStatus = safe(gpsStatus);
            this.wifiFreshness = safe(wifiFreshness);
        }

        String compact() {
            return officeRole
                    + " office=" + officeVerdict
                    + " distance=" + distanceClass
                    + " movement=" + movementTrend
                    + " gps=" + gpsStatus
                    + " wifi=" + wifiFreshness;
        }
    }
}
