package net.afterday.compas.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.AnticipateOvershootInterpolator;
import net.afterday.compas.R;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class Geiger extends View {
    private static final String TAG = "Geiger";
    private static final int WIDGET_HEIGHT = 1010;
    private static final int WIDGET_WIDTH = 1010;
    private Bitmap brokenGlass;
    private Bitmap fingerPrint;
    private boolean hasFingerPrint;
    private boolean isBroken;
    private int level;
    private float mAnomaly;
    private Bitmap mBulbAnomaly;
    private Bitmap mBulbBack;
    private Bitmap mBulbMental;
    private Bitmap mFrontSide;
    private int mHeight;
    private Bitmap mIndicator;
    private Matrix mMatrix;
    private float mMental;
    private float mMonolith;
    private Paint mPaint;
    private Bitmap mPeakOff;
    private Bitmap mPeakOn;
    private RectF mRect;
    private Bitmap mScale;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private float mSvh;
    private ValueAnimator mSvhAnimator;
    private int mWidth;
    private Bitmap scaleLvl5;
    private float tSvh;

    static /* synthetic */ float access$002(Geiger x0, float x1) {
        x0.mSvh = x1;
        return x1;
    }

    static /* synthetic */ ValueAnimator access$100(Geiger x0) {
        return x0.mSvhAnimator;
    }

    public Geiger(Context context) {
        super(context);
        init();
    }

    public Geiger(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public Geiger(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setAnomaly(float anomaly) {
        if (anomaly == this.mAnomaly) {
            return;
        }
        this.mAnomaly = anomaly;
        invalidate();
    }

    public void setMental(float mental) {
        if (mental == this.mMental) {
            return;
        }
        this.mMental = mental;
        invalidate();
    }

    public void setMonolith(float monolith) {
        if (monolith == this.mMonolith) {
            return;
        }
        this.mMonolith = monolith;
        invalidate();
    }

    public void setSvh(float svh) {
        if (svh == this.mSvh) {
            return;
        }
        this.mSvh = svh;
        invalidate();
    }

    public float getSvh() {
        return this.mSvh;
    }

    public void toSvh(float svh, long duration) {
        if (svh == this.tSvh) {
            return;
        }
        this.tSvh = svh;
        this.mSvhAnimator.cancel();
        this.mSvhAnimator = ValueAnimator.ofFloat(this.mSvh, svh);
        this.mSvhAnimator.setInterpolator(new AnticipateOvershootInterpolator());
        this.mSvhAnimator.setDuration(duration);
        this.mSvhAnimator.addUpdateListener(new AnonymousClass1());
        this.mSvhAnimator.start();
    }

    /* JADX INFO: renamed from: net.afterday.compas.view.Geiger$1, reason: invalid class name */
    class AnonymousClass1 implements ValueAnimator.AnimatorUpdateListener {
        AnonymousClass1() {
        }

        @Override // android.animation.ValueAnimator.AnimatorUpdateListener
        public void onAnimationUpdate(ValueAnimator animation) {
            Geiger geiger = Geiger.this;
            Geiger.access$002(geiger, ((Float) Geiger.access$100(geiger).getAnimatedValue()).floatValue());
            Geiger.this.postInvalidate();
        }
    }

    public void setBrokenGlass(boolean broken) {
        if (this.isBroken == broken) {
            return;
        }
        this.isBroken = broken;
        invalidate();
    }

    public void setFingerPrint(boolean fPrint) {
        if (this.hasFingerPrint == fPrint) {
            return;
        }
        this.hasFingerPrint = fPrint;
        invalidate();
    }

    public void setLevel(int level) {
        this.level = level;
        invalidate();
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
        this.mScaleFactorX = this.mWidth / 1010.0f;
        this.mScaleFactorY = this.mHeight / 1010.0f;
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "Geiger - onDraw");
        super.onDraw(canvas);
        drawBack(canvas);
        drawPeak(canvas);
        drawIndicator(canvas);
        drawFront(canvas);
    }

    protected void init() {
        this.mScale = BitmapFactory.decodeResource(getResources(), R.drawable.scale);
        this.mIndicator = BitmapFactory.decodeResource(getResources(), R.drawable.indicator);
        this.mFrontSide = BitmapFactory.decodeResource(getResources(), R.drawable.geiger_top);
        this.brokenGlass = BitmapFactory.decodeResource(getResources(), R.drawable.broken_glass);
        this.fingerPrint = BitmapFactory.decodeResource(getResources(), R.drawable.fingerprint);
        this.mPeakOff = BitmapFactory.decodeResource(getResources(), R.drawable.peak_off);
        this.mPeakOn = BitmapFactory.decodeResource(getResources(), R.drawable.peak_on);
        this.mBulbBack = BitmapFactory.decodeResource(getResources(), R.drawable.bulb_back);
        this.mBulbAnomaly = BitmapFactory.decodeResource(getResources(), R.drawable.bulb_anomaly);
        this.mBulbMental = BitmapFactory.decodeResource(getResources(), R.drawable.bulb_mental);
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mPaint = new Paint();
        this.mSvhAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
    }

    protected void drawBack(Canvas canvas) {
        convertRect(this.mScale.getWidth(), this.mScale.getHeight(), 31, 31, this.mMatrix);
        canvas.drawBitmap(this.mScale, this.mMatrix, null);
        if (this.level >= 2) {
            drawAnomaly(canvas);
        }
        if (this.level >= 3) {
            drawMental(canvas);
        }
        if (this.hasFingerPrint) {
            convertRect(this.fingerPrint.getWidth(), this.fingerPrint.getHeight(), -35, 210, this.mMatrix);
            canvas.drawBitmap(this.fingerPrint, this.mMatrix, null);
        }
        if (this.isBroken) {
            convertRect(this.brokenGlass.getWidth(), this.brokenGlass.getHeight(), 170, 100, this.mMatrix);
            canvas.drawBitmap(this.brokenGlass, this.mMatrix, null);
        }
    }

    protected void drawIndicator(Canvas canvas) {
        convertRect(this.mIndicator.getWidth(), this.mIndicator.getHeight(), 487, 350, this.mMatrix);
        this.mMatrix.postRotate(svhToRotation(this.mSvh), this.mScaleFactorX * 505.0f, this.mScaleFactorY * 1010.0f);
        canvas.drawBitmap(this.mIndicator, this.mMatrix, null);
    }

    protected void drawFront(Canvas canvas) {
        convertRect(this.mFrontSide.getWidth(), this.mFrontSide.getHeight(), 0, 0, this.mMatrix);
        canvas.drawBitmap(this.mFrontSide, this.mMatrix, null);
    }

    protected void drawAnomaly(Canvas canvas) {
        convertRect(this.mBulbBack.getWidth(), this.mBulbBack.getHeight(), 281, 115, this.mMatrix);
        canvas.drawBitmap(this.mBulbBack, this.mMatrix, null);
        drawRect(151, 151, 286, 120, this.mRect);
        int[] color = Convert.numberToRGB(this.mAnomaly <= 0.0f ? Convert.RGB_GREY : Convert.map(this.mAnomaly, 0.0f, 15.0f, 100.0f, 0.0f));
        if (this.mAnomaly >= 15.0f) {
            color[1] = 255;
            color[2] = 0;
            color[3] = 0;
        }
        this.mPaint.setARGB(255, 255, 255, 255);
        this.mPaint.setARGB(255, color[1], color[2], color[3]);
        canvas.drawOval(this.mRect, this.mPaint);
        convertRect(161, 161, 281, 115, this.mMatrix);
        canvas.drawBitmap(this.mBulbAnomaly, this.mMatrix, null);
    }

    protected void drawMental(Canvas canvas) {
        convertRect(this.mBulbBack.getWidth(), this.mBulbBack.getHeight(), 572, 115, this.mMatrix);
        canvas.drawBitmap(this.mBulbBack, this.mMatrix, null);
        drawRect(151, 151, 577, 120, this.mRect);
        int[] color = Convert.numberToRGB(this.mMental <= 0.0f ? Convert.RGB_GREY : Convert.map(this.mMental, 0.0f, 15.0f, 100.0f, 0.0f));
        if (this.mMental >= 15.0f) {
            color[1] = 255;
            color[2] = 0;
            color[3] = 0;
        }
        this.mPaint.setARGB(255, color[1], color[2], color[3]);
        canvas.drawOval(this.mRect, this.mPaint);
        convertRect(161, 161, 572, 115, this.mMatrix);
        canvas.drawBitmap(this.mBulbMental, this.mMatrix, null);
    }

    protected void drawPeak(Canvas canvas) {
        convertRect(this.mPeakOff.getWidth(), this.mPeakOff.getHeight(), 369, 60, this.mMatrix);
        if (this.mSvh >= 15.0f) {
            canvas.drawBitmap(this.mPeakOn, this.mMatrix, null);
        } else {
            canvas.drawBitmap(this.mPeakOff, this.mMatrix, null);
        }
    }

    private void convertRect(int bitmapWidth, int bitmapHeight, int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }

    private float svhToRotation(float svh) {
        if (svh >= 0.0f && svh < 1.0f) {
            return Convert.map(svh, 0.0f, 1.0f, -37.0f, -20.0f);
        }
        if (svh >= 1.0f && svh < 7.0f) {
            return Convert.map(svh, 1.0f, 7.0f, -20.0f, 10.0f);
        }
        if (svh >= 7.0f && svh < 9.0f) {
            return Convert.map(svh, 7.0f, 9.0f, 10.0f, 21.0f);
        }
        if (svh < 9.0f || svh >= 15.0f) {
            return svh >= 15.0f ? 37.0f : -37.0f;
        }
        return Convert.map(svh, 9.0f, 15.0f, 21.0f, 37.0f);
    }

    private void drawRect(int width, int height, int left, int top, RectF rect) {
        float f = this.mScaleFactorX;
        float f2 = this.mScaleFactorY;
        rect.set(left * f, top * f2, (left + width) * f, (top + height) * f2);
    }
}
