import java.util.Arrays;
import java.util.List;
import net.afterday.compas.iff.IffApproachState;
import net.afterday.compas.iff.IffCoordinateMessage;
import net.afterday.compas.iff.IffGpsSanity;
import net.afterday.compas.iff.IffGpsStabilizer;
import net.afterday.compas.iff.IffLocationFreshness;
import net.afterday.compas.iff.IffParticipantMapModel;
import net.afterday.compas.iff.IffParticipantState;
import net.afterday.compas.iff.IffParticipantStore;

public final class IffCoordinateCoreTest {
    public static void main(String[] args) {
        approachExpiresAfterTtl();
        approachCanBeCleared();
        approachActivationExtendsTtl();
        keepsFreshLowerHopParticipantState();
        rejectsRelayedSelfStateOverLocalState();
        rejectsFutureReceivedStateAsStale();
        rejectsImplausibleWalkingSpeedJump();
        treatsSmallFutureLocationClockSkewAsFresh();
        rejectsLargeFutureLocationClockSkew();
        rejectsTooOldLocationByWallClock();
        rejectsNullIslandGpsAsOutlier();
        snapshotCanRunDuringConcurrentMerge();
        roundTripsCoordinateMessage();
        rejectsMalformedCoordinateMessage();
        rejectsInvalidCoordinateStateInMessage();
        rejectsMalformedPercentEncodingInMessage();
        skipsNonFiniteAccuracyInCoordinateState();
        buildsSpatialMapFromLocalAndRemoteCoordinates();
        buildsSpatialMapWithDisplayName();
        ordersSpatialMapByNearestParticipant();
        scalesFieldMapForSeventyFiveMeters();
        buildsSpatialMapFromPoorButFactualGpsCoordinates();
        buildsSpatialMapFromStaleButRecentCoordinates();
        hidesMapCoordinatesOlderThanTwoMinutes();
        degradesWhenLocalGpsMissing();
        hidesInaccurateRemoteParticipant();
        showsStaleRemoteParticipantAsStale();
        keepsAntipodalMapPointFinite();
    }

    private static void approachExpiresAfterTtl() {
        IffApproachState state = new IffApproachState(30000L);

        state.activate(1000L);

        assertTrue(state.isActive(31000L), "approach should stay active inside ttl");
        assertFalse(state.isActive(31001L), "approach should expire after ttl");
    }

    private static void approachCanBeCleared() {
        IffApproachState state = new IffApproachState(30000L);

        state.activate(1000L);
        state.clear();

        assertFalse(state.isActive(1000L), "cleared approach should be inactive");
    }

    private static void approachActivationExtendsTtl() {
        IffApproachState state = new IffApproachState(30000L);

        state.activate(1000L);
        state.activate(20000L);

        assertTrue(state.isActive(49000L), "second activation should extend ttl");
        assertFalse(state.isActive(51001L), "extended approach should expire after new ttl");
    }

    private static void keepsFreshLowerHopParticipantState() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        IffParticipantState direct = state("petya", "petya", 55.7558, 37.6173, 5.0f, 1000L, 1000L, 0);
        IffParticipantState olderRelay = state("petya", "zhenya", 55.7559, 37.6174, 4.0f, 900L, 2000L, 1);
        IffParticipantState fresherRelay = state("petya", "zhenya", 55.7560, 37.6175, 4.0f, 20000L, 20000L, 1);

        assertTrue(store.merge(direct, 1000L), "direct participant state should be accepted");
        assertFalse(
                store.merge(olderRelay, 2000L),
                "fresh direct state should not be replaced by lower-quality relay");
        assertSame(direct, store.get("petya"), "store should keep the direct state");

        assertTrue(
                store.merge(fresherRelay, 20000L),
                "fresh relay should replace a stale direct state");
        assertSame(fresherRelay, store.get("petya"), "store should keep the fresh relayed state");

        List<IffParticipantState> snapshot = store.snapshot(20000L);
        assertEquals(1, snapshot.size());
        assertSame(fresherRelay, snapshot.get(0), "snapshot should contain only the fresh state");
    }

    private static void rejectsRelayedSelfStateOverLocalState() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 5.0f, 1000L, 1000L, 0);
        IffParticipantState relayedSelf = state("vasya", "petya", 55.7568, 37.6183, 3.0f, 2000L, 2000L, 1);

        assertTrue(store.merge(local, 1000L), "local self state should be accepted");
        assertFalse(store.merge(relayedSelf, 2000L), "relayed self state should be rejected");
        assertSame(local, store.get("vasya"), "store should keep local self state");

        List<IffParticipantState> snapshot = store.snapshot(2000L);
        assertEquals(1, snapshot.size());
        assertSame(local, snapshot.get(0), "snapshot should keep local self state");
    }

    private static void rejectsFutureReceivedStateAsStale() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        IffParticipantState existing = state("petya", "petya", 55.7558, 37.6173, 5.0f, 1000L, 1000L, 0);
        IffParticipantState future = state("petya", "petya", 55.7568, 37.6183, 3.0f, 50000L, 50000L, 0);

        assertTrue(store.merge(existing, 1000L), "fresh existing state should be accepted");
        assertFalse(store.merge(future, 2000L), "future received state should not replace fresh state");
        assertSame(existing, store.get("petya"), "store should keep existing fresh state");

        IffParticipantStore futureOnlyStore = new IffParticipantStore("vasya");
        assertTrue(futureOnlyStore.merge(future, 2000L), "future-only state may be stored until snapshot filtering");
        assertEquals(0, futureOnlyStore.snapshot(2000L).size());
    }

    private static void rejectsImplausibleWalkingSpeedJump() {
        IffGpsStabilizer stabilizer = new IffGpsStabilizer();

        IffGpsStabilizer.Decision first = stabilizer.evaluate(
                59.9914623,
                30.3262865,
                true,
                8.0f,
                100000L,
                "gps");
        IffGpsStabilizer.Decision jump = stabilizer.evaluate(
                59.9922049,
                30.3265806,
                true,
                8.0f,
                101000L,
                "gps");

        assertTrue(first.accepted, "first GPS point should be accepted");
        assertFalse(jump.accepted, "walking mode should reject a 1 second 80m GPS jump");
        assertEquals("rejected_speed", jump.reason);
    }

    private static void treatsSmallFutureLocationClockSkewAsFresh() {
        long ageMs = IffLocationFreshness.usableAgeMs(10000L, 10500L, 120000L);

        assertEquals(0L, ageMs);
    }

    private static void rejectsLargeFutureLocationClockSkew() {
        long ageMs = IffLocationFreshness.usableAgeMs(10000L, 45000L, 120000L);

        assertEquals(-1L, ageMs);
    }

    private static void rejectsTooOldLocationByWallClock() {
        long ageMs = IffLocationFreshness.usableAgeMs(200000L, 79000L, 120000L);

        assertEquals(-1L, ageMs);
    }

    private static void rejectsNullIslandGpsAsOutlier() {
        assertFalse(
                IffGpsSanity.isPlausibleCoordinate(0.5703163, 0.1707378),
                "MI null-island GPS should be quarantined even with good reported accuracy");
        assertTrue(
                IffGpsSanity.isPlausibleCoordinate(59.9916580, 30.3265505),
                "real local field coordinates should remain usable");
    }

    private static void snapshotCanRunDuringConcurrentMerge() {
        final IffParticipantStore store = new IffParticipantStore("vasya");
        final long nowMillis = 100000L;
        final Throwable[] writerFailure = new Throwable[1];
        Thread writer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 2000; i++) {
                        IffParticipantState state = state(
                                "petya-" + i,
                                "petya-" + i,
                                55.7000 + (i * 0.00001),
                                37.6000 + (i * 0.00001),
                                5.0f,
                                nowMillis,
                                nowMillis,
                                0);
                        store.merge(state, nowMillis);
                    }
                } catch (Throwable throwable) {
                    writerFailure[0] = throwable;
                }
            }
        });

        writer.start();
        for (int i = 0; i < 2000; i++) {
            List<IffParticipantState> snapshot = store.snapshot(nowMillis);
            for (int j = 0; j < snapshot.size(); j++) {
                assertTrue(
                        snapshot.get(j).isFresh(nowMillis, IffParticipantStore.STALE_AFTER_MILLIS),
                        "snapshot should only contain fresh states");
            }
        }
        join(writer);
        if (writerFailure[0] != null) {
            throw new AssertionError("writer thread failed", writerFailure[0]);
        }
    }

    private static void roundTripsCoordinateMessage() {
        IffParticipantState petya = state(
                "petya",
                "petya",
                55.7558,
                37.6173,
                5.5f,
                1000L,
                1100L,
                0,
                -64,
                true);
        IffParticipantState vasya = state(
                "vasya|field,unit",
                "petya/source",
                "Вася-1",
                55.7568,
                37.6183,
                8.25f,
                2000L,
                2200L,
                2,
                -91,
                false);

        String encoded = IffCoordinateMessage.encode("zhenya", 42L, Arrays.asList(petya, vasya));
        IffCoordinateMessage.Parsed parsed = IffCoordinateMessage.parse(encoded, 3000L);

        assertNotNull(parsed, "coordinate message should parse");
        assertEquals("zhenya", parsed.senderPlayerId);
        assertEquals(42L, parsed.sequence);
        assertEquals(2, parsed.states.size());

        IffParticipantState parsedPetya = parsed.states.get(0);
        assertEquals("petya", parsedPetya.playerId);
        assertEquals("petya", parsedPetya.sourcePlayerId);
        assertEquals(0, parsedPetya.hopCount);
        assertEquals(-64, parsedPetya.rssiDbm);
        assertTrue(parsedPetya.approachActive, "approach flag should round-trip");
        assertEquals(55.7558, parsedPetya.latitude, 0.0000001);
        assertEquals(37.6173, parsedPetya.longitude, 0.0000001);
        assertEquals(5.5f, parsedPetya.accuracyMeters, 0.0001f);
        assertEquals(1000L, parsedPetya.locationTimeMillis);
        assertEquals(3000L, parsedPetya.receivedTimeMillis);

        IffParticipantState parsedVasya = parsed.states.get(1);
        assertEquals("vasya|field,unit", parsedVasya.playerId);
        assertEquals("Вася-1", parsedVasya.displayName);
        assertEquals("petya/source", parsedVasya.sourcePlayerId);
        assertEquals(2, parsedVasya.hopCount);
        assertEquals(-91, parsedVasya.rssiDbm);
        assertFalse(parsedVasya.approachActive, "approach flag should round-trip as false");
        assertEquals(55.7568, parsedVasya.latitude, 0.0000001);
        assertEquals(37.6183, parsedVasya.longitude, 0.0000001);
        assertEquals(8.25f, parsedVasya.accuracyMeters, 0.0001f);
        assertEquals(2000L, parsedVasya.locationTimeMillis);
        assertEquals(3000L, parsedVasya.receivedTimeMillis);
    }

    private static void rejectsMalformedCoordinateMessage() {
        assertNull(IffCoordinateMessage.parse(null, 3000L), "null coordinate message should be rejected");
        assertNull(IffCoordinateMessage.parse("not-a-coordinate-message", 3000L), "bad coordinate message should be rejected");
        assertNull(IffCoordinateMessage.parse("CIFF2||1|", 3000L), "message with empty sender should be rejected");
        assertNull(IffCoordinateMessage.parse("CIFF2|%FF|1|", 3000L), "message with invalid UTF-8 sender should be rejected");

        String encoded = IffCoordinateMessage.encode("zhenya", -7L, Arrays.<IffParticipantState>asList());
        IffCoordinateMessage.Parsed parsed = IffCoordinateMessage.parse(encoded, 3000L);
        assertNotNull(parsed, "negative sequence should encode as a valid message");
        assertEquals(0L, parsed.sequence);
    }

    private static void rejectsInvalidCoordinateStateInMessage() {
        IffCoordinateMessage.Parsed parsed = IffCoordinateMessage.parse(
                "CIFF2|zhenya|7|bad-entry;petya,petya,999.0,37.6173,5.0,1000,0,-70,1"
                        + ";petya,petya,55.7558,37.6173,Infinity,1000,0,-70,1"
                        + ";%FF,petya,55.7558,37.6173,5.0,1000,0,-70,1",
                3000L);

        assertNotNull(parsed, "message with sender and invalid entries may still parse");
        assertEquals("zhenya", parsed.senderPlayerId);
        assertEquals(7L, parsed.sequence);
        assertEquals(0, parsed.states.size());
    }

    private static void rejectsMalformedPercentEncodingInMessage() {
        assertNull(
                IffCoordinateMessage.parse("CIFF2|bad%G0|1|", 3000L),
                "sender with invalid percent escape should be rejected");
        assertNull(
                IffCoordinateMessage.parse("CIFF2|%C3%28|1|", 3000L),
                "sender with invalid UTF-8 percent payload should be rejected");

        IffCoordinateMessage.Parsed parsed = IffCoordinateMessage.parse(
                "CIFF2|zhenya|1|%C3%28,petya,55.7558,37.6173,5.0,1000,0,-70,1",
                3000L);
        assertNotNull(parsed, "message with invalid state id may still parse");
        assertEquals(0, parsed.states.size());
    }

    private static void skipsNonFiniteAccuracyInCoordinateState() {
        IffCoordinateMessage.Parsed parsed = IffCoordinateMessage.parse(
                "CIFF2|zhenya|1|petya,petya,55.7558,37.6173,Infinity,1000,0,-70,1",
                3000L);

        assertNotNull(parsed, "message with non-finite accuracy may still parse");
        assertEquals(0, parsed.states.size());
    }

    private static void buildsSpatialMapFromLocalAndRemoteCoordinates() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState remote = state(
                "petya",
                "zhenya",
                55.7562,
                37.6178,
                7.5f,
                9100L,
                nowMillis,
                1,
                -72,
                true);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(remote, nowMillis), "remote participant state should be accepted");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());
        assertEquals(0, snapshot.hiddenCount);
        assertNotNull(snapshot.reason, "spatial snapshot should provide a reason");

        IffParticipantMapModel.Point point = snapshot.points.get(0);
        assertEquals("petya", point.playerId);
        assertTrue(point.distanceM > 0, "remote point should have positive distance");
        assertTrue(point.bearingDeg >= 20 && point.bearingDeg <= 60, "remote bearing should be north-east");
        assertTrue(point.x >= 0.0f && point.x <= 1.0f, "point x should be normalized");
        assertTrue(point.y >= 0.0f && point.y <= 1.0f, "point y should be normalized");
        assertTrue(Math.abs(point.x - 0.5f) > 0.001f || Math.abs(point.y - 0.5f) > 0.001f,
                "remote point should not overlap map center");
        assertTrue(point.approachActive, "approach flag should be preserved");
        assertEquals("zhenya", point.sourcePlayerId);
        assertEquals(1, point.hopCount);
        assertEquals(-72, point.rssiDbm);
        assertEquals(7.5f, point.accuracyMeters, 0.0001f);
        assertTrue(point.distanceAccuracyMeters > 8.0f && point.distanceAccuracyMeters < 10.0f,
                "combined distance accuracy should include local and remote GPS accuracy");
    }

    private static void buildsSpatialMapWithDisplayName() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState remote = state(
                "petya",
                "petya",
                "Петя тест",
                55.7562,
                37.6178,
                7.5f,
                9100L,
                nowMillis,
                0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(remote, nowMillis), "remote participant state should be accepted");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());
        assertEquals("petya", snapshot.points.get(0).playerId);
        assertEquals("Петя тест", snapshot.points.get(0).displayName);
    }

    private static void ordersSpatialMapByNearestParticipant() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 59.9914000, 30.3264000, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState far = state("far", "far", 59.9920740, 30.3264000, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState near = state("near", "near", 59.9916246, 30.3264000, 5.0f, 9000L, nowMillis, 0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(far, nowMillis), "far participant state should be accepted first");
        assertTrue(store.merge(near, nowMillis), "near participant state should be accepted second");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(2, snapshot.points.size());
        assertEquals("near", snapshot.points.get(0).playerId);
        assertEquals("far", snapshot.points.get(1).playerId);
    }

    private static void scalesFieldMapForSeventyFiveMeters() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 59.9914000, 30.3264000, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState near = state("near", "near", 59.9916246, 30.3264000, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState edge = state("edge", "edge", 59.9920740, 30.3264000, 5.0f, 9000L, nowMillis, 0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(near, nowMillis), "near participant state should be accepted");
        assertTrue(store.merge(edge, nowMillis), "edge participant state should be accepted");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        IffParticipantMapModel.Point nearPoint = pointById(snapshot, "near");
        IffParticipantMapModel.Point edgePoint = pointById(snapshot, "edge");
        assertNotNull(nearPoint, "near point should be visible");
        assertNotNull(edgePoint, "edge point should be visible");
        assertTrue(mapRadius(nearPoint) >= 0.13f && mapRadius(nearPoint) <= 0.18f,
                "25m should be roughly one third of the 75m map radius");
        assertTrue(mapRadius(edgePoint) >= 0.43f,
                "75m should land near the map edge");
    }

    private static void degradesWhenLocalGpsMissing() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState remote = state("petya", "petya", 55.7562, 37.6178, 7.5f, 9100L, nowMillis, 0);

        assertTrue(store.merge(remote, nowMillis), "remote participant state should be accepted");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("NO_LOCAL_GPS", snapshot.mode);
        assertEquals(0, snapshot.points.size());
        assertEquals(1, snapshot.hiddenCount);
        assertNotNull(snapshot.reason, "missing-local snapshot should provide a reason");
    }

    private static void buildsSpatialMapFromPoorButFactualGpsCoordinates() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 247.0f, 9000L, nowMillis, 0);
        IffParticipantState remote = state("petya", "petya", 55.7562, 37.6178, 250.0f, 9100L, nowMillis, 0);

        assertTrue(store.merge(local, nowMillis), "poor local GPS should be accepted by store");
        assertTrue(store.merge(remote, nowMillis), "poor remote GPS should be accepted by store");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());
        assertEquals(250.0f, snapshot.points.get(0).accuracyMeters, 0.0001f);
    }

    private static void buildsSpatialMapFromStaleButRecentCoordinates() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 100000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 248.0f, 1000L, 80000L, 0);
        IffParticipantState remote = state("petya", "petya", 55.7562, 37.6178, 100.0f, 2000L, 95000L, 0);

        assertTrue(store.merge(local, nowMillis), "stale local GPS should be stored");
        assertTrue(store.merge(remote, nowMillis), "recent remote GPS should be stored");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());
        assertTrue(
                snapshot.points.get(0).ageMs > IffParticipantStore.STALE_AFTER_MILLIS,
                "map point should remain visible with stale age");
    }

    private static void hidesMapCoordinatesOlderThanTwoMinutes() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 300000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 248.0f, 1000L, 179999L, 0);
        IffParticipantState remote = state("petya", "petya", 55.7562, 37.6178, 100.0f, 2000L, 299000L, 0);

        assertTrue(store.merge(local, nowMillis), "old local GPS should still be stored");
        assertTrue(store.merge(remote, nowMillis), "recent remote GPS should be stored");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("NO_LOCAL_GPS", snapshot.mode);
        assertEquals(0, snapshot.points.size());
        assertEquals(1, snapshot.hiddenCount);
    }

    private static void hidesInaccurateRemoteParticipant() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState remote = state("petya", "petya", 55.7562, 37.6178, 501.0f, 9100L, nowMillis, 0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(remote, nowMillis), "inaccurate remote participant state should be accepted by store");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("NO_PARTICIPANTS", snapshot.mode);
        assertEquals(0, snapshot.points.size());
        assertEquals(1, snapshot.hiddenCount);
        assertNotNull(snapshot.reason, "no-participants snapshot should provide a reason");
    }

    private static void showsStaleRemoteParticipantAsStale() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 20000L;
        IffParticipantState local = state("vasya", "vasya", 55.7558, 37.6173, 5.0f, 19000L, nowMillis, 0);
        IffParticipantState remote = state(
                "petya",
                "petya",
                55.7562,
                37.6178,
                7.5f,
                9100L,
                nowMillis - IffParticipantStore.STALE_AFTER_MILLIS - 1L,
                0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(remote, nowMillis), "stale remote participant state may be stored");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());
        assertEquals(0, snapshot.hiddenCount);
        assertTrue(
                snapshot.points.get(0).ageMs > IffParticipantStore.STALE_AFTER_MILLIS,
                "stale remote participant should remain visible with stale age");
    }

    private static void keepsAntipodalMapPointFinite() {
        IffParticipantStore store = new IffParticipantStore("vasya");
        long nowMillis = 10000L;
        IffParticipantState local = state("vasya", "vasya", -60.0, 0.0, 5.0f, 9000L, nowMillis, 0);
        IffParticipantState remote = state("petya", "petya", 59.999999999, 179.999999, 7.5f, 9100L, nowMillis, 0);

        assertTrue(store.merge(local, nowMillis), "local participant state should be accepted");
        assertTrue(store.merge(remote, nowMillis), "antipodal remote participant state should be accepted");

        IffParticipantMapModel.Snapshot snapshot = IffParticipantMapModel.from(store, "vasya", nowMillis);

        assertEquals("SPATIAL", snapshot.mode);
        assertEquals(1, snapshot.points.size());

        IffParticipantMapModel.Point point = snapshot.points.get(0);
        assertTrue(point.distanceM > 0, "antipodal point should have positive distance");
        assertFalse(Float.isNaN(point.x), "antipodal point x should be finite");
        assertFalse(Float.isNaN(point.y), "antipodal point y should be finite");
        assertTrue(point.x >= 0.0f && point.x <= 1.0f, "antipodal point x should be normalized");
        assertTrue(point.y >= 0.0f && point.y <= 1.0f, "antipodal point y should be normalized");
    }

    private static IffParticipantState state(
            String playerId,
            String sourcePlayerId,
            double latitude,
            double longitude,
            float accuracyMeters,
            long locationTimeMillis,
            long receivedTimeMillis,
            int hopCount) {
        IffParticipantState state = IffParticipantState.create(
                playerId,
                sourcePlayerId,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                Integer.MIN_VALUE,
                false);
        assertNotNull(state, "test state should be valid");
        return state;
    }

    private static IffParticipantState state(
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
        IffParticipantState state = IffParticipantState.create(
                playerId,
                sourcePlayerId,
                displayName,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                rssiDbm,
                approachActive);
        assertNotNull(state, "test state should be valid");
        return state;
    }

    private static IffParticipantState state(
            String playerId,
            String sourcePlayerId,
            String displayName,
            double latitude,
            double longitude,
            float accuracyMeters,
            long locationTimeMillis,
            long receivedTimeMillis,
            int hopCount) {
        IffParticipantState state = IffParticipantState.create(
                playerId,
                sourcePlayerId,
                displayName,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                Integer.MIN_VALUE,
                false);
        assertNotNull(state, "test state should be valid");
        return state;
    }

    private static IffParticipantState state(
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
        IffParticipantState state = IffParticipantState.create(
                playerId,
                sourcePlayerId,
                latitude,
                longitude,
                accuracyMeters,
                locationTimeMillis,
                receivedTimeMillis,
                hopCount,
                rssiDbm,
                approachActive);
        assertNotNull(state, "test state should be valid");
        return state;
    }

    private static IffParticipantMapModel.Point pointById(IffParticipantMapModel.Snapshot snapshot, String playerId) {
        for (int i = 0; i < snapshot.points.size(); i++) {
            if (playerId.equals(snapshot.points.get(i).playerId)) {
                return snapshot.points.get(i);
            }
        }
        return null;
    }

    private static float mapRadius(IffParticipantMapModel.Point point) {
        float dx = point.x - 0.5f;
        float dy = point.y - 0.5f;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean value, String message) {
        if (value) {
            throw new AssertionError(message);
        }
    }

    private static void assertSame(Object expected, Object actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message);
        }
    }

    private static void assertEquals(String expected, String actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(double expected, double actual, double tolerance) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(float expected, float actual, float tolerance) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message);
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("interrupted while waiting for writer thread", exception);
        }
    }
}
