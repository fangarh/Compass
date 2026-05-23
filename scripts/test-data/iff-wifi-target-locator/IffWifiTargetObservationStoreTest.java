import net.afterday.compas.iff.IffWifiTargetLocator;
import net.afterday.compas.iff.IffWifiTargetObservationStore;

public final class IffWifiTargetObservationStoreTest {
    public static void main(String[] args) {
        combinesLocalAndRemoteAnchorObservations();
        ignoresStaleRemoteObservation();
    }

    private static void combinesLocalAndRemoteAnchorObservations() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "zhenya", -66, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("petya", "zhenya", -60, 2000L);

        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(3000L);

        assertEquals("OK", snapshot.status);
        assertEquals("1", snapshot.clockDirection);
        assertEquals(15, snapshot.distanceBucketM);
    }

    private static void ignoresStaleRemoteObservation() {
        IffWifiTargetObservationStore.resetForTest();
        IffWifiTargetObservationStore.updateLocalObservation("vasya", "zhenya", -66, 1000L);
        IffWifiTargetObservationStore.updateRemoteObservation("petya", "zhenya", -60, 2000L);

        IffWifiTargetLocator.Snapshot snapshot = IffWifiTargetObservationStore.snapshot(60000L);

        assertEquals("INSUFFICIENT_DATA", snapshot.status);
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
