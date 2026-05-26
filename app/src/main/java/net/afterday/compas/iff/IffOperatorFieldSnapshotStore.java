package net.afterday.compas.iff;

public final class IffOperatorFieldSnapshotStore {
    private static final int MAX_SAMPLES = 8;
    private static final long HOLD_MS = 10000L;

    private final Sample[] samples = new Sample[MAX_SAMPLES];
    private int count;
    private int nextIndex;

    public IffFieldMapSnapshot update(IffFieldMapSnapshot raw) {
        return update(raw, System.currentTimeMillis());
    }

    public synchronized IffFieldMapSnapshot update(IffFieldMapSnapshot raw, long nowMs) {
        if (raw == null) {
            trim(nowMs);
            return fallback(null, nowMs);
        }
        if ("GPS_ASSISTED".equals(raw.source)) {
            trim(nowMs);
            return raw;
        }
        if (isTwoAnchorFix(raw)) {
            samples[nextIndex] = new Sample(raw.distanceBucketM, raw.clockDirection, nowMs);
            nextIndex = (nextIndex + 1) % MAX_SAMPLES;
            if (count < MAX_SAMPLES) {
                count++;
            }
        }
        trim(nowMs);
        return fallback(raw, nowMs);
    }

    public synchronized void resetForTest() {
        for (int i = 0; i < samples.length; i++) {
            samples[i] = null;
        }
        count = 0;
        nextIndex = 0;
    }

    private IffFieldMapSnapshot fallback(IffFieldMapSnapshot raw, long nowMs) {
        Sample newest = newestSample();
        if (newest == null) {
            return raw;
        }

        boolean currentTwoAnchor = raw != null && isTwoAnchorFix(raw);
        int stableDistance = modeDistance();
        String stableClock = modeClock();
        long ageMs = Math.max(0L, nowMs - newest.elapsedMs);
        String source = currentTwoAnchor ? "WIFI_TARGET_STABLE" : "WIFI_TARGET_HOLD";
        String mode = currentTwoAnchor ? "stable" : "hold";
        return IffFieldMapSnapshot.operatorSnapshot(
                "TWO_ANCHORS",
                source,
                stableDistance,
                stableClock,
                mode + " " + stableDistance + "m clock=" + stableClock
                        + " samples=" + count
                        + " ageMs=" + ageMs
                        + (raw == null ? "" : " raw=[" + raw.statusLine + "]"));
    }

    private boolean isTwoAnchorFix(IffFieldMapSnapshot raw) {
        return raw.targetVisible
                && raw.directionKnown
                && raw.distanceBucketM > 0
                && "TWO_ANCHORS".equals(raw.readiness)
                && raw.clockDirection.length() > 0
                && !"na".equals(raw.clockDirection);
    }

    private void trim(long nowMs) {
        for (int i = 0; i < samples.length; i++) {
            Sample sample = samples[i];
            if (sample != null && nowMs - sample.elapsedMs > HOLD_MS) {
                samples[i] = null;
            }
        }
        count = 0;
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] != null) {
                count++;
            }
        }
    }

    private Sample newestSample() {
        Sample newest = null;
        for (int i = 0; i < samples.length; i++) {
            Sample sample = samples[i];
            if (sample != null && (newest == null || sample.elapsedMs >= newest.elapsedMs)) {
                newest = sample;
            }
        }
        return newest;
    }

    private int modeDistance() {
        Sample best = newestSample();
        int bestCount = -1;
        for (int i = 0; i < samples.length; i++) {
            Sample candidate = samples[i];
            if (candidate == null) {
                continue;
            }
            int candidateCount = countDistance(candidate.distanceBucketM);
            if (best == null
                    || candidateCount > bestCount
                    || (candidateCount == bestCount && candidate.elapsedMs >= best.elapsedMs)) {
                best = candidate;
                bestCount = candidateCount;
            }
        }
        return best == null ? -1 : best.distanceBucketM;
    }

    private String modeClock() {
        Sample best = newestSample();
        int bestCount = -1;
        for (int i = 0; i < samples.length; i++) {
            Sample candidate = samples[i];
            if (candidate == null) {
                continue;
            }
            int candidateCount = countClock(candidate.clockDirection);
            if (best == null
                    || candidateCount > bestCount
                    || (candidateCount == bestCount && candidate.elapsedMs >= best.elapsedMs)) {
                best = candidate;
                bestCount = candidateCount;
            }
        }
        return best == null ? "na" : best.clockDirection;
    }

    private int countDistance(int distanceBucketM) {
        int total = 0;
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] != null && samples[i].distanceBucketM == distanceBucketM) {
                total++;
            }
        }
        return total;
    }

    private int countClock(String clockDirection) {
        int total = 0;
        for (int i = 0; i < samples.length; i++) {
            if (samples[i] != null && samples[i].clockDirection.equals(clockDirection)) {
                total++;
            }
        }
        return total;
    }

    private static final class Sample {
        final int distanceBucketM;
        final String clockDirection;
        final long elapsedMs;

        Sample(int distanceBucketM, String clockDirection, long elapsedMs) {
            this.distanceBucketM = distanceBucketM;
            this.clockDirection = clockDirection == null ? "na" : clockDirection;
            this.elapsedMs = elapsedMs;
        }
    }
}
