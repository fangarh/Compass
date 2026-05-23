package net.afterday.compas.iff;

public final class IffWifiTargetObservationStore {
    public static final String LEFT_ANCHOR_PLAYER_ID = "vasya";
    public static final String RIGHT_ANCHOR_PLAYER_ID = "petya";
    public static final String TARGET_PLAYER_ID = "zhenya";
    private static final long FRESH_MS = 30000L;
    private static final Object LOCK = new Object();

    private static Observation left;
    private static Observation right;

    private IffWifiTargetObservationStore() {
    }

    public static void updateLocalObservation(String anchorPlayerId, String targetPlayerId, int rssi) {
        updateLocalObservation(anchorPlayerId, targetPlayerId, rssi, System.currentTimeMillis());
    }

    public static void updateLocalObservation(
            String anchorPlayerId,
            String targetPlayerId,
            int rssi,
            long elapsedMs) {
        update(anchorPlayerId, targetPlayerId, rssi, elapsedMs);
    }

    public static void updateRemoteObservation(String anchorPlayerId, String targetPlayerId, int rssi) {
        updateRemoteObservation(anchorPlayerId, targetPlayerId, rssi, System.currentTimeMillis());
    }

    public static void updateRemoteObservation(
            String anchorPlayerId,
            String targetPlayerId,
            int rssi,
            long elapsedMs) {
        update(anchorPlayerId, targetPlayerId, rssi, elapsedMs);
    }

    public static IffWifiTargetLocator.Snapshot snapshot() {
        return snapshot(System.currentTimeMillis());
    }

    public static IffWifiTargetLocator.Snapshot snapshot(long nowMs) {
        Observation leftCopy;
        Observation rightCopy;
        synchronized (LOCK) {
            leftCopy = left;
            rightCopy = right;
        }
        boolean leftFresh = leftCopy != null && nowMs - leftCopy.elapsedMs <= FRESH_MS;
        boolean rightFresh = rightCopy != null && nowMs - rightCopy.elapsedMs <= FRESH_MS;
        return IffWifiTargetLocator.estimate(
                leftFresh ? leftCopy.rssi : 0,
                leftFresh ? 1 : 0,
                rightFresh ? rightCopy.rssi : 0,
                rightFresh ? 1 : 0);
    }

    public static String compactStatus() {
        return compactStatus(System.currentTimeMillis());
    }

    public static String compactStatus(long nowMs) {
        Observation leftCopy;
        Observation rightCopy;
        synchronized (LOCK) {
            leftCopy = left;
            rightCopy = right;
        }
        IffWifiTargetLocator.Snapshot locator = snapshot(nowMs);
        return "target=" + TARGET_PLAYER_ID
                + " left=" + compactAnchor(LEFT_ANCHOR_PLAYER_ID, leftCopy, nowMs)
                + " right=" + compactAnchor(RIGHT_ANCHOR_PLAYER_ID, rightCopy, nowMs)
                + " " + compactLocator(locator);
    }

    public static void resetForTest() {
        synchronized (LOCK) {
            left = null;
            right = null;
        }
    }

    private static void update(String anchorPlayerId, String targetPlayerId, int rssi, long elapsedMs) {
        if (!TARGET_PLAYER_ID.equals(targetPlayerId)) {
            return;
        }
        if (rssi >= 0) {
            return;
        }
        Observation observation = new Observation(anchorPlayerId, targetPlayerId, rssi, elapsedMs);
        synchronized (LOCK) {
            if (LEFT_ANCHOR_PLAYER_ID.equals(anchorPlayerId)) {
                left = observation;
            } else if (RIGHT_ANCHOR_PLAYER_ID.equals(anchorPlayerId)) {
                right = observation;
            }
        }
    }

    private static String compactAnchor(String anchorPlayerId, Observation observation, long nowMs) {
        if (observation == null) {
            return anchorPlayerId + ":missing";
        }
        long ageMs = Math.max(0L, nowMs - observation.elapsedMs);
        if (ageMs > FRESH_MS) {
            return anchorPlayerId + ":stale ageMs=" + ageMs;
        }
        return anchorPlayerId + ":" + observation.rssi + " ageMs=" + ageMs;
    }

    private static String compactLocator(IffWifiTargetLocator.Snapshot locator) {
        if (locator == null || !"OK".equals(locator.status)) {
            return "locator=INSUFFICIENT_DATA";
        }
        return "locator=" + locator.distanceBucketM + "m"
                + " clock=" + locator.clockDirection
                + " deltaDb=" + locator.deltaRightMinusLeftDb
                + " confidence=" + locator.confidence;
    }

    public static final class Observation {
        public final String anchorPlayerId;
        public final String targetPlayerId;
        public final int rssi;
        public final long elapsedMs;

        private Observation(String anchorPlayerId, String targetPlayerId, int rssi, long elapsedMs) {
            this.anchorPlayerId = anchorPlayerId;
            this.targetPlayerId = targetPlayerId;
            this.rssi = rssi;
            this.elapsedMs = elapsedMs;
        }
    }
}
