import net.afterday.compas.iff.IffRemoteWitnessFrame;

public final class IffRemoteWitnessFrameTest {
    public static void main(String[] args) {
        shouldRoundTripGpsFields();
        shouldParseLegacyFrameWithoutGps();
    }

    private static void shouldRoundTripGpsFields() {
        IffRemoteWitnessFrame frame = new IffRemoteWitnessFrame(
                "iff-remote-witness-v1",
                "phone a",
                "vasya",
                "BLE_IFF_VASYA",
                "ble:aa",
                -52,
                2412,
                900L,
                "SIGNATURE_PENDING",
                557558000,
                376173000,
                8,
                1100L);

        String wire = frame.toWire();
        assertContains(wire, "|gpsLatE7=557558000");
        assertContains(wire, "|gpsLonE7=376173000");
        assertContains(wire, "|gpsAccuracyM=8");
        assertContains(wire, "|gpsAgeMs=1100");

        IffRemoteWitnessFrame parsed = IffRemoteWitnessFrame.parse(wire);
        assertEquals("phone-a", parsed.sourcePlayerId);
        assertEquals("vasya", parsed.targetPlayerId);
        assertEquals(-52L, parsed.rssi);
        assertTrue(parsed.hasGps(), "parsed frame should have gps");
        assertEquals(557558000L, parsed.gpsLatE7);
        assertEquals(376173000L, parsed.gpsLonE7);
        assertEquals(8L, parsed.gpsAccuracyM);
        assertEquals(1100L, parsed.gpsAgeMs);
    }

    private static void shouldParseLegacyFrameWithoutGps() {
        IffRemoteWitnessFrame parsed = IffRemoteWitnessFrame.parse(
                "COMPASS_IFF_REMOTE|v=iff-remote-witness-v1|source=debug-oneplus"
                        + "|target=petya|ssid=BLE_IFF_PETYA|bssid=ble:bb"
                        + "|rssi=-63|freq=2412|ageMs=2500|signature=SIGNATURE_PENDING");

        assertEquals("debug-oneplus", parsed.sourcePlayerId);
        assertEquals("petya", parsed.targetPlayerId);
        assertFalse(parsed.hasGps(), "legacy frame should not have gps");
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
