package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class IffParticipantStore {
    public static final long STALE_AFTER_MILLIS = 10000L;

    private final String localPlayerId;
    private final Object lock = new Object();
    private final Map<String, IffParticipantState> states = new LinkedHashMap<>();

    public IffParticipantStore(String localPlayerId) {
        this.localPlayerId = normalize(localPlayerId);
    }

    public boolean merge(IffParticipantState state, long nowMillis) {
        if (state == null) {
            return false;
        }
        if (state.playerId.equals(localPlayerId) && state.hopCount > 0) {
            return false;
        }

        synchronized (lock) {
            IffParticipantState existing = states.get(state.playerId);
            if (!state.isBetterThan(existing, nowMillis, STALE_AFTER_MILLIS)) {
                return false;
            }

            states.put(state.playerId, state);
            return true;
        }
    }

    public IffParticipantState get(String playerId) {
        synchronized (lock) {
            return states.get(playerId);
        }
    }

    public boolean remove(String playerId) {
        synchronized (lock) {
            return states.remove(playerId) != null;
        }
    }

    public List<IffParticipantState> snapshot(long nowMillis) {
        synchronized (lock) {
            List<IffParticipantState> freshStates = new ArrayList<>();
            for (IffParticipantState state : states.values()) {
                if (state.isFresh(nowMillis, STALE_AFTER_MILLIS)) {
                    freshStates.add(state);
                }
            }
            return freshStates;
        }
    }

    public List<IffParticipantState> snapshotAll() {
        synchronized (lock) {
            return new ArrayList<>(states.values());
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
