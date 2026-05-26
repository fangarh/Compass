package net.afterday.compas.iff;

public final class IffFieldLocatorSnapshotTest {
    public static void main(String[] args) {
        prefersGpsPairOverWifiAndRadioGeometry();
        prefersWifiTargetEstimate();
        fallsBackToRadioDistanceWhenWifiMissing();
        usesGpsAsAssistedDistanceOnly();
        rejectsWeakGpsAsPrimaryLocator();
        reportsUnavailableWhenAllSourcesMissing();
        reportsTwoAnchorObservationReadiness();
        rejectsOldOppositeAnchorForTwoAnchorLocator();
        rejectsShadowedWeakAnchorForWifiDirection();
        rejectsMarginalShadowedWeakAnchorForWifiDirection();
        ignoresInvalidBleRssi127ForTargetObservation();
        IffTargetObservationPolicyTest.run();
        IffFieldMapSnapshotTest.run();
        IffOperatorFieldSnapshotStoreTest.run();
        System.out.println("IFF field locator snapshot test passed.");
    }

    private static void prefersGpsPairOverWifiAndRadioGeometry() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(-75, 3, -45, 3);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -44, 5, 0, 500L),
                null);
        IffGpsSnapshot gps = IffGpsSnapshot.fromPair(
                1000L,
                true,
                5.0f,
                55.0,
                37.0,
                1000L,
                true,
                6.0f,
                55.00018,
                37.0);

        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, gps);

        assertEquals("OK", snapshot.status, "gps-first status");
        assertEquals("GPS_ASSISTED", snapshot.source, "gps-first source");
        assertEquals(20, snapshot.distanceBucketM, "gps-first distance bucket");
        assertEquals("na", snapshot.clockDirection, "gps-first clock");
        assertContains(snapshot.compact(), "bearingDeg=0", "gps-first bearing");
    }

    private static void prefersWifiTargetEstimate() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(-65, 3, -54, 3);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -44, 5, 0, 500L),
                null);
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, IffGpsSnapshot.unavailable());

        assertEquals("OK", snapshot.status, "wifi status");
        assertEquals("WIFI_TARGET", snapshot.source, "wifi source");
        assertEquals(15, snapshot.distanceBucketM, "wifi distance bucket");
        assertEquals("2", snapshot.clockDirection, "wifi clock");
        assertContains(snapshot.compact(), "locator=WIFI_TARGET", "wifi compact source");
        assertContains(snapshot.compact(), "clock=2", "wifi compact clock");
    }

    private static void fallsBackToRadioDistanceWhenWifiMissing() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(0, 0, 0, 0);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -62, 5, 0, 500L),
                IffDistanceTrend.Sample.window(true, -67, 5, 0, 4500L));
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, IffGpsSnapshot.unavailable());

        assertEquals("OK", snapshot.status, "radio status");
        assertEquals("FIELD_RADIO_RSSI", snapshot.source, "radio source");
        assertEquals(15, snapshot.distanceBucketM, "radio distance bucket");
        assertEquals("na", snapshot.clockDirection, "radio clock");
        assertContains(snapshot.reason, "APPROACHING", "radio trend reason");
    }

    private static void usesGpsAsAssistedDistanceOnly() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(0, 0, 0, 0);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(null, null);
        IffGpsSnapshot gps = IffGpsSnapshot.fromPair(
                1000L,
                true,
                8.0f,
                55.0,
                37.0,
                1000L,
                true,
                8.0f,
                55.00009,
                37.0);
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, gps);

        assertEquals("OK", snapshot.status, "gps status");
        assertEquals("GPS_ASSISTED", snapshot.source, "gps source");
        assertEquals(10, snapshot.distanceBucketM, "gps distance bucket");
        assertEquals("na", snapshot.clockDirection, "gps clock");
        assertContains(snapshot.compact(), "bearingDeg=0", "gps bearing diagnostic");
    }

    private static void rejectsWeakGpsAsPrimaryLocator() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(0, 0, 0, 0);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(null, null);
        IffGpsSnapshot gps = IffGpsSnapshot.fromPair(
                1000L,
                true,
                100.0f,
                55.0,
                37.0,
                1000L,
                true,
                100.0f,
                55.5,
                37.5);
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, gps);

        assertEquals("INSUFFICIENT_DATA", snapshot.status, "weak gps status");
        assertEquals("NONE", snapshot.source, "weak gps source");
        assertContains(snapshot.reason, "GPS_WEAK", "weak gps reason");
    }

    private static void reportsUnavailableWhenAllSourcesMissing() {
        IffFieldLocatorSnapshot snapshot = IffFieldLocatorSnapshot.from(
                IffWifiTargetLocator.estimate(0, 0, 0, 0),
                IffDistanceTrend.evaluate(null, null),
                IffGpsSnapshot.unavailable());

        assertEquals("INSUFFICIENT_DATA", snapshot.status, "missing status");
        assertEquals("NONE", snapshot.source, "missing source");
        assertEquals(-1, snapshot.distanceBucketM, "missing distance");
        assertContains(snapshot.compact(), "locator=INSUFFICIENT_DATA", "missing compact");
    }

    private static void reportsTwoAnchorObservationReadiness() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -54, 1000L);

        String oneAnchor = IffWifiTargetObservationStore.compactStatus(2500L);

        assertContains(oneAnchor, "target=petya", "one-anchor target");
        assertContains(oneAnchor, "left=vasya:-54 ageMs=1500", "one-anchor left");
        assertContains(oneAnchor, "right=zhenya:missing", "one-anchor missing right");
        assertContains(oneAnchor, "locator=INSUFFICIENT_DATA", "one-anchor locator");

        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -66, 2000L);
        String twoAnchor = IffWifiTargetObservationStore.compactStatus(3000L);

        assertContains(twoAnchor, "left=vasya:-54 ageMs=2000", "two-anchor left");
        assertContains(twoAnchor, "right=zhenya:-66 ageMs=1000", "two-anchor right");
        assertContains(twoAnchor, "locator=15m clock=10", "two-anchor locator");
        IffWifiTargetObservationStore.resetForTest();
    }

    private static void rejectsOldOppositeAnchorForTwoAnchorLocator() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -49, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -42, 12000L);

        String status = IffWifiTargetObservationStore.compactStatus(13000L);
        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(14000L);

        status = IffWifiTargetObservationStore.compactStatus(14000L);

        assertContains(status, "left=vasya:old rssi=-49 ageMs=13000", "old left marker");
        assertContains(status, "right=zhenya:-42 ageMs=2000", "fresh right marker");
        assertContains(status, "locator=INSUFFICIENT_DATA", "old left cannot form locator");
        assertEquals("INSUFFICIENT_DATA", snapshot.status, "old left snapshot");
        IffWifiTargetObservationStore.resetForTest();
    }

    private static void rejectsShadowedWeakAnchorForWifiDirection() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(-92, 1, -45, 1);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -52, 10, 0, 500L),
                IffDistanceTrend.Sample.window(true, -62, 8, 0, 4500L));
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, IffGpsSnapshot.unavailable());

        assertEquals("OK", snapshot.status, "shadow fallback status");
        assertEquals("FIELD_RADIO_RSSI", snapshot.source, "shadow fallback source");
        assertEquals(10, snapshot.distanceBucketM, "shadow fallback distance");
        assertEquals("na", snapshot.clockDirection, "shadow fallback clock");
        assertEquals("INSUFFICIENT_DATA", wifi.status, "shadowed wifi status");
    }

    private static void rejectsMarginalShadowedWeakAnchorForWifiDirection() {
        IffWifiTargetLocator.Snapshot wifi =
                IffWifiTargetLocator.estimate(-93, 1, -74, 1);
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -74, 3, 0, 500L),
                IffDistanceTrend.Sample.window(true, -80, 2, 0, 4500L));
        IffFieldLocatorSnapshot snapshot =
                IffFieldLocatorSnapshot.from(wifi, radio, IffGpsSnapshot.unavailable());

        assertEquals("OK", snapshot.status, "marginal shadow fallback status");
        assertEquals("FIELD_RADIO_RSSI", snapshot.source, "marginal shadow fallback source");
        assertEquals(25, snapshot.distanceBucketM, "marginal shadow fallback distance");
        assertEquals("na", snapshot.clockDirection, "marginal shadow fallback clock");
        assertEquals("INSUFFICIENT_DATA", wifi.status, "marginal shadowed wifi status");
    }

    private static void ignoresInvalidBleRssi127ForTargetObservation() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -54, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", 127, 2000L);

        String status = IffWifiTargetObservationStore.compactStatus(3000L);

        assertContains(status, "left=vasya:-54 ageMs=2000", "valid left remains");
        assertContains(status, "right=zhenya:missing", "invalid right ignored");
        assertContains(status, "locator=INSUFFICIENT_DATA", "invalid right cannot form locator");
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -66, 3000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", 127, 3500L);

        String preserved = IffWifiTargetObservationStore.compactStatus(4000L);

        assertContains(preserved, "right=zhenya:-66 ageMs=1000", "invalid right cannot overwrite valid right");
        assertContains(preserved, "locator=15m clock=10", "valid two-anchor locator remains");
        IffWifiTargetObservationStore.resetForTest();
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual, String label) {
        if (expected != actual) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertContains(String actual, String expectedPart, String label) {
        if (actual == null || !actual.contains(expectedPart)) {
            throw new AssertionError(label + ": expected to contain " + expectedPart + " in " + actual);
        }
    }
}
