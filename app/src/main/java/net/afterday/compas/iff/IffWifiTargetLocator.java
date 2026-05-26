package net.afterday.compas.iff;

public final class IffWifiTargetLocator {
    private static final int MIN_SAMPLES_PER_ANCHOR = 1;
    private static final int SHADOWED_ANCHOR_RSSI_DBM = -88;
    private static final int SHADOWED_ANCHOR_DELTA_DB = 16;

    private IffWifiTargetLocator() {
    }

    public static Snapshot estimate(int leftRssi, int leftCount, int rightRssi, int rightCount) {
        if (leftCount < MIN_SAMPLES_PER_ANCHOR || rightCount < MIN_SAMPLES_PER_ANCHOR) {
            return new Snapshot("INSUFFICIENT_DATA", -1, "na", 0, "LOW",
                    leftRssi, rightRssi, 0);
        }
        int deltaRightMinusLeft = rightRssi - leftRssi;
        if (Math.min(leftRssi, rightRssi) <= SHADOWED_ANCHOR_RSSI_DBM
                && Math.abs(deltaRightMinusLeft) >= SHADOWED_ANCHOR_DELTA_DB) {
            return new Snapshot("INSUFFICIENT_DATA", -1, "na", deltaRightMinusLeft, "LOW",
                    leftRssi, rightRssi, Math.round((leftRssi + rightRssi) / 2.0f));
        }
        int meanRssi = Math.round((leftRssi + rightRssi) / 2.0f);
        String clock = clockDirection(deltaRightMinusLeft);
        int distanceBucketM = distanceBucket(meanRssi);
        String confidence = confidence(leftCount, rightCount, Math.abs(deltaRightMinusLeft));
        return new Snapshot("OK", distanceBucketM, clock, deltaRightMinusLeft, confidence,
                leftRssi, rightRssi, meanRssi);
    }

    public static String compact(Snapshot snapshot) {
        if (snapshot == null || !"OK".equals(snapshot.status)) {
            return "wifiTarget=INSUFFICIENT_DATA";
        }
        return "wifiTarget=" + snapshot.distanceBucketM + "m"
                + " clock=" + snapshot.clockDirection
                + " deltaDb=" + snapshot.deltaRightMinusLeftDb
                + " confidence=" + snapshot.confidence
                + " left=" + snapshot.leftRssi
                + " right=" + snapshot.rightRssi;
    }

    private static String clockDirection(int deltaRightMinusLeft) {
        if (deltaRightMinusLeft >= 15) {
            return "3";
        }
        if (deltaRightMinusLeft >= 9) {
            return "2";
        }
        if (deltaRightMinusLeft >= 4) {
            return "1";
        }
        if (deltaRightMinusLeft <= -15) {
            return "9";
        }
        if (deltaRightMinusLeft <= -9) {
            return "10";
        }
        if (deltaRightMinusLeft <= -4) {
            return "11";
        }
        return "12";
    }

    private static int distanceBucket(int meanRssi) {
        if (meanRssi >= -50) {
            return 5;
        }
        if (meanRssi >= -58) {
            return 10;
        }
        if (meanRssi >= -65) {
            return 15;
        }
        if (meanRssi >= -71) {
            return 20;
        }
        return 25;
    }

    private static String confidence(int leftCount, int rightCount, int absDeltaDb) {
        int minCount = Math.min(leftCount, rightCount);
        if (minCount >= 3 && absDeltaDb >= 4) {
            return "HIGH";
        }
        if (minCount >= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }

    public static final class Snapshot {
        public final String status;
        public final int distanceBucketM;
        public final String clockDirection;
        public final int deltaRightMinusLeftDb;
        public final String confidence;
        public final int leftRssi;
        public final int rightRssi;
        public final int meanRssi;

        private Snapshot(
                String status,
                int distanceBucketM,
                String clockDirection,
                int deltaRightMinusLeftDb,
                String confidence,
                int leftRssi,
                int rightRssi,
                int meanRssi) {
            this.status = status;
            this.distanceBucketM = distanceBucketM;
            this.clockDirection = clockDirection;
            this.deltaRightMinusLeftDb = deltaRightMinusLeftDb;
            this.confidence = confidence;
            this.leftRssi = leftRssi;
            this.rightRssi = rightRssi;
            this.meanRssi = meanRssi;
        }
    }
}
