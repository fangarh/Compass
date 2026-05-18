package net.afterday.compas.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import net.afterday.compas.R;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class Indicator extends View {
    private static final String TAG = "Indicator";
    private static final int WIDGET_HEIGHT = 300;
    private static final int WIDGET_WIDTH = 600;
    private Bitmap indicatorBck;
    private Bitmap indicatorOn;
    private int level;
    private int mHeight;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private float mStrength;
    private int mWidth;
    private Matrix matrix;
    private int maxWidth;
    private ValueAnimator vAnimator;
    private int x;

    static /* synthetic */ int access$002(Indicator x0, int x1) {
        x0.x = x1;
        return x1;
    }

    public Indicator(Context context) {
        super(context);
        this.x = 1;
        init();
    }

    public Indicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.x = 1;
        init();
    }

    public Indicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.x = 1;
        init();
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public void setStrength(float strength) {
        if (this.level < 5 || strength == this.mStrength) {
            return;
        }
        this.mStrength = strength;
        this.vAnimator.cancel();
        int nw = (int) Convert.map(strength > 100.0f ? 100.0f : strength, 0.0f, 100.0f, this.maxWidth, 0.0f);
        this.vAnimator = ValueAnimator.ofInt(this.x, nw);
        this.vAnimator.setDuration(1000L);
        this.vAnimator.addUpdateListener(new AnonymousClass1());
        this.vAnimator.start();
    }

    /* JADX INFO: renamed from: net.afterday.compas.view.Indicator$1, reason: invalid class name */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass1() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            Indicator.access$002(Indicator.this, ((Integer) animation.getAnimatedValue()).intValue());
            Indicator.this.postInvalidate();
        }
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
        this.mScaleFactorX = this.mWidth / 600.0f;
        this.mScaleFactorY = this.mHeight / 300.0f;
        this.maxWidth = this.indicatorOn.getWidth();
        this.x = this.maxWidth;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "Indicator - onDraw");
        super.onDraw(canvas);
        Matrix m = new Matrix();
        convertRect(this.indicatorOn.getWidth(), this.indicatorOn.getHeight(), 0, 0, m);
        convertRect(this.indicatorBck.getWidth(), this.indicatorBck.getHeight(), 0, 0, this.matrix);
        int width = this.indicatorOn.getWidth() - this.x;
        canvas.drawBitmap(this.indicatorBck, this.matrix, null);
        if (this.maxWidth - this.x > 0) {
            canvas.translate((this.x / 2) * this.mScaleFactorX, 0.0f);
            Bitmap bitmap = this.indicatorOn;
            int i = this.x;
            canvas.drawBitmap(Bitmap.createBitmap(bitmap, i / 2, 0, this.maxWidth - i, bitmap.getHeight()), this.matrix, null);
        }
    }

    @Override // android.view.View
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
    }

    private void convertRect(int bitmapWidth, int bitmapHeight, int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }

    protected void init() {
        this.indicatorOn = BitmapFactory.decodeResource(getResources(), R.drawable.light_bars);
        this.indicatorBck = BitmapFactory.decodeResource(getResources(), R.drawable.light_bars_off);
        this.vAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        this.matrix = new Matrix();
    }
}
