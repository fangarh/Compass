package net.afterday.compas.iff;

public final class IffTargetObservationPolicyTest {
    public static void run() {
        recordsAnchorObservationForTarget();
        rejectsTargetPhoneAsAnchor();
        rejectsNonTargetObservation();
    }

    private static void recordsAnchorObservationForTarget() {
        if (!IffTargetObservationPolicy.shouldRecordAnchorObservation("vasya", "zhenya")) {
            throw new AssertionError("vasya should record zhenya target RSSI");
        }
        if (!IffTargetObservationPolicy.shouldRecordAnchorObservation("petya", "zhenya")) {
            throw new AssertionError("petya should record zhenya target RSSI");
        }
    }

    private static void rejectsTargetPhoneAsAnchor() {
        if (IffTargetObservationPolicy.shouldRecordAnchorObservation("zhenya", "zhenya")) {
            throw new AssertionError("target phone must not act as its own anchor");
        }
    }

    private static void rejectsNonTargetObservation() {
        if (IffTargetObservationPolicy.shouldRecordAnchorObservation("vasya", "petya")) {
            throw new AssertionError("non-target RSSI must not update target locator");
        }
    }
}
