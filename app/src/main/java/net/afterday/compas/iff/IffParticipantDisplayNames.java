package net.afterday.compas.iff;

import java.util.HashMap;
import java.util.Map;

public final class IffParticipantDisplayNames {
    private final Object lock = new Object();
    private final Map<String, String> names = new HashMap<String, String>();

    public void remember(String playerId, String displayName) {
        String normalizedPlayerId = normalize(playerId);
        String normalizedDisplayName = normalizeDisplayName(displayName);
        if (normalizedPlayerId == null || normalizedDisplayName == null) {
            return;
        }
        synchronized (lock) {
            names.put(normalizedPlayerId, normalizedDisplayName);
        }
    }

    public String displayNameFor(String playerId) {
        String normalizedPlayerId = normalize(playerId);
        if (normalizedPlayerId == null) {
            return "phone";
        }
        synchronized (lock) {
            String displayName = names.get(normalizedPlayerId);
            if (displayName != null && displayName.length() > 0) {
                return displayName;
            }
        }
        return normalizedPlayerId;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() == 0 ? null : trimmed;
    }

    private static String normalizeDisplayName(String value) {
        String normalized = normalize(value);
        if (normalized == null) {
            return null;
        }
        return normalized.length() > 18 ? normalized.substring(0, 18) : normalized;
    }
}
