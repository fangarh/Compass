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
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;
import net.afterday.compas.R;
import net.afterday.compas.util.Convert;

/* JADX INFO: loaded from: classes.dex */
public class Healthbar extends View {
    private static final String TAG = "Healthbar";
    private static final int WIDGET_HEIGHT = 224;
    private static final int WIDGET_WIDTH = 749;
    private Bitmap arrow;
    private View.OnTouchListener clicker;
    private PorterDuffColorFilter colorFilter;
    private boolean hasHealthInstant;
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
    private RectF mRect;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private Bitmap mTopImage;
    private Typeface mTypefaceSeg;
    private int mWidth;

    public /* synthetic */ boolean lambda$new$0$Healthbar(View v, MotionEvent t) {
        View.OnTouchListener onTouchListener = this.listener;
        if (onTouchListener != null) {
            onTouchListener.onTouch(v, t);
        }
        if (t.getAction() == 0) {
            this.isPressed = true;
            this.mPaint.setColorFilter(this.colorFilter);
            this.mPaintGrey.setColorFilter(this.colorFilter);
            invalidate();
        } else if (t.getAction() == 1) {
            this.isPressed = false;
            this.mPaint.setColorFilter(null);
            this.mPaintGrey.setColorFilter(null);
            invalidate();
        }
        return true;
    }

    public Healthbar(Context context) {
        super(context);
        this.mHealth = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Healthbar$InupTIAtEvPSpcFWwckIoSlbLus(this);
        init();
    }

    public Healthbar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.mHealth = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Healthbar$InupTIAtEvPSpcFWwckIoSlbLus(this);
        init();
    }

    public Healthbar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.mHealth = 0.0d;
        this.mControlled = 0L;
        this.mHealing = 0.0d;
        this.colorFilter = new PorterDuffColorFilter(1996488704, PorterDuff.Mode.SRC_ATOP);
        this.clicker = new $$Lambda$Healthbar$InupTIAtEvPSpcFWwckIoSlbLus(this);
        init();
    }

    public Healthbar setHealth(double health) {
        this.mHealth = health;
        invalidate();
        return this;
    }

    public Healthbar setHealing(double healing) {
        this.mHealing = healing;
        invalidate();
        return this;
    }

    public Healthbar setControlled(long controlled) {
        this.mControlled = controlled;
        invalidate();
        return this;
    }

    public Healthbar setInfo(double health, double healing, long controlled, boolean hasHealthInstant) {
        if (this.mHealth == health && this.mHealing == healing && this.mControlled == controlled && this.hasHealthInstant == hasHealthInstant) {
            return this;
        }
        this.mHealth = health;
        this.mHealing = healing;
        this.mControlled = controlled;
        this.hasHealthInstant = hasHealthInstant;
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
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        convertRect(this.mBackImage.getWidth(), this.mBackImage.getHeight(), 0, 0, this.mMatrix);
        canvas.drawBitmap(this.mBackImage, this.mMatrix, null);
        drawColor(canvas);
        drawText(canvas);
        canvas.drawBitmap(this.mTopImage, this.mMatrix, null);
        if (this.hasHealthInstant) {
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
        this.mPaint.setTextSize(this.mScaleFactorY * 100.0f);
        this.mPaintGrey.setTextSize(this.mScaleFactorY * 100.0f);
        String[] fullTxt = String.format(Locale.US, "%.2f", Double.valueOf(this.mHealth)).split("\\.");
        String txt = "!!!" + fullTxt[0];
        String txt2 = txt.substring(txt.length() - 3);
        canvas.drawText("188", this.mScaleFactorX * 220.0f, this.mScaleFactorY * 160.0f, this.mPaintGrey);
        canvas.drawText(txt2, this.mScaleFactorX * 220.0f, this.mScaleFactorY * 160.0f, this.mPaint);
        float txtWidth = this.mPaint.measureText("188");
        this.mPaint.setTextSize(this.mScaleFactorY * 75.0f);
        this.mPaintGrey.setTextSize(this.mScaleFactorY * 75.0f);
        String txt3 = "." + fullTxt[1];
        canvas.drawText("88", (this.mScaleFactorX * 220.0f) + txtWidth, this.mScaleFactorY * 160.0f, this.mPaintGrey);
        canvas.drawText(txt3, (this.mScaleFactorX * 220.0f) + txtWidth, this.mScaleFactorY * 160.0f, this.mPaint);
        float txtWidth2 = txtWidth + this.mPaint.measureText("88");
        this.mPaint.setTextSize(this.mScaleFactorY * 50.0f);
        canvas.drawText("%", (this.mScaleFactorX * 220.0f) + txtWidth2, this.mScaleFactorY * 160.0f, this.mPaint);
    }

    protected void drawUpgrade(Canvas canvas) {
    }

    protected void init() {
        super.setOnTouchListener(this.clicker);
        this.mBackImage = BitmapFactory.decodeResource(getResources(), R.drawable.seg_back);
        this.mTopImage = BitmapFactory.decodeResource(getResources(), R.drawable.seg_health);
        this.arrow = BitmapFactory.decodeResource(getResources(), R.drawable.seg_upgrade);
        this.mMatrix = new Matrix();
        this.mRect = new RectF();
        this.mPaint = new Paint();
        this.mPaintGrey = new Paint();
        this.mPaintGrey.setARGB(0, 35, 35, 35);
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
        double d = this.mHealth;
        if (d <= 0.0d || this.mControlled > 0) {
            return Convert.numberToRGB(Convert.RGB_GREY);
        }
        if (this.mHealing > 0.0d) {
            return Convert.numberToRGB(Convert.RGB_BLUE);
        }
        if (d > 100.0d) {
            return new int[]{255, 0, 255, 0};
        }
        return Convert.numberToRGB(d);
    }

    @Override // android.view.View
    public void setOnTouchListener(View.OnTouchListener l) {
        this.listener = l;
    }
}
