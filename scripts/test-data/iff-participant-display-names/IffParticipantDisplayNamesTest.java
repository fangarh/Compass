import net.afterday.compas.iff.IffParticipantDisplayNames;

public final class IffParticipantDisplayNamesTest {
    public static void main(String[] args) {
        returnsPlayerIdBeforeNameIsLearned();
        remembersLearnedDisplayName();
        rejectsEmptyNames();
        limitsLongNames();
    }

    private static void returnsPlayerIdBeforeNameIsLearned() {
        IffParticipantDisplayNames names = new IffParticipantDisplayNames();

        assertEquals("petya", names.displayNameFor("petya"));
    }

    private static void remembersLearnedDisplayName() {
        IffParticipantDisplayNames names = new IffParticipantDisplayNames();

        names.remember("petya", "DDS");

        assertEquals("DDS", names.displayNameFor("petya"));
    }

    private static void rejectsEmptyNames() {
        IffParticipantDisplayNames names = new IffParticipantDisplayNames();

        names.remember("petya", "DDS");
        names.remember("petya", "   ");

        assertEquals("DDS", names.displayNameFor("petya"));
    }

    private static void limitsLongNames() {
        IffParticipantDisplayNames names = new IffParticipantDisplayNames();

        names.remember("petya", "12345678901234567890");

        assertEquals("123456789012345678", names.displayNameFor("petya"));
    }

    private static void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
