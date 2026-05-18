package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;
import net.afterday.compas.R;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class Radbar extends View {
    private static final String TAG = "Radbar";
    private static final int WIDGET_HEIGHT = 224;
    private static final int WIDGET_WIDTH = 749;
    private Bitmap arrow;
    private View.OnTouchListener clicker;
    private PorterDuffColorFilter colorFilter;
    private boolean hasRadInstant;
    private boolean isPressed;
    private View.OnTouchListener listener;
    private Bitmap mBackImage;
    private long mControlled;
    private double mHealing;
    private double mHealth;
    private int mHeight;
    private Matrix mMatrix;
    private Paint mPaint;
    private Paint mPaintGrey;
    private Paint mPaintMore;
    private Paint mPaintSymbol;
    private double mRadiation;
    private RectF mRect;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private Bitmap mTopImage;
    private Typeface mTypefaceSeg;
    private int mWidth;

    public /* synthetic */ boolean lambda$new$0$Radbar(View v, MotionEvent t) {
        View.OnTouchListener onTouchListener = this.listener;
        if (onTouchListener != null) {
            onTouchListener.onTouch(v, t);
        }
        if (t.getAction() == 0) {
            this.isPressed = true;
            this.mPaint.setColorFilter(this.colorFilter);
            this.mPaintSymbol.setColorFilter(this.colorFilter);
            invalidate();
        } else if (t.getAction() == 1) {
            this.isPressed = false;
            this.mPaint.setColorFilter(null);
            this.mPaintSymbol.setColorFilter(null);
            invalidate();
        }
        return true;
    }

    public Radbar(Context context) {
        super(context);
        this.mHealth = 100.0d;
        this.mRadiation = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Radbar$nR3D9CFuVizAR0Q8DMePXfPLOA(this);
        init();
    }

    public Radbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mHealth = 100.0d;
        this.mRadiation = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Radbar$nR3D9CFuVizAR0Q8DMePXfPLOA(this);
        init();
    }

    public Radbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mHealth = 100.0d;
        this.mRadiation = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Radbar$nR3D9CFuVizAR0Q8DMePXfPLOA(this);
        init();
    }

    public Radbar setHealth(double health) {
        this.mHealth = health;
        invalidate();
        return this;
    }

    public Radbar setRadiation(double radiation) {
        this.mRadiation = radiation;
        invalidate();
        return this;
    }

    public Radbar setHealing(double healing) {
        this.mHealing = healing;
        invalidate();
        return this;
    }

    public Radbar setControlled(long controlled) {
        this.mControlled = controlled;
        invalidate();
        return this;
    }

    public Radbar setInfo(double health, double radiation, double healing, long controlled, boolean hasRadInstant) {
        if (this.mHealth == health && this.mRadiation == radiation && this.mHealing == healing && this.mControlled == controlled && this.hasRadInstant == hasRadInstant) {
            return this;
        }
        this.mHealth = health;
        this.mRadiation = radiation;
        this.mHealing = healing;
        this.mControlled = controlled;
        this.hasRadInstant = hasRadInstant;
        invalidate();
        return this;
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
        this.mScaleFactorX = this.mWidth / 749.0f;
        this.mScaleFactorY = this.mHeight / 224.0f;
        drawRect(185, 185, 44, 19, this.mRect);
        this.mPaintSymbol.setTextSize(this.mScaleFactorY * 50.0f);
        this.mPaintMore.setTextSize(this.mScaleFactorY * 80.0f);
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        Log.d(TAG, "RadBar - onDraw");
        super.onDraw(canvas);
        convertRect(this.mBackImage.getWidth(), this.mBackImage.getHeight(), 0, 0, this.mMatrix);
        canvas.drawBitmap(this.mBackImage, this.mMatrix, null);
        drawColor(canvas);
        drawText(canvas);
        canvas.drawBitmap(this.mTopImage, this.mMatrix, null);
        if (this.hasRadInstant) {
            convertRect(this.arrow.getWidth(), this.arrow.getHeight(), 4, 142, this.mMatrix);
            canvas.drawBitmap(this.arrow, this.mMatrix, null);
        }
    }

    protected void drawColor(Canvas canvas) {
        int[] color = getColor();
        this.mPaint.setARGB(255, color[1], color[2], color[3]);
        canvas.drawOval(this.mRect, this.mPaint);
    }

    protected void drawText(Canvas canvas) {
        this.mPaint.setARGB(255, 255, 127, 0);
        this.mPaint.setTextSize(this.mScaleFactorY * 90.0f);
        this.mPaintGrey.setTextSize(this.mScaleFactorY * 90.0f);
        double rad = this.mRadiation;
        if (rad > 15.0d) {
            rad = 15.0d;
            this.mPaintMore.setARGB(0, 255, 127, 0);
        } else {
            this.mPaintMore.setARGB(0, 35, 35, 35);
        }
        canvas.drawText(">", this.mScaleFactorX * 270.0f, this.mScaleFactorY * 135.0f, this.mPaintMore);
        String[] fullTxt = String.format(Locale.US, "%.3f", Double.valueOf(rad)).split("\\.");
        String txt = "!!" + fullTxt[0];
        String txt2 = txt.substring(txt.length() - 2);
        canvas.drawText("18", this.mScaleFactorX * 270.0f, this.mScaleFactorY * 155.0f, this.mPaintGrey);
        canvas.drawText(txt2, this.mScaleFactorX * 270.0f, this.mScaleFactorY * 155.0f, this.mPaint);
        float txtWidth = this.mPaint.measureText("18");
        this.mPaint.setTextSize(this.mScaleFactorY * 65.0f);
        this.mPaintGrey.setTextSize(this.mScaleFactorY * 65.0f);
        String txt3 = "." + fullTxt[1];
        canvas.drawText("888", (this.mScaleFactorX * 270.0f) + txtWidth, this.mScaleFactorY * 155.0f, this.mPaintGrey);
        canvas.drawText(txt3, (this.mScaleFactorX * 270.0f) + txtWidth, this.mScaleFactorY * 155.0f, this.mPaint);
        float txtWidth2 = txtWidth + this.mPaint.measureText("888");
        this.mPaint.setTextSize(this.mScaleFactorY * 40.0f);
        this.mPaintGrey.setTextSize(this.mScaleFactorY * 40.0f);
        canvas.drawText("Sv", (this.mScaleFactorX * 270.0f) + txtWidth2, this.mScaleFactorY * 155.0f, this.mPaintSymbol);
    }

    protected void drawUpgrade(Canvas canvas) {
    }

    protected void init() {
        super.setOnTouchListener(this.clicker);
        this.mBackImage = BitmapFactory.decodeResource(getResources(), R.drawable.seg_back);
        this.mTopImage = BitmapFactory.decodeResource(getResources(), R.drawable.seg_rad);
        this.arrow = BitmapFactory.decodeResource(getResources(), R.drawable.seg_upgrade);
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mPaint = new Paint();
        this.mPaintGrey = new Paint();
        this.mPaintGrey.setARGB(0, 35, 35, 35);
        this.mPaintSymbol = new Paint();
        this.mPaintSymbol.setARGB(255, 255, 127, 0);
        this.mPaintMore = new Paint();
        try {
            this.mTypefaceSeg = Typeface.createFromAsset(getContext().getAssets(), "fonts/segment.ttf");
            this.mPaint.setTypeface(this.mTypefaceSeg);
            this.mPaintGrey.setTypeface(this.mTypefaceSeg);
        } catch (RuntimeException e) {
        }
    }

    private void convertRect(int bitmapWidth, int bitmapHeight, int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }

    private void drawRect(int width, int height, int left, int top, RectF rect) {
        float f = this.mScaleFactorX;
        float f2 = this.mScaleFactorY;
        rect.set(left * f, top * f2, (left + width) * f, (top + height) * f2);
    }

    private int[] getColor() {
        if (this.mHealth <= 0.0d || this.mControlled > 0) {
            return Convert.numberToRGB(Convert.RGB_GREY);
        }
        if (this.mHealing > 0.0d) {
            return Convert.numberToRGB(Convert.RGB_BLUE);
        }
        double d = this.mRadiation;
        if (d >= 15.0d) {
            return new int[]{255, 255, 0, 0};
        }
        if (d < 15.0d && d >= 7.0d) {
            return Convert.numberToRGB(Convert.map(d, 7.0d, 15.0d, 15.0d, 0.0d));
        }
        double d2 = this.mRadiation;
        if (d2 < 7.0d && d2 >= 1.0d) {
            return Convert.numberToRGB(Convert.map(d2, 1.0d, 7.0d, 47.0d, 30.0d));
        }
        double d3 = this.mRadiation;
        if (d3 < 1.0d && d3 >= 0.0d) {
            return Convert.numberToRGB(Convert.map(d3, 0.0d, 1.0d, 100.0d, 60.0d));
        }
        return Convert.numberToRGB(Convert.map(this.mRadiation, 0.0d, 15.0d, 100.0d, 0.0d));
    }

    @Override // android.view.View
    public void setOnTouchListener(View.OnTouchListener l) {
        this.listener = l;
    }
}
