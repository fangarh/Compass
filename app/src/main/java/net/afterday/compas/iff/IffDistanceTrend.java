package net.afterday.compas.iff;

public final class IffDistanceTrend {
    private static final int MIN_DISTANCE_SAMPLES = 2;
    private static final int MIN_TREND_SAMPLES = 2;
    private static final int TREND_DELTA_DB = 4;

    private IffDistanceTrend() {
    }

    public static Snapshot evaluate(Sample current, Sample previous) {
        if (current == null || !current.fresh || current.validCount < MIN_DISTANCE_SAMPLES) {
            return new Snapshot("LOST", 0, "UNKNOWN", 0, 0,
                    "no usable fresh RSSI window");
        }

        String distanceClass = distanceClass(current.averageRssi);
        int distanceConfidence = distanceConfidence(distanceClass, current);
        Trend trend = movementTrend(current, previous);
        return new Snapshot(distanceClass, distanceConfidence,
                trend.label, trend.confidence, trend.deltaDb,
                trend.reason);
    }

    private static String distanceClass(int rssi) {
        if (rssi >= -45) {
            return "VERY_NEAR";
        }
        if (rssi >= -58) {
            return "NEAR";
        }
        if (rssi >= -70) {
            return "MID";
        }
        if (rssi >= -84) {
            return "FAR";
        }
        return "EDGE";
    }

    private static int distanceConfidence(String distanceClass, Sample current) {
        int base;
        if ("VERY_NEAR".equals(distanceClass)) {
            base = 90;
        } else if ("NEAR".equals(distanceClass)) {
            base = 78;
        } else if ("MID".equals(distanceClass)) {
            base = 62;
        } else if ("FAR".equals(distanceClass)) {
            base = 45;
        } else if ("EDGE".equals(distanceClass)) {
            base = 30;
        } else {
            base = 0;
        }
        if (current.validCount < 4) {
            base -= 10;
        }
        if (current.outlier127Count > current.validCount) {
            base -= 10;
        }
        return clamp(base);
    }

    private static Trend movementTrend(Sample current, Sample previous) {
        if (previous == null
                || !previous.fresh
                || previous.validCount < MIN_TREND_SAMPLES
                || current.validCount < MIN_TREND_SAMPLES) {
            return new Trend("UNKNOWN", 0, 0, "not enough consecutive RSSI windows");
        }
        int deltaDb = current.averageRssi - previous.averageRssi;
        if (deltaDb >= TREND_DELTA_DB) {
            return new Trend("APPROACHING", trendConfidence(deltaDb), deltaDb,
                    "RSSI strengthened across windows");
        }
        if (deltaDb <= -TREND_DELTA_DB) {
            return new Trend("LEAVING", trendConfidence(-deltaDb), deltaDb,
                    "RSSI weakened across windows");
        }
        return new Trend("STABLE", 35, deltaDb, "RSSI delta below trend threshold");
    }

    private static int trendConfidence(int absDeltaDb) {
        if (absDeltaDb >= 12) {
            return 75;
        }
        if (absDeltaDb >= 8) {
            return 65;
        }
        return 50;
    }

    private static int clamp(int value) {
        if (value < 0) {
            return 0;
        }
        if (value > 100) {
            return 100;
        }
        return value;
    }

    private static final class Trend {
        final String label;
        final int confidence;
        final int deltaDb;
        final String reason;

        Trend(String label, int confidence, int deltaDb, String reason) {
            this.label = label;
            this.confidence = confidence;
            this.deltaDb = deltaDb;
            this.reason = reason;
        }
    }

    public static final class Sample {
        public final boolean fresh;
        public final int averageRssi;
        public final int validCount;
        public final int outlier127Count;
        public final long newestAgeMs;

        private Sample(boolean fresh, int averageRssi, int validCount,
                       int outlier127Count, long newestAgeMs) {
            this.fresh = fresh;
            this.averageRssi = averageRssi;
            this.validCount = validCount;
            this.outlier127Count = outlier127Count;
            this.newestAgeMs = newestAgeMs;
        }

        public static Sample window(boolean fresh, int averageRssi, int validCount,
                                    int outlier127Count, long newestAgeMs) {
            return new Sample(fresh, averageRssi, validCount, outlier127Count, newestAgeMs);
        }
    }

    public static final class Snapshot {
        public final String distanceClass;
        public final int distanceConfidence;
        public final String movementTrend;
        public final int movementConfidence;
        public final int movementRssiDeltaDb;
        public final String reason;

        Snapshot(String distanceClass, int distanceConfidence, String movementTrend,
                 int movementConfidence, int movementRssiDeltaDb, String reason) {
            this.distanceClass = distanceClass;
            this.distanceConfidence = distanceConfidence;
            this.movementTrend = movementTrend;
            this.movementConfidence = movementConfidence;
            this.movementRssiDeltaDb = movementRssiDeltaDb;
            this.reason = reason;
        }

        public String compact() {
            return distanceClass + " " + distanceConfidence + "% / "
                    + movementTrend + " " + movementConfidence + "%";
        }
    }
}
