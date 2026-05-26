package net.afterday.compas.iff;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IffCoordinateMessage {
    public static final String PREFIX = "CIFF2";

    private IffCoordinateMessage() {
    }

    public static String encode(String senderPlayerId, long sequence, List<IffParticipantState> states) {
        String normalizedSenderPlayerId = normalize(senderPlayerId);
        if (normalizedSenderPlayerId == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(PREFIX)
                .append('|')
                .append(escape(normalizedSenderPlayerId))
                .append('|')
                .append(Math.max(0L, sequence))
                .append('|');

        if (states != null) {
            boolean first = true;
            for (int i = 0; i < states.size(); i++) {
                IffParticipantState state = states.get(i);
                if (state == null) {
                    continue;
                }
                if (!first) {
                    builder.append(';');
                }
                appendState(builder, state);
                first = false;
            }
        }

        return builder.toString();
    }

    public static Parsed parse(String encoded, long receivedTimeMillis) {
        if (encoded == null) {
            return null;
        }

        String[] parts = encoded.split("\\|", -1);
        if (parts.length != 4 || !PREFIX.equals(parts[0])) {
            return null;
        }

        String senderPlayerId = normalize(unescape(parts[1]));
        if (senderPlayerId == null) {
            return null;
        }

        long sequence = parseLong(parts[2], Long.MIN_VALUE);
        if (sequence == Long.MIN_VALUE) {
            return null;
        }
        if (sequence < 0L) {
            sequence = 0L;
        }

        List<IffParticipantState> states = new ArrayList<IffParticipantState>();
        if (!parts[3].isEmpty()) {
            String[] stateParts = parts[3].split(";", -1);
            for (int i = 0; i < stateParts.length; i++) {
                IffParticipantState state = parseState(stateParts[i], receivedTimeMillis);
                if (state != null) {
                    states.add(state);
                }
            }
        }

        return new Parsed(senderPlayerId, sequence, states);
    }

    private static void appendState(StringBuilder builder, IffParticipantState state) {
        builder.append(escape(state.playerId))
                .append(',')
                .append(escape(state.sourcePlayerId))
                .append(',')
                .append(escape(state.displayName))
                .append(',')
                .append(state.latitude)
                .append(',')
                .append(state.longitude)
                .append(',')
                .append(state.accuracyMeters)
                .append(',')
                .append(state.locationTimeMillis)
                .append(',')
                .append(state.hopCount)
                .append(',')
                .append(state.rssiDbm)
                .append(',')
                .append(state.approachActive ? '1' : '0');
    }

    private static IffParticipantState parseState(String encodedState, long receivedTimeMillis) {
        String[] fields = encodedState.split(",", -1);
        if (fields.length != 9 && fields.length != 10) {
            return null;
        }

        String playerId = unescape(fields[0]);
        String sourcePlayerId = unescape(fields[1]);
        String displayName = fields.length == 10 ? unescape(fields[2]) : playerId;
        if (playerId == null || sourcePlayerId == null || displayName == null) {
            return null;
        }
        int offset = fields.length == 10 ? 1 : 0;

        try {
            double latitude = Double.parseDouble(fields[2 + offset]);
            double longitude = Double.parseDouble(fields[3 + offset]);
            float accuracyMeters = Float.parseFloat(fields[4 + offset]);
            long locationTimeMillis = Long.parseLong(fields[5 + offset]);
            int hopCount = Integer.parseInt(fields[6 + offset]);
            int rssiDbm = Integer.parseInt(fields[7 + offset]);
            Boolean approachActive = parseBoolean(fields[8 + offset]);
            if (approachActive == null) {
                return null;
            }
            if (Double.isNaN(latitude) || Double.isInfinite(latitude)
                    || Double.isNaN(longitude) || Double.isInfinite(longitude)
                    || Float.isNaN(accuracyMeters) || Float.isInfinite(accuracyMeters)) {
                return null;
            }
            return IffParticipantState.create(
                    playerId,
                    sourcePlayerId,
                    displayName,
                    latitude,
                    longitude,
                    accuracyMeters,
                    locationTimeMillis,
                    receivedTimeMillis,
                    hopCount,
                    rssiDbm,
                    approachActive.booleanValue());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Boolean parseBoolean(String value) {
        if ("1".equals(value) || "true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("0".equals(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder(value.length());
        byte[] bytes = toUtf8(value);
        for (int i = 0; i < bytes.length; i++) {
            int unsignedByte = bytes[i] & 0xff;
            if (isUnreserved(unsignedByte)) {
                builder.append((char) unsignedByte);
            } else {
                builder.append('%');
                appendHex(builder, unsignedByte);
            }
        }
        return builder.toString();
    }

    private static String unescape(String value) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(value.length());
        for (int i = 0; i < value.length(); i++) {
            char character = value.charAt(i);
            if (character != '%') {
                if (character > 0x7f) {
                    return null;
                }
                bytes.write((byte) character);
                continue;
            }
            if (i + 2 >= value.length()) {
                return null;
            }
            int high = hexValue(value.charAt(i + 1));
            int low = hexValue(value.charAt(i + 2));
            if (high < 0 || low < 0) {
                return null;
            }
            bytes.write((high << 4) | low);
            i += 2;
        }

        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return decoder.decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
        } catch (CharacterCodingException exception) {
            return null;
        }
    }

    private static boolean isUnreserved(int value) {
        return (value >= 'A' && value <= 'Z')
                || (value >= 'a' && value <= 'z')
                || (value >= '0' && value <= '9')
                || value == '-'
                || value == '.'
                || value == '_'
                || value == '~';
    }

    private static void appendHex(StringBuilder builder, int value) {
        builder.append(Character.toUpperCase(Character.forDigit((value >> 4) & 0xf, 16)));
        builder.append(Character.toUpperCase(Character.forDigit(value & 0xf, 16)));
    }

    private static int hexValue(char character) {
        if (character >= '0' && character <= '9') {
            return character - '0';
        }
        if (character >= 'A' && character <= 'F') {
            return character - 'A' + 10;
        }
        if (character >= 'a' && character <= 'f') {
            return character - 'a' + 10;
        }
        return -1;
    }

    private static byte[] toUtf8(String value) {
        try {
            return value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException exception) {
            throw new AssertionError(exception);
        }
    }

    public static final class Parsed {
        public final String senderPlayerId;
        public final long sequence;
        public final List<IffParticipantState> states;

        private Parsed(String senderPlayerId, long sequence, List<IffParticipantState> states) {
            this.senderPlayerId = senderPlayerId;
            this.sequence = sequence;
            this.states = Collections.unmodifiableList(new ArrayList<IffParticipantState>(states));
        }
    }
}
