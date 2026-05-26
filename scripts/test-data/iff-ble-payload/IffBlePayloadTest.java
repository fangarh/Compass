import net.afterday.compas.iff.IffBlePayload;
import net.afterday.compas.iff.IffBleAdvertiseRestartPolicy;
import net.afterday.compas.iff.IffBleGpsAgeTracker;
import net.afterday.compas.iff.IffBleScanRetryPolicy;

public final class IffBlePayloadTest {
    public static void main(String[] args) {
        parsesLegacyPlayerPayload();
        roundTripsGpsPayload();
        treatsGpsAgeOnlyChangeAsSameAdvertiseContent();
        growsBleGpsAgeForRepeatedAdvertiseContent();
        restartsAdvertiserWhenItIsNotAdvertising();
        throttlesAdvertiseRestartWhileStartIsPending();
        throttlesGpsContentAdvertiseRestarts();
        backsOffBleScanRetryAfterRegistrationFailure();
        rejectsInvalidPayloads();
    }

    private static void parsesLegacyPlayerPayload() {
        byte[] payload = IffBlePayload.forPlayer(2);

        assertEquals(4, payload.length);
        IffBlePayload.Parsed parsed = IffBlePayload.parse(payload);

        assertNotNull(parsed, "legacy payload should parse");
        assertEquals(1, parsed.contractVersion);
        assertEquals(2, parsed.playerCode);
        assertFalse(parsed.hasGps, "legacy payload should not contain GPS");
    }

    private static void roundTripsGpsPayload() {
        byte[] payload = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                4200L);

        assertEquals(17, payload.length);
        IffBlePayload.Parsed parsed = IffBlePayload.parse(payload);

        assertNotNull(parsed, "GPS payload should parse");
        assertEquals(2, parsed.contractVersion);
        assertEquals(1, parsed.playerCode);
        assertTrue(parsed.hasGps, "v2 payload should contain GPS");
        assertEquals(557558000, parsed.gpsLatE7);
        assertEquals(376173000, parsed.gpsLonE7);
        assertEquals(12, parsed.gpsAccuracyM);
        assertEquals(4200L, parsed.gpsAgeMs);
    }

    private static void treatsGpsAgeOnlyChangeAsSameAdvertiseContent() {
        byte[] first = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                1000L);
        byte[] second = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                2000L);
        byte[] moved = IffBlePayload.forPlayerWithGps(
                1,
                557558500,
                376173000,
                12,
                2000L);

        assertTrue(
                IffBlePayload.sameAdvertiseContent(first, second),
                "age-only GPS change should not force BLE advertiser restart");
        assertFalse(
                IffBlePayload.sameAdvertiseContent(first, moved),
                "coordinate change should force BLE advertiser restart");
    }

    private static void growsBleGpsAgeForRepeatedAdvertiseContent() {
        IffBleGpsAgeTracker tracker = new IffBleGpsAgeTracker();
        byte[] payload = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                1000L);

        assertEquals(1000L, tracker.effectiveGpsAgeMs("zhenya/aa", payload, 5000L));
        assertEquals(4000L, tracker.effectiveGpsAgeMs("zhenya/aa", payload, 8000L));

        byte[] moved = IffBlePayload.forPlayerWithGps(
                1,
                557558500,
                376173000,
                12,
                1200L);
        assertEquals(1200L, tracker.effectiveGpsAgeMs("zhenya/aa", moved, 9000L));
    }

    private static void restartsAdvertiserWhenItIsNotAdvertising() {
        byte[] first = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                1000L);
        byte[] ageOnly = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                2000L);

        assertFalse(
                IffBleAdvertiseRestartPolicy.shouldRestart(true, true, true, first, ageOnly),
                "active advertiser should not restart for age-only GPS changes");
        assertTrue(
                IffBleAdvertiseRestartPolicy.shouldRestart(true, true, false, first, ageOnly),
                "inactive advertiser should restart even when payload content did not change");
    }

    private static void throttlesAdvertiseRestartWhileStartIsPending() {
        byte[] first = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                1000L);
        byte[] moved = IffBlePayload.forPlayerWithGps(
                1,
                557558500,
                376173000,
                12,
                2000L);

        assertFalse(
                IffBleAdvertiseRestartPolicy.shouldRestart(
                        true, true, false, true, 1000L, 5000L, first, moved),
                "pending advertiser start should not be restarted inside the grace window");
        assertTrue(
                IffBleAdvertiseRestartPolicy.shouldRestart(
                        true, true, false, true, 1000L, 6000L, first, moved),
                "stale pending advertiser start can be retried after the grace window");
    }

    private static void throttlesGpsContentAdvertiseRestarts() {
        byte[] first = IffBlePayload.forPlayerWithGps(
                1,
                557558000,
                376173000,
                12,
                1000L);
        byte[] moved = IffBlePayload.forPlayerWithGps(
                1,
                557558500,
                376173000,
                12,
                2000L);

        assertFalse(
                IffBleAdvertiseRestartPolicy.shouldRestart(
                        true, true, true, false, 1000L, 5000L, first, moved),
                "GPS content changes should not restart BLE advertising inside throttle");
        assertTrue(
                IffBleAdvertiseRestartPolicy.shouldRestart(
                        true, true, true, false, 1000L, 6000L, first, moved),
                "GPS content changes can restart BLE advertising after the throttle interval");
    }

    private static void backsOffBleScanRetryAfterRegistrationFailure() {
        assertEquals(2000L, IffBleScanRetryPolicy.delayMs(1));
        assertEquals(5000L, IffBleScanRetryPolicy.delayMs(2));
        assertEquals(10000L, IffBleScanRetryPolicy.delayMs(3));
        assertEquals(10000L, IffBleScanRetryPolicy.delayMs(7));
    }

    private static void rejectsInvalidPayloads() {
        assertNull(IffBlePayload.parse(null), "null payload should be invalid");
        assertNull(IffBlePayload.parse(new byte[] {0x43, 0x49, 2}), "short v2 payload should be invalid");
        assertNull(IffBlePayload.parse(new byte[] {0x42, 0x49, 1, 1}), "wrong marker should be invalid");
    }

    private static void assertEquals(long expected, long actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
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
