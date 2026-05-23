package net.afterday.compas.iff;

public final class IffGpsSnapshot {
    private static final double EARTH_RADIUS_M = 6371000.0;
    private static final long STALE_AGE_MS = 15000L;
    private static final int NEARBY_RADIO_GPS_OUTLIER_M = 500;

    public final String status;
    public final int accuracyM;
    public final int distanceM;
    public final int bearingDeg;

    private IffGpsSnapshot(String status, int accuracyM, int distanceM, int bearingDeg) {
        this.status = safe(status);
        this.accuracyM = accuracyM;
        this.distanceM = distanceM;
        this.bearingDeg = bearingDeg;
    }

    public static IffGpsSnapshot unavailable() {
        return new IffGpsSnapshot("GPS_UNAVAILABLE", -1, -1, -1);
    }

    public static IffGpsSnapshot from(
            long ageMs,
            boolean hasAccuracy,
            float accuracyM,
            boolean hasBearing,
            float bearingDeg) {
        if (ageMs < 0L) {
            return unavailable();
        }
        String status;
        if (ageMs > STALE_AGE_MS) {
            status = "GPS_STALE";
        } else if (!hasAccuracy) {
            status = "GPS_WEAK";
        } else if (accuracyM <= 25.0f) {
            status = "GPS_OK";
        } else if (accuracyM <= 75.0f) {
            status = "GPS_WEAK";
        } else {
            status = "GPS_WEAK";
        }
        return new IffGpsSnapshot(
                status,
                hasAccuracy ? Math.round(accuracyM) : -1,
                -1,
                hasBearing ? Math.round(bearingDeg) : -1);
    }

    public static IffGpsSnapshot fromPair(
            long localAgeMs,
            boolean localHasAccuracy,
            float localAccuracyM,
            double localLat,
            double localLon,
            long remoteAgeMs,
            boolean remoteHasAccuracy,
            float remoteAccuracyM,
            double remoteLat,
            double remoteLon) {
        if (localAgeMs < 0L || remoteAgeMs < 0L
                || !validCoordinate(localLat, localLon)
                || !validCoordinate(remoteLat, remoteLon)) {
            return unavailable();
        }
        int combinedAccuracyM = combinedAccuracy(
                localHasAccuracy,
                localAccuracyM,
                remoteHasAccuracy,
                remoteAccuracyM);
        if (localAgeMs > STALE_AGE_MS || remoteAgeMs > STALE_AGE_MS) {
            return new IffGpsSnapshot("GPS_STALE", combinedAccuracyM, -1, -1);
        }
        String status = "GPS_OK";
        if (!localHasAccuracy || !remoteHasAccuracy
                || localAccuracyM > 25.0f
                || remoteAccuracyM > 25.0f) {
            status = "GPS_WEAK";
        }
        return new IffGpsSnapshot(
                status,
                combinedAccuracyM,
                (int) Math.round(distanceMeters(localLat, localLon, remoteLat, remoteLon)),
                (int) Math.round(bearingDegrees(localLat, localLon, remoteLat, remoteLon)));
    }

    public static IffGpsSnapshot fromPair(
            long localAgeMs,
            boolean localHasAccuracy,
            float localAccuracyM,
            double localLat,
            double localLon,
            long remoteAgeMs,
            boolean remoteHasAccuracy,
            float remoteAccuracyM,
            double remoteLat,
            double remoteLon,
            String radioDistanceClass) {
        IffGpsSnapshot snapshot = fromPair(
                localAgeMs,
                localHasAccuracy,
                localAccuracyM,
                localLat,
                localLon,
                remoteAgeMs,
                remoteHasAccuracy,
                remoteAccuracyM,
                remoteLat,
                remoteLon);
        if (snapshot.distanceM > NEARBY_RADIO_GPS_OUTLIER_M && nearbyRadioClass(radioDistanceClass)) {
            return new IffGpsSnapshot("GPS_OUTLIER", snapshot.accuracyM, -1, -1);
        }
        return snapshot;
    }

    public String fieldValue(int value) {
        return value < 0 ? "na" : String.valueOf(value);
    }

    public String compact() {
        String text = status + " acc=" + fieldValue(accuracyM) + "m";
        if (distanceM >= 0) {
            text += " dist=" + distanceM + "m";
        }
        if (bearingDeg >= 0) {
            text += " brg=" + bearingDeg + "deg";
        }
        return text;
    }

    private static int combinedAccuracy(
            boolean localHasAccuracy,
            float localAccuracyM,
            boolean remoteHasAccuracy,
            float remoteAccuracyM) {
        if (!localHasAccuracy && !remoteHasAccuracy) {
            return -1;
        }
        if (!localHasAccuracy) {
            return Math.round(remoteAccuracyM);
        }
        if (!remoteHasAccuracy) {
            return Math.round(localAccuracyM);
        }
        return Math.round(Math.max(localAccuracyM, remoteAccuracyM));
    }

    private static boolean validCoordinate(double lat, double lon) {
        return !Double.isNaN(lat)
                && !Double.isNaN(lon)
                && lat >= -90.0
                && lat <= 90.0
                && lon >= -180.0
                && lon <= 180.0;
    }

    private static boolean nearbyRadioClass(String radioDistanceClass) {
        String value = safe(radioDistanceClass);
        return "VERY_NEAR".equals(value) || "NEAR".equals(value) || "MID".equals(value);
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

    private static double bearingDegrees(double lat1, double lon1, double lat2, double lon2) {
        double rLat1 = Math.toRadians(lat1);
        double rLat2 = Math.toRadians(lat2);
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(rLat2);
        double x = Math.cos(rLat1) * Math.sin(rLat2)
                - Math.sin(rLat1) * Math.cos(rLat2) * Math.cos(dLon);
        double degrees = Math.toDegrees(Math.atan2(y, x));
        if (degrees < 0.0) {
            degrees += 360.0;
        }
        if (degrees >= 360.0) {
            degrees -= 360.0;
        }
        return degrees;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
