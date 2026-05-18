package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/* JADX INFO: loaded from: classes.dex */
public class Clock extends View {
    private static final String TAG = "Clock";
    private static final int WIDGET_HEIGHT = 85;
    private static final int WIDGET_WIDTH = 195;
    private int mHeight;
    private Paint mPaint;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private String mText;
    private Typeface mTypeface;
    private int mWidth;
    private Disposable subscription;
    private Observable<Long> ticks;

    public Clock(Context context) {
        super(context);
        this.mText = "23:59";
        init();
    }

    public Clock(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mText = "23:59";
        init();
    }

    public Clock(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mText = "23:59";
        init();
    }

    public Clock setText(String text) {
        this.mText = text;
        invalidate();
        return this;
    }

    @Override // android.view.View
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        this.subscription = this.ticks.subscribe(new $$Lambda$Clock$2X3wBUVjZw0igvJS_dWUuH18rLc(this));
    }

    public /* synthetic */ void lambda$onAttachedToWindow$0$Clock(Long i) {
        postInvalidate();
    }

    @Override // android.view.View
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.subscription.dispose();
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
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Calendar c = Calendar.getInstance();
        int hours = c.get(11);
        int mins = c.get(12);
        StringBuilder sb = new StringBuilder();
        sb.append(hours < 10 ? "0" : "");
        sb.append(hours);
        sb.append(":");
        sb.append(mins >= 10 ? "" : "0");
        sb.append(mins);
        this.mText = sb.toString();
        canvas.drawText(this.mText, this.mScaleFactorX * 23.0f, this.mScaleFactorY * 60.0f, this.mPaint);
    }

    protected void init() {
        this.ticks = Observable.interval(0L, 1L, TimeUnit.SECONDS);
        this.mPaint = new Paint();
        this.mPaint.setARGB(255, 255, 127, 0);
        try {
            this.mTypeface = Typeface.createFromAsset(getContext().getAssets(), "fonts/console.ttf");
            this.mPaint.setTypeface(this.mTypeface);
        } catch (RuntimeException e) {
            Log.e(TAG, "Cannot create typeface");
        }
    }
}
