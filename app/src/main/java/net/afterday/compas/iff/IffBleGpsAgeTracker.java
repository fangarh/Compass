package net.afterday.compas.iff;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class IffBleGpsAgeTracker {
    private final Map<String, SeenPayload> seenBySource = new HashMap<>();

    public synchronized long effectiveGpsAgeMs(String sourceKey, byte[] payload, long nowElapsedMs) {
        IffBlePayload.Parsed parsed = IffBlePayload.parse(payload);
        if (parsed == null || !parsed.hasGps || nowElapsedMs <= 0L) {
            forget(sourceKey);
            return -1L;
        }
        String key = safe(sourceKey);
        byte[] stableContent = stableGpsContent(payload);
        SeenPayload seen = seenBySource.get(key);
        if (seen == null || !Arrays.equals(seen.stableContent, stableContent)
                || parsed.gpsAgeMs < seen.advertisedGpsAgeMs) {
            seen = new SeenPayload(stableContent, parsed.gpsAgeMs, nowElapsedMs);
            seenBySource.put(key, seen);
        }
        return parsed.gpsAgeMs + Math.max(0L, nowElapsedMs - seen.firstSeenElapsedMs);
    }

    public synchronized void clear() {
        seenBySource.clear();
    }

    private synchronized void forget(String sourceKey) {
        seenBySource.remove(safe(sourceKey));
    }

    private static byte[] stableGpsContent(byte[] payload) {
        byte[] copy = payload == null ? new byte[0] : Arrays.copyOf(payload, payload.length);
        if (copy.length >= IffBlePayload.V2_LENGTH) {
            copy[15] = 0;
            copy[16] = 0;
        }
        return copy;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static final class SeenPayload {
        final byte[] stableContent;
        final long advertisedGpsAgeMs;
        final long firstSeenElapsedMs;

        SeenPayload(byte[] stableContent, long advertisedGpsAgeMs, long firstSeenElapsedMs) {
            this.stableContent = stableContent;
            this.advertisedGpsAgeMs = advertisedGpsAgeMs;
            this.firstSeenElapsedMs = firstSeenElapsedMs;
        }
    }
}
