package net.afterday.compas.iff;

public final class IffBleScanRetryPolicy {
    private IffBleScanRetryPolicy() {
    }

    public static long delayMs(int failureCount) {
        if (failureCount <= 1) {
            return 2000L;
        }
        if (failureCount == 2) {
            return 5000L;
        }
        return 10000L;
    }
}
