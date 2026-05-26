package net.afterday.compas.iff;

public final class IffWifiTargetObservationStore {
    public static final String LEFT_ANCHOR_PLAYER_ID = "vasya";
    public static final String RIGHT_ANCHOR_PLAYER_ID = "zhenya";
    public static final String TARGET_PLAYER_ID = "petya";
    private static final long LOCATOR_FRESH_MS = 10000L;
    private static final long DISPLAY_FRESH_MS = 30000L;
    private static final int MAX_OBSERVATIONS_PER_ANCHOR = 8;
    private static final Object LOCK = new Object();

    private static final Observation[] left = new Observation[MAX_OBSERVATIONS_PER_ANCHOR];
    private static final Observation[] right = new Observation[MAX_OBSERVATIONS_PER_ANCHOR];
    private static int leftNextIndex;
    private static int rightNextIndex;

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
        AnchorStats leftStats;
        AnchorStats rightStats;
        synchronized (LOCK) {
            leftStats = stats(left, nowMs);
            rightStats = stats(right, nowMs);
        }
        return IffWifiTargetLocator.estimate(
                leftStats.rssi,
                leftStats.count,
                rightStats.rssi,
                rightStats.count);
    }

    public static String compactStatus() {
        return compactStatus(System.currentTimeMillis());
    }

    public static String compactStatus(long nowMs) {
        Observation leftCopy;
        Observation rightCopy;
        IffWifiTargetLocator.Snapshot locator;
        synchronized (LOCK) {
            leftCopy = latest(left);
            rightCopy = latest(right);
            locator = IffWifiTargetLocator.estimate(
                    stats(left, nowMs).rssi,
                    stats(left, nowMs).count,
                    stats(right, nowMs).rssi,
                    stats(right, nowMs).count);
        }
        return "target=" + TARGET_PLAYER_ID
                + " left=" + compactAnchor(LEFT_ANCHOR_PLAYER_ID, leftCopy, nowMs)
                + " right=" + compactAnchor(RIGHT_ANCHOR_PLAYER_ID, rightCopy, nowMs)
                + " " + compactLocator(locator);
    }

    public static void resetForTest() {
        synchronized (LOCK) {
            clear(left);
            clear(right);
            leftNextIndex = 0;
            rightNextIndex = 0;
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
                leftNextIndex = store(left, leftNextIndex, observation);
            } else if (RIGHT_ANCHOR_PLAYER_ID.equals(anchorPlayerId)) {
                rightNextIndex = store(right, rightNextIndex, observation);
            }
        }
    }

    private static int store(Observation[] observations, int nextIndex, Observation observation) {
        observations[nextIndex] = observation;
        return (nextIndex + 1) % observations.length;
    }

    private static void clear(Observation[] observations) {
        for (int i = 0; i < observations.length; i++) {
            observations[i] = null;
        }
    }

    private static AnchorStats stats(Observation[] observations, long nowMs) {
        int count = 0;
        int total = 0;
        for (int i = 0; i < observations.length; i++) {
            Observation observation = observations[i];
            if (observation != null && nowMs - observation.elapsedMs <= LOCATOR_FRESH_MS) {
                count++;
                total += observation.rssi;
            }
        }
        if (count == 0) {
            return new AnchorStats(0, 0);
        }
        return new AnchorStats(Math.round(total / (float) count), count);
    }

    private static Observation latest(Observation[] observations) {
        Observation best = null;
        for (int i = 0; i < observations.length; i++) {
            Observation observation = observations[i];
            if (observation != null && (best == null || observation.elapsedMs >= best.elapsedMs)) {
                best = observation;
            }
        }
        return best;
    }

    private static String compactAnchor(String anchorPlayerId, Observation observation, long nowMs) {
        if (observation == null) {
            return anchorPlayerId + ":missing";
        }
        long ageMs = Math.max(0L, nowMs - observation.elapsedMs);
        if (ageMs > DISPLAY_FRESH_MS) {
            return anchorPlayerId + ":stale ageMs=" + ageMs;
        }
        if (ageMs > LOCATOR_FRESH_MS) {
            return anchorPlayerId + ":old rssi=" + observation.rssi + " ageMs=" + ageMs;
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

    private static final class AnchorStats {
        final int rssi;
        final int count;

        AnchorStats(int rssi, int count) {
            this.rssi = rssi;
            this.count = count;
        }
    }
}
