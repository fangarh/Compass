import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.iff.IffTeamRosterStore;

public final class IffTeamRosterStoreTest {
    public static void main(String[] args) {
        defaultRosterContainsOfficePlayers();
        removeHidesMemberAndMarksRemoved();
        removedMemberCanBeExplicitlyRestored();
        serializationRoundTripsTeamAndRemovedIds();
        rejectsInvalidIds();
    }

    private static void defaultRosterContainsOfficePlayers() {
        List<IffTeamRosterStore.Entry> team = IffTeamRosterStore.defaultEntries();

        assertTrue(IffTeamRosterStore.contains(team, "local-you"));
        assertTrue(IffTeamRosterStore.contains(team, "petya"));
        assertTrue(IffTeamRosterStore.contains(team, "vasya"));
        assertTrue(IffTeamRosterStore.contains(team, "zhenya"));
    }

    private static void removeHidesMemberAndMarksRemoved() {
        List<IffTeamRosterStore.Entry> team = IffTeamRosterStore.defaultEntries();
        List<String> removed = new ArrayList<String>();

        boolean changed = IffTeamRosterStore.remove(team, removed, "petya", "vasya");

        assertTrue(changed);
        assertFalse(IffTeamRosterStore.contains(team, "petya"));
        assertTrue(IffTeamRosterStore.isRemoved(removed, "petya"));
    }

    private static void removedMemberCanBeExplicitlyRestored() {
        List<IffTeamRosterStore.Entry> team = IffTeamRosterStore.defaultEntries();
        List<String> removed = new ArrayList<String>();
        IffTeamRosterStore.remove(team, removed, "petya", "vasya");

        boolean changed = IffTeamRosterStore.addOrRestore(team, removed, "petya", "DDS");

        assertTrue(changed);
        assertTrue(IffTeamRosterStore.contains(team, "petya"));
        assertFalse(IffTeamRosterStore.isRemoved(removed, "petya"));
        assertEquals("DDS", findDisplayName(team, "petya"));
    }

    private static void serializationRoundTripsTeamAndRemovedIds() {
        List<IffTeamRosterStore.Entry> team = new ArrayList<IffTeamRosterStore.Entry>();
        team.add(new IffTeamRosterStore.Entry("alpha", "Alpha One"));
        team.add(new IffTeamRosterStore.Entry("bravo", "Bravo"));
        List<String> removed = new ArrayList<String>();
        removed.add("petya");
        removed.add("zhenya");

        List<IffTeamRosterStore.Entry> restoredTeam =
                IffTeamRosterStore.deserializeTeam(IffTeamRosterStore.serializeTeam(team));
        List<String> restoredRemoved =
                IffTeamRosterStore.deserializeRemoved(IffTeamRosterStore.serializeRemoved(removed));

        assertTrue(IffTeamRosterStore.contains(restoredTeam, "alpha"));
        assertEquals("Alpha One", findDisplayName(restoredTeam, "alpha"));
        assertTrue(IffTeamRosterStore.isRemoved(restoredRemoved, "petya"));
        assertTrue(IffTeamRosterStore.isRemoved(restoredRemoved, "zhenya"));
    }

    private static void rejectsInvalidIds() {
        List<IffTeamRosterStore.Entry> team = IffTeamRosterStore.defaultEntries();
        List<String> removed = new ArrayList<String>();

        assertFalse(IffTeamRosterStore.addOrRestore(team, removed, "bad id", "Bad"));
        assertFalse(IffTeamRosterStore.remove(team, removed, "local-you", "local-you"));
    }

    private static String findDisplayName(List<IffTeamRosterStore.Entry> team, String playerId) {
        for (int i = 0; i < team.size(); i++) {
            IffTeamRosterStore.Entry entry = team.get(i);
            if (entry.playerId.equals(playerId)) {
                return entry.displayName;
            }
        }
        return null;
    }

    private static void assertTrue(boolean value) {
        if (!value) {
            throw new AssertionError("Expected true");
        }
    }

    private static void assertFalse(boolean value) {
        if (value) {
            throw new AssertionError("Expected false");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
