package net.afterday.compas.iff;

import net.afterday.compas.iff.IffRadioWitnessStore.WitnessSnapshot;

public final class IffConfidence {
    private IffConfidence() {
    }

    public static Snapshot evaluate(String playerId, boolean localPlayer, boolean localApproachActive, WitnessSnapshot witness) {
        Confidence identity = identity(localPlayer, localApproachActive, witness);
        Confidence proximity = proximity(localPlayer, localApproachActive, witness);
        Confidence position = new Confidence("UNKNOWN", 0, "GPS слой еще не подключен");
        Confidence direction = new Confidence("UNKNOWN", 0, "Wi-Fi RSSI не дает азимут");
        return new Snapshot(playerId, identity, proximity, position, direction);
    }

    private static Confidence identity(boolean localPlayer, boolean localApproachActive, WitnessSnapshot witness) {
        if (localPlayer && localApproachActive) {
            return new Confidence("LOCAL_SELF_APPROACH", 80, "локальный игрок сам заявил подход");
        }
        if (localPlayer) {
            return new Confidence("LOCAL_SELF", 70, "локальная запись этого устройства");
        }
        if (witness != null && witness.isFresh()) {
            return new Confidence("ROSTER_PLUS_RADIO_CLAIM", 60, "roster совпал со свежим beacon SSID; crypto нет");
        }
        return new Confidence("ROSTER_ONLY", 40, "участник известен только из локального roster");
    }

    private static Confidence proximity(boolean localPlayer, boolean localApproachActive, WitnessSnapshot witness) {
        if (localPlayer && localApproachActive) {
            return new Confidence("LOCAL_DECLARED_UNKNOWN", 20, "локальная кнопка не является radio proof");
        }
        if (witness == null) {
            return new Confidence("UNKNOWN", 0, "beacon не слышен");
        }
        long ageMs = witness.ageMs();
        if (ageMs > IffRadioWitnessStore.STALE_MS) {
            return new Confidence("UNKNOWN", 0, "последний witness старше 60s");
        }
        if (!witness.isFresh()) {
            return new Confidence("STALE_RADIO", 25, "beacon был слышен, но это уже не current proof");
        }
        if (witness.rssi >= -55) {
            return new Confidence("RADIO_NEAR", 75, "свежий сильный RSSI; близость вероятна, азимута нет");
        }
        if (witness.rssi >= -70) {
            return new Confidence("RADIO_WEAK_HINT", 45, "свежий RSSI слышен, но дистанция не точная");
        }
        return new Confidence("RADIO_EDGE_HINT", 30, "свежий слабый RSSI; только факт слышимости");
    }

    public static final class Snapshot {
        public final String playerId;
        public final Confidence identity;
        public final Confidence proximity;
        public final Confidence position;
        public final Confidence direction;

        Snapshot(String playerId, Confidence identity, Confidence proximity, Confidence position, Confidence direction) {
            this.playerId = playerId;
            this.identity = identity;
            this.proximity = proximity;
            this.position = position;
            this.direction = direction;
        }

        public String compactStatus() {
            return "IDENTITY: " + identity.compact() + "\n"
                    + "PROXIMITY: " + proximity.compact() + "\n"
                    + "POSITION: " + position.compact() + "\n"
                    + "DIRECTION: " + direction.compact();
        }
    }

    public static final class Confidence {
        public final String label;
        public final int score;
        public final String reason;

        Confidence(String label, int score, String reason) {
            this.label = label;
            this.score = score;
            this.reason = reason;
        }

        public String compact() {
            return label + " " + score + "%";
        }

        public String detailLine(String name) {
            return "- " + name + ": " + compact() + " - " + reason;
        }
    }
}
