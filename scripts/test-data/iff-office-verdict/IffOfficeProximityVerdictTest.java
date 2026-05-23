import net.afterday.compas.iff.IffOfficeProximityVerdict;

public final class IffOfficeProximityVerdictTest {
    public static void main(String[] args) {
        reportsCloserToAWhenAMovingTargetSignalIsClearlyStronger();
        reportsCloserToBWhenBMovingTargetSignalIsClearlyStronger();
        reportsAmbiguousWhenSignalsAreTooClose();
        reportsOnlyVisibleSideWhenOneSideIsMissingOrOutlier();
        reportsInsufficientDataWhenBothSidesAreMissingOrOutliers();
        reportsInsufficientDataWhenLocalDeviceIsNotMovingTarget();
        reportsCloserToAAfterPairCalibrationRemovesDeviceBias();
        reportsAmbiguousWhenCalibratedSignalsAreTooClose();
    }

    private static void reportsCloserToAWhenAMovingTargetSignalIsClearlyStronger() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-41, 5),
                window(-66, 6));

        assertEquals("CLOSER_TO_A", snapshot.label);
        assertEquals(25, snapshot.deltaDb);
    }

    private static void reportsCloserToBWhenBMovingTargetSignalIsClearlyStronger() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-79, 5),
                window(-52, 6));

        assertEquals("CLOSER_TO_B", snapshot.label);
        assertEquals(-27, snapshot.deltaDb);
    }

    private static void reportsAmbiguousWhenSignalsAreTooClose() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-58, 5),
                window(-63, 6));

        assertEquals("BETWEEN_OR_AMBIGUOUS", snapshot.label);
        assertEquals(5, snapshot.deltaDb);
    }

    private static void reportsOnlyVisibleSideWhenOneSideIsMissingOrOutlier() {
        IffOfficeProximityVerdict.Snapshot missing = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-50, 5),
                null);
        IffOfficeProximityVerdict.Snapshot outlier = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-50, 5),
                IffOfficeProximityVerdict.Sample.window(true, 0, 0, 5, 100L));

        assertEquals("ONLY_A_VISIBLE", missing.label);
        assertEquals("ONLY_A_VISIBLE", outlier.label);
    }

    private static void reportsInsufficientDataWhenBothSidesAreMissingOrOutliers() {
        IffOfficeProximityVerdict.Snapshot missing = IffOfficeProximityVerdict.evaluate(
                "petya",
                null,
                null);
        IffOfficeProximityVerdict.Snapshot outlier = IffOfficeProximityVerdict.evaluate(
                "petya",
                IffOfficeProximityVerdict.Sample.window(true, 0, 0, 5, 100L),
                IffOfficeProximityVerdict.Sample.window(true, 0, 0, 5, 100L));

        assertEquals("INSUFFICIENT_DATA", missing.label);
        assertEquals("INSUFFICIENT_DATA", outlier.label);
    }

    private static void reportsInsufficientDataWhenLocalDeviceIsNotMovingTarget() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "vasya",
                window(-40, 5),
                window(-80, 6));

        assertEquals("INSUFFICIENT_DATA", snapshot.label);
    }

    private static void reportsCloserToAAfterPairCalibrationRemovesDeviceBias() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-50, 10),
                window(-47, 10),
                IffOfficeProximityVerdict.Calibration.fixed(-61, -47));

        assertEquals("CLOSER_TO_A", snapshot.label);
        assertEquals(11, snapshot.deltaDb);
    }

    private static void reportsAmbiguousWhenCalibratedSignalsAreTooClose() {
        IffOfficeProximityVerdict.Snapshot snapshot = IffOfficeProximityVerdict.evaluate(
                "petya",
                window(-56, 10),
                window(-47, 10),
                IffOfficeProximityVerdict.Calibration.fixed(-61, -47));

        assertEquals("BETWEEN_OR_AMBIGUOUS", snapshot.label);
        assertEquals(5, snapshot.deltaDb);
    }

    private static IffOfficeProximityVerdict.Sample window(int averageRssi, int validCount) {
        return IffOfficeProximityVerdict.Sample.window(true, averageRssi, validCount, 0, 100L);
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but was " + actual);
        }
    }
}
