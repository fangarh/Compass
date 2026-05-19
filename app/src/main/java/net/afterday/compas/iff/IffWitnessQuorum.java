package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;

public final class IffWitnessQuorum {
    private IffWitnessQuorum() {
    }

    public static Snapshot evaluate(String targetPlayerId, WitnessSnapshot localWitness,
                                    List<IffRemoteWitnessReport> remoteReports, int possibleSources) {
        int freshSources = localWitness != null && localWitness.isFresh() ? 1 : 0;
        int staleSources = localWitness != null && !localWitness.isFresh()
                && localWitness.ageMs() <= IffRadioWitnessStore.STALE_MS ? 1 : 0;
        int remoteFreshSources = 0;
        int remoteStaleSources = 0;
        List<IffRemoteWitnessReport> safeReports = remoteReports == null ? new ArrayList<IffRemoteWitnessReport>() : remoteReports;
        for (int i = 0; i < safeReports.size(); i++) {
            IffRemoteWitnessReport report = safeReports.get(i);
            if (report == null) {
                continue;
            }
            if (report.isFresh()) {
                remoteFreshSources++;
            } else if (report.isStale()) {
                remoteStaleSources++;
            }
        }
        freshSources += remoteFreshSources;
        staleSources += remoteStaleSources;
        String label;
        if (freshSources >= 2) {
            label = "MULTI_WITNESS";
        } else if (freshSources == 1) {
            label = "LOCAL_WITNESS_ONLY";
        } else if (staleSources == 1) {
            label = "STALE_LOCAL_WITNESS";
        } else {
            label = "NO_CURRENT_WITNESS";
        }
        return new Snapshot(targetPlayerId, label, freshSources, staleSources, remoteFreshSources,
                remoteStaleSources, safeReports.size(), Math.max(1, possibleSources), localWitness, safeReports);
    }

    public static final class Snapshot {
        public final String targetPlayerId;
        public final String label;
        public final int freshSources;
        public final int staleSources;
        public final int remoteFreshSources;
        public final int remoteStaleSources;
        public final int remoteReportCount;
        public final int possibleSources;
        public final WitnessSnapshot localWitness;
        public final List<IffRemoteWitnessReport> remoteReports;

        Snapshot(String targetPlayerId, String label, int freshSources, int staleSources, int remoteFreshSources,
                 int remoteStaleSources, int remoteReportCount, int possibleSources, WitnessSnapshot localWitness,
                 List<IffRemoteWitnessReport> remoteReports) {
            this.targetPlayerId = targetPlayerId;
            this.label = label;
            this.freshSources = freshSources;
            this.staleSources = staleSources;
            this.remoteFreshSources = remoteFreshSources;
            this.remoteStaleSources = remoteStaleSources;
            this.remoteReportCount = remoteReportCount;
            this.possibleSources = possibleSources;
            this.localWitness = localWitness;
            this.remoteReports = remoteReports;
        }

        public boolean hasMultiWitness() {
            return freshSources >= 2;
        }

        public String compact() {
            return label + " " + freshSources + "/" + possibleSources;
        }
    }
}
