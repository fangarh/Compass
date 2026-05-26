package net.afterday.compas.iff;

public final class IffApproachState {
    private static final long MIN_TTL_MS = 1000L;

    private final long ttlMs;
    private long activeUntilMillis;

    public IffApproachState(long ttlMs) {
        this.ttlMs = Math.max(MIN_TTL_MS, ttlMs);
    }

    public synchronized void activate(long nowMillis) {
        long safeNowMillis = Math.max(0L, nowMillis);
        activeUntilMillis = safeNowMillis + ttlMs;
    }

    public synchronized void clear() {
        activeUntilMillis = 0L;
    }

    public synchronized boolean isActive(long nowMillis) {
        long safeNowMillis = Math.max(0L, nowMillis);
        return activeUntilMillis > 0L && safeNowMillis <= activeUntilMillis;
    }
}
