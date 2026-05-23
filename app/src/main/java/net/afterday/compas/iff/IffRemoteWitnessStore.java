package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.afterday.compas.logging.FieldDiagnosticLog;

public final class IffRemoteWitnessStore {
    private static final Object LOCK = new Object();
    private static final Map<String, List<IffRemoteWitnessReport>> REPORTS_BY_TARGET = new HashMap<>();

    private IffRemoteWitnessStore() {
    }

    public static boolean receiveReport(IffRemoteWitnessReport report) {
        if (report == null || !report.hasValidShape()) {
            FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_rejected reason=invalid_shape"
                    + " contract=" + IffRemoteWitnessReport.CONTRACT_VERSION);
            return false;
        }
        synchronized (LOCK) {
            List<IffRemoteWitnessReport> reports = REPORTS_BY_TARGET.get(report.targetPlayerId);
            if (reports == null) {
                reports = new ArrayList<>();
                REPORTS_BY_TARGET.put(report.targetPlayerId, reports);
            }
            replaceSameSource(reports, report);
        }
        FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_received"
                + " contract=" + IffRemoteWitnessReport.CONTRACT_VERSION
                + " sourcePlayerId=" + report.sourcePlayerId
                + " targetPlayerId=" + report.targetPlayerId
                + " freshness=" + report.freshnessLabel()
                + " rssi=" + report.rssi
                + " ageMs=" + report.ageMs()
                + " signatureStatus=" + report.signatureStatus);
        return true;
    }

    public static List<IffRemoteWitnessReport> getReportsFor(String targetPlayerId) {
        synchronized (LOCK) {
            List<IffRemoteWitnessReport> reports = REPORTS_BY_TARGET.get(targetPlayerId);
            if (reports == null) {
                return new ArrayList<>();
            }
            return new ArrayList<>(reports);
        }
    }

    public static IffRemoteWitnessReport getFreshGpsReportFor(String targetPlayerId) {
        synchronized (LOCK) {
            List<IffRemoteWitnessReport> reports = REPORTS_BY_TARGET.get(targetPlayerId);
            if (reports == null) {
                return null;
            }
            IffRemoteWitnessReport best = null;
            for (int i = 0; i < reports.size(); i++) {
                IffRemoteWitnessReport report = reports.get(i);
                if (report == null || !report.hasGpsFix() || !report.isFresh()) {
                    continue;
                }
                if (best == null || report.gpsAgeMs() < best.gpsAgeMs()) {
                    best = report;
                }
            }
            return best;
        }
    }

    public static int reportCountFor(String targetPlayerId) {
        synchronized (LOCK) {
            List<IffRemoteWitnessReport> reports = REPORTS_BY_TARGET.get(targetPlayerId);
            return reports == null ? 0 : reports.size();
        }
    }

    public static int clearReportsFor(String targetPlayerId) {
        synchronized (LOCK) {
            List<IffRemoteWitnessReport> reports = REPORTS_BY_TARGET.remove(targetPlayerId);
            int removed = reports == null ? 0 : reports.size();
            FieldDiagnosticLog.event("IFF_DIAG", "event=remote_witness_cleared"
                    + " targetPlayerId=" + targetPlayerId
                    + " removed=" + removed);
            return removed;
        }
    }

    private static void replaceSameSource(List<IffRemoteWitnessReport> reports, IffRemoteWitnessReport next) {
        for (int i = 0; i < reports.size(); i++) {
            IffRemoteWitnessReport previous = reports.get(i);
            if (previous.sourcePlayerId.equals(next.sourcePlayerId)) {
                if (previous.observedElapsedMs <= next.observedElapsedMs) {
                    reports.set(i, next);
                }
                return;
            }
        }
        reports.add(next);
    }
}
