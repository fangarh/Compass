package net.afterday.compas.iff;

public final class IffGpsSanity {
    private static final double NULL_ISLAND_BOUND_DEG = 1.0d;

    private IffGpsSanity() {
    }

    public static boolean isPlausibleCoordinate(double latitude, double longitude) {
        return isFinite(latitude)
                && isFinite(longitude)
                && latitude >= -90.0d
                && latitude <= 90.0d
                && longitude >= -180.0d
                && longitude <= 180.0d
                && !(Math.abs(latitude) < NULL_ISLAND_BOUND_DEG
                && Math.abs(longitude) < NULL_ISLAND_BOUND_DEG);
    }

    private static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }
}
