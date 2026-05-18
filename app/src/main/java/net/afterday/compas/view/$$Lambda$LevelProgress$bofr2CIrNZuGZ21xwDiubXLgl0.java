package net.afterday.compas.view;

import android.animation.ValueAnimator;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$LevelProgress$bofr2CIrNZuGZ21xwDi-ubXLgl0, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LevelProgress$bofr2CIrNZuGZ21xwDiubXLgl0 implements ValueAnimator.AnimatorUpdateListener {
    private final /* synthetic */ LevelProgress f$0;

    public /* synthetic */ $$Lambda$LevelProgress$bofr2CIrNZuGZ21xwDiubXLgl0(LevelProgress levelProgress) {
        this.f$0 = levelProgress;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.f$0.lambda$setProgress$1$LevelProgress(valueAnimator);
    }
}
