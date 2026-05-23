package net.afterday.compas.iff;

public final class IffOperatorFieldSnapshotStoreTest {
    private IffOperatorFieldSnapshotStoreTest() {
    }

    public static void run() {
        smoothsTwoAnchorClockAndDistance();
        holdsLastTwoAnchorFixDuringShortRelayGap();
    }

    private static void smoothsTwoAnchorClockAndDistance() {
        IffOperatorFieldSnapshotStore store = new IffOperatorFieldSnapshotStore();

        store.update(twoAnchor(15, "3"), 1000L);
        store.update(twoAnchor(20, "9"), 3000L);
        IffFieldMapSnapshot stable = store.update(twoAnchor(15, "3"), 5000L);

        assertEquals("TWO_ANCHORS", stable.readiness, "readiness");
        assertEquals("WIFI_TARGET_STABLE", stable.source, "source");
        assertEquals(15, stable.distanceBucketM, "distance");
        assertEquals("3", stable.clockDirection, "clock");
        assertTrue(stable.directionKnown, "direction known");
        assertContains(stable.statusLine, "stable", "stable status");
    }

    private static void holdsLastTwoAnchorFixDuringShortRelayGap() {
        IffOperatorFieldSnapshotStore store = new IffOperatorFieldSnapshotStore();

        store.update(twoAnchor(10, "10"), 1000L);
        store.update(twoAnchor(10, "10"), 3000L);
        IffFieldMapSnapshot held = store.update(oneAnchorFallback(), 5000L);

        assertEquals("TWO_ANCHORS", held.readiness, "held readiness");
        assertEquals("WIFI_TARGET_HOLD", held.source, "held source");
        assertEquals(10, held.distanceBucketM, "held distance");
        assertEquals("10", held.clockDirection, "held clock");
        assertTrue(held.directionKnown, "held direction known");
        assertContains(held.statusLine, "hold", "held status");
    }

    private static IffFieldMapSnapshot twoAnchor(int distanceM, String clock) {
        return IffFieldMapSnapshot.operatorSnapshot(
                "TWO_ANCHORS",
                "WIFI_TARGET",
                distanceM,
                clock,
                "raw " + distanceM + "m clock=" + clock);
    }

    private static IffFieldMapSnapshot oneAnchorFallback() {
        return IffFieldMapSnapshot.from(
                IffFieldLocatorSnapshot.from(
                        IffWifiTargetLocator.estimate(0, 0, 0, 0),
                        IffDistanceTrend.evaluate(
                                IffDistanceTrend.Sample.window(true, -58, 3, 0, 1000L),
                                null),
                        IffGpsSnapshot.unavailable()),
                "target=zhenya left=vasya:-58 ageMs=1000 right=petya:missing locator=INSUFFICIENT_DATA");
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

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static void assertContains(String actual, String expectedPart, String label) {
        if (actual == null || !actual.contains(expectedPart)) {
            throw new AssertionError(label + ": expected to contain " + expectedPart + " in " + actual);
        }
    }
}
