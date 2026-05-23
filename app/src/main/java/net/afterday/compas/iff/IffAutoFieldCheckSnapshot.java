package net.afterday.compas.iff;

public final class IffAutoFieldCheckSnapshot {
    public static final long INTERVAL_MS = 2000L;

    private IffAutoFieldCheckSnapshot() {
    }

    public static boolean shouldRecord(long nowElapsedMs, long lastSnapshotElapsedMs) {
        return lastSnapshotElapsedMs <= 0L || nowElapsedMs - lastSnapshotElapsedMs >= INTERVAL_MS;
    }

    public static String officeTestRole(String playerId) {
        if ("vasya".equals(playerId)) {
            return "PHONE_A_WITNESS";
        }
        if ("zhenya".equals(playerId)) {
            return "PHONE_B_WITNESS";
        }
        if ("petya".equals(playerId)) {
            return "PHONE_C_MOVING_TARGET";
        }
        if ("local-you".equals(playerId)) {
            return "PHONE_OPERATOR";
        }
        return "UNASSIGNED";
    }
}
