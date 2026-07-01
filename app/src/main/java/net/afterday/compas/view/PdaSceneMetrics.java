package net.afterday.compas.view;

import java.util.Locale;

public final class PdaSceneMetrics {
    public static final int LANDSCAPE_BASE_WIDTH = 1920;
    public static final int LANDSCAPE_BASE_HEIGHT = 1080;
    public static final int PORTRAIT_BASE_WIDTH = 1080;
    public static final int PORTRAIT_BASE_HEIGHT = 1920;

    public final int screenWidth;
    public final int screenHeight;
    public final int baseWidth;
    public final int baseHeight;
    public final float scale;
    public final int sceneWidth;
    public final int sceneHeight;
    public final int sceneLeft;
    public final int sceneTop;

    private PdaSceneMetrics(int screenWidth, int screenHeight, int baseWidth, int baseHeight) {
        this.screenWidth = Math.max(0, screenWidth);
        this.screenHeight = Math.max(0, screenHeight);
        this.baseWidth = baseWidth;
        this.baseHeight = baseHeight;

        if (this.screenWidth == 0 || this.screenHeight == 0 || baseWidth <= 0 || baseHeight <= 0) {
            this.scale = 0.0f;
            this.sceneWidth = 0;
            this.sceneHeight = 0;
            this.sceneLeft = 0;
            this.sceneTop = 0;
            return;
        }

        float widthScale = this.screenWidth / (float) baseWidth;
        float heightScale = this.screenHeight / (float) baseHeight;
        this.scale = Math.min(widthScale, heightScale);
        this.sceneWidth = Math.min(this.screenWidth, Math.round(baseWidth * this.scale));
        this.sceneHeight = Math.min(this.screenHeight, Math.round(baseHeight * this.scale));
        this.sceneLeft = Math.max(0, (this.screenWidth - this.sceneWidth) / 2);
        this.sceneTop = Math.max(0, (this.screenHeight - this.sceneHeight) / 2);
    }

    public static PdaSceneMetrics landscape(int screenWidth, int screenHeight) {
        return new PdaSceneMetrics(screenWidth, screenHeight, LANDSCAPE_BASE_WIDTH, LANDSCAPE_BASE_HEIGHT);
    }

    public static PdaSceneMetrics portrait(int screenWidth, int screenHeight) {
        return new PdaSceneMetrics(screenWidth, screenHeight, PORTRAIT_BASE_WIDTH, PORTRAIT_BASE_HEIGHT);
    }

    public static PdaSceneMetrics forScreen(int screenWidth, int screenHeight) {
        if (screenWidth >= screenHeight) {
            return landscape(screenWidth, screenHeight);
        }
        return portrait(screenWidth, screenHeight);
    }

    @Override
    public String toString() {
        return String.format(Locale.US,
                "PdaSceneMetrics{screen=%dx%d base=%dx%d scale=%.4f scene=%dx%d offset=%d,%d}",
                screenWidth,
                screenHeight,
                baseWidth,
                baseHeight,
                scale,
                sceneWidth,
                sceneHeight,
                sceneLeft,
                sceneTop);
    }
}
