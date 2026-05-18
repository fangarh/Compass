package net.afterday.compas.view;

import android.animation.ValueAnimator;

/* JADX INFO: renamed from: net.afterday.compas.view.-$$Lambda$LevelProgress$ZeUjc6Svxya9o6t-R0YGpzrOrbM, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$LevelProgress$ZeUjc6Svxya9o6tR0YGpzrOrbM implements ValueAnimator.AnimatorUpdateListener {
    private final /* synthetic */ LevelProgress f$0;

    public /* synthetic */ $$Lambda$LevelProgress$ZeUjc6Svxya9o6tR0YGpzrOrbM(LevelProgress levelProgress) {
        this.f$0 = levelProgress;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.f$0.lambda$setProgress$0$LevelProgress(valueAnimator);
    }
}
