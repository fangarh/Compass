import net.afterday.compas.iff.IffWifiTargetLocator;
import net.afterday.compas.iff.IffWifiTargetObservationStore;

public final class IffWifiTargetObservationStoreTest {
    public static void main(String[] args) {
        combinesLocalAndRemoteAnchorObservations();
        averagesFreshSamplesBeforeEstimatingDirection();
        ignoresStaleRemoteObservation();
    }

    private static void combinesLocalAndRemoteAnchorObservations() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -66, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -60, 2000L);

        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(3000L);

        assertEquals("OK", snapshot.status);
        assertEquals("1", snapshot.clockDirection);
        assertEquals(15, snapshot.distanceBucketM);
    }

    private static void ignoresStaleRemoteObservation() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -66, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -60, 2000L);

        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(60000L);

        assertEquals("INSUFFICIENT_DATA", snapshot.status);
    }

    private static void averagesFreshSamplesBeforeEstimatingDirection() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -50, 1000L);
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -50, 2000L);
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "petya", -50, 3000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -60, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -60, 2000L);
        IffWifiTargetObservationStore.updateRemoteObservation("zhenya", "petya", -45, 3000L);

        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(3500L);

        assertEquals("OK", snapshot.status);
        assertEquals("11", snapshot.clockDirection);
        assertEquals("HIGH", snapshot.confidence);
        assertEquals(10, snapshot.distanceBucketM);
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
