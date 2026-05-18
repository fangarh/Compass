package net.afterday.compas.fragment;

import android.animation.ValueAnimator;
import android.view.View;

/* JADX INFO: renamed from: net.afterday.compas.fragment.-$$Lambda$BloodFragment$10vGyI8Fl01ekKIK4EBJ6Kb2mrE, reason: invalid class name */
/* JADX INFO: compiled from: lambda */
/* JADX INFO: loaded from: classes.dex */
public final /* synthetic */ class $$Lambda$BloodFragment$10vGyI8Fl01ekKIK4EBJ6Kb2mrE implements ValueAnimator.AnimatorUpdateListener {
    private final /* synthetic */ View f$0;

    public /* synthetic */ $$Lambda$BloodFragment$10vGyI8Fl01ekKIK4EBJ6Kb2mrE(View view) {
        this.f$0 = view;
    }

    @Override // android.animation.ValueAnimator.AnimatorUpdateListener
    public final void onAnimationUpdate(ValueAnimator valueAnimator) {
        BloodFragment.lambda$onCreateView$0(this.f$0, valueAnimator);
    }
}
