package net.afterday.compas.iff;

public final class IffBleAdvertiseRestartPolicy {
    public static final long START_GRACE_MS = 5000L;
    public static final long CONTENT_RESTART_THROTTLE_MS = 5000L;

    private IffBleAdvertiseRestartPolicy() {
    }

    public static boolean shouldRestart(
            boolean running,
            boolean hasAdvertiser,
            boolean advertising,
            byte[] lastPayload,
            byte[] nextPayload) {
        if (!running || !hasAdvertiser || nextPayload == null) {
            return false;
        }
        if (!advertising) {
            return true;
        }
        return !IffBlePayload.sameAdvertiseContent(lastPayload, nextPayload);
    }

    public static boolean shouldRestart(
            boolean running,
            boolean hasAdvertiser,
            boolean advertising,
            boolean advertiseStartPending,
            long lastRestartElapsedMs,
            long nowElapsedMs,
            byte[] lastPayload,
            byte[] nextPayload) {
        if (!running || !hasAdvertiser || nextPayload == null) {
            return false;
        }
        if (advertiseStartPending
                && elapsedSince(lastRestartElapsedMs, nowElapsedMs) < START_GRACE_MS) {
            return false;
        }
        if (!advertising) {
            return true;
        }
        if (IffBlePayload.sameAdvertiseContent(lastPayload, nextPayload)) {
            return false;
        }
        return elapsedSince(lastRestartElapsedMs, nowElapsedMs) >= CONTENT_RESTART_THROTTLE_MS;
    }

    private static long elapsedSince(long thenElapsedMs, long nowElapsedMs) {
        if (thenElapsedMs <= 0L || nowElapsedMs < thenElapsedMs) {
            return Long.MAX_VALUE;
        }
        return nowElapsedMs - thenElapsedMs;
    }
}
