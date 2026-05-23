package net.afterday.compas.iff;

import android.os.SystemClock;

public final class IffRemoteWitnessReport {
    public static final String CONTRACT_VERSION = "iff-remote-witness-v1";
    public static final String SIGNATURE_PENDING = "SIGNATURE_PENDING";

    public final String sourcePlayerId;
    public final String targetPlayerId;
    public final String targetBeaconSsid;
    public final String bssid;
    public final int rssi;
    public final int frequency;
    public final long observedElapsedMs;
    public final long receivedElapsedMs;
    public final String signatureStatus;
    public final int gpsLatE7;
    public final int gpsLonE7;
    public final int gpsAccuracyM;
    public final long gpsObservedElapsedMs;

    public IffRemoteWitnessReport(String sourcePlayerId, String targetPlayerId, String targetBeaconSsid,
                                  String bssid, int rssi, int frequency, long observedElapsedMs,
                                  long receivedElapsedMs, String signatureStatus) {
        this(sourcePlayerId, targetPlayerId, targetBeaconSsid, bssid, rssi, frequency,
                observedElapsedMs, receivedElapsedMs, signatureStatus,
                IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT,
                IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT,
                IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT,
                -1L);
    }

    public IffRemoteWitnessReport(String sourcePlayerId, String targetPlayerId, String targetBeaconSsid,
                                  String bssid, int rssi, int frequency, long observedElapsedMs,
                                  long receivedElapsedMs, String signatureStatus,
                                  int gpsLatE7, int gpsLonE7, int gpsAccuracyM,
                                  long gpsObservedElapsedMs) {
        this.sourcePlayerId = safe(sourcePlayerId);
        this.targetPlayerId = safe(targetPlayerId);
        this.targetBeaconSsid = safe(targetBeaconSsid);
        this.bssid = safe(bssid);
        this.rssi = rssi;
        this.frequency = frequency;
        this.observedElapsedMs = observedElapsedMs;
        this.receivedElapsedMs = receivedElapsedMs;
        this.signatureStatus = safe(signatureStatus);
        this.gpsLatE7 = gpsLatE7;
        this.gpsLonE7 = gpsLonE7;
        this.gpsAccuracyM = gpsAccuracyM;
        this.gpsObservedElapsedMs = gpsObservedElapsedMs;
    }

    public long ageMs() {
        return Math.max(0L, SystemClock.elapsedRealtime() - this.observedElapsedMs);
    }

    public boolean isFresh() {
        return ageMs() <= IffRadioWitnessStore.FRESH_MS;
    }

    public boolean isStale() {
        long age = ageMs();
        return age > IffRadioWitnessStore.FRESH_MS && age <= IffRadioWitnessStore.STALE_MS;
    }

    public String freshnessLabel() {
        if (isFresh()) {
            return "REMOTE_FRESH";
        }
        if (isStale()) {
            return "REMOTE_STALE";
        }
        return "REMOTE_UNKNOWN";
    }

    public boolean hasSignatureProof() {
        return !SIGNATURE_PENDING.equals(signatureStatus) && signatureStatus.length() > 0;
    }

    public boolean hasGpsFix() {
        return gpsLatE7 != IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT
                && gpsLonE7 != IffRemoteWitnessFrame.GPS_UNAVAILABLE_INT
                && gpsAccuracyM >= 0
                && gpsObservedElapsedMs > 0L;
    }

    public long gpsAgeMs() {
        if (!hasGpsFix()) {
            return -1L;
        }
        return Math.max(0L, SystemClock.elapsedRealtime() - gpsObservedElapsedMs);
    }

    public double gpsLatitude() {
        return IffRemoteWitnessFrame.coordinateFromE7(gpsLatE7);
    }

    public double gpsLongitude() {
        return IffRemoteWitnessFrame.coordinateFromE7(gpsLonE7);
    }

    public boolean hasValidShape() {
        return sourcePlayerId.length() > 0
                && targetPlayerId.length() > 0
                && !sourcePlayerId.equals(targetPlayerId)
                && targetBeaconSsid.length() > 0
                && observedElapsedMs > 0L
                && receivedElapsedMs > 0L;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }
}
