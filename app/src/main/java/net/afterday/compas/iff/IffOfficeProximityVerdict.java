package net.afterday.compas.iff;

public final class IffOfficeProximityVerdict {
    public static final String MOVING_TARGET_PLAYER_ID = "petya";
    public static final long WINDOW_MS = 15000L;
    public static final int CLEAR_DELTA_DB = 8;
    public static final int MIN_VALID_SAMPLES = 3;
    public static final int RSSI_OUTLIER_127 = 127;

    private IffOfficeProximityVerdict() {
    }

    public static Snapshot evaluate(String localPlayerId, Sample sideA, Sample sideB) {
        return evaluate(localPlayerId, sideA, sideB, Calibration.none());
    }

    public static Snapshot evaluate(String localPlayerId, Sample sideA, Sample sideB,
                                    Calibration calibration) {
        if (!MOVING_TARGET_PLAYER_ID.equals(localPlayerId)) {
            return new Snapshot("INSUFFICIENT_DATA", 0,
                    "local device is not PHONE_C_MOVING_TARGET");
        }
        if (!isUsable(sideA) || !isUsable(sideB)) {
            return new Snapshot("INSUFFICIENT_DATA", 0,
                    "fresh A and B BLE windows need at least " + MIN_VALID_SAMPLES
                            + " valid samples each; rssi=127 is ignored");
        }

        int deltaDb = calibratedDeltaDb(sideA, sideB, calibration);
        if (deltaDb >= CLEAR_DELTA_DB) {
            return new Snapshot("CLOSER_TO_A", deltaDb,
                    reason("C hears A stronger than B by " + deltaDb + "dB", calibration));
        }
        if (deltaDb <= -CLEAR_DELTA_DB) {
            return new Snapshot("CLOSER_TO_B", deltaDb,
                    reason("C hears B stronger than A by " + (-deltaDb) + "dB", calibration));
        }
        return new Snapshot("BETWEEN_OR_AMBIGUOUS", deltaDb,
                reason("A/B RSSI delta is below " + CLEAR_DELTA_DB + "dB", calibration));
    }

    private static int calibratedDeltaDb(Sample sideA, Sample sideB, Calibration calibration) {
        if (calibration == null || !calibration.valid) {
            return sideA.rssi - sideB.rssi;
        }
        int sideAGain = sideA.rssi - calibration.sideABaselineRssi;
        int sideBGain = sideB.rssi - calibration.sideBBaselineRssi;
        return sideAGain - sideBGain;
    }

    private static String reason(String base, Calibration calibration) {
        if (calibration == null || !calibration.valid) {
            return base;
        }
        return base + " after pair calibration";
    }

    private static boolean isUsable(Sample sample) {
        return sample != null
                && sample.fresh
                && sample.rssi != RSSI_OUTLIER_127
                && sample.validCount >= MIN_VALID_SAMPLES;
    }

    public static final class Sample {
        public final boolean fresh;
        public final int rssi;
        public final int validCount;
        public final int outlier127Count;
        public final long newestAgeMs;

        private Sample(boolean fresh, int rssi, int validCount, int outlier127Count, long newestAgeMs) {
            this.fresh = fresh;
            this.rssi = rssi;
            this.validCount = validCount;
            this.outlier127Count = outlier127Count;
            this.newestAgeMs = newestAgeMs;
        }

        public static Sample fresh(int rssi) {
            return new Sample(true, rssi, 1, rssi == RSSI_OUTLIER_127 ? 1 : 0, 0L);
        }

        public static Sample stale(int rssi) {
            return new Sample(false, rssi, 1, rssi == RSSI_OUTLIER_127 ? 1 : 0, 0L);
        }

        public static Sample window(boolean fresh, int averageRssi, int validCount,
                                    int outlier127Count, long newestAgeMs) {
            return new Sample(fresh, averageRssi, validCount, outlier127Count, newestAgeMs);
        }
    }

    public static final class Snapshot {
        public final String label;
        public final int deltaDb;
        public final String reason;

        Snapshot(String label, int deltaDb, String reason) {
            this.label = label;
            this.deltaDb = deltaDb;
            this.reason = reason;
        }

        public String compact() {
            return label + " delta=" + deltaDb + "dB";
        }
    }

    public static final class Calibration {
        public final boolean valid;
        public final int sideABaselineRssi;
        public final int sideBBaselineRssi;

        private Calibration(boolean valid, int sideABaselineRssi, int sideBBaselineRssi) {
            this.valid = valid;
            this.sideABaselineRssi = sideABaselineRssi;
            this.sideBBaselineRssi = sideBBaselineRssi;
        }

        public static Calibration none() {
            return new Calibration(false, 0, 0);
        }

        public static Calibration fixed(int sideABaselineRssi, int sideBBaselineRssi) {
            return new Calibration(true, sideABaselineRssi, sideBBaselineRssi);
        }
    }
}
