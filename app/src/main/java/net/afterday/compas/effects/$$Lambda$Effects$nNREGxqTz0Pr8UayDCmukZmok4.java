package net.afterday.compas.effects;

import android.animation.ValueAnimator;

/* JADX INFO: renamed from: net.afterday.compas.effects.-$$Lambda$Effects$-nNREGxqTz0Pr8UayDCmukZmok4, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$Effects$nNREGxqTz0Pr8UayDCmukZmok4 implements ValueAnimator.AnimatorUpdateListener {
    private final /* synthetic */ Effects f$0;
    private final /* synthetic */ double f$1;

    public /* synthetic */ $$Lambda$Effects$nNREGxqTz0Pr8UayDCmukZmok4(Effects effects, double d) {
        this.f$0 = effects;
        this.f$1 = d;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        this.f$0.lambda$playRad$12$Effects(this.f$1, valueAnimator);
    }
}
