package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class IffParticipantMapModel {
    public static final float MAP_MAX_ACCURACY_METERS = 500.0f;

    private static final String MODE_NO_LOCAL_GPS = "NO_LOCAL_GPS";
    private static final String MODE_NO_PARTICIPANTS = "NO_PARTICIPANTS";
    private static final String MODE_SPATIAL = "SPATIAL";
    private static final long MAP_MAX_POINT_AGE_MS = 120000L;
    private static final double EARTH_RADIUS_METERS = 6371000.0;
    private static final double MAP_EDGE_DISTANCE_METERS = 75.0;
    private static final float CENTER = 0.5f;
    private static final float MIN_VISIBLE_RADIUS = 0.06f;
    private static final float MAX_VISIBLE_RADIUS = 0.45f;
    private static final float MIN_COORDINATE = 0.0f;
    private static final float MAX_COORDINATE = 1.0f;

    private IffParticipantMapModel() {
    }

    public static Snapshot from(IffParticipantStore store, String localPlayerId, long nowMillis) {
        List<IffParticipantState> states = store == null
                ? Collections.<IffParticipantState>emptyList()
                : store.snapshotAll();
        IffParticipantState localState = store == null ? null : store.get(localPlayerId);
        if (!isUsableForMap(localState, nowMillis)) {
            return new Snapshot(
                    MODE_NO_LOCAL_GPS,
                    Collections.<Point>emptyList(),
                    countRemoteStates(states, localPlayerId),
                    "Local GPS is missing or too inaccurate.");
        }

        List<Point> points = new ArrayList<Point>();
        int hiddenCount = 0;
        for (int i = 0; i < states.size(); i++) {
            IffParticipantState remoteState = states.get(i);
            if (remoteState == null || remoteState.playerId.equals(localState.playerId)) {
                continue;
            }
            if (!isUsableForMap(remoteState, nowMillis)) {
                hiddenCount++;
                continue;
            }
            points.add(toPoint(localState, remoteState, nowMillis));
        }
        Collections.sort(points, Point.ORDER_BY_MAP_PRIORITY);

        if (points.isEmpty()) {
            return new Snapshot(
                    MODE_NO_PARTICIPANTS,
                    Collections.<Point>emptyList(),
                    hiddenCount,
                    hiddenCount == 0
                            ? "No remote participant coordinates are available."
                            : "Remote participants are hidden because their coordinates are unusable.");
        }

        return new Snapshot(
                MODE_SPATIAL,
                points,
                hiddenCount,
                hiddenCount == 0
                        ? "Fresh participant positions are available."
                        : "Fresh participant positions are available; some participants are hidden.");
    }

    private static Point toPoint(
            IffParticipantState localState,
            IffParticipantState remoteState,
            long nowMillis) {
        double distanceMeters = distanceMeters(
                localState.latitude,
                localState.longitude,
                remoteState.latitude,
                remoteState.longitude);
        int bearingDeg = bearingDeg(
                localState.latitude,
                localState.longitude,
                remoteState.latitude,
                remoteState.longitude);
        float radius = screenRadius(distanceMeters);
        double bearingRadians = Math.toRadians(bearingDeg);
        float x = clamp(CENTER + (float) (Math.sin(bearingRadians) * radius));
        float y = clamp(CENTER - (float) (Math.cos(bearingRadians) * radius));

        return new Point(
                remoteState.playerId,
                remoteState.displayName,
                x,
                y,
                Math.max(0, (int) Math.round(distanceMeters)),
                bearingDeg,
                Math.max(
                        Math.max(0L, nowMillis - localState.receivedTimeMillis),
                        Math.max(0L, nowMillis - remoteState.receivedTimeMillis)),
                remoteState.accuracyMeters,
                combinedAccuracyMeters(localState.accuracyMeters, remoteState.accuracyMeters),
                remoteState.sourcePlayerId,
                remoteState.hopCount,
                remoteState.rssiDbm,
                remoteState.approachActive);
    }

    private static boolean isUsableForMap(IffParticipantState state, long nowMillis) {
        return state != null
                && Math.max(0L, nowMillis - state.receivedTimeMillis) <= MAP_MAX_POINT_AGE_MS
                && IffGpsSanity.isPlausibleCoordinate(state.latitude, state.longitude)
                && !Float.isNaN(state.accuracyMeters)
                && !Float.isInfinite(state.accuracyMeters)
                && state.accuracyMeters > 0.0f
                && state.accuracyMeters <= MAP_MAX_ACCURACY_METERS;
    }

    private static int countRemoteStates(List<IffParticipantState> states, String localPlayerId) {
        int count = 0;
        for (int i = 0; i < states.size(); i++) {
            IffParticipantState state = states.get(i);
            if (state != null && !state.playerId.equals(localPlayerId)) {
                count++;
            }
        }
        return count;
    }

    private static double distanceMeters(
            double fromLatitude,
            double fromLongitude,
            double toLatitude,
            double toLongitude) {
        double fromLatRadians = Math.toRadians(fromLatitude);
        double toLatRadians = Math.toRadians(toLatitude);
        double deltaLatRadians = Math.toRadians(toLatitude - fromLatitude);
        double deltaLonRadians = Math.toRadians(toLongitude - fromLongitude);

        double sinDeltaLat = Math.sin(deltaLatRadians / 2.0);
        double sinDeltaLon = Math.sin(deltaLonRadians / 2.0);
        double a = (sinDeltaLat * sinDeltaLat)
                + Math.cos(fromLatRadians)
                * Math.cos(toLatRadians)
                * sinDeltaLon
                * sinDeltaLon;
        a = Math.max(0.0, Math.min(1.0, a));
        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        return EARTH_RADIUS_METERS * c;
    }

    private static int bearingDeg(
            double fromLatitude,
            double fromLongitude,
            double toLatitude,
            double toLongitude) {
        double fromLatRadians = Math.toRadians(fromLatitude);
        double toLatRadians = Math.toRadians(toLatitude);
        double deltaLonRadians = Math.toRadians(toLongitude - fromLongitude);
        double y = Math.sin(deltaLonRadians) * Math.cos(toLatRadians);
        double x = Math.cos(fromLatRadians) * Math.sin(toLatRadians)
                - Math.sin(fromLatRadians) * Math.cos(toLatRadians) * Math.cos(deltaLonRadians);
        double degrees = Math.toDegrees(Math.atan2(y, x));
        int rounded = (int) Math.round(degrees);
        return ((rounded % 360) + 360) % 360;
    }

    private static float screenRadius(double distanceMeters) {
        double scaled = distanceMeters / MAP_EDGE_DISTANCE_METERS * MAX_VISIBLE_RADIUS;
        return (float) Math.max(MIN_VISIBLE_RADIUS, Math.min(MAX_VISIBLE_RADIUS, scaled));
    }

    private static float clamp(float value) {
        return Math.max(MIN_COORDINATE, Math.min(MAX_COORDINATE, value));
    }

    private static float combinedAccuracyMeters(float localAccuracyMeters, float remoteAccuracyMeters) {
        double combined = Math.sqrt(
                (localAccuracyMeters * localAccuracyMeters)
                        + (remoteAccuracyMeters * remoteAccuracyMeters));
        if (Double.isNaN(combined) || Double.isInfinite(combined)) {
            return MAP_MAX_ACCURACY_METERS;
        }
        return (float) combined;
    }

    public static final class Snapshot {
        public final String mode;
        public final List<Point> points;
        public final int hiddenCount;
        public final String reason;

        private Snapshot(String mode, List<Point> points, int hiddenCount, String reason) {
            this.mode = mode;
            this.points = Collections.unmodifiableList(new ArrayList<Point>(points));
            this.hiddenCount = hiddenCount;
            this.reason = reason;
        }

        public Snapshot filteredToPlayerIds(Set<String> allowedPlayerIds) {
            if (allowedPlayerIds == null || allowedPlayerIds.isEmpty() || points.isEmpty()) {
                return new Snapshot(mode, Collections.<Point>emptyList(), hiddenCount + points.size(),
                        reason + " Team roster filter hid all remote participants.");
            }
            List<Point> filtered = new ArrayList<Point>();
            int filteredOut = 0;
            for (int i = 0; i < points.size(); i++) {
                Point point = points.get(i);
                if (allowedPlayerIds.contains(point.playerId)) {
                    filtered.add(point);
                } else {
                    filteredOut++;
                }
            }
            String filteredReason = filteredOut == 0
                    ? reason
                    : reason + " Team roster filter hid " + filteredOut + " non-team participants.";
            return new Snapshot(mode, filtered, hiddenCount + filteredOut, filteredReason);
        }
    }

    public static final class Point {
        private static final Comparator<Point> ORDER_BY_MAP_PRIORITY = new Comparator<Point>() {
            @Override
            public int compare(Point left, Point right) {
                int distance = left.distanceM - right.distanceM;
                if (distance != 0) {
                    return distance;
                }
                int age = Long.compare(left.ageMs, right.ageMs);
                if (age != 0) {
                    return age;
                }
                return left.playerId.compareTo(right.playerId);
            }
        };

        public final String playerId;
        public final String displayName;
        public final float x;
        public final float y;
        public final int distanceM;
        public final int bearingDeg;
        public final long ageMs;
        public final float accuracyMeters;
        public final float distanceAccuracyMeters;
        public final String sourcePlayerId;
        public final int hopCount;
        public final int rssiDbm;
        public final boolean approachActive;

        private Point(
                String playerId,
                String displayName,
                float x,
                float y,
                int distanceM,
                int bearingDeg,
                long ageMs,
                float accuracyMeters,
                float distanceAccuracyMeters,
                String sourcePlayerId,
                int hopCount,
                int rssiDbm,
                boolean approachActive) {
            this.playerId = playerId;
            this.displayName = displayName;
            this.x = x;
            this.y = y;
            this.distanceM = distanceM;
            this.bearingDeg = bearingDeg;
            this.ageMs = ageMs;
            this.accuracyMeters = accuracyMeters;
            this.distanceAccuracyMeters = distanceAccuracyMeters;
            this.sourcePlayerId = sourcePlayerId;
            this.hopCount = hopCount;
            this.rssiDbm = rssiDbm;
            this.approachActive = approachActive;
        }
    }
}
