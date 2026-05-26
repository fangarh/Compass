package net.afterday.compas.iff;

public final class IffParticipantState {
    public final String playerId;
    public final String sourcePlayerId;
    public final String displayName;
    public final double latitude;
    public final double longitude;
    public final float accuracyMeters;
    public final long locationTimeMillis;
    public final long receivedTimeMillis;
    public final int hopCount;
    public final int rssiDbm;
    public final boolean approachActive;

    private IffParticipantState(
            String playerId,
            String sourcePlayerId,
            String displayName,
            double latitude,
            double longitude,
            float accuracyMeters,
            long locationTimeMillis,
            long receivedTimeMillis,
            int hopCount,
            int rssiDbm,
            boolean approachActive) {
        this.playerId = playerId;
        this.sourcePlayerId = sourcePlayerId;
        this.displayName = displayName;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracyMeters = accuracyMeters;
        this.locationTimeMillis = locationTimeMillis;
        this.receivedTimeMillis = receivedTimeMillis;
        this.hopCount = hopCount;
        this.rssiDbm = rssiDbm;
        this.approachActive = approachActive;
    }

    public static IffParticipantState create(
            String playerId,
            String sourcePlayerId,
            double latitude,
            double longitude,
            float accuracyMeters,
            long locationTimeMillis,
            long receivedTimeMillis,
            int hopCount,
            int rssiDbm,
            boolean approachActive) {
        return create(
                playerId,
                sourcePlayerId,
                playerId,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                rssiDbm,
                approachActive);
    }

    public static IffParticipantState create(
            String playerId,
            String sourcePlayerId,
            String displayName,
            double latitude,
            double longitude,
            float accuracyMeters,
            long locationTimeMillis,
            long receivedTimeMillis,
            int hopCount,
            int rssiDbm,
            boolean approachActive) {
        String normalizedPlayerId = normalize(playerId);
        if (normalizedPlayerId == null
                || Double.isNaN(latitude)
                || latitude < -90.0
                || latitude > 90.0
                || Double.isNaN(longitude)
                || longitude < -180.0
                || longitude > 180.0
                || Float.isNaN(accuracyMeters)
                || accuracyMeters <= 0.0f
                || hopCount < 0) {
            return null;
        }

        String normalizedSourcePlayerId = normalize(sourcePlayerId);
        if (normalizedSourcePlayerId == null) {
            normalizedSourcePlayerId = normalizedPlayerId;
        }
        String normalizedDisplayName = normalize(displayName);
        if (normalizedDisplayName == null) {
            normalizedDisplayName = normalizedPlayerId;
        }

        return new IffParticipantState(
                normalizedPlayerId,
                normalizedSourcePlayerId,
                normalizedDisplayName,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                rssiDbm,
                approachActive);
    }

    public boolean isFresh(long nowMillis, long maxAgeMillis) {
        long ageMillis = nowMillis - receivedTimeMillis;
        return ageMillis >= 0L && ageMillis <= maxAgeMillis;
    }

    public boolean isBetterThan(IffParticipantState existing, long nowMillis, long staleAfterMillis) {
        if (existing == null) {
            return true;
        }

        boolean fresh = isFresh(nowMillis, staleAfterMillis);
        boolean existingFresh = existing.isFresh(nowMillis, staleAfterMillis);
        if (fresh && !existingFresh) {
            return true;
        }
        if (!fresh && existingFresh) {
            return false;
        }
        if (hopCount != existing.hopCount) {
            return hopCount < existing.hopCount;
        }
        if (locationTimeMillis != existing.locationTimeMillis) {
            return locationTimeMillis > existing.locationTimeMillis;
        }
        return receivedTimeMillis > existing.receivedTimeMillis;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
