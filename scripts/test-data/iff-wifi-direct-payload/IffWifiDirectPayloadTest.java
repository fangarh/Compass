import java.util.Map;
import net.afterday.compas.iff.IffWifiDirectPayload;

public final class IffWifiDirectPayloadTest {
    public static void main(String[] args) {
        roundTripsWifiDirectTxtPayload();
        roundTripsWifiDirectTxtPayloadWithOwnGps();
        roundTripsWifiDirectTxtPayloadWithRelayedTargetGps();
        carriesCoordinateMessage();
        roundTripsWifiDirectInstanceNamePayload();
        roundTripsWifiDirectInstanceNameTargetObservation();
        rejectsUnknownContract();
    }

    private static void roundTripsWifiDirectTxtPayload() {
        Map<String, String> txt = IffWifiDirectPayload.build("zhenya", 7L, 123456L);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txt);

        assertNotNull(parsed, "payload should parse");
        assertEquals("zhenya", parsed.playerId);
        assertEquals(7L, parsed.sequence);
        assertEquals(123456L, parsed.timestampMs);
    }

    private static void roundTripsWifiDirectTxtPayloadWithOwnGps() {
        Map<String, String> txt = IffWifiDirectPayload.build(
                "zhenya",
                8L,
                123456L,
                599916522,
                303270116,
                4,
                1200L);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txt);

        assertNotNull(parsed, "payload should parse");
        assertTrue(parsed.hasGps, "own gps should parse");
        assertEquals(599916522L, parsed.gpsLatE7);
        assertEquals(303270116L, parsed.gpsLonE7);
        assertEquals(4L, parsed.gpsAccuracyM);
        assertEquals(1200L, parsed.gpsAgeMs);
    }

    private static void roundTripsWifiDirectTxtPayloadWithRelayedTargetGps() {
        Map<String, String> txt = IffWifiDirectPayload.build(
                "vasya",
                9L,
                123456L,
                "petya",
                -63,
                599916700,
                303270300,
                6,
                900L);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txt);

        assertNotNull(parsed, "payload should parse");
        assertEquals("petya", parsed.targetPlayerId);
        assertEquals(-63, parsed.targetRssi);
        assertTrue(parsed.hasTargetGps, "target gps should parse");
        assertEquals(599916700L, parsed.targetGpsLatE7);
        assertEquals(303270300L, parsed.targetGpsLonE7);
        assertEquals(6L, parsed.targetGpsAccuracyM);
        assertEquals(900L, parsed.targetGpsAgeMs);
    }

    private static void carriesCoordinateMessage() {
        String coordinateMessage = "CIFF2|vasya|10|vasya,vasya,59.9916522,30.3270116,4.0,123456,0,-2147483648,0";
        Map<String, String> txt = IffWifiDirectPayload.build("vasya", 10L, 123456L);
        IffWifiDirectPayload.putCoordinateMessage(txt, coordinateMessage);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parse(txt);

        assertNotNull(parsed, "payload should parse");
        assertEquals(coordinateMessage, parsed.coordinateMessage);
    }

    private static void roundTripsWifiDirectInstanceNamePayload() {
        String instanceName = IffWifiDirectPayload.buildInstanceName("vasya", 12L);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parseInstanceName(instanceName);

        assertNotNull(parsed, "instance name payload should parse");
        assertEquals("vasya", parsed.playerId);
        assertEquals(12L, parsed.sequence);
    }

    private static void roundTripsWifiDirectInstanceNameTargetObservation() {
        String instanceName = IffWifiDirectPayload.buildInstanceName("vasya", 12L, "zhenya", -64);

        IffWifiDirectPayload.Parsed parsed = IffWifiDirectPayload.parseInstanceName(instanceName);

        assertNotNull(parsed, "target observation instance name should parse");
        assertEquals("vasya", parsed.playerId);
        assertEquals(12L, parsed.sequence);
        assertEquals("zhenya", parsed.targetPlayerId);
        assertEquals(-64, parsed.targetRssi);
    }

    private static void rejectsUnknownContract() {
        Map<String, String> txt = IffWifiDirectPayload.build("zhenya", 7L, 123456L);
        txt.put(IffWifiDirectPayload.KEY_CONTRACT, "other");

        assertNull(IffWifiDirectPayload.parse(txt), "unknown contract should be ignored");
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertNotNull(Object value, String message) {
        if (value == null) {
            throw new AssertionError(message);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message);
        }
    }
}
