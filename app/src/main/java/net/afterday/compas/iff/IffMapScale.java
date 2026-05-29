package net.afterday.compas.iff;

public final class IffMapScale {
    private static final int[] RANGE_METERS = new int[] {25, 50, 75, 150, 300};
    private static final int DEFAULT_RANGE_METERS = 75;
    private static final float MIN_VISIBLE_RADIUS = 0.06f;
    private static final float MAX_VISIBLE_RADIUS = 0.45f;

    private IffMapScale() {
    }

    public static int defaultRangeMeters() {
        return DEFAULT_RANGE_METERS;
    }

    public static int normalizeRangeMeters(int rangeMeters) {
        int closest = RANGE_METERS[0];
        int closestDelta = Math.abs(rangeMeters - closest);
        for (int i = 1; i < RANGE_METERS.length; i++) {
            int delta = Math.abs(rangeMeters - RANGE_METERS[i]);
            if (delta < closestDelta) {
                closest = RANGE_METERS[i];
                closestDelta = delta;
            }
        }
        return closest;
    }

    public static int zoomInRangeMeters(int rangeMeters) {
        int normalized = normalizeRangeMeters(rangeMeters);
        for (int i = 0; i < RANGE_METERS.length; i++) {
            if (RANGE_METERS[i] == normalized) {
                return RANGE_METERS[Math.max(0, i - 1)];
            }
        }
        return DEFAULT_RANGE_METERS;
    }

    public static int zoomOutRangeMeters(int rangeMeters) {
        int normalized = normalizeRangeMeters(rangeMeters);
        for (int i = 0; i < RANGE_METERS.length; i++) {
            if (RANGE_METERS[i] == normalized) {
                return RANGE_METERS[Math.min(RANGE_METERS.length - 1, i + 1)];
            }
        }
        return DEFAULT_RANGE_METERS;
    }

    public static float screenRadius(double distanceMeters, int rangeMeters) {
        int normalized = normalizeRangeMeters(rangeMeters);
        double scaled = distanceMeters / normalized * MAX_VISIBLE_RADIUS;
        return (float) Math.max(MIN_VISIBLE_RADIUS, Math.min(MAX_VISIBLE_RADIUS, scaled));
    }

    public static String label(int rangeMeters) {
        return "SCALE " + normalizeRangeMeters(rangeMeters) + "m";
    }
}
