package net.afterday.compas.iff;

public final class IffTargetObservationPolicy {
    private IffTargetObservationPolicy() {
    }

    public static boolean shouldRecordAnchorObservation(String anchorPlayerId, String observedPlayerId) {
        String anchor = safe(anchorPlayerId);
        String observed = safe(observedPlayerId);
        return IffWifiTargetObservationStore.TARGET_PLAYER_ID.equals(observed)
                && !IffWifiTargetObservationStore.TARGET_PLAYER_ID.equals(anchor)
                && (IffWifiTargetObservationStore.LEFT_ANCHOR_PLAYER_ID.equals(anchor)
                || IffWifiTargetObservationStore.RIGHT_ANCHOR_PLAYER_ID.equals(anchor));
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
