package net.afterday.compas.view;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import net.afterday.compas.R;

/* JADX INFO: loaded from: classes.dex */
public class Bar extends AbstractView {
    private static final int HORIZONTAL = 1;
    private static final String TAG = "Bar";
    private static final int VERTICAL = 0;
    private int bottom;
    private int imgResId;
    private Bitmap mTopImage;
    private int measurement;
    private int offsetBottom;
    private int offsetLeft;
    private Paint paint;
    private float percentage;
    private RectF rectf;
    private int scaleColor;
    private int scaleHeight;
    private int scaleWidth;

    public Bar(Context context) {
        this(context, null);
    }

    public Bar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public Bar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.percentage = 0.0f;
        this.imgResId = -1;
        this.offsetLeft = 0;
        this.offsetBottom = 0;
        this.scaleHeight = 0;
        this.scaleWidth = 0;
        this.measurement = 0;
        this.scaleColor = ViewCompat.MEASURED_SIZE_MASK;
        getAttrs(context, attrs, defStyleAttr);
    }

    private void getAttrs(Context context, AttributeSet attrs, int defStyleAttr) {
        if (attrs == null) {
            return;
        }
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.Bar, 0, 0);
        this.imgResId = a.getResourceId(0, 0);
        this.offsetLeft = a.getInt(3, 0);
        this.offsetBottom = a.getInt(2, 0);
        this.scaleHeight = a.getInt(5, 0);
        this.scaleWidth = a.getInt(6, 0);
        this.measurement = a.getInt(1, 0);
        this.scaleColor = a.getInt(4, ViewCompat.MEASURED_SIZE_MASK);
        a.recycle();
    }

    @Override // net.afterday.compas.view.AbstractView
    protected Bitmap getBackgroundDrawable() {
        if (this.mTopImage == null) {
            this.mTopImage = BitmapFactory.decodeResource(getResources(), this.imgResId);
        }
        return this.mTopImage;
    }

    public void setPercents(double percents) {
        if (this.percentage == percents) {
            return;
        }
        if (percents > 100.0d) {
            percents = 100.0d;
        } else if (percents < 0.0d) {
            percents = 0.0d;
        }
        this.percentage = (float) percents;
        invalidate();
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(this.mTopImage, this.matrix, null);
        float f = this.percentage;
        if (f > 0.0f) {
            if (this.measurement == 0) {
                drawRect(this.scaleWidth, (int) ((this.scaleHeight / 100) * f), this.rectf);
            } else {
                drawRect((int) ((this.scaleWidth / 100) * f), this.scaleHeight, this.rectf);
            }
        }
        canvas.drawRect(this.rectf, this.paint);
    }

    @Override // net.afterday.compas.view.AbstractView
    protected void init() {
        this.rectf = new RectF();
        this.paint = new Paint();
        this.paint.setColor(this.scaleColor);
        this.bottom = this.backgroundHeight - this.offsetBottom;
    }

    private void drawRect(int width, int height, RectF rect) {
        Log.e(TAG, "W: " + this.width + " H: " + this.height + " BW: " + getBackgroundDrawable().getWidth() + " BH: " + getBackgroundDrawable().getHeight() + " SX: " + this.scaleX + " SY: " + this.scaleY + " SH: " + this.scaleHeight);
        if (this.measurement == 0) {
            rect.set(this.offsetLeft * this.scaleX, ((this.offsetBottom + this.scaleHeight) - height) * this.scaleY, (this.offsetLeft + this.scaleWidth) * this.scaleX, this.scaleHeight * this.scaleY);
        } else {
            rect.set(this.offsetLeft * this.scaleX, (this.bottom - height) * this.scaleY, (this.offsetLeft + width) * this.scaleX, this.bottom * this.scaleY);
        }
    }

    public static float convertPixelsToDp(float px) {
        DisplayMetrics metrics = Resources.getSystem().getDisplayMetrics();
        float dp = px / (metrics.densityDpi / 160.0f);
        return Math.round(dp);
    }
}
