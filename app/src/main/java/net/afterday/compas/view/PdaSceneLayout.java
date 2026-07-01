package net.afterday.compas.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.support.percent.PercentRelativeLayout;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

public class PdaSceneLayout extends PercentRelativeLayout {
    private Drawable sceneBackground;
    private PdaSceneMetrics metrics = PdaSceneMetrics.landscape(0, 0);

    public PdaSceneLayout(Context context) {
        super(context);
        init();
    }

    public PdaSceneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PdaSceneLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        setWillNotDraw(false);
    }

    public void setSceneBackgroundResource(int resId) {
        if (resId <= 0) {
            setSceneBackgroundDrawable(null);
            return;
        }
        setSceneBackgroundDrawable(ContextCompat.getDrawable(getContext(), resId));
    }

    public void setSceneBackgroundDrawable(Drawable drawable) {
        this.sceneBackground = drawable;
        invalidate();
    }

    public PdaSceneMetrics getSceneMetrics() {
        return this.metrics;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int screenWidth = MeasureSpec.getSize(widthMeasureSpec);
        int screenHeight = MeasureSpec.getSize(heightMeasureSpec);
        this.metrics = PdaSceneMetrics.forScreen(screenWidth, screenHeight);
        if (this.metrics.sceneWidth <= 0 || this.metrics.sceneHeight <= 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int sceneWidthSpec = MeasureSpec.makeMeasureSpec(this.metrics.sceneWidth, MeasureSpec.EXACTLY);
        int sceneHeightSpec = MeasureSpec.makeMeasureSpec(this.metrics.sceneHeight, MeasureSpec.EXACTLY);
        super.onMeasure(sceneWidthSpec, sceneHeightSpec);
        setMeasuredDimension(screenWidth, screenHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (this.metrics.sceneWidth <= 0 || this.metrics.sceneHeight <= 0) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }

        super.onLayout(changed, 0, 0, this.metrics.sceneWidth, this.metrics.sceneHeight);
        int childCount = getChildCount();
        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() != GONE) {
                child.offsetLeftAndRight(this.metrics.sceneLeft);
                child.offsetTopAndBottom(this.metrics.sceneTop);
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.sceneBackground != null && this.metrics.sceneWidth > 0 && this.metrics.sceneHeight > 0) {
            this.sceneBackground.setBounds(
                    this.metrics.sceneLeft,
                    this.metrics.sceneTop,
                    this.metrics.sceneLeft + this.metrics.sceneWidth,
                    this.metrics.sceneTop + this.metrics.sceneHeight);
            this.sceneBackground.draw(canvas);
        }
        super.onDraw(canvas);
    }
}
