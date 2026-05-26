package net.afterday.compas.iff;

public final class IffLocationFreshness {
    private static final long FUTURE_CLOCK_SKEW_TOLERANCE_MS = 30000L;

    private IffLocationFreshness() {
    }

    public static long usableAgeMs(long nowWallMs, long locationWallMs, long maxAgeMs) {
        if (nowWallMs <= 0L || locationWallMs <= 0L || maxAgeMs < 0L) {
            return -1L;
        }
        long ageMs = nowWallMs - locationWallMs;
        if (ageMs < 0L) {
            return -ageMs <= FUTURE_CLOCK_SKEW_TOLERANCE_MS ? 0L : -1L;
        }
        return ageMs <= maxAgeMs ? ageMs : -1L;
    }
}
