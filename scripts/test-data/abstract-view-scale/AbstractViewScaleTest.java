package net.afterday.compas.view;

public final class AbstractViewScaleTest {
    public static void main(String[] args) {
        keepsFractionalScaleWhenViewIsSmallerThanBitmap();
        keepsUnitScaleWhenViewMatchesBitmap();
        handlesInvalidBitmapSize();
    }

    private static void keepsFractionalScaleWhenViewIsSmallerThanBitmap() {
        assertClose(729.0f / 778.0f, AbstractView.scaleFactor(729, 778));
        assertTrue(AbstractView.scaleFactor(729, 778) > 0.0f,
                "scale must stay positive for a smaller view");
    }

    private static void keepsUnitScaleWhenViewMatchesBitmap() {
        assertClose(1.0f, AbstractView.scaleFactor(92, 92));
    }

    private static void handlesInvalidBitmapSize() {
        assertClose(0.0f, AbstractView.scaleFactor(729, 0));
    }

    private static void assertClose(float expected, float actual) {
        if (Math.abs(expected - actual) > 0.0001f) {
            throw new AssertionError("Expected " + expected + " but got " + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
