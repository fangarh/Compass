package net.afterday.compas.view;

import android.animation.ArgbEvaluator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.sensors.Battery.BatteryStatus;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class Battery extends View {
    private static final int GREEN = -14483712;
    private static final int RED = -65511;
    private static final String TAG = "Clock";
    private static final int WIDGET_HEIGHT = 85;
    private static final int WIDGET_WIDTH = 195;
    private int color;
    private int energy;
    private boolean isVisible;
    private int mHeight;
    private Paint mPaint;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private String mText;
    private Typeface mTypeface;
    private int mWidth;
    private Disposable subscription;

    public Battery(Context context) {
        super(context);
        this.mText = "0%";
        this.color = GREEN;
        this.isVisible = true;
        init();
    }

    public Battery(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mText = "0%";
        this.color = GREEN;
        this.isVisible = true;
        init();
    }

    public Battery(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mText = "0%";
        this.color = GREEN;
        this.isVisible = true;
        init();
    }

    public Battery setText(String text) {
        this.mText = text;
        invalidate();
        return this;
    }

    public void setLevel(int level) {
        this.energy = level;
        this.mText = level + "%";
        this.color = getColor();
        invalidate();
    }

    public void setStatus(BatteryStatus batteryStatus) {
        this.energy = batteryStatus.getEnergyLevel();
        this.mText = this.energy + "%";
        if (this.energy > 15) {
            this.mPaint = Fonts.instance().setDefaultColor(this.mPaint);
        } else {
            this.mPaint.setColor(RED);
        }
        if (batteryStatus.isCharging()) {
            Disposable disposable = this.subscription;
            if (disposable != null && !disposable.isDisposed()) {
                this.subscription.dispose();
            }
            this.subscription = Observable.interval(1L, TimeUnit.SECONDS).subscribe(new $$Lambda$Battery$e2kEKbjHOoL0R90y65AyXpLVR3s(this));
            return;
        }
        Disposable disposable2 = this.subscription;
        if (disposable2 != null && !disposable2.isDisposed()) {
            this.subscription.dispose();
        }
        this.subscription = null;
        this.isVisible = true;
        invalidate();
    }

    public /* synthetic */ void lambda$setStatus$0$Battery(Long s) {
        this.isVisible = !this.isVisible;
        postInvalidate();
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        int finalMeasureSpecX = View.MeasureSpec.makeMeasureSpec(widthSize, 1073741824);
        int finalMeasureSpecY = View.MeasureSpec.makeMeasureSpec(heightSize, 1073741824);
        super.onMeasure(finalMeasureSpecX, finalMeasureSpecY);
        this.mWidth = widthSize;
        this.mHeight = heightSize;
        this.mScaleFactorX = this.mWidth / 195.0f;
        this.mScaleFactorY = this.mHeight / 85.0f;
        this.mPaint.setTextSize(this.mScaleFactorY * 60.0f);
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Disposable disposable = this.subscription;
        if (disposable != null && !disposable.isDisposed()) {
            this.subscription.dispose();
            this.subscription = null;
        }
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (this.isVisible) {
            canvas.drawText(this.mText, this.mScaleFactorX * 23.0f, this.mScaleFactorY * 60.0f, this.mPaint);
        }
    }

    protected void init() {
        this.mTypeface = Fonts.instance().getDefaultTypeFace();
        this.mPaint = new Paint();
        this.mPaint.setTypeface(this.mTypeface);
    }

    private int getColor() {
        return ((Integer) new ArgbEvaluator().evaluate(this.energy / 100, Integer.valueOf(RED), Integer.valueOf(GREEN))).intValue();
    }

    private float interpolate(float a, float b, float proportion) {
        return ((b - a) * proportion) + a;
    }

    private Paint setPaintColor(int a, int b, float proportion, Paint p) {
        float[] hsva = new float[3];
        float[] hsvb = new float[3];
        Color.colorToHSV(a, hsva);
        Color.colorToHSV(b, hsvb);
        for (int i = 0; i < 3; i++) {
            hsvb[i] = interpolate(hsva[i], hsvb[i], proportion);
        }
        p.setColor(Color.HSVToColor(hsvb));
        return p;
    }
}
