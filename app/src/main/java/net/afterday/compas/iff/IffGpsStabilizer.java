package net.afterday.compas.iff;

public final class IffGpsStabilizer {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final int MAX_SHORT_JUMP_M = 500;
    private static final int MIN_SPEED_CHECK_JUMP_M = 25;
    private static final float MAX_WALKING_SPEED_MPS = 12.0f;
    private static final int RECOVERY_JUMP_CONFIRMATIONS = 3;
    private static final int RECOVERY_CLUSTER_M = 100;
    private static final float RECOVERY_MAX_ACCURACY_M = 80.0f;
    private static final long SHORT_JUMP_WINDOW_MS = 10L * 60L * 1000L;

    private boolean hasAccepted;
    private double acceptedLat;
    private double acceptedLon;
    private long acceptedTimeMs;
    private boolean hasPendingJump;
    private double pendingJumpLat;
    private double pendingJumpLon;
    private int pendingJumpCount;

    public Decision evaluate(
            double lat,
            double lon,
            boolean hasAccuracy,
            float accuracyM,
            long timeMs,
            String provider) {
        if (!validCoordinate(lat, lon) || timeMs <= 0L) {
            return new Decision(false, "rejected_invalid", -1);
        }
        if (!hasAccepted) {
            accept(lat, lon, timeMs);
            return new Decision(true, "accepted_first", 0);
        }
        int jumpDistanceM = (int) Math.round(distanceMeters(acceptedLat, acceptedLon, lat, lon));
        long deltaMs = Math.abs(timeMs - acceptedTimeMs);
        if (jumpDistanceM > MIN_SPEED_CHECK_JUMP_M && deltaMs > 0L && deltaMs <= SHORT_JUMP_WINDOW_MS) {
            float speedMps = jumpDistanceM / (deltaMs / 1000.0f);
            if (speedMps > MAX_WALKING_SPEED_MPS) {
                return new Decision(false, "rejected_speed", jumpDistanceM);
            }
        }
        if (jumpDistanceM > MAX_SHORT_JUMP_M && deltaMs <= SHORT_JUMP_WINDOW_MS) {
            if (rememberRepeatedJump(lat, lon, hasAccuracy, accuracyM)) {
                accept(lat, lon, timeMs);
                return new Decision(true, "accepted_repeated_jump", jumpDistanceM);
            }
            return new Decision(false, "rejected_jump", jumpDistanceM);
        }
        accept(lat, lon, timeMs);
        return new Decision(true, "accepted", jumpDistanceM);
    }

    private void accept(double lat, double lon, long timeMs) {
        hasAccepted = true;
        acceptedLat = lat;
        acceptedLon = lon;
        acceptedTimeMs = timeMs;
        hasPendingJump = false;
        pendingJumpCount = 0;
    }

    private boolean rememberRepeatedJump(
            double lat,
            double lon,
            boolean hasAccuracy,
            float accuracyM) {
        if (hasAccuracy && accuracyM > RECOVERY_MAX_ACCURACY_M) {
            return false;
        }
        if (!hasPendingJump || distanceMeters(pendingJumpLat, pendingJumpLon, lat, lon) > RECOVERY_CLUSTER_M) {
            hasPendingJump = true;
            pendingJumpLat = lat;
            pendingJumpLon = lon;
            pendingJumpCount = 1;
            return false;
        }
        pendingJumpLat = lat;
        pendingJumpLon = lon;
        pendingJumpCount++;
        return pendingJumpCount >= RECOVERY_JUMP_CONFIRMATIONS;
    }

    private static boolean validCoordinate(double lat, double lon) {
        return !Double.isNaN(lat)
                && !Double.isNaN(lon)
                && lat >= -90.0
                && lat <= 90.0
                && lon >= -180.0
                && lon <= 180.0;
    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_M * c;
    }

    public static final class Decision {
        public final boolean accepted;
        public final String reason;
        public final int jumpDistanceM;

        Decision(boolean accepted, String reason, int jumpDistanceM) {
            this.accepted = accepted;
            this.reason = reason;
            this.jumpDistanceM = jumpDistanceM;
        }
    }
}
