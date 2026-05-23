package net.afterday.compas.iff;

public final class IffFieldMapSnapshot {
    public final String readiness;
    public final String source;
    public final int distanceBucketM;
    public final String clockDirection;
    public final boolean targetVisible;
    public final boolean directionKnown;
    public final float targetX;
    public final float targetY;
    public final String statusLine;

    private IffFieldMapSnapshot(
            String readiness,
            String source,
            int distanceBucketM,
            String clockDirection,
            boolean targetVisible,
            boolean directionKnown,
            float targetX,
            float targetY,
            String statusLine) {
        this.readiness = safe(readiness);
        this.source = safe(source);
        this.distanceBucketM = distanceBucketM;
        this.clockDirection = safe(clockDirection);
        this.targetVisible = targetVisible;
        this.directionKnown = directionKnown;
        this.targetX = targetX;
        this.targetY = targetY;
        this.statusLine = safe(statusLine);
    }

    public static IffFieldMapSnapshot from(
            IffFieldLocatorSnapshot locator,
            String wifiTargetStatus) {
        String readiness = readiness(wifiTargetStatus);
        if (locator == null || !"OK".equals(locator.status) || locator.distanceBucketM <= 0) {
            return new IffFieldMapSnapshot(
                    readiness,
                    "NONE",
                    -1,
                    "na",
                    false,
                    false,
                    0.5f,
                    0.42f,
                    readiness + " / no field fix");
        }

        boolean directionKnown = "WIFI_TARGET".equals(locator.source)
                && "TWO_ANCHORS".equals(readiness)
                && !"na".equals(locator.clockDirection);
        float[] target = targetPosition(locator.distanceBucketM, locator.clockDirection, directionKnown);
        String status = locator.source
                + " " + locator.distanceBucketM + "m"
                + " clock=" + (directionKnown ? locator.clockDirection : "na")
                + " " + readiness;
        return new IffFieldMapSnapshot(
                readiness,
                locator.source,
                locator.distanceBucketM,
                directionKnown ? locator.clockDirection : "na",
                true,
                directionKnown,
                target[0],
                target[1],
                status);
    }

    public static IffFieldMapSnapshot operatorSnapshot(
            String readiness,
            String source,
            int distanceBucketM,
            String clockDirection,
            String statusLine) {
        boolean directionKnown = "TWO_ANCHORS".equals(readiness)
                && clockDirection != null
                && !"na".equals(clockDirection)
                && distanceBucketM > 0;
        float[] target = targetPosition(distanceBucketM, clockDirection, directionKnown);
        return new IffFieldMapSnapshot(
                readiness,
                source,
                distanceBucketM,
                directionKnown ? clockDirection : "na",
                distanceBucketM > 0,
                directionKnown,
                target[0],
                target[1],
                statusLine);
    }

    public static String readiness(String wifiTargetStatus) {
        if (wifiTargetStatus == null || wifiTargetStatus.length() == 0) {
            return "MISSING_STATUS";
        }
        boolean leftReady = wifiTargetStatus.matches(".*left=vasya:-\\d+.*");
        boolean rightReady = wifiTargetStatus.matches(".*right=petya:-\\d+.*");
        if (leftReady && rightReady) {
            return "TWO_ANCHORS";
        }
        if (leftReady || rightReady) {
            return "ONE_ANCHOR";
        }
        return "NO_ANCHORS";
    }

    private static float[] targetPosition(int distanceBucketM, String clockDirection, boolean directionKnown) {
        float radius = radiusFor(distanceBucketM);
        if (!directionKnown) {
            return new float[] {0.5f, 0.42f - radius};
        }
        float angleDeg = angleForClock(clockDirection);
        double angleRad = Math.toRadians(angleDeg);
        float x = 0.5f + (float) Math.sin(angleRad) * radius;
        float y = 0.64f - (float) Math.cos(angleRad) * radius;
        return new float[] {clamp(x, 0.12f, 0.88f), clamp(y, 0.16f, 0.82f)};
    }

    private static float radiusFor(int distanceBucketM) {
        if (distanceBucketM <= 5) {
            return 0.16f;
        }
        if (distanceBucketM <= 10) {
            return 0.24f;
        }
        if (distanceBucketM <= 15) {
            return 0.32f;
        }
        if (distanceBucketM <= 20) {
            return 0.40f;
        }
        return 0.46f;
    }

    private static float angleForClock(String clockDirection) {
        if ("3".equals(clockDirection)) {
            return 90.0f;
        }
        if ("2".equals(clockDirection)) {
            return 60.0f;
        }
        if ("1".equals(clockDirection)) {
            return 30.0f;
        }
        if ("9".equals(clockDirection)) {
            return -90.0f;
        }
        if ("10".equals(clockDirection)) {
            return -60.0f;
        }
        if ("11".equals(clockDirection)) {
            return -30.0f;
        }
        return 0.0f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
