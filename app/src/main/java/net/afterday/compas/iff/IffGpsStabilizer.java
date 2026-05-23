package net.afterday.compas.iff;

public final class IffGpsStabilizer {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final int MAX_SHORT_JUMP_M = 500;
    private static final long SHORT_JUMP_WINDOW_MS = 10L * 60L * 1000L;

    private boolean hasAccepted;
    private double acceptedLat;
    private double acceptedLon;
    private long acceptedTimeMs;

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
        if (jumpDistanceM > MAX_SHORT_JUMP_M && deltaMs <= SHORT_JUMP_WINDOW_MS) {
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
