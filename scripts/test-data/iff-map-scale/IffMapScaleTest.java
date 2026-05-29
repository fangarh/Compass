import net.afterday.compas.iff.IffMapScale;

public final class IffMapScaleTest {
    public static void main(String[] args) {
        usesExistingSeventyFiveMeterDefault();
        zoomsInAndOutAcrossFixedRanges();
        clampsAtMinimumAndMaximumRanges();
        mapsDistanceToStableRadarRadius();
    }

    private static void usesExistingSeventyFiveMeterDefault() {
        assertEquals(75, IffMapScale.defaultRangeMeters());
        assertEquals("SCALE 75m", IffMapScale.label(75));
    }

    private static void zoomsInAndOutAcrossFixedRanges() {
        assertEquals(50, IffMapScale.zoomInRangeMeters(75));
        assertEquals(25, IffMapScale.zoomInRangeMeters(50));
        assertEquals(150, IffMapScale.zoomOutRangeMeters(75));
        assertEquals(300, IffMapScale.zoomOutRangeMeters(150));
    }

    private static void clampsAtMinimumAndMaximumRanges() {
        assertEquals(25, IffMapScale.zoomInRangeMeters(25));
        assertEquals(300, IffMapScale.zoomOutRangeMeters(300));
    }

    private static void mapsDistanceToStableRadarRadius() {
        assertClose(0.06f, IffMapScale.screenRadius(1, 75));
        assertClose(0.225f, IffMapScale.screenRadius(37.5, 75));
        assertClose(0.45f, IffMapScale.screenRadius(75, 75));
        assertClose(0.45f, IffMapScale.screenRadius(150, 75));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }

    private static void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.001f) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
