import net.afterday.compas.view.PdaSceneMetrics;

public final class PdaSceneMetricsTest {
    public static void main(String[] args) {
        landscapeBaseUsesWholeScreen();
        widerLandscapeCentersScene();
        portraitBaseUsesWholeScreen();
        oddSizesStayInsideScreen();
    }

    private static void landscapeBaseUsesWholeScreen() {
        PdaSceneMetrics metrics = PdaSceneMetrics.landscape(1920, 1080);

        assertClose(1.0f, metrics.scale);
        assertEquals(1920, metrics.sceneWidth);
        assertEquals(1080, metrics.sceneHeight);
        assertEquals(0, metrics.sceneLeft);
        assertEquals(0, metrics.sceneTop);
    }

    private static void widerLandscapeCentersScene() {
        PdaSceneMetrics metrics = PdaSceneMetrics.landscape(3088, 1440);

        assertClose(1.3333334f, metrics.scale);
        assertEquals(2560, metrics.sceneWidth);
        assertEquals(1440, metrics.sceneHeight);
        assertEquals(264, metrics.sceneLeft);
        assertEquals(0, metrics.sceneTop);
    }

    private static void portraitBaseUsesWholeScreen() {
        PdaSceneMetrics metrics = PdaSceneMetrics.portrait(1080, 1920);

        assertClose(1.0f, metrics.scale);
        assertEquals(1080, metrics.sceneWidth);
        assertEquals(1920, metrics.sceneHeight);
        assertEquals(0, metrics.sceneLeft);
        assertEquals(0, metrics.sceneTop);
    }

    private static void oddSizesStayInsideScreen() {
        assertInside(PdaSceneMetrics.landscape(2340, 1080), 2340, 1080);
        assertInside(PdaSceneMetrics.portrait(1440, 3088), 1440, 3088);
        assertInside(PdaSceneMetrics.portrait(1000, 1600), 1000, 1600);
    }

    private static void assertInside(PdaSceneMetrics metrics, int screenWidth, int screenHeight) {
        if (metrics.sceneLeft < 0 || metrics.sceneTop < 0) {
            throw new AssertionError("Scene offset must not be negative: " + metrics);
        }
        if (metrics.sceneWidth < 0 || metrics.sceneHeight < 0) {
            throw new AssertionError("Scene size must not be negative: " + metrics);
        }
        if (metrics.sceneLeft + metrics.sceneWidth > screenWidth) {
            throw new AssertionError("Scene exceeds screen width: " + metrics);
        }
        if (metrics.sceneTop + metrics.sceneHeight > screenHeight) {
            throw new AssertionError("Scene exceeds screen height: " + metrics);
        }
    }

    private static void assertEquals(int expected, int actual) {
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }
}
