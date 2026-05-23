import net.afterday.compas.iff.IffWifiTargetLocator;

public final class IffWifiTargetLocatorTest {
    public static void main(String[] args) {
        reportsTwelveOClockWhenAnchorsHearSimilarPower();
        reportsRightClockWhenRightAnchorIsStronger();
        reportsLeftClockWhenLeftAnchorIsStronger();
        bucketsDistanceFromMeanRssi();
        reportsInsufficientDataWhenAnchorMissing();
    }

    private static void reportsTwelveOClockWhenAnchorsHearSimilarPower() {
        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetLocator.estimate(-61, 4, -59, 5);

        assertEquals("12", snapshot.clockDirection);
        assertEquals("MEDIUM", snapshot.confidence);
    }

    private static void reportsRightClockWhenRightAnchorIsStronger() {
        IffWifiTargetLocator.Snapshot one = IffWifiTargetLocator.estimate(-66, 4, -60, 5);
        IffWifiTargetLocator.Snapshot two = IffWifiTargetLocator.estimate(-72, 4, -61, 5);
        IffWifiTargetLocator.Snapshot three = IffWifiTargetLocator.estimate(-78, 4, -60, 5);

        assertEquals("1", one.clockDirection);
        assertEquals("2", two.clockDirection);
        assertEquals("3", three.clockDirection);
    }

    private static void reportsLeftClockWhenLeftAnchorIsStronger() {
        IffWifiTargetLocator.Snapshot eleven = IffWifiTargetLocator.estimate(-60, 4, -66, 5);
        IffWifiTargetLocator.Snapshot ten = IffWifiTargetLocator.estimate(-61, 4, -72, 5);
        IffWifiTargetLocator.Snapshot nine = IffWifiTargetLocator.estimate(-60, 4, -78, 5);

        assertEquals("11", eleven.clockDirection);
        assertEquals("10", ten.clockDirection);
        assertEquals("9", nine.clockDirection);
    }

    private static void bucketsDistanceFromMeanRssi() {
        assertEquals(5, IffWifiTargetLocator.estimate(-44, 4, -46, 4).distanceBucketM);
        assertEquals(10, IffWifiTargetLocator.estimate(-54, 4, -56, 4).distanceBucketM);
        assertEquals(15, IffWifiTargetLocator.estimate(-61, 4, -63, 4).distanceBucketM);
        assertEquals(20, IffWifiTargetLocator.estimate(-67, 4, -69, 4).distanceBucketM);
        assertEquals(25, IffWifiTargetLocator.estimate(-72, 4, -74, 4).distanceBucketM);
    }

    private static void reportsInsufficientDataWhenAnchorMissing() {
        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetLocator.estimate(-60, 0, -62, 5);

        assertEquals("INSUFFICIENT_DATA", snapshot.status);
        assertEquals("na", snapshot.clockDirection);
        assertEquals(-1, snapshot.distanceBucketM);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
