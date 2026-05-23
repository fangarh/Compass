package net.afterday.compas.iff;

public final class IffFieldLocatorSnapshot {
    public final String status;
    public final String source;
    public final int distanceBucketM;
    public final String clockDirection;
    public final int confidencePercent;
    public final int bearingDeg;
    public final String reason;

    private IffFieldLocatorSnapshot(
            String status,
            String source,
            int distanceBucketM,
            String clockDirection,
            int confidencePercent,
            int bearingDeg,
            String reason) {
        this.status = safe(status);
        this.source = safe(source);
        this.distanceBucketM = distanceBucketM;
        this.clockDirection = safe(clockDirection);
        this.confidencePercent = confidencePercent;
        this.bearingDeg = bearingDeg;
        this.reason = safe(reason);
    }

    public static IffFieldLocatorSnapshot from(
            IffWifiTargetLocator.Snapshot wifiTarget,
            IffDistanceTrend.Snapshot radioDistance,
            IffGpsSnapshot gps) {
        if (wifiTarget != null && "OK".equals(wifiTarget.status)) {
            return new IffFieldLocatorSnapshot(
                    "OK",
                    "WIFI_TARGET",
                    wifiTarget.distanceBucketM,
                    wifiTarget.clockDirection,
                    wifiConfidence(wifiTarget.confidence),
                    -1,
                    "two-anchor Wi-Fi RSSI delta "
                            + wifiTarget.deltaRightMinusLeftDb
                            + "dB mean "
                            + wifiTarget.meanRssi
                            + "dBm");
        }
        if (radioDistance != null && !"LOST".equals(radioDistance.distanceClass)) {
            return new IffFieldLocatorSnapshot(
                    "OK",
                    "FIELD_RADIO_RSSI",
                    radioDistanceBucket(radioDistance.distanceClass),
                    "na",
                    radioDistance.distanceConfidence,
                    -1,
                    "radio "
                            + radioDistance.distanceClass
                            + " "
                            + radioDistance.movementTrend
                            + " delta="
                            + radioDistance.movementRssiDeltaDb
                            + "dB");
        }
        if (gps != null
                && "GPS_OK".equals(gps.status)
                && gps.distanceM >= 0) {
            return new IffFieldLocatorSnapshot(
                    "OK",
                    "GPS_ASSISTED",
                    metricDistanceBucket(gps.distanceM),
                    "na",
                    gpsConfidence(gps),
                    gps.bearingDeg,
                    "gps " + gps.status + " accuracy=" + gps.fieldValue(gps.accuracyM) + "m");
        }
        return new IffFieldLocatorSnapshot(
                "INSUFFICIENT_DATA",
                "NONE",
                -1,
                "na",
                0,
                -1,
                "no usable Wi-Fi target, radio RSSI, or GPS pair"
                        + gpsDiagnostic(gps));
    }

    public String compact() {
        if (!"OK".equals(status)) {
            return "locator=INSUFFICIENT_DATA source=" + source + " reason=" + reason;
        }
        String text = "locator=" + source
                + " distance=" + distanceBucketM + "m"
                + " clock=" + clockDirection
                + " confidence=" + confidencePercent
                + " reason=" + reason;
        if (bearingDeg >= 0) {
            text += " bearingDeg=" + bearingDeg;
        }
        return text;
    }

    private static int wifiConfidence(String confidence) {
        if ("HIGH".equals(confidence)) {
            return 80;
        }
        if ("MEDIUM".equals(confidence)) {
            return 60;
        }
        if ("LOW".equals(confidence)) {
            return 40;
        }
        return 0;
    }

    private static int radioDistanceBucket(String distanceClass) {
        if ("VERY_NEAR".equals(distanceClass)) {
            return 5;
        }
        if ("NEAR".equals(distanceClass)) {
            return 10;
        }
        if ("MID".equals(distanceClass)) {
            return 15;
        }
        if ("FAR".equals(distanceClass)) {
            return 25;
        }
        if ("EDGE".equals(distanceClass)) {
            return 25;
        }
        return -1;
    }

    private static int metricDistanceBucket(int distanceM) {
        if (distanceM <= 7) {
            return 5;
        }
        if (distanceM <= 12) {
            return 10;
        }
        if (distanceM <= 17) {
            return 15;
        }
        if (distanceM <= 22) {
            return 20;
        }
        int rounded = Math.round(distanceM / 5.0f) * 5;
        return Math.max(25, rounded);
    }

    private static int gpsConfidence(IffGpsSnapshot gps) {
        if ("GPS_OK".equals(gps.status)) {
            return gps.accuracyM >= 0 && gps.accuracyM <= 10 ? 65 : 55;
        }
        if ("GPS_WEAK".equals(gps.status)) {
            return 35;
        }
        return 0;
    }

    private static String gpsDiagnostic(IffGpsSnapshot gps) {
        if (gps == null || gps.status.length() == 0 || "GPS_UNAVAILABLE".equals(gps.status)) {
            return "";
        }
        return "; gps=" + gps.status + " accuracy=" + gps.fieldValue(gps.accuracyM) + "m";
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
