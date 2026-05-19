package net.afterday.compas.iff;

import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;

public final class IffWitnessQuorum {
    private IffWitnessQuorum() {
    }

    public static Snapshot evaluate(String targetPlayerId, WitnessSnapshot localWitness, int possibleSources) {
        int freshSources = localWitness != null && localWitness.isFresh() ? 1 : 0;
        int staleSources = localWitness != null && !localWitness.isFresh()
                && localWitness.ageMs() <= IffRadioWitnessStore.STALE_MS ? 1 : 0;
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
        return new Snapshot(targetPlayerId, label, freshSources, staleSources, Math.max(1, possibleSources), localWitness);
    }

    public static final class Snapshot {
        public final String targetPlayerId;
        public final String label;
        public final int freshSources;
        public final int staleSources;
        public final int possibleSources;
        public final WitnessSnapshot localWitness;

        Snapshot(String targetPlayerId, String label, int freshSources, int staleSources,
                 int possibleSources, WitnessSnapshot localWitness) {
            this.targetPlayerId = targetPlayerId;
            this.label = label;
            this.freshSources = freshSources;
            this.staleSources = staleSources;
            this.possibleSources = possibleSources;
            this.localWitness = localWitness;
        }

        public boolean hasMultiWitness() {
            return freshSources >= 2;
        }

        public String compact() {
            return label + " " + freshSources + "/" + possibleSources;
        }
    }
}
