import android.net.wifi.ScanResult;
import java.util.Arrays;
import java.util.Collections;
import net.afterday.compas.core.influences.InfluencesPack;
import net.afterday.compas.engine.influences.WifiInfluences.ByMacExtractionStrategy;

public final class ByMacExtractionStrategyTest {
    private static final long NOW_MS = 100000L;
    private static final String REGISTERED = "8e:aa:b5:51:30:d3";

    public static void main(String[] args) {
        freshRegisteredMacWithStrongSignalPasses();
        staleRegisteredMacDoesNotPass();
        weakRegisteredMacDoesNotPass();
        unknownMacDoesNotPass();
        registeredMacIsMatchedCaseInsensitively();
    }

    private static void freshRegisteredMacWithStrongSignalPasses() {
        InfluencesPack pack = strategy().makeInfluences(Collections.singletonList(
                scan("R150", REGISTERED, -60, NOW_MS - 1000L)));

        assertTrue(pack.influencedBy(0), "fresh registered MAC should produce radiation influence");
        assertTrue(pack.getInfluence(0) > 0.0d, "fresh registered MAC should have non-zero strength");
    }

    private static void staleRegisteredMacDoesNotPass() {
        InfluencesPack pack = strategy().makeInfluences(Collections.singletonList(
                scan("R150", REGISTERED, -60, NOW_MS - 3501L)));

        assertFalse(pack.influencedBy(0), "stale registered MAC should be ignored");
    }

    private static void weakRegisteredMacDoesNotPass() {
        InfluencesPack pack = strategy().makeInfluences(Collections.singletonList(
                scan("R150", REGISTERED, -89, NOW_MS - 1000L)));

        assertFalse(pack.influencedBy(0), "registered MAC weaker than -88 dBm should be ignored");
    }

    private static void unknownMacDoesNotPass() {
        InfluencesPack pack = strategy().makeInfluences(Collections.singletonList(
                scan("R150", "00:11:22:33:44:55", -60, NOW_MS - 1000L)));

        assertFalse(pack.influencedBy(0), "unknown MAC should be ignored");
    }

    private static void registeredMacIsMatchedCaseInsensitively() {
        InfluencesPack pack = strategy().makeInfluences(Collections.singletonList(
                scan("A150", "8E:AA:B5:51:30:D3", -60, NOW_MS - 1000L)));

        assertTrue(pack.influencedBy(1), "registered MAC should match regardless of BSSID case");
    }

    private static ByMacExtractionStrategy strategy() {
        return new ByMacExtractionStrategy(Arrays.asList(REGISTERED));
    }

    private static ScanResult scan(String ssid, String bssid, int level, long seenElapsedMs) {
        ScanResult result = new ScanResult();
        result.SSID = ssid;
        result.BSSID = bssid;
        result.level = level;
        result.timestamp = seenElapsedMs * 1000L;
        return result;
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }
}
