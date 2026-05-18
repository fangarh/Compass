package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageButton;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import net.afterday.compas.R;

/* JADX INFO: loaded from: classes.dex */
public class LevelIndicator extends AppCompatImageButton {
    private static final String TAG = "QrBtnLevelIndicator";
    private int backgroundHeight;
    private int backgroundWidth;
    private Paint imgPaint;
    private int level;
    private int mHeight;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private int mWidth;
    private Matrix matrix;
    private Paint paint;
    private Bitmap qrImage;

    public LevelIndicator(Context context) {
        super(context);
        this.level = 1;
        init();
    }

    public LevelIndicator(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.level = 1;
        init();
    }

    public LevelIndicator(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.level = 1;
        init();
    }

    public void setLevel(int level) {
        if (level == this.level) {
            return;
        }
        this.level = level;
        invalidate();
    }

    @Override // android.widget.ImageView, android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        this.mWidth = widthSize;
        this.mHeight = heightSize;
        this.mScaleFactorX = this.mWidth / this.backgroundWidth;
        this.mScaleFactorY = this.mHeight / this.backgroundHeight;
        this.paint.setTextSize(this.mScaleFactorY * 100.0f);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override // android.widget.ImageView, android.view.View
    public void onDraw(Canvas canvas) {
        setAlpha(0);
        super.onDraw(canvas);
        Log.d("LevelIndicator", "draw   ---- " + this.level);
        convertRect(-1, -2, this.matrix);
        canvas.drawBitmap(this.qrImage, this.matrix, this.imgPaint);
        int i = this.level;
        if (i > 0) {
            canvas.drawText(Integer.toString(i), this.mScaleFactorX * (this.level > 1 ? 35 : 6), this.mScaleFactorY * 125.0f, this.paint);
        }
    }

    private void init() {
        this.imgPaint = new Paint();
        this.paint = new Paint();
        this.paint.setARGB(255, 255, 127, 0);
        this.paint.setAlpha(180);
        this.matrix = new Matrix();
        this.qrImage = BitmapFactory.decodeResource(getResources(), R.drawable.qr_button);
        this.backgroundWidth = this.qrImage.getWidth();
        this.backgroundHeight = this.qrImage.getHeight();
        try {
            this.paint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/segment.ttf"));
        } catch (RuntimeException e) {
        }
    }

    private void convertRect(int left, int top, Matrix matrix) {
        matrix.reset();
        matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        matrix.postTranslate(this.mScaleFactorX * left, this.mScaleFactorY * top);
    }
}
