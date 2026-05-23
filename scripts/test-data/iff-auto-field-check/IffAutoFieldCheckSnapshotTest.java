import net.afterday.compas.iff.IffAutoFieldCheckSnapshot;
import net.afterday.compas.iff.IffDistanceTrend;
import net.afterday.compas.iff.IffFieldRunSummary;
import net.afterday.compas.iff.IffFieldSnapshotFormatter;
import net.afterday.compas.iff.IffGpsSnapshot;
import net.afterday.compas.iff.IffGpsStabilizer;
import net.afterday.compas.iff.IffOfficeProximityVerdict;

public final class IffAutoFieldCheckSnapshotTest {
    public static void main(String[] args) {
        shouldRecordFirstSnapshotImmediately();
        shouldThrottleSnapshotsUntilIntervalPasses();
        shouldMapOfficeRoles();
        shouldHideOutlier127FromHumanBleStatus();
        shouldFormatCompactWifiFingerprint();
        shouldClassifyWifiFingerprintFreshness();
        shouldClassifyOneSidedWallShadowProximity();
        shouldClassifyDistanceFromFreshRssiWindow();
        shouldClassifyMovementTrendFromRssiDelta();
        shouldClassifyGpsFreshnessForFieldRuns();
        shouldComputeGpsDistanceAndBearingBetweenFreshFixes();
        shouldRejectGpsOutlierWhenRadioSaysNearby();
        shouldRejectImpossibleLocalGpsJumpBeforeBleAdvertise();
        shouldSummarizeLiveFieldRunChecks();
    }

    private static void shouldRecordFirstSnapshotImmediately() {
        assertTrue(IffAutoFieldCheckSnapshot.shouldRecord(1000L, 0L),
                "first snapshot should be recorded immediately");
    }

    private static void shouldThrottleSnapshotsUntilIntervalPasses() {
        long last = 1000L;
        assertEquals(2000L, IffAutoFieldCheckSnapshot.INTERVAL_MS);
        assertFalse(IffAutoFieldCheckSnapshot.shouldRecord(last + 1999L, last),
                "snapshot before 2s field interval should be skipped");
        assertTrue(IffAutoFieldCheckSnapshot.shouldRecord(last + 2000L, last),
                "snapshot at 2s field interval should be recorded");
    }

    private static void shouldMapOfficeRoles() {
        assertEquals("PHONE_A_WITNESS", IffAutoFieldCheckSnapshot.officeTestRole("vasya"));
        assertEquals("PHONE_B_WITNESS", IffAutoFieldCheckSnapshot.officeTestRole("zhenya"));
        assertEquals("PHONE_C_MOVING_TARGET", IffAutoFieldCheckSnapshot.officeTestRole("petya"));
        assertEquals("PHONE_OPERATOR", IffAutoFieldCheckSnapshot.officeTestRole("local-you"));
        assertEquals("UNASSIGNED", IffAutoFieldCheckSnapshot.officeTestRole("unknown"));
        assertEquals("UNASSIGNED", IffAutoFieldCheckSnapshot.officeTestRole(null));
    }

    private static void shouldHideOutlier127FromHumanBleStatus() {
        assertEquals("rx zhenya no-valid-rssi out127=1",
                IffFieldSnapshotFormatter.bleRxStatus("zhenya", 127, "", 1));
        assertEquals("rx zhenya -88dBm out127=1",
                IffFieldSnapshotFormatter.bleRxStatus("zhenya", -88, "rx zhenya no-valid-rssi out127=1", 1));
    }

    private static void shouldFormatCompactWifiFingerprint() {
        IffFieldSnapshotFormatter.WifiEntry[] entries = new IffFieldSnapshotFormatter.WifiEntry[] {
                new IffFieldSnapshotFormatter.WifiEntry("Office B", "bb:bb:bb:bb:bb:bb", -42, 5180),
                new IffFieldSnapshotFormatter.WifiEntry("Office A", "aa:aa:aa:aa:aa:aa", -55, 2412),
                new IffFieldSnapshotFormatter.WifiEntry("Office C", "cc:cc:cc:cc:cc:cc", -80, 2412)
        };

        assertEquals("count=3 strongest=Office B/bb:bb:bb:bb:bb:bb@-42dBm/5180MHz;"
                        + "Office A/aa:aa:aa:aa:aa:aa@-55dBm/2412MHz",
                IffFieldSnapshotFormatter.wifiFingerprint(entries, 2));
    }

    private static void shouldClassifyWifiFingerprintFreshness() {
        assertEquals("empty", IffFieldSnapshotFormatter.wifiFreshness(0, -1L));
        assertEquals("unknown", IffFieldSnapshotFormatter.wifiFreshness(3, -1L));
        assertEquals("fresh", IffFieldSnapshotFormatter.wifiFreshness(3, 1200L));
        assertEquals("cached", IffFieldSnapshotFormatter.wifiFreshness(3, 5000L));
        assertEquals("stale", IffFieldSnapshotFormatter.wifiFreshness(3, 20000L));
    }

    private static void shouldClassifyOneSidedWallShadowProximity() {
        IffOfficeProximityVerdict.Sample sideA =
                IffOfficeProximityVerdict.Sample.window(true, -58, 3, 0, 400L);
        IffOfficeProximityVerdict.Sample sideB =
                IffOfficeProximityVerdict.Sample.window(true, -64, 3, 0, 400L);

        assertEquals("ONLY_A_VISIBLE",
                IffOfficeProximityVerdict.evaluate("petya", sideA, null).label);
        assertEquals("ONLY_B_VISIBLE",
                IffOfficeProximityVerdict.evaluate("petya", null, sideB).label);
        assertEquals("INSUFFICIENT_DATA",
                IffOfficeProximityVerdict.evaluate("petya", null, null).label);
    }

    private static void shouldClassifyDistanceFromFreshRssiWindow() {
        assertDistance("VERY_NEAR", 90, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -42, 8, 0, 500L), null));
        assertDistance("NEAR", 78, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -54, 8, 0, 500L), null));
        assertDistance("MID", 62, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -66, 8, 0, 500L), null));
        assertDistance("FAR", 45, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -78, 8, 0, 500L), null));
        assertDistance("EDGE", 30, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -91, 8, 0, 500L), null));
        assertDistance("LOST", 0, IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(false, 0, 0, 2, 1200L), null));
    }

    private static void shouldClassifyMovementTrendFromRssiDelta() {
        IffDistanceTrend.Snapshot approaching = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -62, 8, 0, 500L),
                IffDistanceTrend.Sample.window(true, -71, 8, 0, 2500L));
        assertEquals("APPROACHING", approaching.movementTrend);
        assertEquals(65L, approaching.movementConfidence);
        assertEquals(9L, approaching.movementRssiDeltaDb);

        IffDistanceTrend.Snapshot leaving = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -78, 8, 0, 500L),
                IffDistanceTrend.Sample.window(true, -66, 8, 0, 2500L));
        assertEquals("LEAVING", leaving.movementTrend);
        assertEquals(-12L, leaving.movementRssiDeltaDb);

        IffDistanceTrend.Snapshot stable = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -64, 8, 0, 500L),
                IffDistanceTrend.Sample.window(true, -66, 8, 0, 2500L));
        assertEquals("STABLE", stable.movementTrend);
        assertEquals(35L, stable.movementConfidence);

        IffDistanceTrend.Snapshot unknown = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -64, 1, 0, 500L), null);
        assertEquals("UNKNOWN", unknown.movementTrend);
        assertEquals(0L, unknown.movementConfidence);
    }

    private static void shouldClassifyGpsFreshnessForFieldRuns() {
        assertEquals("GPS_OK", IffGpsSnapshot.from(1200L, true, 12.4f, true, 86.0f).status);
        assertEquals("12", IffGpsSnapshot.from(1200L, true, 12.4f, true, 86.0f).fieldValue(
                IffGpsSnapshot.from(1200L, true, 12.4f, true, 86.0f).accuracyM));
        assertEquals("GPS_WEAK", IffGpsSnapshot.from(1200L, true, 61.0f, false, 0.0f).status);
        assertEquals("GPS_WEAK", IffGpsSnapshot.from(1200L, true, 100.0f, false, 0.0f).status);
        assertEquals("GPS_STALE", IffGpsSnapshot.from(16000L, true, 10.0f, false, 0.0f).status);
        assertEquals("GPS_UNAVAILABLE", IffGpsSnapshot.unavailable().status);
        assertEquals("na", IffGpsSnapshot.unavailable().fieldValue(IffGpsSnapshot.unavailable().accuracyM));
    }

    private static void shouldComputeGpsDistanceAndBearingBetweenFreshFixes() {
        IffGpsSnapshot north = IffGpsSnapshot.fromPair(
                1000L, true, 8.0f, 55.755800, 37.617300,
                1200L, true, 10.0f, 55.756800, 37.617300);
        assertEquals("GPS_OK", north.status);
        assertEquals(111L, north.distanceM);
        assertEquals(0L, north.bearingDeg);
        assertContains(north.compact(), "dist=111m");
        assertContains(north.compact(), "brg=0deg");

        IffGpsSnapshot weak = IffGpsSnapshot.fromPair(
                1000L, true, 8.0f, 55.755800, 37.617300,
                1200L, true, 80.0f, 55.756800, 37.617300);
        assertEquals("GPS_WEAK", weak.status);
        assertEquals(111L, weak.distanceM);

        IffGpsSnapshot stale = IffGpsSnapshot.fromPair(
                1000L, true, 8.0f, 55.755800, 37.617300,
                20000L, true, 10.0f, 55.756800, 37.617300);
        assertEquals("GPS_STALE", stale.status);
        assertEquals("na", stale.fieldValue(stale.distanceM));
        assertEquals("na", stale.fieldValue(stale.bearingDeg));
    }

    private static void shouldRejectGpsOutlierWhenRadioSaysNearby() {
        IffGpsSnapshot outlier = IffGpsSnapshot.fromPair(
                1000L, true, 8.0f, 55.755800, 37.617300,
                1200L, true, 10.0f, 56.295800, 38.157300,
                "NEAR");

        assertEquals("GPS_OUTLIER", outlier.status);
        assertEquals("na", outlier.fieldValue(outlier.distanceM));
        assertEquals("na", outlier.fieldValue(outlier.bearingDeg));

        IffGpsSnapshot farAllowed = IffGpsSnapshot.fromPair(
                1000L, true, 8.0f, 55.755800, 37.617300,
                1200L, true, 10.0f, 56.295800, 38.157300,
                "LOST");

        assertEquals("GPS_OK", farAllowed.status);
        assertTrue(farAllowed.distanceM > 500, "far/lost radio class should not suppress large GPS distance");
    }

    private static void shouldRejectImpossibleLocalGpsJumpBeforeBleAdvertise() {
        IffGpsStabilizer stabilizer = new IffGpsStabilizer();

        IffGpsStabilizer.Decision first = stabilizer.evaluate(
                55.755800, 37.617300, true, 9.0f, 100000L, "gps");
        assertTrue(first.accepted, "first fresh GPS fix should be accepted");
        assertEquals("accepted_first", first.reason);

        IffGpsStabilizer.Decision jump = stabilizer.evaluate(
                56.295800, 38.157300, true, 8.0f, 103000L, "gps");
        assertFalse(jump.accepted, "60km jump after 3s should be rejected");
        assertEquals("rejected_jump", jump.reason);
        assertTrue(jump.jumpDistanceM > 500, "jump distance should be measured");

        IffGpsStabilizer.Decision nearby = stabilizer.evaluate(
                55.755900, 37.617300, true, 9.0f, 104000L, "gps");
        assertTrue(nearby.accepted, "nearby follow-up GPS fix should remain accepted");
        assertEquals("accepted", nearby.reason);
    }


    private static void shouldSummarizeLiveFieldRunChecks() {
        IffFieldRunSummary.reset("petya");
        IffFieldRunSummary.record(new IffFieldRunSummary.Check(
                "petya",
                "PHONE_C_MOVING_TARGET",
                "CLOSER_TO_B",
                "VERY_NEAR",
                "APPROACHING",
                "GPS_OK",
                "fresh"));
        IffFieldRunSummary.record(new IffFieldRunSummary.Check(
                "petya",
                "PHONE_C_MOVING_TARGET",
                "CLOSER_TO_A",
                "FAR",
                "LEAVING",
                "GPS_STALE",
                "cached"));

        String compact = IffFieldRunSummary.compact();
        assertContains(compact, "checks=2");
        assertContains(compact, "office=CLOSER_TO_B:1 CLOSER_TO_A:1");
        assertContains(compact, "distance=VERY_NEAR:1 FAR:1");
        assertContains(compact, "movement=APPROACHING:1 LEAVING:1");
        assertContains(compact, "gps=GPS_OK:1 GPS_STALE:1");
    }

    private static void assertDistance(String expectedClass, long expectedConfidence,
                                       IffDistanceTrend.Snapshot snapshot) {
        assertEquals(expectedClass, snapshot.distanceClass);
        assertEquals(expectedConfidence, snapshot.distanceConfidence);
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

    private static void assertEquals(String expected, String actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("expected " + expected + " but got " + actual);
        }
    }

    private static void assertContains(String actual, String expectedPart) {
        if (!actual.contains(expectedPart)) {
            throw new AssertionError("expected '" + actual + "' to contain '" + expectedPart + "'");
        }
    }
}
