import java.util.Map;
import net.afterday.compas.iff.IffWifiDirectPayload;

public final class IffWifiDirectPayloadTest {
    public static void main(String[] args) {
        roundTripsWifiDirectTxtPayload();
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

    private static void assertNull(Object value, String message) {
        if (value != null) {
            throw new AssertionError(message);
        }
    }
}
