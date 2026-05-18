package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class CountDownTimer extends View {
    private static final String TAG = "CountDownTimer";
    private static final int WIDGET_HEIGHT = 120;
    private static final int WIDGET_WIDTH = 200;
    private int mHeight;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private int mWidth;
    private Paint paint;
    private long secondsLeft;

    public CountDownTimer(Context context) {
        super(context);
        this.secondsLeft = 0L;
        init();
    }

    public CountDownTimer(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.secondsLeft = 0L;
        init();
    }

    public CountDownTimer(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.secondsLeft = 0L;
        init();
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
        this.mScaleFactorX = this.mWidth / 200.0f;
        this.mScaleFactorY = this.mHeight / 120.0f;
        this.paint.setTextSize(this.mScaleFactorY * 120.0f);
    }

    @Override // android.view.View
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        long j = this.secondsLeft;
        if (j < 0) {
            return;
        }
        canvas.drawText(secondsToString(j), this.mScaleFactorX * 23.0f, this.mScaleFactorY * 60.0f, this.paint);
    }

    public void setSecondsLeft(Long timeLeft) {
        this.secondsLeft = timeLeft.longValue();
        invalidate();
    }

    private void init() {
        this.paint = Fonts.instance().getDefaultFontPaint();
    }

    private String secondsToString(long secondsLeft) {
        String mins = Long.toString(secondsLeft / 60);
        if (mins.length() < 2) {
            mins = "0" + mins;
        }
        String secs = Long.toString(secondsLeft % 60);
        if (secs.length() < 2) {
            secs = "0" + secs;
        }
        return mins + ":" + secs;
    }
}
