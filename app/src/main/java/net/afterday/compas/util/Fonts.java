package net.afterday.compas.util;

import android.content.res.AssetManager;
import android.graphics.Paint;
import android.graphics.Typeface;

/* JADX INFO: loaded from: classes.dex */
public class Fonts {
    private static Fonts instance;
    private AssetManager assetManager;
    private Typeface defaultTypeFace;

    private Fonts(AssetManager assetManager) {
        this.assetManager = assetManager;
    }

    public static Fonts instance() {
        Fonts fonts = instance;
        if (fonts == null) {
            throw new IllegalStateException("AssetsManager not set. Must call instance(AssetsManager) first");
        }
        return fonts;
    }

    public static Fonts instance(AssetManager assetManager) {
        instance = new Fonts(assetManager);
        return instance;
    }

    public Paint getDefaultFontPaint() {
        Typeface tf = getDefaultTypeFace();
        Paint p = new Paint();
        if (tf != null) {
            p.setTypeface(tf);
        }
        p.setARGB(255, 255, 127, 0);
        return p;
    }

    public Paint setDefaultColor(Paint paint) {
        paint.setARGB(255, 255, 127, 0);
        return paint;
    }

    public Typeface getDefaultTypeFace() {
        if (this.defaultTypeFace == null) {
            try {
                this.defaultTypeFace = Typeface.createFromAsset(this.assetManager, "fonts/console.ttf");
            } catch (Exception e) {
            }
        }
        return this.defaultTypeFace;
    }
}
