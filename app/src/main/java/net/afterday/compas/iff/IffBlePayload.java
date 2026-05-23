package net.afterday.compas.iff;

public final class IffBlePayload {
    public static final byte MARKER_0 = 0x43; // C
    public static final byte MARKER_1 = 0x49; // I
    public static final int CONTRACT_V1 = 1;
    public static final int CONTRACT_V2 = 2;
    public static final int V1_LENGTH = 4;
    public static final int V2_LENGTH = 17;

    private static final int FLAG_GPS = 0x01;
    private static final int AGE_UNIT_MS = 100;
    private static final int UINT16_MAX = 65535;

    private IffBlePayload() {
    }

    public static byte[] forPlayer(int playerCode) {
        return new byte[] {MARKER_0, MARKER_1, (byte) CONTRACT_V1, (byte) playerCode};
    }

    public static byte[] forPlayerWithGps(
            int playerCode,
            int gpsLatE7,
            int gpsLonE7,
            int gpsAccuracyM,
            long gpsAgeMs) {
        byte[] payload = new byte[V2_LENGTH];
        payload[0] = MARKER_0;
        payload[1] = MARKER_1;
        payload[2] = (byte) CONTRACT_V2;
        payload[3] = (byte) playerCode;
        payload[4] = FLAG_GPS;
        writeInt(payload, 5, gpsLatE7);
        writeInt(payload, 9, gpsLonE7);
        writeUnsignedShort(payload, 13, clampUnsignedShort(gpsAccuracyM));
        writeUnsignedShort(payload, 15, clampUnsignedShort((int) ((Math.max(0L, gpsAgeMs) + AGE_UNIT_MS / 2) / AGE_UNIT_MS)));
        return payload;
    }

    public static Parsed parse(byte[] payload) {
        if (payload == null || payload.length < V1_LENGTH) {
            return null;
        }
        if (payload[0] != MARKER_0 || payload[1] != MARKER_1) {
            return null;
        }
        int contractVersion = payload[2] & 0xff;
        int playerCode = payload[3] & 0xff;
        if (contractVersion == CONTRACT_V1 && payload.length == V1_LENGTH) {
            return new Parsed(contractVersion, playerCode, false, 0, 0, -1, -1L);
        }
        if (contractVersion != CONTRACT_V2 || payload.length < V2_LENGTH) {
            return null;
        }
        boolean hasGps = (payload[4] & FLAG_GPS) == FLAG_GPS;
        int gpsLatE7 = readInt(payload, 5);
        int gpsLonE7 = readInt(payload, 9);
        int gpsAccuracyM = readUnsignedShort(payload, 13);
        long gpsAgeMs = readUnsignedShort(payload, 15) * (long) AGE_UNIT_MS;
        return new Parsed(contractVersion, playerCode, hasGps, gpsLatE7, gpsLonE7, gpsAccuracyM, gpsAgeMs);
    }

    public static boolean sameAdvertiseContent(byte[] left, byte[] right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null || left.length != right.length) {
            return false;
        }
        int contractVersion = left.length >= V1_LENGTH ? left[2] & 0xff : -1;
        for (int i = 0; i < left.length; i++) {
            if (contractVersion == CONTRACT_V2 && left.length >= V2_LENGTH && (i == 15 || i == 16)) {
                continue;
            }
            if (left[i] != right[i]) {
                return false;
            }
        }
        return true;
    }

    private static void writeInt(byte[] payload, int offset, int value) {
        payload[offset] = (byte) ((value >> 24) & 0xff);
        payload[offset + 1] = (byte) ((value >> 16) & 0xff);
        payload[offset + 2] = (byte) ((value >> 8) & 0xff);
        payload[offset + 3] = (byte) (value & 0xff);
    }

    private static int readInt(byte[] payload, int offset) {
        return ((payload[offset] & 0xff) << 24)
                | ((payload[offset + 1] & 0xff) << 16)
                | ((payload[offset + 2] & 0xff) << 8)
                | (payload[offset + 3] & 0xff);
    }

    private static void writeUnsignedShort(byte[] payload, int offset, int value) {
        payload[offset] = (byte) ((value >> 8) & 0xff);
        payload[offset + 1] = (byte) (value & 0xff);
    }

    private static int readUnsignedShort(byte[] payload, int offset) {
        return ((payload[offset] & 0xff) << 8) | (payload[offset + 1] & 0xff);
    }

    private static int clampUnsignedShort(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(UINT16_MAX, value);
    }

    public static final class Parsed {
        public final int contractVersion;
        public final int playerCode;
        public final boolean hasGps;
        public final int gpsLatE7;
        public final int gpsLonE7;
        public final int gpsAccuracyM;
        public final long gpsAgeMs;

        private Parsed(
                int contractVersion,
                int playerCode,
                boolean hasGps,
                int gpsLatE7,
                int gpsLonE7,
                int gpsAccuracyM,
                long gpsAgeMs) {
            this.contractVersion = contractVersion;
            this.playerCode = playerCode;
            this.hasGps = hasGps;
            this.gpsLatE7 = gpsLatE7;
            this.gpsLonE7 = gpsLonE7;
            this.gpsAccuracyM = gpsAccuracyM;
            this.gpsAgeMs = gpsAgeMs;
        }
    }
}
