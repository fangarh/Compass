import java.util.ArrayList;
import java.util.List;
import net.afterday.compas.iff.IffFieldTeamProfile;
import net.afterday.compas.iff.IffTeamRosterStore;

public final class IffFieldTeamProfileTest {
    public static void main(String[] args) {
        exposesThreeFixedRoles();
        defaultsContainOnlyFieldEntries();
        preservesKnownNamesWhenResetting();
        rejectsNonFieldPlayerIds();
    }

    private static void exposesThreeFixedRoles() {
        List<IffFieldTeamProfile.Role> roles = IffFieldTeamProfile.roles();

        assertEquals(3, roles.size());
        assertEquals("A", roles.get(0).label);
        assertEquals("vasya", roles.get(0).playerId);
        assertEquals("B", roles.get(1).label);
        assertEquals("zhenya", roles.get(1).playerId);
        assertEquals("C", roles.get(2).label);
        assertEquals("petya", roles.get(2).playerId);
    }

    private static void defaultsContainOnlyFieldEntries() {
        List<IffTeamRosterStore.Entry> entries = IffFieldTeamProfile.defaultFieldEntries();

        assertEquals(3, entries.size());
        assertTrue(IffTeamRosterStore.contains(entries, "vasya"));
        assertTrue(IffTeamRosterStore.contains(entries, "zhenya"));
        assertTrue(IffTeamRosterStore.contains(entries, "petya"));
        assertFalse(IffTeamRosterStore.contains(entries, "local-you"));
    }

    private static void preservesKnownNamesWhenResetting() {
        List<IffTeamRosterStore.Entry> current = new ArrayList<IffTeamRosterStore.Entry>();
        current.add(new IffTeamRosterStore.Entry("local-you", "Old Local"));
        current.add(new IffTeamRosterStore.Entry("vasya", "Ivan"));
        current.add(new IffTeamRosterStore.Entry("zhenya", "Maria"));
        current.add(new IffTeamRosterStore.Entry("petya", "Oleg"));
        current.add(new IffTeamRosterStore.Entry("extra", "Extra"));

        List<IffTeamRosterStore.Entry> reset =
                IffFieldTeamProfile.fieldEntriesPreservingNames(current);

        assertEquals(3, reset.size());
        assertEquals("Ivan", findDisplayName(reset, "vasya"));
        assertEquals("Maria", findDisplayName(reset, "zhenya"));
        assertEquals("Oleg", findDisplayName(reset, "petya"));
        assertFalse(IffTeamRosterStore.contains(reset, "local-you"));
        assertFalse(IffTeamRosterStore.contains(reset, "extra"));
    }

    private static void rejectsNonFieldPlayerIds() {
        assertTrue(IffFieldTeamProfile.isFieldPlayerId("vasya"));
        assertTrue(IffFieldTeamProfile.isFieldPlayerId("zhenya"));
        assertTrue(IffFieldTeamProfile.isFieldPlayerId("petya"));
        assertFalse(IffFieldTeamProfile.isFieldPlayerId("local-you"));
        assertEquals(null, IffFieldTeamProfile.roleForPlayerId("unknown"));
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
