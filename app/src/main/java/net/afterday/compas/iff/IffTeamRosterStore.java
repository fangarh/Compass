package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.List;

public final class IffTeamRosterStore {
    public static final int MAX_DISPLAY_NAME_LENGTH = 18;
    public static final int MAX_PLAYER_ID_LENGTH = 32;

    private IffTeamRosterStore() {
    }

    public static List<Entry> defaultEntries() {
        List<Entry> entries = new ArrayList<Entry>();
        entries.add(new Entry("local-you", "Вы"));
        entries.add(new Entry("petya", "Петя"));
        entries.add(new Entry("vasya", "Вася"));
        entries.add(new Entry("zhenya", "Женя"));
        return entries;
    }

    public static List<Entry> deserializeTeam(String serialized) {
        if (isBlank(serialized)) {
            return defaultEntries();
        }
        List<Entry> entries = new ArrayList<Entry>();
        String[] lines = serialized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() == 0) {
                continue;
            }
            int separator = firstUnescapedTab(line);
            if (separator <= 0) {
                continue;
            }
            String playerId = normalizePlayerId(unescape(line.substring(0, separator)));
            String displayName = normalizeDisplayName(unescape(line.substring(separator + 1)), playerId);
            if (playerId == null || contains(entries, playerId)) {
                continue;
            }
            entries.add(new Entry(playerId, displayName));
        }
        return entries.isEmpty() ? defaultEntries() : entries;
    }

    public static String serializeTeam(List<Entry> entries) {
        StringBuilder builder = new StringBuilder();
        if (entries == null) {
            return "";
        }
        for (int i = 0; i < entries.size(); i++) {
            Entry entry = entries.get(i);
            if (entry == null || normalizePlayerId(entry.playerId) == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(escape(normalizePlayerId(entry.playerId)))
                    .append('\t')
                    .append(escape(normalizeDisplayName(entry.displayName, entry.playerId)));
        }
        return builder.toString();
    }

    public static List<String> deserializeRemoved(String serialized) {
        List<String> ids = new ArrayList<String>();
        if (isBlank(serialized)) {
            return ids;
        }
        String[] lines = serialized.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String playerId = normalizePlayerId(unescape(lines[i]));
            if (playerId != null && !containsId(ids, playerId)) {
                ids.add(playerId);
            }
        }
        return ids;
    }

    public static String serializeRemoved(List<String> ids) {
        StringBuilder builder = new StringBuilder();
        if (ids == null) {
            return "";
        }
        for (int i = 0; i < ids.size(); i++) {
            String playerId = normalizePlayerId(ids.get(i));
            if (playerId == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(escape(playerId));
        }
        return builder.toString();
    }

    public static boolean addOrRestore(List<Entry> team, List<String> removed, String playerId, String displayName) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        if (team == null || normalizedPlayerId == null) {
            return false;
        }
        String normalizedDisplayName = normalizeDisplayName(displayName, normalizedPlayerId);
        int existingIndex = indexOf(team, normalizedPlayerId);
        if (existingIndex >= 0) {
            team.set(existingIndex, new Entry(normalizedPlayerId, normalizedDisplayName));
            removeId(removed, normalizedPlayerId);
            return true;
        }
        team.add(new Entry(normalizedPlayerId, normalizedDisplayName));
        removeId(removed, normalizedPlayerId);
        return true;
    }

    public static boolean remove(List<Entry> team, List<String> removed, String playerId, String localPlayerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        if (team == null || normalizedPlayerId == null || normalizedPlayerId.equals(normalizePlayerId(localPlayerId))) {
            return false;
        }
        int index = indexOf(team, normalizedPlayerId);
        if (index < 0) {
            return false;
        }
        team.remove(index);
        if (removed != null && !containsId(removed, normalizedPlayerId)) {
            removed.add(normalizedPlayerId);
        }
        return true;
    }

    public static boolean contains(List<Entry> team, String playerId) {
        return indexOf(team, playerId) >= 0;
    }

    public static boolean isRemoved(List<String> removed, String playerId) {
        return containsId(removed, playerId);
    }

    public static String defaultDisplayNameFor(String playerId) {
        List<Entry> defaults = defaultEntries();
        int index = indexOf(defaults, playerId);
        if (index >= 0) {
            return defaults.get(index).displayName;
        }
        String normalizedPlayerId = normalizePlayerId(playerId);
        return normalizedPlayerId == null ? "phone" : normalizedPlayerId;
    }

    public static String normalizePlayerId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() == 0) {
            return null;
        }
        if (trimmed.length() > MAX_PLAYER_ID_LENGTH) {
            trimmed = trimmed.substring(0, MAX_PLAYER_ID_LENGTH);
        }
        for (int i = 0; i < trimmed.length(); i++) {
            char ch = trimmed.charAt(i);
            boolean allowed = (ch >= 'a' && ch <= 'z')
                    || (ch >= 'A' && ch <= 'Z')
                    || (ch >= '0' && ch <= '9')
                    || ch == '-'
                    || ch == '_'
                    || ch == '.';
            if (!allowed) {
                return null;
            }
        }
        return trimmed;
    }

    public static String normalizeDisplayName(String value, String fallback) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.length() == 0) {
            String normalizedFallback = normalizePlayerId(fallback);
            return normalizedFallback == null ? "phone" : normalizedFallback;
        }
        if (trimmed.length() > MAX_DISPLAY_NAME_LENGTH) {
            return trimmed.substring(0, MAX_DISPLAY_NAME_LENGTH);
        }
        return trimmed;
    }

    private static int indexOf(List<Entry> team, String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        if (team == null || normalizedPlayerId == null) {
            return -1;
        }
        for (int i = 0; i < team.size(); i++) {
            Entry entry = team.get(i);
            if (entry != null && normalizedPlayerId.equals(entry.playerId)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean containsId(List<String> ids, String playerId) {
        String normalizedPlayerId = normalizePlayerId(playerId);
        if (ids == null || normalizedPlayerId == null) {
            return false;
        }
        for (int i = 0; i < ids.size(); i++) {
            if (normalizedPlayerId.equals(normalizePlayerId(ids.get(i)))) {
                return true;
            }
        }
        return false;
    }

    private static void removeId(List<String> ids, String playerId) {
        if (ids == null) {
            return;
        }
        String normalizedPlayerId = normalizePlayerId(playerId);
        for (int i = ids.size() - 1; i >= 0; i--) {
            if (normalizedPlayerId != null && normalizedPlayerId.equals(normalizePlayerId(ids.get(i)))) {
                ids.remove(i);
            }
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static int firstUnescapedTab(String value) {
        boolean escaping = false;
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (escaping) {
                escaping = false;
                continue;
            }
            if (ch == '\\') {
                escaping = true;
                continue;
            }
            if (ch == '\t') {
                return i;
            }
        }
        return -1;
    }

    private static String escape(String value) {
        String safe = value == null ? "" : value;
        return safe.replace("\\", "\\\\")
                .replace("\n", "\\n")
                .replace("\t", "\\t");
    }

    private static String unescape(String value) {
        String safe = value == null ? "" : value;
        StringBuilder builder = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < safe.length(); i++) {
            char ch = safe.charAt(i);
            if (escaping) {
                if (ch == 'n') {
                    builder.append('\n');
                } else if (ch == 't') {
                    builder.append('\t');
                } else {
                    builder.append(ch);
                }
                escaping = false;
            } else if (ch == '\\') {
                escaping = true;
            } else {
                builder.append(ch);
            }
        }
        if (escaping) {
            builder.append('\\');
        }
        return builder.toString();
    }

    public static final class Entry {
        public final String playerId;
        public final String displayName;

        public Entry(String playerId, String displayName) {
            this.playerId = normalizePlayerId(playerId);
            this.displayName = normalizeDisplayName(displayName, this.playerId);
        }
    }
}
