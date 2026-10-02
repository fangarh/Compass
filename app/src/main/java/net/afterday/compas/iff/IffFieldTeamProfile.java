package net.afterday.compas.iff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class IffFieldTeamProfile {
    private static final List<Role> ROLES = createRoles();

    private IffFieldTeamProfile() {
    }

    public static List<Role> roles() {
        return ROLES;
    }

    public static boolean isFieldPlayerId(String playerId) {
        return roleForPlayerId(playerId) != null;
    }

    public static Role roleForPlayerId(String playerId) {
        String normalizedPlayerId = IffTeamRosterStore.normalizePlayerId(playerId);
        if (normalizedPlayerId == null) {
            return null;
        }
        for (int i = 0; i < ROLES.size(); i++) {
            Role role = ROLES.get(i);
            if (role.playerId.equals(normalizedPlayerId)) {
                return role;
            }
        }
        return null;
    }

    public static List<IffTeamRosterStore.Entry> defaultFieldEntries() {
        return fieldEntriesPreservingNames(null);
    }

    public static List<IffTeamRosterStore.Entry> fieldEntriesPreservingNames(
            List<IffTeamRosterStore.Entry> current) {
        List<IffTeamRosterStore.Entry> entries = new ArrayList<IffTeamRosterStore.Entry>();
        for (int i = 0; i < ROLES.size(); i++) {
            Role role = ROLES.get(i);
            entries.add(new IffTeamRosterStore.Entry(
                    role.playerId,
                    preservedDisplayName(current, role.playerId, role.defaultDisplayName)));
        }
        return entries;
    }

    private static String preservedDisplayName(
            List<IffTeamRosterStore.Entry> current,
            String playerId,
            String fallback) {
        if (current != null) {
            for (int i = 0; i < current.size(); i++) {
                IffTeamRosterStore.Entry entry = current.get(i);
                if (entry != null && playerId.equals(entry.playerId)) {
                    return IffTeamRosterStore.normalizeDisplayName(entry.displayName, fallback);
                }
            }
        }
        return fallback;
    }

    private static List<Role> createRoles() {
        List<Role> roles = new ArrayList<Role>();
        roles.add(new Role("A", "vasya", "Роль A"));
        roles.add(new Role("B", "zhenya", "Роль B"));
        roles.add(new Role("C", "petya", "Роль C"));
        return Collections.unmodifiableList(roles);
    }

    public static final class Role {
        public final String label;
        public final String playerId;
        public final String defaultDisplayName;

        private Role(String label, String playerId, String defaultDisplayName) {
            this.label = label;
            this.playerId = playerId;
            this.defaultDisplayName = defaultDisplayName;
        }
    }
}
