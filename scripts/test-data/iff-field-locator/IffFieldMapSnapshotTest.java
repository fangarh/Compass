package net.afterday.compas.iff;

public final class IffFieldMapSnapshotTest {
    private IffFieldMapSnapshotTest() {
    }

    public static void run() {
        showsTwoAnchorClockPosition();
        fallsBackToRingWithoutDirectionForOneAnchor();
        treatsRssi127AsMissingAnchor();
    }

    private static void showsTwoAnchorClockPosition() {
        IffWifiTargetLocator.Snapshot wifi = IffWifiTargetLocator.estimate(-66, 1, -54, 1);
        IffFieldLocatorSnapshot locator = IffFieldLocatorSnapshot.from(
                wifi,
                IffDistanceTrend.evaluate(null, null),
                IffGpsSnapshot.unavailable());

        IffFieldMapSnapshot map = IffFieldMapSnapshot.from(
                locator,
                "target=zhenya left=vasya:-66 ageMs=1000 right=petya:-54 ageMs=1000 locator=15m clock=2");

        assertEquals("TWO_ANCHORS", map.readiness, "readiness");
        assertEquals("WIFI_TARGET", map.source, "source");
        assertEquals("2", map.clockDirection, "clock");
        assertTrue(map.directionKnown, "direction known");
        assertTrue(map.targetVisible, "target visible");
        assertTrue(map.targetX > 0.5f, "clock 2 is right of center");
        assertTrue(map.targetY < 0.64f, "clock 2 is forward of anchor line");
    }

    private static void fallsBackToRingWithoutDirectionForOneAnchor() {
        IffDistanceTrend.Snapshot radio = IffDistanceTrend.evaluate(
                IffDistanceTrend.Sample.window(true, -55, 5, 0, 1000L),
                null);
        IffFieldLocatorSnapshot locator = IffFieldLocatorSnapshot.from(
                IffWifiTargetLocator.estimate(0, 0, 0, 0),
                radio,
                IffGpsSnapshot.unavailable());

        IffFieldMapSnapshot map = IffFieldMapSnapshot.from(
                locator,
                "target=zhenya left=vasya:-55 ageMs=1000 right=petya:missing locator=INSUFFICIENT_DATA");

        assertEquals("ONE_ANCHOR", map.readiness, "readiness");
        assertEquals("FIELD_RADIO_RSSI", map.source, "source");
        assertEquals("na", map.clockDirection, "clock");
        assertTrue(!map.directionKnown, "direction unknown");
        assertTrue(map.targetVisible, "ring visible");
        assertTrue(map.targetX == 0.5f, "ring fallback stays centered");
    }

    private static void treatsRssi127AsMissingAnchor() {
        IffFieldMapSnapshot map = IffFieldMapSnapshot.from(
                IffFieldLocatorSnapshot.from(
                        IffWifiTargetLocator.estimate(0, 0, 0, 0),
                        IffDistanceTrend.evaluate(null, null),
                        IffGpsSnapshot.unavailable()),
                "target=zhenya left=vasya:-55 ageMs=1000 right=petya:127 ageMs=20 locator=INSUFFICIENT_DATA");

        assertEquals("ONE_ANCHOR", map.readiness, "127 readiness");
        assertTrue(!map.directionKnown, "127 cannot create direction");
    }

    private static void assertEquals(String expected, String actual, String label) {
        if (!expected.equals(actual)) {
            throw new AssertionError(label + ": expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String label) {
        if (!condition) {
            throw new AssertionError(label);
        }
    }
}
