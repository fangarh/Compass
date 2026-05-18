package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

/* JADX INFO: loaded from: classes.dex */
public abstract class AbstractView extends View {
    protected int backgroundHeight;
    protected int backgroundWidth;
    protected int height;
    protected Matrix matrix;
    protected float scaleX;
    protected float scaleY;
    protected int width;

    protected abstract Bitmap getBackgroundDrawable();

    protected abstract void init();

    public AbstractView(Context context) {
        this(context, null);
    }

    public AbstractView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AbstractView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.backgroundWidth = 1;
        this.backgroundHeight = 1;
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        this.matrix = new Matrix();
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        this.width = widthSize;
        this.height = heightSize;
        Bitmap background = getBackgroundDrawable();
        if (background != null) {
            this.backgroundWidth = background.getWidth();
            this.backgroundHeight = background.getHeight();
            this.scaleX = this.width / this.backgroundWidth;
            this.scaleY = this.height / this.backgroundHeight;
            this.matrix.postScale(this.scaleX, this.scaleY);
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        init();
    }
}
