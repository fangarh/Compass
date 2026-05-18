package net.afterday.compas.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.animation.LinearInterpolator;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import net.afterday.compas.core.inventory.items.Events.ItemAdded;
import net.afterday.compas.util.Fonts;

/* JADX INFO: loaded from: classes.dex */
public class LevelProgress extends View {
    private static final String TAG = "LevelProgress";
    private static final int WIDGET_HEIGHT = 100;
    private static final int WIDGET_WIDTH = 350;
    private Paint fPaint;
    private List<OnLevelChangedListener> levelChangedListeners;
    private int mHeight;
    private float mScaleFactorX;
    private float mScaleFactorY;
    private int mWidth;
    private Matrix matrix;
    private Paint paint;
    private int percents;
    private RectF rect;
    private boolean showMax;
    private boolean showXp;
    private ValueAnimator vAnimator;
    private int xpAdded;

    public interface OnLevelChangedListener {
        void levelChanged(int i);
    }

    static /* synthetic */ List<OnLevelChangedListener> access$000(LevelProgress x0) {
        return x0.levelChangedListeners;
    }

    public LevelProgress(Context context) {
        super(context);
        this.percents = 0;
        this.xpAdded = 0;
        this.showXp = false;
        this.showMax = false;
        this.levelChangedListeners = new ArrayList();
        init();
    }

    public LevelProgress(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.percents = 0;
        this.xpAdded = 0;
        this.showXp = false;
        this.showMax = false;
        this.levelChangedListeners = new ArrayList();
        init();
    }

    public LevelProgress(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.percents = 0;
        this.xpAdded = 0;
        this.showXp = false;
        this.showMax = false;
        this.levelChangedListeners = new ArrayList();
        init();
    }

    public void setProgress(int progress) {
        this.percents = progress;
        invalidate();
    }

    public void showMax(boolean show) {
        this.showMax = show;
        invalidate();
    }

    public void setProgress(ItemAdded itemAdded) {
        this.xpAdded = itemAdded.getItem().getItemDescriptor().getXpPoints();
        if (this.xpAdded == 0) {
            return;
        }
        this.showXp = true;
        invalidate();
        Observable.timer(1L, TimeUnit.SECONDS).take(1L).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$LevelProgress$ifDHHP7zs9dR7O72KWe8l84Aa68(this, itemAdded));
        if (itemAdded.getLevel() == 5) {
            Observable.timer(2L, TimeUnit.SECONDS).take(1L).observeOn(AndroidSchedulers.mainThread()).subscribe(new $$Lambda$LevelProgress$vB8mkOCvjN8NCdGxQ14V9zQ_nt4(this));
        }
    }

    public /* synthetic */ void lambda$setProgress$2$LevelProgress(ItemAdded itemAdded, Long t) {
        this.showXp = false;
        int progress = itemAdded.getLevelXpPercents();
        boolean levelChanged = itemAdded.levelChanged();
        if (progress == this.percents && !levelChanged) {
            postInvalidate();
            return;
        }
        if (progress <= this.percents) {
            this.vAnimator.cancel();
            this.vAnimator = ValueAnimator.ofInt(this.percents, progress + 100);
            this.vAnimator.setDuration(700L);
            this.vAnimator.setInterpolator(new LinearInterpolator());
            this.vAnimator.addUpdateListener(new $$Lambda$LevelProgress$ZeUjc6Svxya9o6tR0YGpzrOrbM(this));
        } else {
            this.vAnimator.cancel();
            this.vAnimator = ValueAnimator.ofInt(this.percents, progress);
            this.vAnimator.setDuration(700L);
            this.vAnimator.setInterpolator(new LinearInterpolator());
            this.vAnimator.addUpdateListener(new $$Lambda$LevelProgress$bofr2CIrNZuGZ21xwDiubXLgl0(this));
        }
        this.vAnimator.start();
        if (itemAdded.levelChanged()) {
            this.vAnimator.addListener(new AnonymousClass1(itemAdded));
        }
    }

    public /* synthetic */ void lambda$setProgress$0$LevelProgress(ValueAnimator v) {
        int val = ((Integer) v.getAnimatedValue()).intValue();
        Log.d(TAG, "Animator 1: " + val);
        this.percents = val <= 100 ? val : val - 100;
        postInvalidate();
    }

    public /* synthetic */ void lambda$setProgress$1$LevelProgress(ValueAnimator v) {
        this.percents = ((Integer) v.getAnimatedValue()).intValue();
        Log.d(TAG, "Animator 1: " + this.percents);
        postInvalidate();
    }

    /* JADX INFO: renamed from: net.afterday.compas.view.LevelProgress$1, reason: invalid class name */
    class AnonymousClass1 extends AnimatorListenerAdapter {
        final /* synthetic */ ItemAdded val$itemAdded;

        AnonymousClass1(ItemAdded itemAdded) {
            this.val$itemAdded = itemAdded;
        }

        @Override // android.animation.AnimatorListenerAdapter, android.animation.Animator.AnimatorListener
        public void onAnimationEnd(Animator animation) {
            for (OnLevelChangedListener l : LevelProgress.access$000(LevelProgress.this)) {
                l.levelChanged(this.val$itemAdded.getLevel());
            }
        }
    }

    public /* synthetic */ void lambda$setProgress$3$LevelProgress(Long t) {
        this.showXp = true;
        this.showMax = true;
        postInvalidate();
    }

    public void addOnLevelChangedListener(OnLevelChangedListener listener) {
        this.levelChangedListeners.add(listener);
    }

    @Override // android.view.View
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = View.MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = View.MeasureSpec.getSize(heightMeasureSpec);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        this.mWidth = widthSize;
        this.mHeight = heightSize;
        this.mScaleFactorX = this.mWidth / 350.0f;
        this.mScaleFactorY = this.mHeight / 100.0f;
        this.matrix.reset();
        this.matrix.postScale(this.mScaleFactorX, this.mScaleFactorY);
        this.matrix.postTranslate(0.0f, 0.0f);
        Paint paint = this.fPaint;
        Double.isNaN(this.mHeight);
        paint.setTextSize((int) (this.mHeight * 1.0d));
    }

    @Override // android.view.View
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.e(TAG, "HEIGHT: " + this.mHeight);
        if (this.showMax) {
            canvas.drawText("MAX", this.mScaleFactorX * 70.0f, (this.mHeight + 15) * this.mScaleFactorY, this.fPaint);
            return;
        }
        if (this.showXp && this.xpAdded > 0) {
            canvas.drawText("+" + Integer.toString(this.xpAdded), this.mScaleFactorX * 70.0f, (this.mHeight + 15) * this.mScaleFactorY, this.fPaint);
            return;
        }
        if (this.percents == 100) {
            this.percents = 0;
        }
        drawRect((this.mWidth * this.percents) / 100, this.rect);
        canvas.drawRoundRect(this.rect, 7.0f, 7.0f, this.paint);
    }

    protected void init() {
        this.matrix = new Matrix();
        this.rect = new RectF();
        this.paint = new Paint();
        this.paint.setARGB(255, 255, 127, 0);
        this.paint.setAlpha(180);
        this.vAnimator = ValueAnimator.ofInt(0, 0);
        this.fPaint = Fonts.instance().getDefaultFontPaint();
    }

    private void drawRect(int width, RectF rect) {
        rect.set(0.0f, 0.0f, width, this.mHeight);
    }
}
